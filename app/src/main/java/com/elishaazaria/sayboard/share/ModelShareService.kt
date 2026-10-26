package com.elishaazaria.sayboard.share

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elishaazaria.sayboard.Constants
import com.elishaazaria.sayboard.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

/**
 * Shares the installed models over the local network, so people who are offline
 * (or simply don't want to download gigabytes again) can copy models from a
 * phone that already has them.
 *
 * The service serves a tiny HTTP API (see [ShareProtocol]) and answers UDP
 * discovery requests, so other devices can find it without typing an IP.
 */
class ModelShareService : Service() {

    private var serverSocket: ServerSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private val connectionSlots = Semaphore(MAX_CONNECTIONS)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!isRunning) {
            startServer()
        }
        updateNotification()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ server

    private fun startServer() {
        try {
            lastError = null
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(0))
            serverSocket = socket
            httpPort = socket.localPort
            isRunning = true
            Log.i(TAG, "Sharing models on port $httpPort")

            thread(name = "sayboard-share-accept", isDaemon = true) {
                while (!socket.isClosed) {
                    val client = try {
                        socket.accept()
                    } catch (e: IOException) {
                        break
                    }
                    if (!connectionSlots.tryAcquire()) {
                        try {
                            client.close()
                        } catch (ignored: IOException) {
                        }
                        continue
                    }
                    thread(isDaemon = true) {
                        try {
                            handleConnection(client)
                        } catch (e: Exception) {
                            Log.w(TAG, "Request failed", e)
                        } finally {
                            connectionSlots.release()
                            try {
                                client.close()
                            } catch (ignored: IOException) {
                            }
                        }
                    }
                }
            }
            startDiscoveryResponder()
            updateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Could not start the sharing server", e)
            lastError = e.message ?: e.toString()
            isRunning = false
            httpPort = 0
            stopSelf()
        }
    }

    private fun stopServer() {
        isRunning = false
        httpPort = 0
        try {
            serverSocket?.close()
        } catch (ignored: IOException) {
        }
        serverSocket = null
        try {
            discoverySocket?.close()
        } catch (ignored: Exception) {
        }
        discoverySocket = null
    }

    /**
     * Answers "who shares models here?" broadcast packets, so the other device
     * can list the phones on the network without knowing their IP addresses.
     */
    private fun startDiscoveryResponder() {
        thread(name = "sayboard-share-discovery", isDaemon = true) {
            try {
                val socket = DatagramSocket(null)
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(ShareProtocol.DISCOVERY_PORT))
                socket.soTimeout = 2000
                discoverySocket = socket
                val buffer = ByteArray(256)
                while (!socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    } catch (e: Exception) {
                        break
                    }
                    val message = String(packet.data, 0, packet.length).trim()
                    if (!message.startsWith(ShareProtocol.DISCOVER_REQUEST)) continue
                    val reply = JSONObject()
                        .put("app", ShareProtocol.APP_ID)
                        .put("version", ShareProtocol.VERSION)
                        .put("name", Build.MODEL ?: "Android")
                        .put("httpPort", httpPort)
                        .put("models", listServedModels().size)
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                    try {
                        socket.send(
                            DatagramPacket(reply, reply.size, packet.address, packet.port)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not answer discovery", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Discovery responder stopped", e)
            }
        }
    }

    // -------------------------------------------------------------- HTTP layer

    private fun handleConnection(client: Socket) {
        client.soTimeout = SOCKET_TIMEOUT_MS
        val input = client.getInputStream().buffered()
        val requestLine = readLine(input) ?: return
        val headers = HashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim().lowercase(Locale.ROOT)] =
                    line.substring(separator + 1).trim()
            }
        }
        val output = BufferedOutputStream(client.getOutputStream())

        val parts = requestLine.split(" ").filter { it.isNotEmpty() }
        if (parts.size < 2) {
            sendText(output, 400, "Bad request")
            output.flush()
            return
        }
        if (!parts[0].equals("GET", ignoreCase = true)) {
            sendText(output, 405, "Method not allowed")
            output.flush()
            return
        }

        val target = parts[1]
        val path = target.substringBefore('?')
        val query = parseQuery(target.substringAfter('?', ""))

        when {
            path == "/api/info" -> sendJson(output, infoJson())
            path == "/api/models" -> sendJson(output, modelsJson())
            path.startsWith("/api/models/") && path.endsWith("/manifest") -> {
                val name = decode(path.removePrefix("/api/models/").removeSuffix("/manifest"))
                val model = findServedModel(name)
                if (model == null) {
                    sendText(output, 404, "Model not found")
                } else {
                    sendJson(output, manifestJson(model))
                }
            }

            path.startsWith("/api/models/") && path.endsWith("/file") -> {
                val name = decode(path.removePrefix("/api/models/").removeSuffix("/file"))
                val relativePath = query["path"]
                if (relativePath == null) {
                    sendText(output, 400, "Missing path")
                } else {
                    sendModelFile(output, name, relativePath, headers["range"])
                }
            }

            else -> sendText(output, 404, "Not found")
        }
        output.flush()
    }

    private fun infoJson(): String {
        return JSONObject()
            .put("app", ShareProtocol.APP_ID)
            .put("version", ShareProtocol.VERSION)
            .put("name", Build.MODEL ?: "Android")
            .put("httpPort", httpPort)
            .toString()
    }

    private fun modelsJson(): String {
        val models = JSONArray()
        for (model in listServedModels()) {
            models.put(
                JSONObject()
                    .put("name", model.name)
                    .put("locale", model.localeTag)
                    .put("sizeBytes", model.sizeBytes)
                    .put("fileCount", model.fileCount)
            )
        }
        return JSONObject().put("models", models).toString()
    }

    private fun manifestJson(model: ServedModel): String {
        val files = JSONArray()
        for (file in listModelFiles(model.directory)) {
            files.put(
                JSONObject()
                    .put("path", file.first)
                    .put("size", file.second)
            )
        }
        return JSONObject()
            .put("name", model.name)
            .put("locale", model.localeTag)
            .put("files", files)
            .toString()
    }

    private fun sendModelFile(
        output: OutputStream,
        modelName: String,
        relativePath: String,
        rangeHeader: String?
    ) {
        // fast lookup: file requests must not walk (and measure) every model
        val modelDir = findServedModelDirectory(modelName)?.canonicalFile
        if (modelDir == null) {
            sendText(output, 404, "Model not found")
            return
        }
        val file = File(modelDir, relativePath).canonicalFile
        // never serve anything outside of the model folder
        if (!file.path.startsWith(modelDir.path + File.separator) || !file.isFile) {
            sendText(output, 404, "File not found")
            return
        }
        val total = file.length()
        val range = parseRange(rangeHeader, total)
        val start = range?.first ?: 0L
        val end = range?.second ?: (total - 1)
        val length = if (total == 0L) 0L else end - start + 1

        val header = StringBuilder()
        if (range != null) {
            header.append("HTTP/1.1 206 Partial Content\r\n")
            header.append("Content-Range: bytes $start-$end/$total\r\n")
        } else {
            header.append("HTTP/1.1 200 OK\r\n")
        }
        header.append("Content-Type: application/octet-stream\r\n")
        header.append("Content-Length: $length\r\n")
        header.append("Accept-Ranges: bytes\r\n")
        header.append("Connection: close\r\n")
        header.append("\r\n")
        output.write(header.toString().toByteArray(Charsets.UTF_8))

        if (length <= 0L) return
        RandomAccessFile(file, "r").use { source ->
            source.seek(start)
            val buffer = ByteArray(BUFFER_SIZE)
            var remaining = length
            while (remaining > 0) {
                val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    private fun sendJson(output: OutputStream, body: String) {
        sendResponse(output, 200, "application/json; charset=utf-8", body.toByteArray(Charsets.UTF_8))
    }

    private fun sendText(output: OutputStream, code: Int, body: String) {
        sendResponse(output, code, "text/plain; charset=utf-8", body.toByteArray(Charsets.UTF_8))
    }

    private fun sendResponse(output: OutputStream, code: Int, contentType: String, body: ByteArray) {
        val reason = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "Error"
        }
        val header = "HTTP/1.1 $code $reason\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(body)
    }

    // --------------------------------------------------------------- inventory

    private class ServedModel(
        val name: String,
        val localeTag: String,
        val directory: File,
        val sizeBytes: Long,
        val fileCount: Int,
    )

    private fun listServedModels(): List<ServedModel> {
        val models = ArrayList<ServedModel>()
        val root = Constants.getModelsDirectory(this)
        if (!root.isDirectory) return models
        for (localeFolder in root.listFiles() ?: return models) {
            if (!localeFolder.isDirectory) continue
            for (modelFolder in localeFolder.listFiles() ?: continue) {
                if (!modelFolder.isDirectory) continue
                val files = listModelFiles(modelFolder)
                if (files.isEmpty()) continue
                models.add(
                    ServedModel(
                        name = modelFolder.name,
                        localeTag = localeFolder.name,
                        directory = modelFolder,
                        sizeBytes = files.sumOf { it.second },
                        fileCount = files.size,
                    )
                )
            }
        }
        return models.sortedBy { it.name }
    }

    private fun findServedModel(name: String): ServedModel? {
        return listServedModels().firstOrNull { it.name == name }
    }

    private fun findServedModelDirectory(name: String): File? {
        val root = Constants.getModelsDirectory(this)
        if (!root.isDirectory) return null
        for (localeFolder in root.listFiles() ?: return null) {
            if (!localeFolder.isDirectory) continue
            val candidate = File(localeFolder, name)
            if (candidate.isDirectory) return candidate
        }
        return null
    }

    private fun listModelFiles(directory: File): List<Pair<String, Long>> {
        val result = ArrayList<Pair<String, Long>>()
        val root = directory.absolutePath.length + 1
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                result.add(
                    file.absolutePath.substring(root).replace(File.separatorChar, '/') to file.length()
                )
            }
        }
        return result
    }

    // ------------------------------------------------------------ notification

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                Constants.SHARE_CHANNEL_ID,
                getString(R.string.share_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.share_notification_channel_description)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ModelShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, Constants.SHARE_CHANNEL_ID)
            .setContentTitle(getString(R.string.share_notification_title))
            .setContentText(notificationText())
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.share_stop), stopIntent)
            .build()
    }

    private fun notificationText(): String {
        val address = localIpv4Address() ?: "0.0.0.0"
        return getString(R.string.share_notification_text, "http://$address:$httpPort")
    }

    private fun updateNotification() {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (ignored: Exception) {
        }
    }

    companion object {
        private const val TAG = "ModelShareService"

        const val ACTION_STOP = "com.elishaazaria.sayboard.share.STOP"
        private const val NOTIFICATION_ID = 2
        private const val MAX_CONNECTIONS = 4
        private const val SOCKET_TIMEOUT_MS = 15_000
        private const val BUFFER_SIZE = 64 * 1024

        @Volatile
        internal var isRunning: Boolean = false

        @Volatile
        internal var httpPort: Int = 0

        @Volatile
        internal var lastError: String? = null

        /**
         * The IPv4 address other devices can use to reach us, or null when the
         * device is not connected to anything.
         */
        fun localIpv4Address(): String? {
            try {
                for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
                    if (!networkInterface.isUp || networkInterface.isLoopback) continue
                    for (address in networkInterface.inetAddresses) {
                        if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                            val text = address.hostAddress ?: continue
                            if (text.startsWith("169.254.")) continue
                            return text
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read the local address", e)
            }
            return null
        }

        fun addressText(): String {
            val ip = localIpv4Address() ?: return "0.0.0.0"
            return "http://$ip:$httpPort"
        }
    }
}

