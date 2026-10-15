package com.testsoposiciones.app.data

import android.content.res.AssetManager
import com.testsoposiciones.core.QuestionBank
import com.testsoposiciones.core.QuestionBankParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Banco de preguntas incluido en la app (assets), leído una sola vez. */
class QuestionRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var bank: QuestionBank? = null

    suspend fun bank(): QuestionBank = mutex.withLock {
        bank ?: withContext(Dispatchers.IO) {
            QuestionBankParser.parse(assets.open(FILE).bufferedReader().use { it.readText() })
        }.also { bank = it }
    }

    private companion object {
        const val FILE = "constitucion.json"
    }
}
