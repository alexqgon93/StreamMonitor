package com.alexqgon.streammonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.alexqgon.streammonitor.core.ui.theme.StreamMonitorTheme
import com.alexqgon.streammonitor.feature.stream.StreamRoute
import com.alexqgon.streammonitor.feature.stream.StreamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: StreamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamMonitorTheme {
                StreamRoute(
                    viewModel = viewModel,
                    showMockBadge = BuildConfig.STREAM_DATA_SOURCE == "mock",
                )
            }
        }
    }
}