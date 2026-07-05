package com.printswithme.badgeverify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.printswithme.badgeverify.ui.theme.BadgeVerifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BadgeVerifyTheme {
                BadgeVerifyApp()
            }
        }
    }
}
