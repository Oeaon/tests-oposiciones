package com.testsoposiciones.core

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Topic(val id: String, val title: String)

/** En el archivo la primera opción es siempre la correcta; los tests las barajan al crearse. */
data class Question(
    val id: String,
    val topicId: String,
    val text: String,
    val correctOption: String,
    val wrongOptions: List<String>,
    val explanation: String,
)

/** Un temario: la Constitución, una ley… [id] es estable y sirve para guardar el progreso y la mejor nota. */
data class QuestionBank(
    val id: String,
    val shortTitle: String,
    val title: String,
    val source: String,
    val topics: List<Topic>,
    val questions: List<Question>,
) {
    fun questionsOf(topicId: String): List<Question> = questions.filter { it.topicId == topicId }
}

class QuestionBankFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object QuestionBankParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): QuestionBank = try {
        val root = json.parseToJsonElement(body).jsonObject
        val topics = root.array("topics").map { it.jsonObject }.map { Topic(it.text("id"), it.text("title")) }
        val topicIds = topics.map { it.id }.toSet()
        val questions = root.array("questions").map { element ->
            val item = element.jsonObject
            val options = item.array("options").map { it.jsonPrimitive.content }
            if (options.size < 2) throw QuestionBankFormatException("Pregunta con menos de dos opciones: ${item.text("id")}")
            val topic = item.text("topic")
            if (topic !in topicIds) throw QuestionBankFormatException("Tema desconocido: $topic")
            Question(item.text("id"), topic, item.text("question"), options.first(), options.drop(1), item.text("explanation"))
        }
        val title = root.text("title")
        QuestionBank(
            id = root.optionalText("id") ?: title,
            shortTitle = root.optionalText("shortTitle") ?: title,
            title = title,
            source = root.text("source"),
            topics = topics,
            questions = questions,
        )
    } catch (e: SerializationException) {
        throw QuestionBankFormatException("JSON no válido", e)
    } catch (e: IllegalArgumentException) {
        throw QuestionBankFormatException("Formato inesperado", e)
    }

    private fun JsonObject.array(key: String): JsonArray =
        this[key] as? JsonArray ?: throw QuestionBankFormatException("Falta $key")

    private fun JsonObject.optionalText(key: String): String? = this[key]?.jsonPrimitive?.content

    private fun JsonObject.text(key: String): String =
        this[key]?.jsonPrimitive?.content ?: throw QuestionBankFormatException("Falta $key")
}
