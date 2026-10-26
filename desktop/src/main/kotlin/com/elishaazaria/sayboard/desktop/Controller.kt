package com.elishaazaria.sayboard.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * All the state the window shows, plus the glue between hotkey, recognition and
 * typing. Compose state can be written from any thread.
 */
class AppController {

    var config by mutableStateOf(ConfigStore.load())
        private set

    /** All window texts in the selected language. */
    var strings by mutableStateOf(Strings.of(resolveLang(config.language)))
        private set

    var status by mutableStateOf(RecognitionState.IDLE)
        private set

    var preview by mutableStateOf("")
        private set

    /** The last finished phrase, so it can be copied by hand at any time. */
    var lastPhrase by mutableStateOf("")
        private set

    var installed by mutableStateOf<List<InstalledModel>>(emptyList())
        private set

    val logLines = mutableStateListOf<String>()

    var downloadLabel by mutableStateOf("")
        private set

    var downloadFraction by mutableStateOf<Float?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    var typingMethod by mutableStateOf("")
        private set

    var diagnostics by mutableStateOf<List<DiagnosticCheck>>(emptyList())
        private set

    /** True while the "what do I need to install" dialog is open. */
    var diagnosticsVisible by mutableStateOf(false)
        private set

    private var hotkeyError: String? = null

    // -------------------------------------------------------------- transcription
    var transcriptionFile by mutableStateOf<File?>(null)
        private set

    var transcriptionProgress by mutableStateOf<Float?>(null)
        private set

    var transcriptionRunning by mutableStateOf(false)
        private set

    var transcript by mutableStateOf<Transcript?>(null)
        private set

    var transcriptionError by mutableStateOf<String?>(null)
        private set

    var transcriptionUsesFfmpeg by mutableStateOf(false)
        private set

    private var transcriptionCancelled = false
    private val transcriber by lazy { AudioTranscriber(engine) }

    /** Result of the last "test the AI connection" run. */
    var aiStatus by mutableStateOf<String?>(null)
        private set

    /** True while the transcript is being polished by the AI. */
    var aiWorking by mutableStateOf(false)
        private set

    var datasetExporting by mutableStateOf(false)
        private set

    var datasetProgress by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine = RecognitionEngine()
    private val hotkeys = GlobalHotkeys()
    private var typer: Typer = Typers.create(config.typingBackend)
    private var renderer: TextRenderer = createRenderer()
    private var job: Job? = null
    private var cancelRequested = false

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

    private fun resolveLang(code: String): Lang = Lang.fromCode(code) ?: Lang.detect()

    init {
        AppPaths.setModelsDirOverride(config.modelsDir)
        refreshInstalled()
        engine.keepModelLoaded = config.keepModelLoaded
        typingMethod = typer.name
        log("Typing method: ${typer.name} · session: ${Session.summary}")
        hotkeys.onToggle = { toggleDictation() }
        hotkeys.onCancel = { cancelDictation() }
        hotkeys.isListening = { engine.isRunning }
        startHotkey()
        refreshDiagnostics()
        // Tell the user right away (instead of only in a terminal) when
        // something has to be installed before dictation can work.
        diagnosticsVisible = !config.requirementsSeen ||
                diagnostics.any { it.state == CheckState.ERROR }
        if (installed.isEmpty()) {
            log("No model installed yet. Download one in the Models tab " +
                    "(vosk-model-small-fa-0.5 for Persian, vosk-model-small-en-us-0.15 for English).")
        } else if (config.model.isBlank() || ModelStore.installed(config.model) == null) {
            setActiveModel(installed.first().name)
        }
    }

    private fun createRenderer(): TextRenderer {
        return TextRenderer(
            typer = typer,
            live = config.livePartial,
            trailingSpace = config.trailingSpace,
            capitalize = config.capitalize,
            onError = { message -> log(message) },
        )
    }

    private fun copyToClipboard(text: String) {
        if (ClipboardTools.set(text)) {
            log("Phrase copied to the clipboard")
        } else {
            log("Could not copy the phrase to the clipboard")
        }
    }

    /** Copies the last phrase again - works even if typing is unavailable. */
    fun copyLastPhrase() {
        val text = if (lastPhrase.isNotBlank()) lastPhrase else preview.trim()
        if (text.isBlank()) {
            log("Nothing to copy yet - dictate something first.")
            return
        }
        copyToClipboard(text)
    }

