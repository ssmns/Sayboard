package com.elishaazaria.sayboard

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import android.util.Log
import androidx.core.content.FileProvider
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.elishaazaria.sayboard.Constants.getModelsDirectory
import com.elishaazaria.sayboard.data.InstalledModelReference
import com.elishaazaria.sayboard.data.VoskLocalModel
import com.elishaazaria.sayboard.data.ModelLink
import com.elishaazaria.sayboard.data.ModelType
import com.elishaazaria.sayboard.share.ShareCancelledException
import com.elishaazaria.sayboard.share.ShareProtocol
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Tools {
    const val VOSK_SERVER_ENABLED = false

    @JvmStatic
    fun isMicrophonePermissionGranted(activity: Activity): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(
            activity.applicationContext,
            Manifest.permission.RECORD_AUDIO
        )
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic
    fun isIMEEnabled(activity: Activity): Boolean {
        val imeManager =
            activity.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        for (i in imeManager.enabledInputMethodList) {
            if (i.packageName == activity.packageName) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun deleteModel(model: InstalledModelReference, context: Context?) {
        val modelFile = File(model.path)
        if (modelFile.exists()) deleteRecursive(modelFile)
    }

    @JvmStatic
    fun deleteRecursive(fileOrDirectory: File, deleteStartingFolder: Boolean = true) {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()!!)
            deleteRecursive(child, true)
        if (deleteStartingFolder) {
            fileOrDirectory.delete()
        }
    }

    fun getVoskModelFromReference(
        reference: InstalledModelReference
    ): VoskLocalModel? {
        val modelFolder = File(reference.path)
        val localeFolder = modelFolder.parentFile ?: return null
        val locale = Locale.forLanguageTag(localeFolder.name)
        // Use the referenced folder itself, so several models of the same
        // language (e.g. a small and a large English model) can be installed
        // side by side and each one is opened correctly.
        if (modelFolder.isDirectory) {
            return VoskLocalModel(modelFolder.absolutePath, locale, modelFolder.name)
        }
        for (folder in localeFolder.listFiles() ?: return null) {
            if (!folder.isDirectory) continue
            return VoskLocalModel(folder.absolutePath, locale, folder.name)
        }
        return null
    }

    /**
     * Free space, in megabytes, on the volume the models are stored on.
     */
    @JvmStatic
    fun getAvailableModelsStorageMb(context: Context): Long {
        return try {
            val modelsDir = getModelsDirectory(context)
            if (!modelsDir.exists()) modelsDir.mkdirs()
            val stat = StatFs(modelsDir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
        } catch (e: Exception) {
            Log.w("Tools", "Could not read free storage space", e)
            -1
        }
    }

    /**
     * Space needed to install a model: the downloaded archive is kept on disk
     * next to the unzipped model until extraction is done, so roughly twice
     * the archive size is required, plus a small margin.
     */
    @JvmStatic
    fun getRequiredStorageMb(archiveSizeMb: Long): Long {
        return (archiveSizeMb * 2.1).toLong() + 200
    }

    @JvmStatic
    fun hasEnoughStorage(context: Context, archiveSizeMb: Long): Boolean {
        val available = getAvailableModelsStorageMb(context)
        return available < 0 || available >= getRequiredStorageMb(archiveSizeMb)
    }

    /**
     * Packs a model folder into a zip file inside the app cache, so it can be
     * shared with another device. The archive keeps the folder structure and a
     * small metadata file with the language, so the receiving app imports it
     * with the right locale.
     */
    @JvmStatic
    @Throws(java.io.IOException::class)
    fun zipModelForSharing(
        modelFolder: File,
        modelName: String,
        context: Context,
        onProgress: (Float) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): File {
        val sharedDir = File(context.cacheDir, "shared")
        if (!sharedDir.exists()) sharedDir.mkdirs()
        val zipFile = File(sharedDir, "$modelName.zip")
        if (zipFile.exists()) zipFile.delete()

        val files = modelFolder.walkTopDown().filter { it.isFile }.toList()
        val totalBytes = files.sumOf { it.length() }.coerceAtLeast(1L)
        val localeTag = modelFolder.parentFile?.name ?: "en-US"
        val rootPath = modelFolder.absolutePath.length + 1
        var copied = 0L

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
            // models are binary blobs - storing them is much faster than compressing
            zip.setLevel(Deflater.NO_COMPRESSION)
            val buffer = ByteArray(64 * 1024)
            for (file in files) {
                if (isCancelled()) throw ShareCancelledException()
                val relative = file.absolutePath.substring(rootPath).replace(File.separatorChar, '/')
                zip.putNextEntry(ZipEntry("$modelName/$relative"))
                file.inputStream().use { input ->
                    while (true) {
                        if (isCancelled()) throw ShareCancelledException()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        zip.write(buffer, 0, read)
                        copied += read
                        onProgress(copied.toFloat() / totalBytes)
                    }
                }
                zip.closeEntry()
            }
            if (isCancelled()) throw ShareCancelledException()
            zip.putNextEntry(ZipEntry("$modelName/${ShareProtocol.EXPORT_INFO_FILE}"))
            zip.write(
                JSONObject()
                    .put("name", modelName)
                    .put("locale", localeTag)
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()
        }
        onProgress(1f)
        return zipFile
    }

    /**
     * A content:// uri that other apps are allowed to read, or null if the file
     * is not in a shared path.
     */
    @JvmStatic
    fun shareUriFor(context: Context, file: File): android.net.Uri? {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.w("Tools", "Could not create a share uri for ${file.absolutePath}", e)
            null
        }
    }

    /**
     * Opens the Android share sheet for a model zip file.
     */
    @JvmStatic
    fun shareModelZip(activity: Activity, zipFile: File, modelName: String) {
        val uri = shareUriFor(activity, zipFile) ?: return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, modelName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(
            Intent.createChooser(sendIntent, activity.getString(R.string.share_chooser_title))
        )
    }

    fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = context.getString(R.string.notification_download_channel_name)
            val description = context.getString(R.string.notification_download_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(Constants.DOWNLOADER_CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)

            val shareChannel = NotificationChannel(
                Constants.SHARE_CHANNEL_ID,
                context.getString(R.string.share_notification_channel_name),
                importance
            )
            shareChannel.description =
                context.getString(R.string.share_notification_channel_description)
            notificationManager.createNotificationChannel(shareChannel)
        }
    }

    fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        if (!outputFile.parentFile!!.exists()) {
            outputFile.parentFile!!.mkdirs()
        }
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }
}