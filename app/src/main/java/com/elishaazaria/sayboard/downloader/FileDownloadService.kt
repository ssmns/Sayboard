package com.elishaazaria.sayboard.downloader

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toFile
import com.elishaazaria.sayboard.Constants
import com.elishaazaria.sayboard.Constants.getDirectoryForModel
import com.elishaazaria.sayboard.Constants.getTemporaryDownloadLocation
import com.elishaazaria.sayboard.Constants.getTemporaryUnzipLocation
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.Tools
import com.elishaazaria.sayboard.data.ModelLink
import com.elishaazaria.sayboard.downloader.messages.*
import com.elishaazaria.sayboard.share.ModelShareClient
import com.elishaazaria.sayboard.share.ShareCancelledException
import com.elishaazaria.sayboard.share.ShareProtocol
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class FileDownloadService : Service() {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var currentModel: ModelInfo? = null
    private val queuedModels: Queue<ModelInfo> = LinkedList()
    private var currentState = State.NONE
    private var downloadProgress = 0f
    private var unzipProgress = 0f

    private var interrupt = false

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, Constants.DOWNLOADER_CHANNEL_ID)
        notificationBuilder.setContentTitle(getString(R.string.notification_download_title))
            .setContentText(getString(R.string.notification_download_content_unknown))
            .setSmallIcon(R.drawable.ic_notification).priority = NotificationCompat.PRIORITY_LOW
        notificationBuilder.setProgress(0, 0, true)
        notificationBuilder.foregroundServiceBehavior =
            NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.getStringExtra(FileDownloader.ACTION)) {
            FileDownloader.ACTION_DOWNLOAD -> {
                val modelInfo = FileDownloader.getInfoForIntent(intent) ?: return START_NOT_STICKY
                Log.d(TAG, "Got message $modelInfo")
                queuedModels.add(modelInfo)
                sendEnqueued(modelInfo)
                executor.execute { main() }
            }

            FileDownloader.ACTION_SHARE_DOWNLOAD -> {
                val modelInfo = FileDownloader.getInfoForIntent(intent) ?: return START_NOT_STICKY
                Log.d(TAG, "Got shared model $modelInfo")
                queuedModels.add(modelInfo)
                sendEnqueued(modelInfo)
                executor.execute { main() }
            }

            FileDownloader.ACTION_EXPORT -> {
                val path = intent.getStringExtra(FileDownloader.EXPORT_MODEL_PATH)
                val name = intent.getStringExtra(FileDownloader.EXPORT_MODEL_NAME)
                if (path != null && name != null) {
                    executor.execute { exportModel(File(path), name) }
                }
            }

            FileDownloader.ACTION_UNZIP -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(FileDownloader.UNZIP_URI, Uri::class.java)
                } else {
                    intent.getParcelableExtra(FileDownloader.UNZIP_URI)
                }
