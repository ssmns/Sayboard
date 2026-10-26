package com.elishaazaria.sayboard.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Real end-to-end transcription of an audio file. Needs a model and an audio
 * file, so it only runs when they are given:
 *
 * ```
 * SAYTYPIST_TEST_MODEL=/path/vosk-model-small-en-us-0.15 \
 * SAYTYPIST_TEST_AUDIO=/path/speech.mp3 \
 * ./gradlew :desktop:test --tests '*TranscriptionIntegrationTest*'
 * ```
 */
class TranscriptionIntegrationTest {

    @Test
    fun `an audio file is turned into text`() {
        val modelPath = System.getenv("SAYTYPIST_TEST_MODEL")
        val audioPath = System.getenv("SAYTYPIST_TEST_AUDIO")
        assumeTrue(!modelPath.isNullOrBlank() && !audioPath.isNullOrBlank(), "no model/audio given")

        val file = File(audioPath)
        assertTrue(file.isFile, "missing audio file $audioPath")
        val transcriber = AudioTranscriber(RecognitionEngine())
        var lastProgress = 0f
        val transcript = transcriber.transcribe(
            modelPath = modelPath!!,
            file = file,
            onProgress = { lastProgress = it },
            isCancelled = { false },
        )

        println("transcript: ${transcript.text}")
        println("sentences: ${transcript.lines.size}, words: ${transcript.wordCount}, " +
                "duration: ${transcript.durationSeconds}")
        assertTrue(transcript.lines.isNotEmpty(), "no text was recognized")
        assertTrue(lastProgress > 0.9f, "progress did not finish: $lastProgress")
        val text = transcript.text.lowercase()
        assertTrue(text.contains("test") || text.contains("transcription") || text.contains("hello"), text)
    }

    @Test
    fun `compressed files need ffmpeg which is detected`() {
        val mp3 = File(System.getenv("SAYTYPIST_TEST_AUDIO") ?: return)
        if (!mp3.isFile) return
        assumeTrue(mp3.extension.lowercase() == "mp3")
        assumeTrue(ClipboardTools.inPath("ffmpeg"))
        AudioFileReader.openPcm(mp3).use { stream ->
            val buffer = ByteArray(16000)
            assertTrue(stream.read(buffer) > 0, "ffmpeg produced no PCM data")
        }
    }
}
