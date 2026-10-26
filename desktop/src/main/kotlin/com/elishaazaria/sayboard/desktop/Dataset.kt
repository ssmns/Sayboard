package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Cuts a transcript into small audio segments with their text, ready to be used
 * as a dataset for training/fine-tuning speech models (LJSpeech style).
 *
 * ```
 * <folder>/
 *   wavs/0001.wav …      one file per sentence (16 kHz mono 16 bit)
 *   metadata.csv         wavs/0001.wav|<text>
 *   metadata.tsv         path, text, raw text, start, end
 *   dataset.json         source file, model, counts, options
 * ```
 */
object DatasetExporter {

    data class Options(
        /** Extra audio kept before and after the sentence. */
        val paddingMs: Int = 150,
        /** false exports what the speech engine said, true the AI corrected text. */
        val useCorrected: Boolean = true,
        val minDurationSeconds: Double = 0.25,
        /** Keep the segments that are already in the folder and add the new ones. */
        val append: Boolean = true,
    )

    data class Result(
        val folder: File,
        val segments: Int,
        val totalSeconds: Double,
        val skipped: Int,
        /** Segments that were already there before this export. */
        val previousSegments: Int = 0,
    ) {
        val added: Int
            get() = segments - previousSegments
    }

    private val segmentPattern = Regex("^([0-9]{4,})\\.wav$")

