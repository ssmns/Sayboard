package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class TranscriptionException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** One recognized sentence of an audio file, with its position in the file. */
data class TranscriptLine(val startSeconds: Double, val endSeconds: Double, val text: String)

data class Transcript(val file: File, val lines: List<TranscriptLine>, val durationSeconds: Double) {
    val text: String
        get() = lines.joinToString("\n") { it.text }

    fun textWithTimecodes(): String =
        lines.joinToString("\n") { "${formatTimecode(it.startSeconds)}  ${it.text}" }

    val wordCount: Int
        get() = lines.sumOf { line -> line.text.split(' ').count { it.isNotBlank() } }
}

fun formatTimecode(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toLong()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

/**
 * Reads an audio file and turns it into text with the Vosk model that is
 * currently active. WAV files are decoded by Java itself; every other format
 * (mp3, ogg, opus, flac, m4a, webm, ...) is decoded with ffmpeg when it is
 * installed.
 */
object AudioFileReader {
    const val TARGET_RATE = 16000f
    private val TARGET_FORMAT = AudioFormat(TARGET_RATE, 16, 1, true, false)

    val supportedExtensions: List<String> =
        listOf("wav", "wave", "mp3", "ogg", "oga", "opus", "flac", "m4a", "aac", "wma", "webm", "mp4")

    fun isWav(file: File): Boolean = file.extension.lowercase() in setOf("wav", "wave")

    fun ffmpegAvailable(): Boolean = ClipboardTools.inPath("ffmpeg")

    fun canRead(file: File): Boolean = isWav(file) || ffmpegAvailable()

    /** Duration in seconds, or 0 when it cannot be determined. */
    fun durationSeconds(file: File): Double {
        if (isWav(file)) {
            return try {
                AudioSystem.getAudioInputStream(file).use { stream ->
                    val frameLength = stream.frameLength
                    if (frameLength > 0) frameLength.toDouble() / stream.format.frameRate else 0.0
                }
            } catch (e: Exception) {
                0.0
            }
        }
        if (!ClipboardTools.inPath("ffprobe")) return 0.0
        return try {
            val process = ProcessBuilder(
                "ffprobe", "-v", "error", "-show_entries", "format=duration",
                "-of", "default=nw=1:nk=1", file.absolutePath
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor(10, TimeUnit.SECONDS)
            output.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Opens the file as 16 kHz mono 16 bit PCM. The returned stream must be
     * closed; for non-WAV files it also terminates ffmpeg.
     */
    fun openPcm(file: File, isCancelled: () -> Boolean = { false }): InputStream {
        if (!file.isFile) throw TranscriptionException("the file does not exist")
        return if (isWav(file)) {
            try {
                val original = AudioSystem.getAudioInputStream(file)
                if (matches(original.format)) {
                    original
                } else {
                    AudioSystem.getAudioInputStream(TARGET_FORMAT, original)
                }
            } catch (e: Exception) {
                throw TranscriptionException("could not read the WAV file: ${e.message}", e)
            }
        } else {
            ffmpegPcm(file, isCancelled)
        }
    }

    private fun matches(format: AudioFormat): Boolean {
        return format.sampleRate == TARGET_RATE && format.channels == 1 &&
                format.sampleSizeInBits == 16 && !format.isBigEndian
    }

    private fun ffmpegPcm(file: File, isCancelled: () -> Boolean): InputStream {
        if (!ffmpegAvailable()) {
            throw TranscriptionException(
                "ffmpeg is needed to read ${file.extension.uppercase()} files"
            )
        }
        val process = ProcessBuilder(
            "ffmpeg", "-nostdin", "-loglevel", "error", "-i", file.absolutePath,
            "-ar", "16000", "-ac", "1", "-f", "s16le", "-"
        ).redirectErrorStream(false).start()
        val input = process.inputStream
        return object : InputStream() {
            override fun read(): Int {
                if (isCancelled()) {
                    process.destroyForcibly()
                    return -1
                }
                return input.read()
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (isCancelled()) {
                    process.destroyForcibly()
                    return -1
                }
                return input.read(buffer, offset, length)
            }

            override fun close() {
                try {
                    input.close()
                } catch (ignored: Exception) {
                }
                process.destroy()
            }
        }
    }
}

/**
 * Runs Vosk over an audio file. Progress is reported as a fraction of the file.
 */
class AudioTranscriber(private val engine: RecognitionEngine) {
    private val json = Json { ignoreUnknownKeys = true }

    fun transcribe(
        modelPath: String,
        file: File,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean,
    ): Transcript {
        val model: Model = engine.loadModel(modelPath)
        val duration = AudioFileReader.durationSeconds(file)
        val lines = ArrayList<TranscriptLine>()
        var processedBytes = 0L
        var sentenceStartSeconds = 0.0

        AudioFileReader.openPcm(file, isCancelled).use { stream ->
            val recognizer = Recognizer(model, AudioFileReader.TARGET_RATE)
            try {
                val buffer = ByteArray(8000) // 250 ms
                while (!isCancelled()) {
                    val read = stream.read(buffer, 0, buffer.size)
                    if (read <= 0) break
                    processedBytes += read.toLong()
                    val position = processedBytes / BYTES_PER_SECOND
                    if (recognizer.acceptWaveForm(buffer, read)) {
                        val text = textOf(recognizer.result)
                        if (text.isNotEmpty()) {
                            lines.add(TranscriptLine(sentenceStartSeconds, position, text))
                            sentenceStartSeconds = position
                        }
                    }
                    if (duration > 0) {
                        onProgress((position / duration).toFloat().coerceIn(0f, 0.999f))
                    }
                }
                if (!isCancelled()) {
                    val text = textOf(recognizer.finalResult)
                    if (text.isNotEmpty()) {
                        lines.add(TranscriptLine(sentenceStartSeconds, processedBytes / BYTES_PER_SECOND, text))
                    }
                    onProgress(1f)
                }
            } finally {
                try {
                    recognizer.close()
                } catch (ignored: Exception) {
                }
            }
        }
        if (isCancelled()) throw TranscriptionException("cancelled")
        val realDuration = if (duration > 0) duration else processedBytes / BYTES_PER_SECOND
        return Transcript(file, lines, realDuration)
    }

    private fun textOf(result: String): String {
        return try {
            json.parseToJsonElement(result).jsonObject["text"]?.jsonPrimitive?.content
                ?.trim()
                .orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    /** A [File] friendly name for the transcript of an audio file. */
    fun suggestedTextFileName(file: File): String =
        file.nameWithoutExtension.ifBlank { "transcript" } + ".txt"

    companion object {
        private const val BYTES_PER_SECOND = 16000.0 * 2.0
    }
}
