package com.example.plantcarecompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.plantcarecompanion.ui.PlantCareApp
import com.example.plantcarecompanion.ui.theme.PlantCareCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantCareCompanionTheme {
                PlantCareApp()
            }
        }
    }
}