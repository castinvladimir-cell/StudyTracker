package com.kuzmin.studytracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kuzmin.studytracker.data.Store
import com.kuzmin.studytracker.ui.TrackerApp
import com.kuzmin.studytracker.ui.theme.TrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val store = Store.get(this)
        setContent {
            TrackerTheme {
                TrackerApp(store)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Store.get(this).flush()
    }
}