    private fun saveConfig() {
        if (!ConfigStore.save(config)) {
            log("Could not save the settings to ${AppPaths.configFile()} - " +
                    "check that the folder is writable.")
        }
    }

    fun log(message: String) {
        val line = "${LocalTime.now().format(timeFormat)}  $message"
        logLines.add(line)
        if (logLines.size > 300) logLines.removeAt(0)
        println(line)
    }

    // ------------------------------------------------------------------ hotkey

    private fun startHotkey() {
        try {
            hotkeys.start(config.hotkey)
            hotkeyError = null
        } catch (e: Exception) {
            hotkeyError = e.message ?: e.toString()
            log("Global hotkey (${config.hotkey}) is not available: ${e.message}")
        }
    }

    fun refreshDiagnostics() {
        diagnostics = Diagnostics.run(config, hotkeyError, typer.name, strings)
    }

    /** Switches the window language (saved immediately). */
    fun setLanguage(lang: Lang) {
        config = config.copy(language = lang.code)
        strings = Strings.of(lang)
        saveConfig()
        refreshDiagnostics()
    }

    fun toggleLog() {
        config = config.copy(logVisible = !config.logVisible)
        saveConfig()
    }

    fun modelsFolder(): File = AppPaths.modelsDir()

    fun totalModelsSize(): String {
        val total = installed.sumOf { it.sizeBytes }
        return formatBytes(total)
    }

    /**
     * Uses a different folder for the models. Existing models can be moved there
     * in one go (they are large, so it happens in the background).
     */
    fun changeModelsFolder(folder: File, moveExisting: Boolean) {
        val oldFolder = AppPaths.modelsDir()
        if (!folder.exists() && !folder.mkdirs()) {
            log(strings.f("settings.invalidFolder", folder.absolutePath))
            return
        }
        if (!folder.canWrite()) {
            log(strings.f("settings.invalidFolder", folder.absolutePath))
            return
        }
        val models = installed
        if (moveExisting && models.isNotEmpty() && oldFolder.absolutePath != folder.absolutePath) {
            if (job?.isActive == true) {
                log("Wait for the current task to finish.")
                return
            }
            busy = true
            downloadFraction = null
            downloadLabel = strings["progress.starting"]
            job = scope.launch {
                var moved = 0
                for (model in models) {
                    try {
                        ModelStore.moveModel(model, folder)
                        moved++
                        downloadLabel = "$moved/${models.size}"
                    } catch (e: Exception) {
                        log(strings.f("settings.moveFailed", model.name))
                    }
                }
                log(strings.f("settings.moved", moved, folder.absolutePath))
                useModelsFolder(folder)
                busy = false
                downloadFraction = null
                downloadLabel = ""
            }
            return
        }
        useModelsFolder(folder)
    }

    private fun useModelsFolder(folder: File) {
        if (engine.isRunning) engine.stop()
        engine.unloadModel()
        AppPaths.setModelsDirOverride(folder.absolutePath)
        config = config.copy(modelsDir = folder.absolutePath)
        saveConfig()
        refreshInstalled()
        if (config.model.isNotBlank() && ModelStore.installed(config.model) == null) {
            config = config.copy(model = installed.firstOrNull()?.name ?: "")
            saveConfig()
        }
        refreshDiagnostics()
        log(strings.f("settings.folderChanged", folder.absolutePath))
    }



    fun openDiagnostics() {
        refreshDiagnostics()
        diagnosticsVisible = true
    }

    fun closeDiagnostics() {
        diagnosticsVisible = false
        if (!config.requirementsSeen) {
            config = config.copy(requirementsSeen = true)
            saveConfig()
        }
    }

    // --------------------------------------------------------------- dictation

    fun toggleDictation() {
        if (engine.isRunning) stopDictation() else startDictation()
    }

    private fun startDictation() {
        val modelName = config.model
        if (modelName.isBlank()) {
            log("Choose a model in the Models tab before dictating.")
            return
        }
        val model = ModelStore.installed(modelName)
        if (model == null) {
            log("$modelName is not installed any more.")
            refreshInstalled()
            return
        }
        renderer.reset()
        engine.keepModelLoaded = config.keepModelLoaded
        engine.start(model.directory.absolutePath, config.device, listener)
    }

