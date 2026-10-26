package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

/** One segment of a dataset folder, as shown in the dataset tab. */
data class DatasetSegment(
    val index: Int,
    val wav: File,
    val text: String,
    val rawText: String,
    val startSeconds: Double,
    val endSeconds: Double,
    val durationSeconds: Double,
)

/**
 * Reads and edits the dataset folders that sayTypist writes (and datasets made
 * with the same layout: `wavs/NNNN.wav`, `metadata.tsv` / `metadata.csv`).
 */
object DatasetStore {
    private val namePattern = Regex("^([0-9]{4,})\\.wav$", RegexOption.IGNORE_CASE)

    fun load(folder: File): List<DatasetSegment> {
        val wavsDir = File(folder, "wavs")
        if (!wavsDir.isDirectory) return emptyList()

        val tsv = readTsv(folder)
        val csv = readCsv(folder)
        val segments = ArrayList<DatasetSegment>()
        wavsDir.listFiles()?.forEach { wav ->
            val match = namePattern.find(wav.name) ?: return@forEach
            val index = match.groupValues[1].toInt()
            val row = tsv[index]
            val text = row?.text ?: csv[index] ?: ""
            segments.add(
                DatasetSegment(
                    index = index,
                    wav = wav,
                    text = text,
                    rawText = row?.rawText ?: text,
                    startSeconds = row?.start ?: 0.0,
                    endSeconds = row?.end ?: 0.0,
                    durationSeconds = wavDuration(wav),
                )
            )
        }
        return segments.sortedBy { it.index }
    }

    /** Changes the text of one segment in both metadata files. */
    fun updateText(folder: File, index: Int, text: String) {
        val existing = load(folder).associateBy { it.index }
        if (!existing.containsKey(index)) throw IOException("segment $index does not exist")
        val updated = existing.mapValues { (key, segment) ->
            if (key == index) segment.copy(text = text.trim()) else segment
        }.values.sortedBy { it.index }
        writeMetadata(folder, updated)
    }

    /** Removes a segment (audio + metadata rows). */
    fun delete(folder: File, index: Int) {
        val segments = load(folder).filter { it.index != index }
        File(folder, "wavs").listFiles()
            ?.firstOrNull { namePattern.find(it.name)?.groupValues?.get(1)?.toInt() == index }
            ?.delete()
        writeMetadata(folder, segments)
    }

    private fun writeMetadata(folder: File, segments: List<DatasetSegment>) {
        val csv = StringBuilder()
        val tsv = StringBuilder("path\ttext\traw_text\tstart\tend\n")
        segments.forEach { segment ->
            val relative = "wavs/" + String.format(Locale.US, "%04d.wav", segment.index)
            csv.append(relative).append('|').append(segment.text.replace('|', ' ')).append('\n')
            tsv.append(relative).append('\t')
                .append(segment.text.replace('\t', ' ')).append('\t')
                .append(segment.rawText.replace('\t', ' ')).append('\t')
                .append(String.format(Locale.US, "%.3f", segment.startSeconds)).append('\t')
                .append(String.format(Locale.US, "%.3f", segment.endSeconds)).append('\n')
        }
        File(folder, "metadata.csv").writeText(csv.toString())
        File(folder, "metadata.tsv").writeText(tsv.toString())
        updateDatasetJson(folder, segments)
    }

    /** Keeps `dataset.json` in sync with the files that exist now. */
    private fun updateDatasetJson(folder: File, segments: List<DatasetSegment>) {
        val file = File(folder, "dataset.json")
        if (!file.isFile) return
        try {
            val root = Json.parseToJsonElement(file.readText()).jsonObject.toMutableMap()
            root["segments"] = JsonPrimitive(segments.size)
            root["total_seconds"] = JsonPrimitive(
                String.format(Locale.US, "%.2f", segments.sumOf { it.durationSeconds })
            )
            file.writeText(JsonObject(root).toString())
        } catch (ignored: Exception) {
        }
    }

    private data class Row(val text: String, val rawText: String, val start: Double, val end: Double)

    private fun readTsv(folder: File): Map<Int, Row> {
        val file = File(folder, "metadata.tsv")
        if (!file.isFile) return emptyMap()
        val rows = HashMap<Int, Row>()
        file.readLines().drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split('\t')
            if (parts.size < 2) return@forEach
            val index = indexOfPath(parts[0]) ?: return@forEach
            rows[index] = Row(
                text = parts[1],
                rawText = parts.getOrElse(2) { parts[1] },
                start = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                end = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
            )
        }
        return rows
    }

    private fun readCsv(folder: File): Map<Int, String> {
        val file = File(folder, "metadata.csv")
        if (!file.isFile) return emptyMap()
        val rows = HashMap<Int, String>()
        file.readLines().forEach { line ->
            val separator = line.indexOf('|')
            if (separator <= 0) return@forEach
            val path = line.substring(0, separator)
            val index = indexOfPath(path) ?: return@forEach
            rows[index] = line.substring(separator + 1)
        }
        return rows
    }

    private fun indexOfPath(path: String): Int? {
        val name = path.substringAfterLast('/')
        return namePattern.find(name)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun wavDuration(file: File): Double {
        return try {
            AudioSystem.getAudioInputStream(file).use { stream ->
                val frames = stream.frameLength
                val rate = stream.format.frameRate
                if (frames > 0 && rate > 0) frames / rate.toDouble() else 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}

/**
 * Plays one dataset segment at a time; the callback runs when playback ends so
 * the window can clear its "playing" indicator.
 */
class AudioPlayer {
    private var clip: Clip? = null

    @Volatile
    var playingFile: File? = null
        private set

    fun play(file: File, onFinished: (File) -> Unit) {
        stop()
        val newClip = try {
            AudioSystem.getClip().apply {
                open(AudioSystem.getAudioInputStream(file))
                addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) {
                        playingFile = null
                        onFinished(file)
                    }
                }
                start()
            }
        } catch (e: Exception) {
            throw IOException("could not play ${file.name}: ${e.message}", e)
        }
        clip = newClip
        playingFile = file
    }

    fun stop() {
        clip?.let {
            try {
                it.stop()
                it.close()
            } catch (ignored: Exception) {
            }
        }
        clip = null
        playingFile = null
    }
}
