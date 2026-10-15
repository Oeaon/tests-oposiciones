package com.testsoposiciones.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class QuizTest {
    private val questions = (1..10).map { Question("q$it", "t", "Pregunta $it", "bien", listOf("mal1", "mal2", "mal3"), "Art. $it") }

    @Test
    fun `baraja opciones y conserva cual es la correcta`() {
        val session = QuizBuilder.build(questions, count = 5, mode = QuizMode.PRACTICE, random = Random(42))

        assertEquals(5, session.items.size)
        session.items.forEach { assertEquals("bien", it.options[it.correctIndex]) }
    }

    @Test
    fun `en practica no se puede cambiar la respuesta`() {
        val item = QuizBuilder.shuffleOptions(questions[0], Random(1))
        val wrong = (item.correctIndex + 1) % 4
        val session = QuizSession(QuizMode.PRACTICE, listOf(item)).answer(wrong).answer(item.correctIndex)

        assertEquals(false, session.isCorrect(0))
    }

    @Test
    fun `el simulacro resta un tercio por fallo`() {
        var session = QuizBuilder.build(questions, count = 9, mode = QuizMode.EXAM, random = Random(7))
        // 6 bien, 3 mal: 6 - 1 = 5 netas de 9 → 5,56.
        for (i in 0 until 9) {
            val item = session.items[i]
            val option = if (i < 6) item.correctIndex else (item.correctIndex + 1) % 4
            session = session.goTo(i).answer(option)
        }
        val result = session.result()

        assertEquals(6, result.correct)
        assertEquals(3, result.wrong)
        assertEquals(5.0, result.netCorrect, 1e-9)
        assertEquals(50.0 / 9, result.score, 1e-9)
    }

    @Test
    fun `las preguntas en blanco no cuentan ni penalizan`() {
        val session = QuizBuilder.build(questions, count = 4, mode = QuizMode.EXAM, random = Random(3))
        val result = session.answer(session.items[0].correctIndex).result()

        assertEquals(1, result.correct)
        assertEquals(3, result.blank)
        assertEquals(1, session.answer(session.items[0].correctIndex).outcomes().size)
        assertNull(session.isCorrect(1))
    }

    @Test
    fun `el progreso guarda el ultimo resultado de cada pregunta`() {
        val progress = Progress().record(mapOf("a" to true, "b" to false)).record(mapOf("b" to true, "c" to false))

        assertEquals(setOf("a", "b"), progress.correctIds)
        assertEquals(setOf("c"), progress.wrongIds)
    }

    @Test
    fun `primero las preguntas pendientes`() {
        val progress = Progress(correctIds = setOf("q1", "q2"))

        val ordered = progress.pendingFirst(questions)

        assertTrue(ordered.take(8).none { it.id in setOf("q1", "q2") })
    }

    @Test
    fun `racha de dias de estudio`() {
        val day = LocalDate.of(2026, 10, 15)
        val streak = StudyStreak().studiedOn(day).studiedOn(day).studiedOn(day.plusDays(1))

        assertEquals(2, streak.days)
        assertEquals(2, streak.current(day.plusDays(2)))
        assertEquals(0, streak.current(day.plusDays(3)))
        assertEquals(1, streak.studiedOn(day.plusDays(5)).days)
    }
}
