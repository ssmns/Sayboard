package com.elishaazaria.sayboard.downloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.elishaazaria.sayboard.data.ModelLink
import com.elishaazaria.sayboard.downloader.FileDownloadService
import com.elishaazaria.sayboard.downloader.messages.ModelInfo
import java.util.*

object FileDownloader {
    const val ACTION = "action"
    const val ACTION_DOWNLOAD = "action_download"
    const val ACTION_UNZIP = "action_unzip"
    const val ACTION_SHARE_DOWNLOAD = "action_share_download"
    const val ACTION_EXPORT = "action_export"

    const val DOWNLOAD_URL = "download_url"
    const val DOWNLOAD_FILENAME = "download_filename"
    const val DOWNLOAD_LOCALE = "download_locale"

    const val UNZIP_URI = "unzip_uri"
    const val UNZIP_LOCALE = "unzip_locale"

    const val EXPORT_MODEL_PATH = "export_model_path"
    const val EXPORT_MODEL_NAME = "export_model_name"

    fun getInfoForIntent(intent: Intent): ModelInfo? {
        val url = intent.getStringExtra(DOWNLOAD_URL)
        val filename = intent.getStringExtra(DOWNLOAD_FILENAME)
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(DOWNLOAD_LOCALE, Locale::class.java)
        } else {
            intent.getSerializableExtra(DOWNLOAD_LOCALE) as Locale?
        }
        return if (url == null || filename == null || locale == null) null else ModelInfo(
            url,
            filename,
            locale
        )
    }

    fun downloadModel(model: ModelLink, context: Context) {
        start(
            context,
            Intent(context, FileDownloadService::class.java)
                .putExtra(ACTION, ACTION_DOWNLOAD)
                .putExtra(DOWNLOAD_URL, model.link)
                .putExtra(DOWNLOAD_FILENAME, model.filename)
                .putExtra(DOWNLOAD_LOCALE, model.locale)
        )
    }

    /**
     * Download a model offered by another device on the local network.
     * [url] is created by [com.elishaazaria.sayboard.share.SharedModel.downloadUrl].
     */
    fun downloadSharedModel(url: String, filename: String, locale: Locale, context: Context) {
        start(
            context,
            Intent(context, FileDownloadService::class.java)
                .putExtra(ACTION, ACTION_SHARE_DOWNLOAD)
                .putExtra(DOWNLOAD_URL, url)
                .putExtra(DOWNLOAD_FILENAME, filename)
                .putExtra(DOWNLOAD_LOCALE, locale)
        )
    }

    /**
     * Pack an installed model into a .zip that can be handed to someone else
     * (Bluetooth, Nearby Share, a USB cable, ...).
     */
    fun exportModel(modelFolderPath: String, modelName: String, context: Context) {
        start(
            context,
            Intent(context, FileDownloadService::class.java)
                .putExtra(ACTION, ACTION_EXPORT)
                .putExtra(EXPORT_MODEL_PATH, modelFolderPath)
                .putExtra(EXPORT_MODEL_NAME, modelName)
        )
    }

    fun importModel(uri: Uri, context: Context) {
        start(
            context,
            Intent(context, FileDownloadService::class.java)
                .putExtra(ACTION, ACTION_UNZIP)
                .putExtra(UNZIP_URI, uri)
        )
    }

    private fun start(context: Context, serviceIntent: Intent) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(serviceIntent)
        } else {
            appContext.startService(serviceIntent)
        }
    }
}
