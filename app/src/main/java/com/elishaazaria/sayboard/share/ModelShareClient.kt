package com.elishaazaria.sayboard.share

import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

class ShareCancelledException : IOException("Canceled")

/**
 * Client side of the local model sharing: finds devices on the network and
 * copies their models into the local models folder.
 */
object ModelShareClient {
    private const val TAG = "ModelShareClient"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val BUFFER_SIZE = 64 * 1024
    private const val MAX_ATTEMPTS_PER_FILE = 4

    /**
     * Sends a discovery broadcast and measures who answers. Each device is then
     * asked for its model list.
     */
    fun discover(timeoutMs: Long = 2500, isCancelled: () -> Boolean = { false }): List<ShareDevice> {
        val found = LinkedHashMap<String, Pair<String, Int>>()
        val socket = DatagramSocket()
        socket.broadcast = true
        socket.soTimeout = 200
        try {
            val payload = ShareProtocol.DISCOVER_REQUEST.toByteArray(Charsets.UTF_8)
            val targets = HashSet<InetAddress>()
            broadcastAddresses().forEach { targets.add(it) }
            try {
                targets.add(InetAddress.getByName("255.255.255.255"))
            } catch (ignored: Exception) {
            }
            for (address in targets) {
                try {
                    socket.send(
                        DatagramPacket(payload, payload.size, address, ShareProtocol.DISCOVERY_PORT)
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "Could not broadcast to $address", e)
                }
            }

            val deadline = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArray(1024)
            while (System.currentTimeMillis() < deadline && !isCancelled()) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                } catch (e: Exception) {
                    break
                }
                val json = try {
                    JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8))
                } catch (e: Exception) {
                    continue
                }
                if (json.optString("app") != ShareProtocol.APP_ID) continue
                val host = packet.address?.hostAddress ?: continue
                val port = json.optInt("httpPort", 0)
                if (port <= 0) continue
                found["$host:$port"] = host to port
            }
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {
            }
        }

        val devices = ArrayList<ShareDevice>()
        for ((_, hostAndPort) in found) {
            val (host, port) = hostAndPort
            try {
                val device = fetchDevice(host, port)
                if (device.models.isNotEmpty()) devices.add(device)
            } catch (e: Exception) {
                Log.w(TAG, "Could not list the models of $host:$port", e)
            }
        }
        return devices
    }

    /**
     * Connects to a device whose address was typed in by hand ("192.168.1.5:41234").
     * Useful when the network blocks discovery broadcasts.
     */
    fun fetchDevice(host: String, port: Int): ShareDevice {
        val name = try {
            JSONObject(httpGetText("http://$host:$port/api/info")).optString("name", host)
        } catch (e: Exception) {
            host
        }
        return ShareDevice(name, host, port, fetchModels(host, port))
    }

    fun parseAddress(text: String): Pair<String, Int>? {
        val trimmed = text.trim().removePrefix("http://").removeSuffix("/")
        val host = trimmed.substringBefore(':')
        val port = trimmed.substringAfter(':', "").toIntOrNull() ?: return null
        if (host.isEmpty() || port !in 1..65535) return null
        return host to port
    }

    fun fetchModels(host: String, port: Int): List<SharedModel> {
        val json = JSONObject(httpGetText("http://$host:$port/api/models"))
        val array = json.optJSONArray("models") ?: return emptyList()
        val models = ArrayList<SharedModel>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name")
            if (name.isEmpty()) continue
            models.add(
                SharedModel(
                    name = name,
                    localeTag = item.optString("locale", "en-US"),
                    sizeBytes = item.optLong("sizeBytes", 0L),
                    fileCount = item.optInt("fileCount", 0),
                )
            )
        }
        return models.sortedBy { it.name }
    }

    /**
     * Copies a shared model into [targetDir]. Files are resumed when a transfer
     * is interrupted, and the whole folder is deleted when it is cancelled.
     */
    fun download(
        host: String,
        port: Int,
        modelName: String,
        targetDir: File,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean,
    ) {
        val encodedName = URLEncoder.encode(modelName, "UTF-8")
        val manifest = JSONObject(
            httpGetText("http://$host:$port/api/models/$encodedName/manifest")
        )
        val files = manifest.optJSONArray("files") ?: throw IOException("Empty manifest")

        val totalBytes = (0 until files.length()).sumOf { index ->
            files.optJSONObject(index)?.optLong("size", 0L) ?: 0L
        }
        var doneBytes = 0L
        onProgress(0L, totalBytes)

        val root = targetDir.canonicalFile
        root.mkdirs()
        try {
            for (index in 0 until files.length()) {
                if (isCancelled()) throw ShareCancelledException()
                val entry = files.optJSONObject(index) ?: continue
                val relativePath = entry.optString("path")
                val expectedSize = entry.optLong("size", 0L)
                if (relativePath.isEmpty()) continue
                val output = File(root, relativePath).canonicalFile
                if (!output.path.startsWith(root.path + File.separator)) continue
                output.parentFile?.mkdirs()
                if (output.isFile && output.length() == expectedSize) {
                    doneBytes += expectedSize
                    onProgress(doneBytes, totalBytes)
                    continue
                }
                val downloaded = downloadFile(
                    "http://$host:$port/api/models/$encodedName/file" +
                            "?path=" + URLEncoder.encode(relativePath, "UTF-8"),
                    output,
                    isCancelled
                ) { writtenSoFar ->
                    onProgress(doneBytes + writtenSoFar, totalBytes)
                }
                doneBytes += downloaded
                onProgress(doneBytes, totalBytes)
            }

            // remember the language so an import of this folder keeps it
            File(root, ShareProtocol.EXPORT_INFO_FILE).writeText(
                JSONObject()
                    .put("name", modelName)
                    .put("locale", manifest.optString("locale", "en-US"))
                    .toString(),
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            if (e is ShareCancelledException) {
                deleteRecursive(targetDir)
            }
            throw e
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun downloadFile(
        url: String,
        target: File,
        isCancelled: () -> Boolean,
        onBytes: (Long) -> Unit,
    ): Long {
        var attempt = 0
        while (true) {
            if (isCancelled()) throw ShareCancelledException()
            var already = if (target.isFile) target.length() else 0L
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            if (already > 0) connection.setRequestProperty("Range", "bytes=$already-")
            try {
                val status = connection.responseCode
                if (status >= 400) {
                    throw IOException("HTTP $status for $url")
                }
                val resuming = already > 0 && status == HttpURLConnection.HTTP_PARTIAL
                if (already > 0 && !resuming) {
                    already = 0
                    target.delete()
                }
                val contentLength = connection.contentLength.toLong()
                var written = already
                onBytes(written)
                BufferedInputStream(connection.inputStream, BUFFER_SIZE).use { input ->
                    FileOutputStream(target, resuming).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            if (isCancelled()) throw ShareCancelledException()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            written += read
                            onBytes(written)
                        }
                        output.flush()
                    }
                }
                if (contentLength > 0 && written - already < contentLength) {
                    throw IOException("Incomplete transfer")
                }
                return written
            } catch (e: ShareCancelledException) {
                throw e
            } catch (e: IOException) {
                attempt++
                Log.w(TAG, "Attempt $attempt failed for $url", e)
                if (attempt >= MAX_ATTEMPTS_PER_FILE) throw e
                try {
                    Thread.sleep(500L * attempt)
                } catch (ignored: InterruptedException) {
                    throw ShareCancelledException()
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun httpGetText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        try {
            val status = connection.responseCode
            if (status >= 400) throw IOException("HTTP $status for $url")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun broadcastAddresses(): List<InetAddress> {
        val addresses = ArrayList<InetAddress>()
        try {
            for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast ?: continue
                    addresses.add(broadcast)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the broadcast addresses", e)
        }
        return addresses
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
}
