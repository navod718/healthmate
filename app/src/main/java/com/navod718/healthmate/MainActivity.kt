package com.navod718.healthmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.navod718.healthmate.feature.health.presentation.HealthMateApp
import com.navod718.healthmate.feature.health.presentation.HealthViewModel
import com.navod718.healthmate.ui.theme.HealthMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                val model: HealthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                        HealthViewModel((application as HealthMateApplication).repository, extras.createSavedStateHandle()) as T
                })
                HealthMateApp(model)
            }
        }
    }
}
