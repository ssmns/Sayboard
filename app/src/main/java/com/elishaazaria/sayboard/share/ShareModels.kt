package com.elishaazaria.sayboard.share

import java.util.Locale

/**
 * A model another device offers on the local network.
 */
data class SharedModel(
    val name: String,
    val localeTag: String,
    val sizeBytes: Long,
    val fileCount: Int,
) {
    val locale: Locale
        get() = Locale.forLanguageTag(localeTag)

    val sizeText: String
        get() = formatSize(sizeBytes)

    /** URL used by [com.elishaazaria.sayboard.downloader.FileDownloadService]. */
    fun downloadUrl(device: ShareDevice): String {
        return "${ShareProtocol.URL_SCHEME}://${device.address}:${device.httpPort}/$name" +
                "?locale=$localeTag&size=$sizeBytes"
    }
}

/**
 * A device that shares its installed models on the local network.
 */
data class ShareDevice(
    val name: String,
    val address: String,
    val httpPort: Int,
    val models: List<SharedModel>,
) {
    val displayAddress: String
        get() = "$address:$httpPort"
}

fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L ->
            String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))

        bytes >= 1024L * 1024L ->
            String.format(Locale.US, "%d MB", bytes / (1024L * 1024L))

        else -> String.format(Locale.US, "%d KB", bytes / 1024L)
    }
}

/**
 * The (very small) protocol used to share models between two phones on the same
 * network. Both apps must speak version 1.
 *
 * ```
 * GET /api/info                       -> {"app":"sayboard","version":1,"name":...,"httpPort":...}
 * GET /api/models                     -> {"models":[{"name":...,"locale":...,"sizeBytes":...,"fileCount":...}]}
 * GET /api/models/<name>/manifest     -> {"name":...,"locale":...,"files":[{"path":...,"size":...}]}
 * GET /api/models/<name>/file?path=.. -> the raw file (supports Range requests)
 * ```
 */
object ShareProtocol {
    const val APP_ID = "sayboard"
    const val VERSION = 1

    const val DISCOVERY_PORT = 8771
    const val DISCOVER_REQUEST = "SAYBOARD_DISCOVER"

    /** Scheme of the fake URL used to hand a shared model to the download service. */
    const val URL_SCHEME = "sayboard-lan"

    /** Metadata written inside exported zips so the receiving app knows the language. */
    const val EXPORT_INFO_FILE = "sayboard-model.json"

    fun isSharedModelUrl(url: String?): Boolean {
        return url != null && url.startsWith("$URL_SCHEME://")
    }
}
