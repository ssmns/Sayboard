package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.Model
import org.vosk.Recognizer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine

enum class RecognitionState { IDLE, LOADING, LISTENING, ERROR }

data class AudioInput(val name: String, val mixerInfo: Mixer.Info?)

/**
 * Owns the Vosk model and records from the microphone in a background thread.
 */
class RecognitionEngine {

    interface Listener {
        fun onState(state: RecognitionState)
        fun onPartial(text: String) = Unit
        fun onFinal(text: String) = Unit
        fun onError(message: String)
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private var model: Model? = null
    private var modelPath: String? = null

    @Volatile
    private var running = false

    @Volatile
    private var cancelled = false

    private var thread: Thread? = null

    var keepModelLoaded: Boolean = true

    val loadedModelPath: String?
        get() = synchronized(lock) { modelPath }

    val isRunning: Boolean
        get() = thread?.isAlive == true

    fun loadModel(path: String): Model = synchronized(lock) {
        model?.let { if (modelPath == path) return it }
        unloadLocked()
        org.vosk.LibVosk.setLogLevel(org.vosk.LogLevel.WARNINGS)
        val loaded = Model(path)
        model = loaded
        modelPath = path
        loaded
    }

    fun unloadModel() {
        synchronized(lock) { unloadLocked() }
    }

    private fun unloadLocked() {
        try {
            model?.close()
        } catch (ignored: Exception) {
        }
        model = null
        modelPath = null
    }

    fun start(path: String, deviceName: String, listener: Listener) {
        if (isRunning) return
        running = true
        cancelled = false
        thread = Thread({ run(path, deviceName, listener) }, "saytypist-recognition").apply {
            isDaemon = true
            start()
        }
    }

    /** Stops recording; the phrase in progress is still recognized. */
    fun stop() {
        running = false
        thread?.join(3000)
        thread = null
    }

    /** Stops recording and throws the phrase in progress away. */
    fun cancel() {
        cancelled = true
        stop()
    }

    private fun run(path: String, deviceName: String, listener: Listener) {
        var line: TargetDataLine? = null
        try {
            listener.onState(RecognitionState.LOADING)
            val model = loadModel(path)
            val recognizer = Recognizer(model, SAMPLE_RATE)
            val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false)
            line = openLine(format, deviceName)
            line.start()
            listener.onState(RecognitionState.LISTENING)

            val buffer = ByteArray(BLOCK_BYTES)
            while (running) {
                val read = try {
                    line.read(buffer, 0, buffer.size)
                } catch (e: Exception) {
                    break
                }
                if (read <= 0) continue
                if (recognizer.acceptWaveForm(buffer, read)) {
                    emitFinal(recognizer.result, listener)
                } else {
                    listener.onPartial(parse(recognizer.partialResult, "partial"))
                }
            }
            if (!cancelled) {
                emitFinal(recognizer.finalResult, listener)
            }
            recognizer.close()
        } catch (e: Exception) {
            listener.onError("${e.javaClass.simpleName}: ${e.message}")
            listener.onState(RecognitionState.ERROR)
        } finally {
            running = false
            try {
                line?.stop()
                line?.close()
            } catch (ignored: Exception) {
            }
            if (!keepModelLoaded) unloadModel()
            listener.onState(RecognitionState.IDLE)
        }
    }

    private fun emitFinal(result: String, listener: Listener) {
        val text = parse(result, "text").trim()
        if (text.isNotEmpty()) listener.onFinal(text)
    }

    private fun parse(result: String, key: String): String {
        return try {
            json.parseToJsonElement(result).jsonObject[key]?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun openLine(format: AudioFormat, deviceName: String): TargetDataLine {
        val info = DataLine.Info(TargetDataLine::class.java, format)
        if (deviceName.isNotBlank()) {
            for (mixerInfo in AudioSystem.getMixerInfo()) {
                if (mixerInfo.name != deviceName) continue
                val mixer = AudioSystem.getMixer(mixerInfo)
                if (mixer.isLineSupported(info)) {
                    val line = mixer.getLine(info) as TargetDataLine
                    line.open(format, BLOCK_BYTES * 8)
                    return line
                }
            }
        }
        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format, BLOCK_BYTES * 8)
        return line
    }

    companion object {
        const val SAMPLE_RATE = 16000f
        private const val BLOCK_BYTES = 8000 // 250 ms of 16 kHz mono 16 bit audio

        fun inputDevices(): List<AudioInput> {
            val format = AudioFormat(SAMPLE_RATE, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            val devices = mutableListOf(AudioInput("Default microphone", null))
            try {
                for (mixerInfo in AudioSystem.getMixerInfo()) {
                    val mixer = try {
                        AudioSystem.getMixer(mixerInfo)
                    } catch (e: Exception) {
                        continue
                    }
                    if (mixer.isLineSupported(info)) {
                        devices.add(AudioInput(mixerInfo.name, mixerInfo))
                    }
                }
            } catch (e: Exception) {
                // no audio system at all
            }
            return devices
        }
    }
}
