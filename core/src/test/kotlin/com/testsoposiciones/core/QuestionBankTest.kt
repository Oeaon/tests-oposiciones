package com.testsoposiciones.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Comprueba los temarios reales que van en la app (todos los .json de assets). */
class QuestionBankTest {
    private val dir = File(System.getProperty("assetsDir", "../app/src/main/assets"))
    private val banks = dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
        .map { QuestionBankParser.parse(it.readText()) }

    @Test
    fun `estan los tres temarios con sus preguntas`() {
        assertEquals(setOf("ce", "l39", "l40"), banks.map { it.id }.toSet())
        assertTrue(banks.sumOf { it.questions.size } >= 240)
    }

    @Test
    fun `cada tema tiene al menos cinco preguntas`() {
        banks.forEach { bank ->
            bank.topics.forEach { topic ->
                assertTrue("Pocas preguntas en ${topic.id}", bank.questionsOf(topic.id).size >= 5)
            }
        }
    }

    @Test
    fun `ids de preguntas y temas unicos entre todos los temarios`() {
        val questionIds = banks.flatMap { b -> b.questions.map { it.id } }
        val topicIds = banks.flatMap { b -> b.topics.map { it.id } }
        assertEquals(questionIds.size, questionIds.toSet().size)
        assertEquals(topicIds.size, topicIds.toSet().size)
    }

    @Test
    fun `cuatro opciones distintas y explicacion en cada pregunta`() {
        banks.flatMap { it.questions }.forEach { q ->
            val options = listOf(q.correctOption) + q.wrongOptions
            assertEquals("Opciones de ${q.id}", 4, options.size)
            assertEquals("Opciones repetidas en ${q.id}", 4, options.toSet().size)
            assertTrue("Sin explicación en ${q.id}", q.explanation.isNotBlank())
        }
    }

    @Test(expected = QuestionBankFormatException::class)
    fun `un tema desconocido es un error`() {
        QuestionBankParser.parse(
            """{"title":"t","source":"s","topics":[{"id":"a","title":"A"}],
               "questions":[{"id":"1","topic":"b","question":"?","options":["x","y"],"explanation":"e"}]}""",
        )
    }
}