    /** Number of segments already in a dataset folder (0 when there is none). */
    fun existingSegments(folder: File): Int {
        val wavs = File(folder, "wavs")
        if (!wavs.isDirectory) return 0
        return wavs.listFiles()
            ?.mapNotNull { segmentPattern.find(it.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.maxOrNull()
            ?: 0
    }

    /** One ready to write segment: canonical 16 kHz PCM plus its texts. */
    data class SegmentInput(
        val pcm: ByteArray,
        val text: String,
        val rawText: String = text,
        val source: String = "",
        val startSeconds: Double = 0.0,
        val endSeconds: Double = 0.0,
    )

    fun export(
        transcript: Transcript,
        folder: File,
        options: Options,
        modelName: String = "",
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): Result {
        val pcm = transcript.pcmFile
        if (pcm == null || !pcm.isFile) {
            throw IOException("the audio of this transcript is not available any more")
        }
        val padding = options.paddingMs.coerceIn(0, 2000) / 1000.0
        val usable = transcript.lines.filter { line ->
            val text = if (options.useCorrected) line.text else line.rawText
            text.isNotBlank() && (line.endSeconds - line.startSeconds) >= options.minDurationSeconds
        }
        val skipped = transcript.lines.size - usable.size

        val segments = ArrayList<SegmentInput>(usable.size)
        RandomAccessFile(pcm, "r").use { source ->
            usable.forEachIndexed { index, line ->
                if (isCancelled()) throw IOException("cancelled")
                val start = (line.startSeconds - padding).coerceAtLeast(0.0)
                val end = (line.endSeconds + padding).coerceAtMost(
                    transcript.durationSeconds.takeIf { it > 0 } ?: line.endSeconds
                )
                segments.add(
                    SegmentInput(
                        pcm = readSegment(source, start, end),
                        text = (if (options.useCorrected) line.text else line.rawText).trim(),
                        rawText = line.rawText.trim(),
                        source = transcript.file.absolutePath,
                        startSeconds = line.startSeconds,
                        endSeconds = line.endSeconds,
                    )
                )
                onProgress(index + 1, usable.size)
            }
        }
        return appendSegments(
            folder = folder,
            segments = segments,
            sourceLabel = transcript.file.name,
            modelName = modelName,
            options = options,
            skipped = skipped,
        )
    }

    /**
     * Adds segments to a dataset folder, continuing the numbering and appending
     * the metadata, so the dataset can grow over time.
     */
    fun appendSegments(
        folder: File,
        segments: List<SegmentInput>,
        sourceLabel: String,
        modelName: String,
        options: Options,
        skipped: Int = 0,
    ): Result {
        val wavDir = File(folder, "wavs")
        if (!wavDir.exists() && !wavDir.mkdirs()) {
            throw IOException("could not create ${wavDir.absolutePath}")
        }
        val previousSegments = existingSegments(folder)
        val appendMode = options.append && previousSegments > 0
        if (!appendMode) {
            wavDir.listFiles()?.forEach { it.delete() }
            File(folder, "metadata.csv").delete()
            File(folder, "metadata.tsv").delete()
            File(folder, "dataset.json").delete()
        }
        val firstIndex = if (appendMode) previousSegments else 0
        val csv = StringBuilder()
        val tsv = StringBuilder()
        if (!appendMode) tsv.append("path\ttext\traw_text\tstart\tend\n")
        var writtenSeconds = 0.0

        segments.forEachIndexed { index, segment ->
            val name = String.format(java.util.Locale.US, "%04d.wav", firstIndex + index + 1)
            writePcmWav(File(wavDir, name), segment.pcm)
            writtenSeconds += segment.pcm.size / (16000.0 * 2.0)
            csv.append("wavs/").append(name).append('|')
                .append(segment.text.replace('|', ' ')).append('\n')
            tsv.append("wavs/").append(name).append('\t')
                .append(segment.text.replace('\t', ' ')).append('\t')
                .append(segment.rawText.replace('\t', ' ')).append('\t')
                .append(String.format(java.util.Locale.US, "%.3f", segment.startSeconds)).append('\t')
                .append(String.format(java.util.Locale.US, "%.3f", segment.endSeconds)).append('\n')
        }
        File(folder, "metadata.csv").appendText(csv.toString())
        File(folder, "metadata.tsv").appendText(tsv.toString())

        val totalSegments = firstIndex + segments.size
        val totalSeconds = previousSeconds(folder) + writtenSeconds
        val sources = readSources(folder).toMutableList()
        sources.add(
            buildJsonObject {
                put("source", sourceLabel)
                put("model", modelName)
                put("segments", segments.size)
                put("seconds", String.format(java.util.Locale.US, "%.2f", writtenSeconds))
                put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                put("options", buildJsonObject {
                    put("padding_ms", options.paddingMs)
                    put("corrected_text", options.useCorrected)
                })
            }
        )
        File(folder, "dataset.json").writeText(
            buildJsonObject {
                put("generator", "${AppInfo.APP_NAME} ${Version.NAME}")
                put("updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                put("segments", totalSegments)
                put("sources", kotlinx.serialization.json.JsonArray(sources))
                put("sample_rate", 16000)
                put("channels", 1)
                put("bits", 16)
                put("total_seconds", String.format(java.util.Locale.US, "%.2f", totalSeconds))
                put("skipped", skipped + skippedBefore(folder))
            }.toString()
        )
        return Result(folder, totalSegments, writtenSeconds, skipped, previousSegments)
    }

    private fun readSegment(source: RandomAccessFile, start: Double, end: Double): ByteArray {
        val bytesPerSecond = 16000.0 * 2.0
        val from = (start * bytesPerSecond).toLong()
        val length = ((end * bytesPerSecond).toLong() - from).coerceAtLeast(0L)
        source.seek(PcmWavWriter.HEADER_SIZE + from)
        val data = ByteArray(length.toInt())
        var read = 0
        while (read < data.size) {
            val count = source.read(data, read, data.size - read)
            if (count <= 0) break
            read += count
        }
        return if (read == data.size) data else data.copyOf(read)
    }

    private fun readSources(folder: File): List<kotlinx.serialization.json.JsonElement> {
        val file = File(folder, "dataset.json")
        if (!file.isFile) return emptyList()
        return try {
            val root = kotlinx.serialization.json.Json.parseToJsonElement(file.readText()).jsonObject
            root["sources"]?.jsonArray?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun previousSeconds(folder: File): Double {
        val file = File(folder, "dataset.json")
        if (!file.isFile) return 0.0
        return try {
            val root = kotlinx.serialization.json.Json.parseToJsonElement(file.readText()).jsonObject
            root["total_seconds"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun skippedBefore(folder: File): Int {
        val file = File(folder, "dataset.json")
        if (!file.isFile) return 0
        return try {
            val root = kotlinx.serialization.json.Json.parseToJsonElement(file.readText()).jsonObject
            root["skipped"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

}
