package com.testsoposiciones.app

import android.app.Application
import com.testsoposiciones.app.data.ProgressRepository
import com.testsoposiciones.app.data.QuestionRepository
import com.testsoposiciones.app.data.progressDataStore
import com.testsoposiciones.app.monetization.Monetization

class TestsOposicionesApp : Application() {
    lateinit var monetization: Monetization
        private set
    lateinit var progress: ProgressRepository
        private set
    lateinit var questions: QuestionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        monetization = Monetization(this)
        progress = ProgressRepository(progressDataStore)
        questions = QuestionRepository(assets)
    }
}
