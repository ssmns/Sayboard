package com.elishaazaria.sayboard.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiPayloadTest {

    private val settings = AiSettings(
        enabled = true,
        provider = AiProvider.OPENAI.id,
        baseUrl = "https://api.openai.com/v1",
        apiKey = "sk-test",
        model = "gpt-4o-mini",
    )

    @Test
    fun `openai body contains the model, system prompt and the text`() {
        val body = AiPayloads.openAi(settings, "fix it", "hello world")
        assertTrue(body.contains("\"model\":\"gpt-4o-mini\""), body)
        assertTrue(body.contains("\"role\":\"system\""), body)
        assertTrue(body.contains("fix it"), body)
        assertTrue(body.contains("hello world"), body)
        assertTrue(body.contains("\"temperature\":0"), body)
    }

    @Test
    fun `openai answer is parsed`() {
        val answer = AiPayloads.parseOpenAi(
            """{"choices":[{"message":{"role":"assistant","content":"Hello, world."}}]}"""
        )
        assertEquals("Hello, world.", answer)
    }

    @Test
    fun `anthropic and gemini bodies and answers`() {
        val anthropic = AiPayloads.anthropic(settings, "sys", "text")
        assertTrue(anthropic.contains("\"max_tokens\""), anthropic)
        assertTrue(anthropic.contains("\"system\":\"sys\""), anthropic)
        assertEquals(
            "Fixed.",
            AiPayloads.parseAnthropic("""{"content":[{"type":"text","text":"Fixed."}]}"""),
        )

        val gemini = AiPayloads.gemini(settings, "sys", "text")
        assertTrue(gemini.contains("systemInstruction"), gemini)
        assertTrue(gemini.contains("generationConfig"), gemini)
        assertEquals(
            "Fixed.",
            AiPayloads.parseGemini(
                """{"candidates":[{"content":{"parts":[{"text":"Fixed."}]}}]}"""
            ),
        )
    }

    @Test
    fun `local servers need no api key`() {
        val local = settings.copy(baseUrl = "http://localhost:11434/v1", apiKey = "")
        assertTrue(local.isLocalServer)
        assertTrue(local.isUsable())
        assertTrue(!settings.copy(apiKey = "").isUsable())
        assertTrue(!settings.copy(enabled = false).isUsable())
    }

    @Test
    fun `the prompt falls back to the built-in one`() {
        assertEquals(AiSettings.DEFAULT_PROMPT, settings.systemPrompt)
        assertEquals("my prompt", settings.copy(prompt = "my prompt").systemPrompt)
    }
}

class DatasetExportTest {

    @Test
    fun `segments are cut with their text and metadata`() {
        val dir = Files.createTempDirectory("saytypist-dataset-").toFile()
        val pcm = File(dir, "source.wav")
        // 3 seconds of silence at 16 kHz mono 16 bit
        PcmWavWriter(pcm).use { writer ->
            writer.write(ByteArray(16000 * 2 * 3), 16000 * 2 * 3)
        }
        val transcript = Transcript(
            file = File(dir, "source.mp3"),
            lines = listOf(
                TranscriptLine(0.2, 1.0, "Hello there.", rawText = "hello there"),
                TranscriptLine(1.2, 2.0, "Second sentence.", rawText = "second sentence"),
                TranscriptLine(2.9, 2.95, "too short"),
            ),
            durationSeconds = 3.0,
            pcmFile = pcm,
        )
        val target = File(dir, "dataset")
        val result = DatasetExporter.export(
            transcript = transcript,
            folder = target,
            options = DatasetExporter.Options(paddingMs = 50, useCorrected = true),
            modelName = "test-model",
        )

        assertEquals(2, result.segments)
        assertEquals(1, result.skipped)
        assertTrue(File(target, "wavs/0001.wav").isFile)
        assertTrue(File(target, "wavs/0002.wav").isFile)

        val csv = File(target, "metadata.csv").readText().trim().lines()
        assertEquals("wavs/0001.wav|Hello there.", csv[0])
        assertEquals("wavs/0002.wav|Second sentence.", csv[1])

        val tsv = File(target, "metadata.tsv").readText().lines()
        assertTrue(tsv[1].contains("hello there"), tsv[1])

        val json = File(target, "dataset.json").readText()
        assertTrue(json.contains("\"segments\":2"), json)
        assertTrue(json.contains("\"sample_rate\":16000"), json)
        assertTrue(json.contains("test-model"), json)

        // the wav is 16 kHz mono 16 bit with the requested padding
        val wav = File(target, "wavs/0001.wav")
        val header = wav.readBytes().copyOfRange(0, 44)
        assertEquals('R'.code.toByte(), header[0])
        val sampleRate = (header[24].toInt() and 0xFF) or ((header[25].toInt() and 0xFF) shl 8)
        assertEquals(16000, sampleRate)
        assertTrue(wav.length() > 44)

        // raw text is kept when the user exports the original one
        val rawTarget = File(dir, "raw")
        DatasetExporter.export(
            transcript, rawTarget, DatasetExporter.Options(paddingMs = 0, useCorrected = false)
        )
        assertEquals(
            "wavs/0001.wav|hello there",
            File(rawTarget, "metadata.csv").readText().trim().lines()[0],
        )

        dir.deleteRecursively()
    }

    @Test
    fun `the wav header is canonical`() {
        val header = PcmWavWriter.header(32000)
        assertEquals(44, header.size)
        assertEquals("RIFF", String(header, 0, 4))
        assertEquals("WAVE", String(header, 8, 4))
        assertEquals("data", String(header, 36, 4))
        val dataSize = (header[40].toInt() and 0xFF) or
                ((header[41].toInt() and 0xFF) shl 8) or
                ((header[42].toInt() and 0xFF) shl 16) or
                ((header[43].toInt() and 0xFF) shl 24)
        assertEquals(32000, dataSize)
    }
}
