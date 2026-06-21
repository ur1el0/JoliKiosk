package com.example.webdev2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Pink,
                    onPrimary = Color.White,
                    background = BgGray,
                    surface = CardWhite,
                    onSurface = TextDark,
                    onBackground = TextDark
                )
            ) {
                AppNavigation()
            }
        }
    }
}