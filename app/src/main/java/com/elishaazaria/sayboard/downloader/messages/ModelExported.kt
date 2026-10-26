package com.elishaazaria.sayboard.downloader.messages

import java.io.File

/**
 * Posted when a model has been packed into a .zip file that can be shared with
 * another device (Bluetooth, Nearby Share, USB, ...).
 */
data class ModelExported(val info: ModelInfo, val zipFile: File)
