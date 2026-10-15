package com.testsoposiciones.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.testsoposiciones.app.ui.StudyViewModel
import com.testsoposiciones.app.ui.TestsRoot
import com.testsoposiciones.app.ui.theme.TestsTheme

class MainActivity : ComponentActivity() {
    private val app by lazy { application as TestsOposicionesApp }
    private val viewModel: StudyViewModel by viewModels { StudyViewModel.factory(app) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        app.monetization.gatherConsent(this)

        setContent {
            TestsTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val monetizationState by app.monetization.state.collectAsStateWithLifecycle()
                TestsRoot(state, viewModel, app.monetization, monetizationState)
            }
        }
    }
}
