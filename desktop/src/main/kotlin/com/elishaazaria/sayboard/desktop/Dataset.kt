package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.json.buildJsonObject
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
    )

    data class Result(
        val folder: File,
        val segments: Int,
        val totalSeconds: Double,
        val skipped: Int,
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
        val wavDir = File(folder, "wavs")
        if (!wavDir.exists() && !wavDir.mkdirs()) {
            throw IOException("could not create ${wavDir.absolutePath}")
        }
        val padding = options.paddingMs.coerceIn(0, 2000) / 1000.0
        val usable = transcript.lines.filter { line ->
            val text = if (options.useCorrected) line.text else line.rawText
            text.isNotBlank() && (line.endSeconds - line.startSeconds) >= options.minDurationSeconds
        }
        val skipped = transcript.lines.size - usable.size

        val csv = StringBuilder()
        val tsv = StringBuilder("path\ttext\traw_text\tstart\tend\n")
        var writtenSeconds = 0.0

        RandomAccessFile(pcm, "r").use { source ->
            usable.forEachIndexed { index, line ->
                if (isCancelled()) throw IOException("cancelled")
                val start = (line.startSeconds - padding).coerceAtLeast(0.0)
                val end = (line.endSeconds + padding).coerceAtMost(
                    transcript.durationSeconds.takeIf { it > 0 } ?: line.endSeconds
                )
                val text = (if (options.useCorrected) line.text else line.rawText).trim()
                val name = String.format(java.util.Locale.US, "%04d.wav", index + 1)
                val target = File(wavDir, name)
                val sliceSeconds = copySegment(source, start, end, target)
                writtenSeconds += sliceSeconds
                csv.append("wavs/").append(name).append('|').append(text.replace('|', ' ')).append('\n')
                tsv.append("wavs/").append(name).append('\t')
                    .append(text.replace('\t', ' ')).append('\t')
                    .append(line.rawText.replace('\t', ' ')).append('\t')
                    .append(String.format(java.util.Locale.US, "%.3f", line.startSeconds)).append('\t')
                    .append(String.format(java.util.Locale.US, "%.3f", line.endSeconds)).append('\n')
                onProgress(index + 1, usable.size)
            }
        }

        File(folder, "metadata.csv").writeText(csv.toString())
        File(folder, "metadata.tsv").writeText(tsv.toString())
        File(folder, "dataset.json").writeText(
            buildJsonObject {
                put("generator", "${AppInfo.APP_NAME} ${Version.NAME}")
                put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                put("source_file", transcript.file.absolutePath)
                put("model", modelName)
                put("segments", usable.size)
                put("skipped", skipped)
                put("padding_ms", options.paddingMs)
                put("corrected_text", options.useCorrected)
                put("sample_rate", 16000)
                put("channels", 1)
                put("bits", 16)
                put("total_seconds", String.format(java.util.Locale.US, "%.2f", writtenSeconds))
            }.toString()
        )
        return Result(folder, usable.size, writtenSeconds, skipped)
    }

    /** Copies `start..end` of the canonical PCM file into a small WAV. */
    private fun copySegment(source: RandomAccessFile, start: Double, end: Double, target: File): Double {
        val bytesPerSecond = 16000.0 * 2.0
        val from = (start * bytesPerSecond).toLong()
        val to = (end * bytesPerSecond).toLong()
        val length = (to - from).coerceAtLeast(0L)
        source.seek(PcmWavWriter.HEADER_SIZE + from)
        target.outputStream().buffered().use { output ->
            output.write(PcmWavWriter.header(length))
            val buffer = ByteArray(64 * 1024)
            var remaining = length
            while (remaining > 0) {
                val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
            output.flush()
        }
        return length / bytesPerSecond
    }
}
