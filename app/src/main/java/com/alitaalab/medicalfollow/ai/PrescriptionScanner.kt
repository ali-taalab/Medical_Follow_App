package com.alitaalab.medicalfollow.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.AnthropicBeta
import com.anthropic.models.beta.messages.BetaBase64ImageSource
import com.anthropic.models.beta.messages.BetaContentBlockParam
import com.anthropic.models.beta.messages.BetaFallbacksParam
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Base64

/** What Claude read from a prescription photo. Empty strings mean "not on the paper / not readable". */
data class ScannedPrescription(
    val isPrescription: Boolean,
    val diagnosis: String,
    val doctorName: String,
    val specialty: String,
    val clinic: String,
    val visitDate: String,
    val medications: List<ScannedMedication>,
    val notes: String,
)

data class ScannedMedication(val name: String, val dose: String, val frequency: String, val duration: String)

class ScanException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { KEY, OFFLINE, REFUSAL, FAILED }
}

/**
 * Reads a photo of a doctor's prescription (روشتة) with Claude and returns structured data.
 * The parent's own API key is used; the request goes from the phone to the Anthropic API.
 * Blocking call: run it off the main thread.
 */
object PrescriptionScanner {
    private const val MODEL = "claude-opus-5-5"

    private fun obj(properties: Map<String, Any>): Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to properties,
        "required" to properties.keys.toList(),
        "additionalProperties" to false,
    )

    private val str = mapOf("type" to "string")

    private val schema: Map<String, Any> = obj(
        linkedMapOf(
            "is_prescription" to mapOf("type" to "boolean"),
            "diagnosis" to str,
            "doctor_name" to str,
            "specialty" to str,
            "clinic" to str,
            "visit_date" to mapOf("type" to "string", "description" to "YYYY-MM-DD, or empty if not written"),
            "medications" to mapOf(
                "type" to "array",
                "items" to obj(linkedMapOf("name" to str, "dose" to str, "frequency" to str, "duration" to str)),
            ),
            "notes" to str,
        )
    )

    private fun systemPrompt(arabic: Boolean): String {
        val language = if (arabic) "Egyptian Arabic" else "English"
        return """
            You read photos of doctors' prescriptions for a parent's child-health tracking app in Egypt. The parent photographs the prescription after a pediatric visit, and your output pre-fills the visit form; the parent reviews and corrects it before saving.

            Prescriptions are often handwritten, mix Arabic and English, and use Latin abbreviations (e.g. "tds", "bid", "q8h", "x 5d", "susp", "supp"). Read them the way an Egyptian pharmacist would.

            - medications: one entry per medicine line. Keep the medicine name as written, in Latin letters, including strength and form when written (e.g. "Augmentin 457mg susp"). Write dose, frequency and duration in $language in plain words a parent understands (e.g. "5 ml", "every 8 hours", "5 days").
            - diagnosis: the condition the doctor wrote, in $language; keep the medical term in brackets if useful.
            - notes: other instructions on the paper (follow-up date, tests, diet), in $language.
            - If something is not on the paper or you cannot read it confidently, return an empty string for it. Do not guess: a wrong medicine name is worse than a blank one the parent fills in.
            - If the photo is not a prescription, set is_prescription to false and leave everything else empty.
        """.trimIndent()
    }

    /** [baseUrl] is only for tests against a local mock server. */
    fun read(apiKey: String, jpeg: ByteArray, arabic: Boolean, baseUrl: String? = null): ScannedPrescription {
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey)
            .apply { if (baseUrl != null) baseUrl(baseUrl) }
            .build()
        try {
            val params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(16000L)
                // On a safety decline, let the API retry on a suitable fallback model.
                .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
                .fallbacks(BetaFallbacksParam.ofDefault())
                .outputConfig(
                    BetaOutputConfig.builder()
                        .effort(BetaOutputConfig.Effort.HIGH) // handwriting is hard; accuracy over speed
                        .format(
                            BetaJsonOutputFormat.builder()
                                .schema(
                                    BetaJsonOutputFormat.Schema.builder()
                                        .apply { schema.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) } }
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .system(systemPrompt(arabic))
                .addUserMessageOfContentBlockParams(
                    listOf(
                        BetaContentBlockParam.ofImage(
                            BetaBase64ImageSource.builder()
                                .mediaType(BetaBase64ImageSource.MediaType.IMAGE_JPEG)
                                .data(Base64.getEncoder().encodeToString(jpeg))
                                .build()
                        ),
                        BetaContentBlockParam.ofText("Read this prescription."),
                    )
                )
                .build()

            val response = try {
                client.beta().messages().create(params)
            } catch (e: UnauthorizedException) {
                throw ScanException(ScanException.Reason.KEY)
            } catch (e: PermissionDeniedException) {
                throw ScanException(ScanException.Reason.KEY)
            } catch (e: AnthropicIoException) {
                throw ScanException(ScanException.Reason.OFFLINE)
            } catch (e: ScanException) {
                throw e
            } catch (e: Exception) {
                throw ScanException(ScanException.Reason.FAILED)
            }

            when (response.stopReason().orElse(null)) {
                BetaStopReason.REFUSAL -> throw ScanException(ScanException.Reason.REFUSAL)
                BetaStopReason.MAX_TOKENS -> throw ScanException(ScanException.Reason.FAILED)
                else -> Unit
            }
            val text = response.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
            return parse(text)
        } finally {
            client.close()
        }
    }

    internal fun parse(json: String): ScannedPrescription {
        val root: JsonNode = try {
            ObjectMapper().readTree(json)
        } catch (e: Exception) {
            throw ScanException(ScanException.Reason.FAILED)
        }
        fun JsonNode.s(field: String): String = get(field)?.asText("")?.trim() ?: ""
        return ScannedPrescription(
            isPrescription = root.get("is_prescription")?.asBoolean(false) ?: false,
            diagnosis = root.s("diagnosis"),
            doctorName = root.s("doctor_name"),
            specialty = root.s("specialty"),
            clinic = root.s("clinic"),
            visitDate = root.s("visit_date"),
            medications = root.get("medications")?.map {
                ScannedMedication(it.s("name"), it.s("dose"), it.s("frequency"), it.s("duration"))
            } ?: emptyList(),
            notes = root.s("notes"),
        )
    }
}