    private fun stopDictation() {
        engine.stop()
    }

    private fun cancelDictation() {
        if (!engine.isRunning) return
        renderer.cancel()
        engine.cancel()
    }

    private val listener = object : RecognitionEngine.Listener {
        override fun onState(state: RecognitionState) {
            status = state
        }

        override fun onPartial(text: String) {
            renderer.partial(text)
            if (config.showPreview) preview = text
        }

        override fun onFinal(text: String) {
            val ai = config.ai
            if (ai.isUsable() && ai.correctDictation) {
                // the local model already produced a phrase: polish it first, then type
                lastPhrase = text
                if (config.showPreview) preview = text
                aiWorking = true
                scope.launch {
                    val corrected = correctWithAi(text)
                    aiWorking = false
                    renderer.final(corrected)
                    lastPhrase = corrected
                    if (config.copyToClipboard) copyToClipboard(corrected)
                    if (config.showPreview) preview = corrected
                }
                return
            }
            renderer.final(text)
            lastPhrase = text
            if (config.copyToClipboard) copyToClipboard(text)
            if (config.showPreview) preview = text
        }

        override fun onError(message: String) {
            log("Recognition error: $message")
        }
    }

    // ------------------------------------------------------------------ models

    fun refreshInstalled() {
        installed = ModelStore.listInstalled()
    }

    fun setActiveModel(name: String) {
        if (engine.isRunning) engine.stop()
        engine.unloadModel()
        config = config.copy(model = name)
        saveConfig()
        refreshInstalled()
        refreshDiagnostics()
        log("Active model: $name")
    }

    fun deleteModel(name: String) {
        if (engine.isRunning) engine.stop()
        engine.unloadModel()
        ModelStore.delete(name)
        refreshInstalled()
        if (config.model == name) {
            val remaining = ModelStore.listInstalled()
            config = config.copy(model = remaining.firstOrNull()?.name ?: "")
            saveConfig()
        }
        log("Deleted $name")
    }

    fun downloadModel(model: CatalogModel) {
        if (job?.isActive == true) {
            log("A download is already running.")
            return
        }
        cancelRequested = false
        busy = true
        downloadFraction = 0f
        downloadLabel = "Starting…"
        log("Downloading ${model.id} (${model.sizeText})")
        job = scope.launch {
            try {
                val destination = ModelStore.install(
                    model = model,
                    onDownload = { done, total ->
                        downloadFraction = if (total > 0) (done.toFloat() / total) else null
                        downloadLabel = if (total > 0) {
                            "Downloading ${formatBytes(done)} / ${formatBytes(total)}"
                        } else {
                            "Downloading ${formatBytes(done)}"
                        }
                    },
                    onExtract = { done, total ->
                        downloadFraction = if (total > 0) (done.toFloat() / total) else null
                        downloadLabel = "Extracting $done/$total files"
                    },
                    isCancelled = { cancelRequested },
                )
                log("Installed ${destination.name}")
                refreshInstalled()
                refreshDiagnostics()
                if (config.model.isBlank()) setActiveModel(destination.name)
            } catch (e: CancelledException) {
                log("Download cancelled")
            } catch (e: Exception) {
                log("Download failed: ${e.message}")
            } finally {
                busy = false
                downloadFraction = null
                downloadLabel = ""
            }
        }
    }

    fun cancelDownload() {
        if (job?.isActive == true) {
            cancelRequested = true
            downloadLabel = "Cancelling…"
        }
    }

    fun importZip(file: File) {
        if (job?.isActive == true) {
            log("Wait for the current task to finish.")
            return
        }
        cancelRequested = false
        busy = true
        downloadFraction = 0f
        downloadLabel = "Importing…"
        log("Importing ${file.name}")
        job = scope.launch {
            try {
                val destination = ModelStore.importZip(
                    archive = file,
                    onProgress = { done, total ->
                        downloadFraction = if (total > 0) done.toFloat() / total else null
                        downloadLabel = "Extracting $done/$total files"
                    },
                    isCancelled = { cancelRequested },
                )
                log("Imported ${destination.name}")
                refreshInstalled()
                refreshDiagnostics()
                if (config.model.isBlank()) setActiveModel(destination.name)
            } catch (e: CancelledException) {
                log("Import cancelled")
            } catch (e: Exception) {
                log("Import failed: ${e.message}")
            } finally {
                busy = false
                downloadFraction = null
                downloadLabel = ""
            }
        }
    }

