package com.nihontabi.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nihontabi.android.ui.nav.NihonTabiApp
import com.nihontabi.android.ui.theme.NihonTabiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NihonTabiTheme {
                NihonTabiApp()
            }
        }
    }
}
