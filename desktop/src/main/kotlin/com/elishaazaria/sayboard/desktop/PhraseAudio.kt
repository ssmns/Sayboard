package com.elishaazaria.sayboard.desktop

import java.io.File

/**
 * Cuts the silence at the beginning and the end of a captured phrase, so the
 * dataset segments start and end with the voice.
 */
object SilenceTrimmer {
    const val SAMPLE_RATE = 16000
    private const val FRAME_MS = 20

    /**
     * Returns the byte range (inclusive start, exclusive end) of the part that
     * contains speech, or an empty range when the buffer is silent.
     */
    fun speechRange(
        pcm: ByteArray,
        length: Int = pcm.size,
        thresholdDbfs: Double = -45.0,
        paddingMs: Int = 150,
    ): IntRange {
        if (length < 2) return 0 until 0
        val frameSamples = SAMPLE_RATE * FRAME_MS / 1000
        val frameBytes = frameSamples * 2
        val totalFrames = length / frameBytes
        var firstFrame = -1
        var lastFrame = -1
        for (frame in 0 until totalFrames) {
            val offset = frame * frameBytes
            var sum = 0.0
            var index = offset
            while (index < offset + frameBytes) {
                val sample = ((pcm[index].toInt() and 0xFF) or (pcm[index + 1].toInt() shl 8)).toShort()
                sum += sample.toDouble() * sample
                index += 2
            }
            val rms = kotlin.math.sqrt(sum / frameSamples)
            val dbfs = if (rms <= 0.0) -100.0 else 20 * kotlin.math.log10(rms / 32768.0)
            if (dbfs > thresholdDbfs) {
                if (firstFrame < 0) firstFrame = frame
                lastFrame = frame
            }
        }
        if (firstFrame < 0) return 0 until 0
        val padFrames = (paddingMs / FRAME_MS).coerceAtLeast(1)
        val from = ((firstFrame - padFrames).coerceAtLeast(0) * frameBytes)
        val to = (((lastFrame + padFrames + 1) * frameBytes)).coerceAtMost(length)
        return from until to
    }
}

/** Writes 16 kHz mono 16 bit PCM bytes as a small WAV file. */
fun writePcmWav(target: File, pcm: ByteArray, offset: Int = 0, length: Int = pcm.size - offset) {
    target.parentFile?.mkdirs()
    target.outputStream().buffered().use { output ->
        output.write(PcmWavWriter.header(length.toLong()))
        if (length > 0) output.write(pcm, offset, length)
        output.flush()
    }
}

/** Reads the PCM part of a canonical 16 kHz mono WAV file. */
fun readPcmWav(file: File): ByteArray {
    val bytes = file.readBytes()
    if (bytes.size <= PcmWavWriter.HEADER_SIZE) return ByteArray(0)
    return bytes.copyOfRange(PcmWavWriter.HEADER_SIZE.toInt(), bytes.size)
}
