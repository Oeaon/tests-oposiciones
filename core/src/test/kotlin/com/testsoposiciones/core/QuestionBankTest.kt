package com.testsoposiciones.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Comprueba el banco real que va en la app. */
class QuestionBankTest {
    private val bank = QuestionBankParser.parse(File(System.getProperty("bankPath", "../app/src/main/assets/constitucion.json")).readText())

    @Test
    fun `el banco tiene preguntas de todos los temas`() {
        assertTrue(bank.questions.size >= 100)
        bank.topics.forEach { topic -> assertTrue("Pocas preguntas en ${topic.id}", bank.questionsOf(topic.id).size >= 5) }
    }

    @Test
    fun `ids unicos y cuatro opciones distintas por pregunta`() {
        assertEquals(bank.questions.size, bank.questions.map { it.id }.toSet().size)
        bank.questions.forEach { q ->
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
