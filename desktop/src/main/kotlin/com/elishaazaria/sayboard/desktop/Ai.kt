package com.elishaazaria.sayboard.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

enum class AiProvider(val id: String, val label: String, val defaultModel: String, val defaultBase: String) {
    OPENAI("openai", "OpenAI compatible", "gpt-4o-mini", "https://api.openai.com/v1"),
    ANTHROPIC("anthropic", "Anthropic Claude", "claude-3-5-haiku-latest", "https://api.anthropic.com/v1"),
    GEMINI("gemini", "Google Gemini", "gemini-2.0-flash", "https://generativelanguage.googleapis.com/v1beta");

    companion object {
        fun fromId(id: String): AiProvider = values().firstOrNull { it.id == id } ?: OPENAI
    }
}

class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Settings of the optional AI post-processing step. Everything is local unless
 * the user switches this on: then the recognized text is sent to the provider
 * they configured.
 */
@Serializable
data class AiSettings(
    val enabled: Boolean = false,
    val provider: String = AiProvider.OPENAI.id,
    val baseUrl: String = AiProvider.OPENAI.defaultBase,
    val apiKey: String = "",
    val model: String = AiProvider.OPENAI.defaultModel,
    /** Empty means [DEFAULT_PROMPT]. */
    val prompt: String = "",
    val correctDictation: Boolean = false,
    val correctTranscripts: Boolean = true,
    val timeoutSeconds: Int = 45,
) {
    val providerType: AiProvider
        get() = AiProvider.fromId(provider)

    val systemPrompt: String
        get() = prompt.ifBlank { DEFAULT_PROMPT }

    /** Local servers (Ollama, LM Studio, ...) need no API key. */
    val isLocalServer: Boolean
        get() = listOf("localhost", "127.0.0.1", "0.0.0.0", "192.168.", "10.", "host.docker.internal")
            .any { baseUrl.contains(it, ignoreCase = true) }

    fun isUsable(): Boolean = enabled && model.isNotBlank() && (apiKey.isNotBlank() || isLocalServer)

    companion object {
        const val DEFAULT_PROMPT =
            "You are a speech recognition post-processor. Fix punctuation, capitalization, " +
                    "spacing and obvious recognition errors in the text. Keep the original language " +
                    "and meaning, do not translate, do not summarize and do not add words. " +
                    "Answer with the corrected text only, without quotes or explanations."
    }
}

/**
 * Small client for the three most common chat APIs. It is intentionally
 * dependent on nothing but the JDK HTTP client.
 */
class AiClient(private val settings: AiSettings) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun correct(text: String): String {
        if (text.isBlank()) return text
        val answer = call(settings.systemPrompt, text.trim())
        return sanitize(answer, text)
    }

    fun testConnection(): String {
        return call("Answer with the single word OK.", "ping").take(200)
    }

    private fun call(system: String, user: String): String {
        if (!settings.enabled) throw AiException("AI correction is turned off")
        val provider = settings.providerType
        val base = settings.baseUrl.trimEnd('/')
        val request: HttpRequest
        val parse: (String) -> String
        when (provider) {
            AiProvider.OPENAI -> {
                if (base.isBlank()) throw AiException("No base URL configured")
                request = jsonRequest(
                    url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions",
                    body = AiPayloads.openAi(settings, system, user),
                    headers = mapOf(
                        "Authorization" to "Bearer ${settings.apiKey}",
                        "Content-Type" to "application/json",
                    ),
                )
                parse = { AiPayloads.parseOpenAi(it) }
            }

            AiProvider.ANTHROPIC -> {
                request = jsonRequest(
                    url = if (base.endsWith("/messages")) base else "$base/messages",
                    body = AiPayloads.anthropic(settings, system, user),
                    headers = mapOf(
                        "x-api-key" to settings.apiKey,
                        "anthropic-version" to "2023-06-01",
                        "Content-Type" to "application/json",
                    ),
                )
                parse = { AiPayloads.parseAnthropic(it) }
            }

            AiProvider.GEMINI -> {
                val url = "$base/models/${settings.model}:generateContent?key=${settings.apiKey}"
                request = jsonRequest(url, AiPayloads.gemini(settings, system, user), emptyMap())
                parse = { AiPayloads.parseGemini(it) }
            }
        }

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        val response = try {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw AiException("could not reach the AI service: ${e.message}", e)
        }
        if (response.statusCode() !in 200..299) {
            throw AiException("AI service answered HTTP ${response.statusCode()}: ${response.body().take(300)}")
        }
        val parsed = parse(response.body()).trim()
        if (parsed.isEmpty()) throw AiException("the AI service returned an empty answer")
        return parsed
    }

    private fun jsonRequest(url: String, body: String, headers: Map<String, String>): HttpRequest {
        val builder = try {
            HttpRequest.newBuilder(URI.create(url))
        } catch (e: Exception) {
            throw AiException("invalid URL: $url", e)
        }
        builder.timeout(Duration.ofSeconds(settings.timeoutSeconds.coerceIn(5, 300).toLong()))
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.POST(HttpRequest.BodyPublishers.ofString(body)).build()
    }

    /** Removes surrounding quotes/prefixes some models like to add. */
    private fun sanitize(answer: String, original: String): String {
        var text = answer.trim()
        if (text.startsWith("```") && text.endsWith("```")) {
            text = text.removePrefix("```").removeSuffix("```").trim()
            text = text.substringAfter('\n', text).trim()
        }
        if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) {
            text = text.substring(1, text.length - 1).trim()
        }
        if (text.isBlank()) return original
        return text
    }
}

/** Request/response bodies, kept separate so they can be unit tested. */
internal object AiPayloads {
    fun openAi(settings: AiSettings, system: String, user: String): String = buildJsonObject {
        put("model", settings.model)
        put("temperature", 0)
        putJsonArray("messages") {
            addJsonObject {
                put("role", "system")
                put("content", system)
            }
            addJsonObject {
                put("role", "user")
                put("content", user)
            }
        }
    }.toString()

    fun anthropic(settings: AiSettings, system: String, user: String): String = buildJsonObject {
        put("model", settings.model)
        put("max_tokens", 4096)
        put("temperature", 0)
        put("system", system)
        putJsonArray("messages") {
            addJsonObject {
                put("role", "user")
                put("content", user)
            }
        }
    }.toString()

    fun gemini(settings: AiSettings, system: String, user: String): String = buildJsonObject {
        putJsonObject("systemInstruction") {
            putJsonArray("parts") { addJsonObject { put("text", system) } }
        }
        putJsonArray("contents") {
            addJsonObject {
                putJsonArray("parts") { addJsonObject { put("text", user) } }
            }
        }
        putJsonObject("generationConfig") { put("temperature", 0) }
    }.toString()

    fun parseOpenAi(body: String): String {
        val root = Json.parseToJsonElement(body).jsonObject
        val choices = root["choices"]?.jsonArray
            ?: throw AiException("unexpected answer: ${body.take(200)}")
        return choices.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")
            ?.jsonPrimitive?.contentOrNull ?: ""
    }

    fun parseAnthropic(body: String): String {
        val root = Json.parseToJsonElement(body).jsonObject
        val content = root["content"]?.jsonArray ?: throw AiException("unexpected answer: ${body.take(200)}")
        return content.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
    }

    fun parseGemini(body: String): String {
        val root = Json.parseToJsonElement(body).jsonObject
        val candidates = root["candidates"]?.jsonArray ?: throw AiException("unexpected answer: ${body.take(200)}")
        val parts = candidates.firstOrNull()?.jsonObject?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
        return parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
    }
}
