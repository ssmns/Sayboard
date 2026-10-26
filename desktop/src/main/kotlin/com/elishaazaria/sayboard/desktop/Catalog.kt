package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * The same model catalog the Android app offers, generated from
 * `catalog/vosk_models.json` into `src/main/resources/vosk_models.json`.
 */
@Serializable
private data class CatalogFile(
    val tiers: List<TierDefinition> = emptyList(),
    val models: List<ModelDefinition> = emptyList(),
)

@Serializable
private data class TierDefinition(
    val id: String,
    @SerialName("max_size_mb") val maxSizeMb: Int,
    @SerialName("ram_mb") val ramMb: Int,
)

@Serializable
private data class ModelDefinition(
    val id: String,
    val lang: String,
    val locale: String,
    @SerialName("size_mb") val sizeMb: Int,
    val license: String = "",
    val notes: String = "",
)

data class ModelTier(val id: String, val maxSizeMb: Int, val ramMb: Int) {
    val title: String
        get() = when (id) {
            "SMALL" -> "lightweight"
            "MEDIUM" -> "medium"
            "LARGE" -> "large"
            else -> "very large"
        }
}

data class CatalogModel(
    val id: String,
    val lang: String,
    val localeTag: String,
    val sizeMb: Int,
    val license: String,
    val notes: String,
    val tier: ModelTier,
) {
    val locale: Locale
        get() = Locale.forLanguageTag(localeTag)

    val url: String
        get() = "https://alphacephei.com/vosk/models/$id.zip"

    val sizeText: String
        get() = formatSizeMb(sizeMb.toLong())

    val isHeavy: Boolean
        get() = tier.id != "SMALL"
}

object ModelCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    val models: List<CatalogModel> by lazy { load() }

    val languages: List<String> by lazy { models.map { it.lang }.distinct().sorted() }

    fun byId(id: String): CatalogModel? = models.firstOrNull { it.id == id }

    private fun load(): List<CatalogModel> {
        val stream = ModelCatalog::class.java.getResourceAsStream("/vosk_models.json")
            ?: error("vosk_models.json is missing from the resources (run catalog/generate.py)")
        val text = stream.bufferedReader().use { it.readText() }
        val file = json.decodeFromString<CatalogFile>(text)
        val tiers = file.tiers.map { ModelTier(it.id, it.maxSizeMb, it.ramMb) }
        return file.models.map { definition ->
            CatalogModel(
                id = definition.id,
                lang = definition.lang,
                localeTag = definition.locale,
                sizeMb = definition.sizeMb,
                license = definition.license,
                notes = definition.notes,
                tier = tiers.first { definition.sizeMb <= it.maxSizeMb },
            )
        }
    }
}

fun formatSizeMb(sizeMb: Long): String {
    return if (sizeMb >= 1000) {
        String.format(Locale.US, "%.1f GB", sizeMb / 1000.0)
    } else {
        String.format(Locale.US, "%d MB", sizeMb)
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L ->
            String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))

        bytes >= 1024L * 1024L -> String.format(Locale.US, "%d MB", bytes / (1024L * 1024L))
        else -> String.format(Locale.US, "%d KB", bytes / 1024L)
    }
}
