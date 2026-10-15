package com.testsoposiciones.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Último resultado de cada pregunta contestada. */
data class Progress(val correctIds: Set<String> = emptySet(), val wrongIds: Set<String> = emptySet()) {
    fun record(outcomes: Map<String, Boolean>): Progress {
        val right = outcomes.filterValues { it }.keys
        val wrong = outcomes.filterValues { !it }.keys
        return Progress(correctIds - wrong + right, wrongIds - right + wrong)
    }

    fun stats(bank: QuestionBank, topicId: String? = null): TopicStats {
        val ids = (if (topicId == null) bank.questions else bank.questionsOf(topicId)).map { it.id }
        return TopicStats(
            total = ids.size,
            correct = ids.count { it in correctIds },
            wrong = ids.count { it in wrongIds },
        )
    }

    fun failedQuestions(bank: QuestionBank): List<Question> = bank.questions.filter { it.id in wrongIds }

    /** Preguntas que nunca se han contestado o que se fallaron, primero. */
    fun pendingFirst(questions: List<Question>): List<Question> =
        questions.sortedBy { if (it.id in correctIds) 1 else 0 }
}

data class TopicStats(val total: Int, val correct: Int, val wrong: Int) {
    val answered: Int get() = correct + wrong
    val mastery: Float get() = if (total == 0) 0f else correct.toFloat() / total
}

/** Racha de días seguidos estudiando. */
data class StudyStreak(val days: Int = 0, val lastDay: LocalDate? = null) {
    fun studiedOn(day: LocalDate): StudyStreak = when {
        lastDay == day -> this
        lastDay != null && ChronoUnit.DAYS.between(lastDay, day) == 1L -> StudyStreak(days + 1, day)
        else -> StudyStreak(1, day)
    }

    /** La racha sigue viva si se estudió hoy o ayer. */
    fun current(today: LocalDate): Int =
        if (lastDay != null && ChronoUnit.DAYS.between(lastDay, today) <= 1) days else 0
}
