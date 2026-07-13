package com.converty.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.converty.app.data.files.SafDocumentGateway
import com.converty.app.feature.app.ConvertyApp
import com.converty.app.feature.app.ConvertyViewModel

class MainActivity : AppCompatActivity() {
    private val appContainer by lazy {
        (application as ConvertyApplication).appContainer
    }

    private val appViewModel by viewModels<ConvertyViewModel> {
        ConvertyViewModel.Factory(
            historyRepository = appContainer.historyRepository,
            settingsRepository = appContainer.settingsRepository,
            documentGateway = SafDocumentGateway(this),
            scheduler = appContainer.conversionScheduler,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ConvertyApp(viewModel = appViewModel)
        }
    }
}
