package com.elishaazaria.sayboard.desktop

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class CancelledException : IOException("Cancelled")

class NotAVoskModelException(message: String) : IOException(message)

data class InstalledModel(
    val name: String,
    val directory: File,
    val sizeBytes: Long,
    val localeTag: String,
) {
    val catalogModel: CatalogModel?
        get() = ModelCatalog.byId(name)

    val lang: String
        get() = catalogModel?.lang ?: localeTag

    val sizeText: String
        get() = formatBytes(sizeBytes)
}

/**
 * Downloads, installs and removes Vosk models. Downloads resume after a broken
 * connection, which matters a lot for the multi-gigabyte models.
 */
object ModelStore {
    private const val USER_AGENT = "sayTypist/1.0"
    private const val BUFFER = 256 * 1024
    private const val MAX_ATTEMPTS = 6

    fun listInstalled(): List<InstalledModel> {
        val root = AppPaths.modelsDir()
        val directories = root.listFiles()?.filter { it.isDirectory } ?: emptyList()
        return directories
            .filter { isVoskModel(it) }
            .map { directory ->
                InstalledModel(
                    name = directory.name,
                    directory = directory,
                    sizeBytes = directorySize(directory),
                    localeTag = localeOf(directory),
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun installed(name: String): InstalledModel? = listInstalled().firstOrNull { it.name == name }

    fun isVoskModel(directory: File): Boolean {
        if (!directory.isDirectory) return false
        return File(directory, "am/final.mdl").isFile || File(directory, "final.mdl").isFile
    }

    fun directorySize(directory: File): Long {
        var total = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) total += try {
                file.length()
            } catch (e: Exception) {
                0L
            }
        }
        return total
    }

    private fun localeOf(directory: File): String {
        val info = File(directory, "saytypist-model.json")
        if (info.isFile) {
            val match = Regex("\"locale\"\\s*:\\s*\"([^\"]+)\"").find(info.readText())
            if (match != null) return match.groupValues[1]
        }
        return ModelCatalog.byId(directory.name)?.localeTag ?: "en-US"
    }

    // --------------------------------------------------------------- downloading

    fun install(
        model: CatalogModel,
        onDownload: (Long, Long) -> Unit,
        onExtract: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): File {
        val archive = download(model, onDownload, isCancelled)
        val destination = extract(archive, model.id, onExtract, isCancelled)
        archive.delete()
        return destination
    }

    fun download(
        model: CatalogModel,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean,
    ): File {
        val archive = File(AppPaths.downloadsDir(), "${model.id}.zip")
        var attempt = 0
        while (true) {
            if (isCancelled()) throw CancelledException()
            var already = if (archive.isFile) archive.length() else 0L
            val connection = URL(model.url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (already > 0) connection.setRequestProperty("Range", "bytes=$already-")
            try {
                val status = connection.responseCode
                if (status >= 400) throw IOException("HTTP $status for ${model.url}")
                val resuming = already > 0 && status == HttpURLConnection.HTTP_PARTIAL
                if (already > 0 && !resuming) {
                    archive.delete()
                    already = 0
                }
                val contentLength = connection.contentLengthLong
                val total = if (contentLength > 0) already + contentLength else -1L
                var written = already
                onProgress(written, total)
                connection.inputStream.buffered(BUFFER).use { input ->
                    RandomAccessFile(archive, "rw").use { output ->
                        output.seek(already)
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            if (isCancelled()) throw CancelledException()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            written += read
                            onProgress(written, total)
                        }
                    }
                }
                if (total > 0 && archive.length() < total) {
                    throw IOException("Incomplete transfer")
                }
                onProgress(archive.length(), archive.length())
                return archive
            } catch (e: CancelledException) {
                throw e
            } catch (e: Exception) {
                attempt++
                if (attempt >= MAX_ATTEMPTS) throw e
                try {
                    Thread.sleep(1000L * attempt)
                } catch (interrupted: InterruptedException) {
                    throw CancelledException()
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    // ----------------------------------------------------------------- extracting

    fun importZip(
        archive: File,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): File {
        return extract(archive, archive.name.removeSuffix(".zip"), onProgress, isCancelled)
    }

    fun extract(
        archive: File,
        targetName: String,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): File {
        val name = targetName.removeSuffix(".zip")
        val target = File(AppPaths.modelsDir(), name)
        if (target.exists()) throw IOException("$name is already installed")

        ZipFile(archive).use { zip ->
            val entries = zip.entries().toList()
            for (entry in entries) {
                val entryName = entry.name
                if (entryName.startsWith("/") || entryName.startsWith("\\") ||
                    entryName.split('/', '\\').contains("..")
                ) {
                    throw NotAVoskModelException("unsafe path in the archive: $entryName")
                }
            }
            if (entries.none {
                    it.name.endsWith("am/final.mdl") || it.name.endsWith("final.mdl")
                }
            ) {
                throw NotAVoskModelException("this archive does not contain a Vosk model")
            }

            val roots = entries.map { it.name.substringBefore('/') }.distinct()
            val singleRoot = roots.size == 1 && entries.all { it.name.contains('/') }
            val tempDir = Files.createTempDirectory(
                AppPaths.downloadsDir().toPath(), "extract-"
            ).toFile()
            try {
                entries.forEachIndexed { index, entry ->
                    if (isCancelled()) throw CancelledException()
                    if (entry.isDirectory) {
                        onProgress(index + 1, entries.size)
                        return@forEachIndexed
                    }
                    val output = File(tempDir, entry.name)
                    output.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        output.outputStream().use { out -> input.copyTo(out, BUFFER) }
                    }
                    onProgress(index + 1, entries.size)
                }
                val source = if (singleRoot) File(tempDir, roots.first()) else tempDir
                target.parentFile?.mkdirs()
                try {
                    Files.move(
                        source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (e: Exception) {
                    source.copyRecursively(target, overwrite = false)
                    source.deleteRecursively()
                }
                return target
            } finally {
                tempDir.deleteRecursively()
                AppPaths.downloadsDir().listFiles()
                    ?.filter { it.name.startsWith("extract-") }
                    ?.forEach { it.deleteRecursively() }
            }
        }
    }

    /** Moves an installed model to another models folder. */
    fun moveModel(model: InstalledModel, destinationRoot: File): File {
        destinationRoot.mkdirs()
        val target = File(destinationRoot, model.name)
        if (target.exists()) throw IOException("${'$'}{target.absolutePath} already exists")
        try {
            Files.move(model.directory.toPath(), target.toPath())
        } catch (e: Exception) {
            model.directory.copyRecursively(target, overwrite = false)
            model.directory.deleteRecursively()
        }
        return target
    }

    fun delete(name: String) {
        val directory = File(AppPaths.modelsDir(), name)
        if (directory.isDirectory) directory.deleteRecursively()
    }

    /**
     * Packs a model into a .zip file so it can be copied to another computer or
     * to an Android device (which imports it with the "Import model" button).
     */
    fun exportZip(
        model: InstalledModel,
        onProgress: (Long, Long) -> Unit,
        isCancelled: () -> Boolean,
    ): File {
        val target = File(AppPaths.downloadsDir(), "${model.name}.zip")
        if (target.isFile) target.delete()
        val files = model.directory.walkTopDown().filter { it.isFile }.toList()
        val total = files.sumOf { it.length() }.coerceAtLeast(1L)
        var copied = 0L
        java.util.zip.ZipOutputStream(target.outputStream().buffered(BUFFER)).use { zip ->
            zip.setLevel(java.util.zip.Deflater.NO_COMPRESSION)
            val buffer = ByteArray(BUFFER)
            for (file in files) {
                if (isCancelled()) throw CancelledException()
                val relative = file.relativeTo(model.directory).invariantSeparatorsPath
                zip.putNextEntry(java.util.zip.ZipEntry("${model.name}/$relative"))
                file.inputStream().use { input ->
                    while (true) {
                        if (isCancelled()) throw CancelledException()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        zip.write(buffer, 0, read)
                        copied += read
                        onProgress(copied, total)
                    }
                }
                zip.closeEntry()
            }
            zip.putNextEntry(java.util.zip.ZipEntry("${model.name}/saytypist-model.json"))
            zip.write(
                ("{\"name\":\"" + model.name + "\",\"locale\":\"" + model.localeTag + "\"}")
                    .toByteArray()
            )
            zip.closeEntry()
        }
        onProgress(total, total)
        return target
    }
}
