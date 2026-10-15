package com.testsoposiciones.core

import kotlin.math.max
import kotlin.random.Random

enum class QuizMode {
    /** Corrige cada pregunta al momento y enseña la explicación. */
    PRACTICE,

    /** Simulacro: con tiempo, sin corrección hasta el final y con penalización por fallo. */
    EXAM,
}

/** Pregunta con las opciones ya barajadas. */
data class QuizItem(val question: Question, val options: List<String>, val correctIndex: Int)

data class QuizResult(val total: Int, val correct: Int, val wrong: Int, val blank: Int, val penaltyPerWrong: Double) {
    /** Aciertos menos la penalización de los fallos (habitual en la AGE: cada fallo resta un tercio). */
    val netCorrect: Double get() = max(correct - wrong * penaltyPerWrong, 0.0)

    /** Nota sobre 10. */
    val score: Double get() = if (total == 0) 0.0 else netCorrect / total * 10
}

/** Estado inmutable de un test en curso. */
data class QuizSession(
    val mode: QuizMode,
    val items: List<QuizItem>,
    /** Índice de opción elegida por pregunta; null si se dejó en blanco. */
    val answers: List<Int?> = List(items.size) { null },
    val current: Int = 0,
    val penaltyPerWrong: Double = if (mode == QuizMode.EXAM) 1.0 / 3 else 0.0,
) {
    val currentItem: QuizItem get() = items[current]
    val isLast: Boolean get() = current == items.lastIndex
    val answeredCount: Int get() = answers.count { it != null }

    fun answer(optionIndex: Int): QuizSession {
        // En práctica la primera respuesta es la que cuenta.
        if (mode == QuizMode.PRACTICE && answers[current] != null) return this
        return copy(answers = answers.toMutableList().also { it[current] = optionIndex })
    }

    fun goTo(index: Int): QuizSession = copy(current = index.coerceIn(0, items.lastIndex))

    fun next(): QuizSession = goTo(current + 1)

    fun isCorrect(index: Int): Boolean? = answers[index]?.let { it == items[index].correctIndex }

    fun result(): QuizResult {
        val correct = items.indices.count { isCorrect(it) == true }
        val wrong = items.indices.count { isCorrect(it) == false }
        return QuizResult(items.size, correct, wrong, items.size - correct - wrong, penaltyPerWrong)
    }

    /** Ids acertados y fallados, para guardar el progreso. Las de en blanco no cuentan. */
    fun outcomes(): Map<String, Boolean> = items.indices
        .mapNotNull { i -> isCorrect(i)?.let { items[i].question.id to it } }
        .toMap()
}

object QuizBuilder {
    fun build(questions: List<Question>, count: Int, mode: QuizMode, random: Random = Random.Default): QuizSession {
        require(questions.isNotEmpty()) { "No hay preguntas" }
        val items = questions.shuffled(random).take(count).map { question -> shuffleOptions(question, random) }
        return QuizSession(mode, items)
    }

    fun shuffleOptions(question: Question, random: Random): QuizItem {
        val options = (listOf(question.correctOption) + question.wrongOptions).shuffled(random)
        return QuizItem(question, options, options.indexOf(question.correctOption))
    }
}
