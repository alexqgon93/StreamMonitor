package com.alexqgon.streammonitor.feature.stream

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StreamRoute(
    viewModel: StreamViewModel,
    showMockBadge: Boolean,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    StreamScreen(
        state = state.value,
        showMockBadge = showMockBadge,
        onIntent = viewModel::onIntent,
    )
}
