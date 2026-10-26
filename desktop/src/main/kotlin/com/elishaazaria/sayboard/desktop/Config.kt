package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Serializable
data class AppConfig(
    val hotkey: String = "ctrl+alt+space",
    val model: String = "",
    val livePartial: Boolean = false,
    val device: String = "",
    val typingBackend: String = "auto",
    val trailingSpace: Boolean = true,
    val capitalize: Boolean = true,
    val keepModelLoaded: Boolean = true,
    val showPreview: Boolean = true,
    /** Also put every recognized phrase in the clipboard (so nothing is ever lost). */
    val copyToClipboard: Boolean = true,
    val startMinimized: Boolean = false,
    /** "en" or "fa"; empty means "follow the system language". */
    val language: String = "",
    /** Custom models folder; empty means the default data folder. */
    val modelsDir: String = "",
    /** The log pane is expanded. */
    val logVisible: Boolean = false,
    /** The "what do I need to install" dialog has been shown once. */
    val requirementsSeen: Boolean = false,
)

object AppPaths {
    val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")
    val isLinux: Boolean = System.getProperty("os.name").lowercase().contains("linux")

    private fun home(): Path = Paths.get(System.getProperty("user.home"))

    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    /** Folder name of the current release. */
    private const val FOLDER = "sayTypist"

    /** Folder name used by earlier builds; its content is moved once. */
    private const val LEGACY_FOLDER = "sayboard"

    private val configDirPath: Path by lazy {
        val base = if (isWindows) {
            env("APPDATA")?.let { Paths.get(it) } ?: home().resolve("AppData/Roaming")
        } else {
            env("XDG_CONFIG_HOME")?.let { Paths.get(it) } ?: home().resolve(".config")
        }
        reuseLegacy(base, FOLDER, LEGACY_FOLDER)
    }

    private val dataDirPath: Path by lazy {
        val base = if (isWindows) {
            env("LOCALAPPDATA")?.let { Paths.get(it) } ?: home().resolve("AppData/Local")
        } else {
            env("XDG_DATA_HOME")?.let { Paths.get(it) } ?: home().resolve(".local/share")
        }
        reuseLegacy(base, FOLDER, LEGACY_FOLDER)
    }

    /**
     * Renames the folder of an older build (models, settings) instead of making
     * the user download gigabytes again. If the rename is not possible the old
     * folder keeps being used, so nothing is lost.
     */
    private fun reuseLegacy(base: Path, name: String, legacy: String): Path {
        val target = base.resolve(name)
        val old = base.resolve(legacy)
        if (!Files.exists(target) && Files.exists(old)) {
            try {
                Files.move(old, target)
                return target
            } catch (e: Exception) {
                System.err.println("Could not rename $old to $target: ${e.message}")
                return old
            }
        }
        return target
    }

    fun configDir(): Path = configDirPath

    fun dataDir(): Path = dataDirPath

    @Volatile
    private var modelsDirOverride: String? = null

    /** Uses a different models folder (see the "Models folder" setting). */
    fun setModelsDirOverride(path: String?) {
        modelsDirOverride = path?.takeIf { it.isNotBlank() }
    }

    fun defaultModelsDir(): File = dataDir().resolve("models").toFile()

    fun modelsDir(): File {
        val dir = modelsDirOverride?.let { File(it) } ?: defaultModelsDir()
        dir.mkdirs()
        return dir
    }

    fun downloadsDir(): File {
        val dir = dataDir().resolve("downloads").toFile()
        dir.mkdirs()
        return dir
    }

    fun configFile(): File = configDir().resolve("config.json").toFile()

    fun freeSpaceMb(): Long {
        return try {
            Files.getFileStore(modelsDir().toPath()).usableSpace / (1024 * 1024)
        } catch (e: Exception) {
            -1
        }
    }
}

object ConfigStore {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun load(): AppConfig {
        val file = AppPaths.configFile()
        if (!file.isFile) return AppConfig()
        return try {
            json.decodeFromString<AppConfig>(file.readText())
        } catch (e: Exception) {
            AppConfig()
        }
    }

    /**
     * Saves the settings. Returns false when the file could not be written, so
     * the window can show the problem instead of silently losing the settings.
     */
    fun save(config: AppConfig): Boolean {
        return try {
            val file = AppPaths.configFile()
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(AppConfig.serializer(), config))
            true
        } catch (e: Exception) {
            System.err.println("Could not save the settings to ${AppPaths.configFile()}: ${e.message}")
            false
        }
    }
}
