package com.testsoposiciones.app.data

import android.content.res.AssetManager
import com.testsoposiciones.core.QuestionBank
import com.testsoposiciones.core.QuestionBankParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Temarios incluidos en la app (assets), leídos una sola vez y en este orden. */
class QuestionRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var banks: List<QuestionBank>? = null

    suspend fun banks(): List<QuestionBank> = mutex.withLock {
        banks ?: withContext(Dispatchers.IO) {
            FILES.map { file -> QuestionBankParser.parse(assets.open(file).bufferedReader().use { it.readText() }) }
        }.also { banks = it }
    }

    private companion object {
        val FILES = listOf("constitucion.json", "ley39.json", "ley40.json")
    }
}