//                val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    intent.getSerializableExtra(FileDownloader.UNZIP_LOCALE, Locale::class.java)
//                } else {
//                    intent.getSerializableExtra(FileDownloader.UNZIP_LOCALE) as Locale
//                }
                executor.execute { unzipUri(uri!!) }
            }
        }
        startForeground(notificationId, notificationBuilder.build())
        return START_NOT_STICKY
    }

    private fun unzipUri(uri: Uri) {
        Log.d(TAG, "Unzipping $uri")
        val filename = "ImportedFile.zip"
        val file = getTemporaryDownloadLocation(this, filename)
        Tools.copyStreamToFile(contentResolver!!.openInputStream(uri)!!, file)
        unzipProgress = 0f
        currentModel = ModelInfo(uri.toString(), file.name)
        setState(State.NONE)
        unzipFile(file)
        if (interrupt) {
            interrupted(file)
        }
        Log.d(TAG, "Finished unzipping")
        file.delete()
        setState(State.FINISHED)
        Log.d(TAG, "Finished processing $currentModel")
        mainEnd()
    }

    private fun main() {
        currentModel = queuedModels.poll()
        val currentModelS = currentModel ?: return
        Log.d(TAG, "Started processing $currentModelS")
        if (ShareProtocol.isSharedModelUrl(currentModelS.url)) {
            downloadSharedModel(currentModelS)
            return
        }
        // Large models need several gigabytes of free space (archive + unzipped
        // model). Check before wasting bandwidth on a doomed download.
        val declaredSizeMb = ModelLink.forFilename(currentModelS.filename)?.sizeMb?.toLong()
        if (declaredSizeMb != null && !Tools.hasEnoughStorage(this, declaredSizeMb)) {
            setError(
                getString(
                    R.string.models_download_error_no_space,
                    Tools.getRequiredStorageMb(declaredSizeMb),
                    Tools.getAvailableModelsStorageMb(this)
                )
            )
            mainEnd()
            return
        }
        downloadProgress = 0f
        unzipProgress = 0f
        setState(State.NONE)
        val downloadLocation = getTemporaryDownloadLocation(
            applicationContext, currentModelS.filename
        )
        if (!downloadLocation.parentFile!!.exists()) {
            downloadLocation.parentFile!!.mkdirs()
        }
        try {
            downloadFile(downloadLocation)
        } catch (e: IOException) {
            e.printStackTrace()
            setError(e.message)
            mainEnd()
            return
        }
        if (interrupt) {
            interrupted(downloadLocation)
            return
        }
        Log.d(TAG, "Finished downloading")
        try {
            unzipFile(downloadLocation)
        } catch (e: IOException) {
            e.printStackTrace()
            setError(e.message)
            mainEnd()
            return
        }
        if (interrupt) {
            interrupted(downloadLocation)
        }
        Log.d(TAG, "Finished unzipping")
        downloadLocation.delete()
        setState(State.FINISHED)
        Log.d(TAG, "Finished processing $currentModel")
        mainEnd()
    }

    /**
     * Copies a model from another device on the same network straight into the
     * models folder. No archive is written, so no extra disk space is needed.
     */
    private fun downloadSharedModel(info: ModelInfo) {
        try {
            val uri = Uri.parse(info.url)
            val host = uri.host
            val port = uri.port
            val name = uri.path?.removePrefix("/")?.let { Uri.decode(it) }
            if (host == null || port <= 0 || name.isNullOrEmpty()) {
                setError(getString(R.string.share_error_bad_url))
                mainEnd()
                return
            }
            val localeTag = uri.getQueryParameter("locale") ?: info.locale.toLanguageTag()
            val locale = Locale.forLanguageTag(localeTag)
            val targetDir = File(Constants.getDirectoryForModel(this, locale), name)
            if (targetDir.exists()) {
                setError(getString(R.string.share_error_already_installed, name))
                mainEnd()
                return
            }
            val sizeMb = (uri.getQueryParameter("size")?.toLongOrNull() ?: 0L) / (1024 * 1024)
            if (sizeMb > 0 && !Tools.hasEnoughStorage(this, sizeMb)) {
                setError(
                    getString(
                        R.string.models_download_error_no_space,
                        Tools.getRequiredStorageMb(sizeMb),
                        Tools.getAvailableModelsStorageMb(this)
                    )
                )
                mainEnd()
                return
            }

            setState(State.DOWNLOAD_STARTED)
            ModelShareClient.download(
                host = host,
                port = port,
                modelName = name,
                targetDir = targetDir,
                onProgress = { done, total ->
                    if (total > 0) {
                        setDownloadProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                },
                isCancelled = { interrupt }
            )
            if (interrupt) {
                interrupted(null)
                return
            }
            setDownloadProgress(1f)
            setState(State.FINISHED)
            Log.d(TAG, "Finished copying shared model $name")
            mainEnd()
        } catch (e: ShareCancelledException) {
            interrupted(null)
        } catch (e: Exception) {
            Log.e(TAG, "Could not copy the shared model", e)
            setError(e.message ?: e.javaClass.simpleName)
            mainEnd()
        }
    }

    /**
     * Packs an installed model into a zip in the cache dir and offers to share it.
     */
    private fun exportModel(modelFolder: File, modelName: String) {
        val info = ModelInfo("export://${modelFolder.absolutePath}", modelName)
        currentModel = info
        currentState = State.NONE
        downloadProgress = 0f
        setState(State.EXPORTING)
        try {
            val zip = Tools.zipModelForSharing(
                modelFolder = modelFolder,
                modelName = modelName,
                context = this,
                onProgress = { progress -> setDownloadProgress(progress) },
                isCancelled = { interrupt }
            )
            setDownloadProgress(1f)
            setState(State.FINISHED)
            EventBus.getDefault().post(ModelExported(info, zip))
            showShareNotification(info, zip)
        } catch (e: ShareCancelledException) {
            setState(State.CANCELED)
        } catch (e: Exception) {
            Log.e(TAG, "Could not export the model", e)
            setError(e.message ?: e.javaClass.simpleName)
        } finally {
            interrupt = false
            mainEnd()
        }
    }

    private fun showShareNotification(info: ModelInfo, zip: File) {
        val uri = Tools.shareUriFor(this, zip) ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, info.filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, getString(R.string.share_chooser_title))
        val pending = PendingIntent.getActivity(
            this, 1, chooser,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, Constants.SHARE_CHANNEL_ID)
            .setContentTitle(getString(R.string.share_export_done_title))
            .setContentText(getString(R.string.share_export_done_text, info.filename))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(shareNotificationId, builder.build())
        }
    }

    private fun interrupted(downloadLocation: File?) {
        downloadLocation?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        val unzipFolder = getTemporaryUnzipLocation(this)
        if (unzipFolder.exists()) {
            Tools.deleteRecursive(unzipFolder)
        }

        Log.d(TAG, "Download Canceled")
        EventBus.getDefault().post(
            CancelFinished(currentModel!!)
        )
        updateNotification()

        interrupt = false
        mainEnd()
    }

    private fun mainEnd() {
        downloadProgress = 0f
        unzipProgress = 0f
        currentState = State.NONE
        currentModel = null
        if (queuedModels.isEmpty()) stopForeground(false)
    }

    @Throws(IOException::class)
    private fun downloadFile(downloadLocation: File) {
        setState(State.DOWNLOAD_STARTED)
        val url = URL(currentModel!!.url)
        if (!downloadLocation.parentFile!!.exists()) {
            downloadLocation.parentFile!!.mkdirs()
        }
        // Big models are up to 4 GB, so a dropped connection must not throw the
        // whole download away. Keep whatever was already fetched and resume it
        // with an HTTP range request.
        var attempt = 0
        while (!interrupt) {
            val alreadyDownloaded = if (downloadLocation.exists()) downloadLocation.length() else 0L
            var totalLength = -1L
            try {
                val urlConnection = url.openConnection()
                urlConnection.connectTimeout = CONNECT_TIMEOUT_MS
                urlConnection.readTimeout = READ_TIMEOUT_MS
                if (alreadyDownloaded > 0L) {
                    urlConnection.setRequestProperty("Range", "bytes=$alreadyDownloaded-")
                }
                urlConnection.connect()
                val responseCode = (urlConnection as? HttpURLConnection)?.responseCode ?: 200
                if (responseCode >= 400) {
                    // 404/403/... - retrying will not help
                    throw NonRetryableDownloadException(
                        getString(
                            R.string.models_download_error,
                            "HTTP $responseCode for ${currentModel!!.url}"
                        )
                    )
                }
                val contentLength = urlConnection.contentLength.toLong()
                val resuming = alreadyDownloaded > 0L && responseCode == HTTP_PARTIAL_CONTENT
                if (alreadyDownloaded > 0L && !resuming) {
                    // Server ignored the range request - start from scratch
                    downloadLocation.delete()
                }
                val startOffset = if (resuming) alreadyDownloaded else 0L
                totalLength = when {
                    contentLength <= 0 -> -1L
                    else -> startOffset + contentLength
                }
                Log.d(TAG, "Downloading ${currentModel!!.url} from byte $startOffset of $totalLength")

                setDownloadProgress(if (totalLength > 0) startOffset.toFloat() / totalLength else 0f)
                urlConnection.getInputStream().use { rawInput ->
                    BufferedInputStream(rawInput, BUFFER_SIZE).use { input ->
                        FileOutputStream(downloadLocation, resuming).use { output ->
                            val data = ByteArray(BUFFER_SIZE)
                            var total = startOffset.toLong()
                            var count: Int
                            while (input.read(data).also { count = it } != -1 && !interrupt) {
                                total += count
                                if (totalLength > 0) {
                                    setDownloadProgress((total.toFloat() / totalLength).coerceAtMost(0.999f))
                                }
                                output.write(data, 0, count)
                            }
                            output.flush()
                        }
                    }
                }
                if (interrupt) return
                if (totalLength > 0 && downloadLocation.length() < totalLength) {
                    throw IOException("Connection closed before the download finished")
                }
                setDownloadProgress(1f)
                setState(State.DOWNLOAD_FINISHED)
                return
            } catch (e: NonRetryableDownloadException) {
                throw e
            } catch (e: IOException) {
                attempt++
                Log.w(TAG, "Download attempt $attempt failed", e)
                if (interrupt) return
                if (attempt >= MAX_DOWNLOAD_ATTEMPTS) {
                    throw IOException(
                        getString(
                            R.string.models_download_error,
                            e.message ?: e.javaClass.simpleName
                        ), e
                    )
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt)
                } catch (ignored: InterruptedException) {
                    return
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun unzipFile(downloadLocation: File) {
        setState(State.UNZIP_STARTED)
//        val unzipDestination = getDirectoryForModel(
//            applicationContext, currentModel!!.locale
//        )
//        if (!unzipDestination.exists()) {
//            unzipDestination.mkdirs()
//        }
//        val unzipFolder = getTemporaryUnzipLocation(this)
//        val currentUnzipFolder = File(unzipFolder, unzipDestination.name)
        ZipTools.unzip(
            downloadLocation, currentModel!!.locale, applicationContext,
            errorObserver = {
                setError(it)
            }
        ) { d: Double -> setUnzipProgress(d.toFloat()) }
        setUnzipProgress(1f)
        setState(State.UNZIP_FINISHED)
    }

    private var lastDownloadProgress = 0f
    private var lastUnzipProgress = 0f
    private var lastState: State = State.NONE
    private var lastUpdateTime: Long = 0
    private fun updateNotification() {
        if (lastDownloadProgress == downloadProgress && lastUnzipProgress == unzipProgress && lastState == currentState) // nothing changed
            return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < minUpdateTime) {
            if (lastState == currentState) { // it's a progress update
                if (!(downloadProgress == 1f && unzipProgress == 0f) && unzipProgress != 1f) { // it's not the last progress update
                    lastUpdateTime = currentTime
                    return
                }
            }
        }
        lastUpdateTime = currentTime
        when (currentState) {
            State.NONE -> notificationBuilder.setContentText(getString(R.string.notification_download_content_unknown))
                .setProgress(0, 0, true)

            State.DOWNLOAD_STARTED, State.DOWNLOAD_FINISHED -> notificationBuilder.setContentText(
                getString(R.string.notification_download_content_downloading)
            ).setProgress(PROGRESS_MAX, (downloadProgress * PROGRESS_MAX).toInt(), false)

            State.UNZIP_STARTED, State.UNZIP_FINISHED -> notificationBuilder.setContentText(
                getString(R.string.notification_download_content_unzipping)
            ).setProgress(PROGRESS_MAX, (unzipProgress * PROGRESS_MAX).toInt(), false)

            State.EXPORTING -> notificationBuilder.setContentText(
                getString(R.string.notification_download_content_exporting)
            ).setProgress(PROGRESS_MAX, (downloadProgress * PROGRESS_MAX).toInt(), false)

            State.FINISHED -> notificationBuilder.setContentText(getString(R.string.notification_download_content_finished))
                .setProgress(0, 0, false)

            State.ERROR -> notificationBuilder.setContentText(getString(R.string.notification_download_content_error))
                .setProgress(0, 0, false)

            State.CANCELED -> notificationBuilder.setContentText("Downloading Canceled")
                .setProgress(0, 0, false)

            else -> {}
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
        lastDownloadProgress = downloadProgress
        lastUnzipProgress = unzipProgress
        lastState = currentState
    }

    private fun setState(state: State) {
        currentState = state
        EventBus.getDefault().post(DownloadState(currentModel!!, state))
        updateNotification()
    }

    private fun setDownloadProgress(progress: Float) {
        downloadProgress = progress
        EventBus.getDefault().post(DownloadProgress(currentModel!!, downloadProgress))
        updateNotification()
    }

    private fun setUnzipProgress(progress: Float) {
        unzipProgress = progress
        EventBus.getDefault().post(UnzipProgress(currentModel!!, unzipProgress))
        updateNotification()
    }

    private fun setError(message: String?) {
        setState(State.ERROR)
        EventBus.getDefault().post(DownloadError(currentModel!!, message ?: ""))
        updateNotification()
    }

    private fun sendEnqueued(modelInfo: ModelInfo) {
        EventBus.getDefault().post(DownloadState(modelInfo, State.QUEUED))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleStatusQuery(event: StatusQuery?) {
        EventBus.getDefault()
            .post(Status(currentModel, queuedModels, downloadProgress, unzipProgress, currentState))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleCancelPending(event: CancelPending) {
        if (queuedModels.remove(event.info)) {
            EventBus.getDefault().post(
                Status(
                    currentModel, queuedModels, downloadProgress, unzipProgress, currentState
                )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleCancelCurrent(event: CancelCurrent) {
        if (currentModel == event.info) {
            interrupt = true
            setState(State.CANCELED)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        EventBus.getDefault().unregister(this)
    }

    companion object {
        private const val TAG = "FileDownloadService"
        private const val notificationId = 1
        private const val shareNotificationId = 2
        private const val PROGRESS_MAX = 100
        private const val minUpdateTime = (1000 / 5 // 5 updates a second;
                ).toLong()
        private const val BUFFER_SIZE = 256 * 1024
        private const val CONNECT_TIMEOUT_MS = 30 * 1000
        private const val READ_TIMEOUT_MS = 60 * 1000
        private const val MAX_DOWNLOAD_ATTEMPTS = 10
        private const val RETRY_DELAY_MS = 2000L
        private const val HTTP_PARTIAL_CONTENT = 206
    }

    private class NonRetryableDownloadException(message: String) : IOException(message) {
    }
}