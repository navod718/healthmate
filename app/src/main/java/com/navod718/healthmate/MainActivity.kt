package com.navod718.healthmate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
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
        setTheme(R.style.Theme_HealthMate)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && savedInstanceState == null) {
            fadeSystemSplash()
        }
        setContent {
            HealthMateTheme {
                val model: HealthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                        HealthViewModel((application as HealthMateApplication).repository, extras.createSavedStateHandle()) as T
                })
                HealthMateApp(model, revealBrand = savedInstanceState == null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun fadeSystemSplash() {
        splashScreen.setOnExitAnimationListener { splash ->
            if (!ValueAnimator.areAnimatorsEnabled()) {
                splash.remove()
            } else {
                ObjectAnimator.ofFloat(splash, View.ALPHA, 0f).apply {
                    duration = 180
                    interpolator = DecelerateInterpolator()
                    doOnEnd { splash.remove() }
                    start()
                }
            }
        }
    }
}