private fun readLine(input: InputStream): String? {
    val buffer = ByteArray(MAX_HEADER_LINE)
    var length = 0
    while (length < buffer.size) {
        val byte = input.read()
        if (byte == -1) {
            return if (length == 0) null else String(buffer, 0, length, Charsets.UTF_8)
        }
        if (byte == '\n'.code) {
            var end = length
            if (end > 0 && buffer[end - 1] == '\r'.code.toByte()) end--
            return String(buffer, 0, end, Charsets.UTF_8)
        }
        buffer[length++] = byte.toByte()
    }
    return String(buffer, 0, length, Charsets.UTF_8)
}

private const val MAX_HEADER_LINE = 8192

private fun decode(value: String): String {
    return try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: Exception) {
        value
    }
}

private fun parseQuery(query: String): Map<String, String> {
    if (query.isEmpty()) return emptyMap()
    val result = HashMap<String, String>()
    for (pair in query.split("&")) {
        if (pair.isEmpty()) continue
        val index = pair.indexOf('=')
        if (index <= 0) continue
        result[decode(pair.substring(0, index))] = decode(pair.substring(index + 1))
    }
    return result
}

/**
 * Parses `Range: bytes=start-end` into an inclusive range, clamped to the file.
 */
private fun parseRange(header: String?, length: Long): Pair<Long, Long>? {
    if (header == null || length <= 0L) return null
    if (!header.startsWith("bytes=", ignoreCase = true)) return null
    val spec = header.substring(6).trim()
    if (spec.isEmpty() || spec.contains(',')) return null
    val dash = spec.indexOf('-')
    if (dash < 0) return null
    val startText = spec.substring(0, dash).trim()
    val endText = spec.substring(dash + 1).trim()
    var start = startText.toLongOrNull() ?: 0L
    var end = endText.toLongOrNull() ?: (length - 1L)
    start = start.coerceIn(0L, length - 1L)
    end = end.coerceIn(start, length - 1L)
    return start to end
}