    fun exportZip(model: InstalledModel) {
        if (job?.isActive == true) {
            log("Wait for the current task to finish.")
            return
        }
        cancelRequested = false
        busy = true
        downloadFraction = 0f
        job = scope.launch {
            try {
                val zip = ModelStore.exportZip(
                    model = model,
                    onProgress = { done, total ->
                        downloadFraction = if (total > 0) done.toFloat() / total else null
                        downloadLabel = "Packing ${formatBytes(done)} / ${formatBytes(total)}"
                    },
                    isCancelled = { cancelRequested },
                )
                log("Model ready to share: ${zip.absolutePath}")
            } catch (e: CancelledException) {
                log("Export cancelled")
            } catch (e: Exception) {
                log("Export failed: ${e.message}")
            } finally {
                busy = false
                downloadFraction = null
                downloadLabel = ""
            }
        }
    }

    // ---------------------------------------------------------------- settings

    fun applySettings(newConfig: AppConfig) {
        HotkeyParser.parse(newConfig.hotkey) // throws on a bad combination
        strings = Strings.of(resolveLang(newConfig.language))
        try {
            hotkeys.start(newConfig.hotkey)
            hotkeyError = null
        } catch (e: Exception) {
            hotkeyError = e.message ?: e.toString()
            log("Global hotkey (${newConfig.hotkey}) is not available: ${e.message}")
        }
        config = newConfig
        saveConfig()
        typer = Typers.create(newConfig.typingBackend)
        renderer = TextRenderer(
            typer = typer,
            live = newConfig.livePartial,
            trailingSpace = newConfig.trailingSpace,
            capitalize = newConfig.capitalize,
            onError = { message -> log(message) },
        )
        typingMethod = typer.name
        refreshDiagnostics()
        log("Settings saved · typing method: ${typer.name}")
    }

    fun testTyping() {
        try {
            typer.typeText("سلام دنیا hello 123")
        } catch (e: Exception) {
            log("Test failed: ${e.message}")
        }
    }

    fun unloadModel() {
        if (engine.isRunning) engine.stop()
        engine.unloadModel()
        log("Model unloaded from memory")
    }

    // ------------------------------------------------------------ transcription

    fun chooseTranscriptionFile(file: File) {
        if (transcriptionRunning) return
        transcriptionFile = file
        transcript = null
        transcriptionError = null
        transcriptionUsesFfmpeg = !AudioFileReader.isWav(file)
        log("Audio file: ${file.name} (${formatBytes(file.length())})")
        if (!AudioFileReader.canRead(file)) {
            transcriptionError = strings.f("transcribe.ffmpeg", strings["transcribe.ffmpegHint"])
        }
    }

    fun startTranscription() {
        val file = transcriptionFile ?: return
        if (transcriptionRunning) return
        val modelName = config.model
        val installed = modelName.takeIf { it.isNotBlank() }?.let { ModelStore.installed(it) }
        if (installed == null) {
            transcriptionError = strings["transcribe.noModel"]
            return
        }
        transcriptionCancelled = false
        transcriptionRunning = true
        transcriptionProgress = null
        transcriptionError = null
        transcript = null
        log("Transcribing ${file.name} with ${installed.name}")
        scope.launch {
            try {
                val result = transcriber.transcribe(
                    modelPath = installed.directory.absolutePath,
                    file = file,
                    onProgress = { value -> transcriptionProgress = value },
                    isCancelled = { transcriptionCancelled },
                )
                val polished = if (config.ai.isUsable() && config.ai.correctTranscripts) {
                    correctTranscript(result)
                } else {
                    result
                }
                transcript = polished
                transcriptionProgress = 1f
                log("Transcribed ${result.file.name}: ${polished.lines.size} sentences")
            } catch (e: TranscriptionException) {
                if (transcriptionCancelled) {
                    log("Transcription cancelled")
                } else {
                    transcriptionError = strings.f("transcribe.failed", e.message ?: "")
                    log("Transcription failed: ${e.message}")
                }
            } catch (e: Exception) {
                transcriptionError = strings.f("transcribe.failed", e.message ?: e.javaClass.simpleName)
                log("Transcription failed: ${e.message}")
            } finally {
                transcriptionRunning = false
                if (transcriptionCancelled) transcriptionProgress = null
            }
        }
    }

