package com.elishaazaria.sayboard.desktop.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Regenerates the Android `ModelLink.kt` / `ModelSizeTier.kt` and the desktop
 * copy of the catalog from `catalog/vosk_models.json`:
 *
 * ```
 * ./gradlew :desktop:generateCatalog
 * ```
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".").absoluteFile
    val catalogFile = File(root, "catalog/vosk_models.json")
    require(catalogFile.isFile) { "missing ${catalogFile.absolutePath}" }

    val source = catalogFile.readText()
    val rootJson = Json.parseToJsonElement(source).jsonObject
    val tiers = rootJson["tiers"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    val models = rootJson["models"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    require(models.isNotEmpty()) { "no models in the catalog" }
    require(tiers.isNotEmpty()) { "no tiers in the catalog" }

    val enumNames = HashSet<String>()
    val ids = HashSet<String>()
    for (model in models) {
        val id = text(model, "id")
        require(ids.add(id)) { "duplicate model id: $id" }
        val name = enumName(id)
        require(enumNames.add(name)) { "duplicate enum name: $name" }
        val locale = text(model, "locale")
        require(Regex("[a-z]{2,3}(-[A-Za-z0-9]{2,8})*").matches(locale)) {
            "bad locale $locale for $id"
        }
    }

    val dataDir = File(root, "app/src/main/java/com/elishaazaria/sayboard/data")
    dataDir.mkdirs()
    val modelLink = File(dataDir, "ModelLink.kt")
    val sizeTier = File(dataDir, "ModelSizeTier.kt")
    modelLink.writeText(buildModelLink(models))
    sizeTier.writeText(buildSizeTier(tiers))

    val desktopCopy = File(root, "desktop/src/main/resources/vosk_models.json")
    desktopCopy.parentFile.mkdirs()
    desktopCopy.writeText(source)

    println("wrote ${modelLink.relativeTo(root)} (${models.size} models)")
    println("wrote ${sizeTier.relativeTo(root)} (${tiers.size} tiers)")
    println("wrote ${desktopCopy.relativeTo(root)}")
}

private fun text(json: JsonObject, key: String): String =
    json[key]?.jsonPrimitive?.content ?: error("missing '$key' in $json")

private fun number(json: JsonObject, key: String): Int =
    json[key]?.jsonPrimitive?.intOrNull ?: error("missing '$key' in $json")

private fun kotlinString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun enumName(id: String): String {
    var name = id.replace(Regex("[^0-9A-Za-z]+"), "_").trim('_').uppercase()
    if (name.firstOrNull()?.isDigit() == true) name = "MODEL_$name"
    return name
}

private fun buildModelLink(models: List<JsonObject>): String {
    val header = """package com.elishaazaria.sayboard.data

import java.util.Locale

/**
 * Catalog of every speech recognition model offered by the built-in downloader.
 *
 * GENERATED FILE - do not edit by hand.
 * Edit `catalog/vosk_models.json` and run `./gradlew :desktop:generateCatalog` instead.
 */
enum class ModelLink(
    val link: String,
    val locale: Locale,
    val sizeMb: Int,
    val license: String,
    val notes: String,
) {
"""
    val blocks = ArrayList<String>()
    var currentLanguage: String? = null
    for (model in models) {
        val language = text(model, "lang")
        val block = StringBuilder()
        if (language != currentLanguage) {
            currentLanguage = language
            block.append("\n    // $language\n")
        }
        val id = text(model, "id")
        block.append("    ${enumName(id)}(\n")
        block.append("        \"https://alphacephei.com/vosk/models/$id.zip\",\n")
        block.append("        Locale.forLanguageTag(\"${text(model, "locale")}\"),\n")
        block.append("        ${number(model, "size_mb")},\n")
        block.append("        ${kotlinString(text(model, "license"))},\n")
        block.append("        ${kotlinString(text(model, "notes"))}\n")
        block.append("    )")
        blocks.add(block.toString())
    }

    return header + blocks.joinToString(",\n") + """
    ;

    val filename: String
        get() = link.substring(link.lastIndexOf('/') + 1, link.lastIndexOf('.'))

    /**
     * The folder the model is unzipped into. Official Vosk archives contain a
     * single top level folder with the same name as the archive itself.
     */
    val folderName: String
        get() = filename

    val sizeTier: ModelSizeTier
        get() = ModelSizeTier.forSizeMb(sizeMb)

    companion object {
        fun forFilename(filename: String): ModelLink? = values().firstOrNull { it.filename == filename }
    }
}
"""
}

private fun buildSizeTier(tiers: List<JsonObject>): String {
    val builder = StringBuilder()
    builder.append(
        """package com.elishaazaria.sayboard.data

/**
 * How heavy a model is, used to warn the user before downloading.
 *
 * GENERATED FILE - do not edit by hand.
 * Edit `catalog/vosk_models.json` and run `./gradlew :desktop:generateCatalog` instead.
 */
enum class ModelSizeTier(val maxSizeMb: Int, val recommendedRamMb: Int) {
"""
    )
    for (tier in tiers) {
        builder.append(
            "    ${text(tier, "id")}(${number(tier, "max_size_mb")}, ${number(tier, "ram_mb")}),\n"
        )
    }
    builder.append(
        """    ;

    companion object {
        fun forSizeMb(sizeMb: Int): ModelSizeTier =
            values().first { sizeMb <= it.maxSizeMb }
    }
}
"""
    )
    return builder.toString()
}