    /** Runs every sentence through the AI, keeping the original text next to it. */
    private fun correctTranscript(result: Transcript): Transcript {
        aiWorking = true
        log("Polishing ${result.lines.size} sentences with ${config.ai.providerType.label}")
        val client = AiClient(config.ai)
        val lines = ArrayList<TranscriptLine>(result.lines.size)
        result.lines.forEachIndexed { index, line ->
            transcriptionProgress = (index.toFloat() / result.lines.size).coerceAtMost(0.99f)
            val corrected = try {
                client.correct(line.rawText)
            } catch (e: Exception) {
                log("AI correction stopped at sentence ${index + 1}: ${e.message}")
                line.rawText
            }
            lines.add(line.copy(text = corrected, rawText = line.rawText))
        }
        aiWorking = false
        return result.copy(lines = lines)
    }

    fun cancelTranscription() {
        transcriptionCancelled = true
        log("Cancelling the transcription…")
    }

    fun copyTranscript(withTimecodes: Boolean) {
        val result = transcript ?: return
        val text = if (withTimecodes) result.textWithTimecodes() else result.text
        copyToClipboard(text)
    }

    fun saveTranscript(target: File): Boolean {
        val result = transcript ?: return false
        return try {
            target.writeText(result.text + "\n")
            log(strings.f("transcribe.saved", target.absolutePath))
            true
        } catch (e: Exception) {
            log(strings.f("transcribe.failed", e.message ?: ""))
            false
        }
    }

    /** Sends one phrase through the configured AI service, falling back to the raw text. */
    private fun correctWithAi(text: String): String {
        val ai = config.ai
        return try {
            val started = System.currentTimeMillis()
            val corrected = AiClient(ai).correct(text)
            log("AI correction (${System.currentTimeMillis() - started} ms): $corrected")
            corrected
        } catch (e: Exception) {
            log("AI correction failed, keeping the original text: ${e.message}")
            text
        }
    }

    fun testAi(settings: AiSettings) {
        aiStatus = "…"
        scope.launch {
            aiStatus = try {
                val answer = AiClient(settings.copy(enabled = true)).testConnection()
                log("AI connection OK: ${answer.take(60)}")
                answer
            } catch (e: Exception) {
                log("AI connection failed: ${e.message}")
                "✖ ${e.message}"
            }
        }
    }

    fun clearAiStatus() {
        aiStatus = null
    }

    fun exportDataset(
        folder: File,
        options: DatasetExporter.Options,
        modelName: String,
    ) {
        val result = transcript ?: return
        if (datasetExporting) return
        datasetExporting = true
        datasetProgress = "0/${result.lines.size}"
        log("Exporting dataset to ${folder.absolutePath}")
        scope.launch {
            try {
                val exported = DatasetExporter.export(
                    transcript = result,
                    folder = folder,
                    options = options,
                    modelName = modelName,
                    onProgress = { done, total -> datasetProgress = "$done/$total" },
                    isCancelled = { false },
                )
                log(
                    "Dataset written: ${exported.segments} segments " +
                            "(${formatTimecode(exported.totalSeconds)}) in ${exported.folder.absolutePath}"
                )
            } catch (e: Exception) {
                log("Dataset export failed: ${e.message}")
            } finally {
                datasetExporting = false
                datasetProgress = null
            }
        }
    }

    fun suggestedTranscriptName(): String {
        val file = transcriptionFile ?: return "transcript.txt"
        return if (AudioFileReader.isWav(file)) {
            file.nameWithoutExtension.ifBlank { "transcript" } + ".txt"
        } else {
            file.nameWithoutExtension.ifBlank { "transcript" } + ".txt"
        }
    }

    // -------------------------------------------------------------------- misc

    fun shutdown() {
        try {
            engine.cancel()
        } catch (ignored: Exception) {
        }
        hotkeys.shutdown()
        engine.unloadModel()
        saveConfig()
    }

    fun requiredSpaceMb(model: CatalogModel): Long = (model.sizeMb * 2.1).toLong() + 100
}
