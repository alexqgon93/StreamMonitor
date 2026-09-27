package com.alexqgon.streammonitor.feature.stream

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.alexqgon.streammonitor.core.ui.ActionButton
import com.alexqgon.streammonitor.core.ui.DiscardedSummary
import com.alexqgon.streammonitor.core.ui.LifecycleBanner
import com.alexqgon.streammonitor.core.ui.MockBadge
import com.alexqgon.streammonitor.core.ui.ResultRow
import com.alexqgon.streammonitor.core.ui.SectionLabel
import com.alexqgon.streammonitor.core.ui.SourceCounts
import com.alexqgon.streammonitor.core.ui.StreamRowKind
import com.alexqgon.streammonitor.core.ui.StreamUiStateKind
import com.alexqgon.streammonitor.core.ui.theme.StreamSpacing

@Composable
fun StreamScreen(
    state: StreamUiState,
    showMockBadge: Boolean,
    onIntent: (StreamIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppBar(showMockBadge)
            // The list scrolls under the navigation bar; its last row still clears it.
            val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                    )
                    .padding(horizontal = StreamSpacing.Lg),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(bottom = StreamSpacing.Xxl + bottomInset),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item(contentType = "status-panel") {
                        StatusPanel(state, onIntent)
                    }
                    if (state.hasSnapshot) {
                        item(contentType = "discarded") {
                            DiscardedSummary(
                                text = pluralStringResource(
                                    R.plurals.discarded_count,
                                    state.discardedCount,
                                    state.discardedCount,
                                ),
                            )
                        }
                    }
                    if (state.sections.isEmpty()) {
                        item(contentType = "empty") {
                            EmptyContent(state.lifecycle)
                        }
                    } else {
                        state.sections.forEach { section ->
                            item(
                                key = "section-${section.sectionNumber}",
                                contentType = "section-header",
                            ) {
                                Surface(color = MaterialTheme.colorScheme.background) {
                                    SectionLabel(
                                        label = stringResource(
                                            R.string.section_title,
                                            section.sectionNumber,
                                        ),
                                        contentDescription = stringResource(
                                            R.string.section_accessibility,
                                            section.sectionNumber,
                                        ),
                                    )
                                }
                            }
                            items(
                                items = section.rows,
                                key = { row -> row.sourceIndex },
                                contentType = { "result-row" },
                            ) { row ->
                                StreamResultRow(row)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBar(showMockBadge: Boolean) {
    val mockBadge = stringResource(R.string.mock_badge)
    // The surface colour extends behind the status bar; only its content is inset.
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                )
                .heightIn(min = 64.dp)
                .padding(horizontal = StreamSpacing.Xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.app_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (showMockBadge) {
                MockBadge(label = mockBadge, contentDescription = mockBadge)
            }
        }
    }
}

@Composable
private fun StatusPanel(
    state: StreamUiState,
    onIntent: (StreamIntent) -> Unit,
) {
    val lifecycle = lifecycleCopy(state.lifecycle)
    Column(
        modifier = Modifier.padding(top = StreamSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StreamSpacing.Lg),
    ) {
        LifecycleBanner(
            title = stringResource(lifecycle.titleRes),
            detail = stringResource(lifecycle.detailRes),
            stateKind = lifecycle.kind,
            contentDescription = stringResource(
                R.string.lifecycle_banner_accessibility,
                stringResource(lifecycle.titleRes),
                stringResource(lifecycle.detailRes),
            ),
        )
        SourceCounts(
            numbersText = stringResource(R.string.numbers_received, state.numbersReceived),
            inputsText = stringResource(R.string.inputs_received, state.inputsReceived),
            numbersContentDescription = stringResource(
                R.string.numbers_received_accessibility,
                state.numbersReceived,
            ),
            inputsContentDescription = stringResource(
                R.string.inputs_received_accessibility,
                state.inputsReceived,
            ),
        )
        lifecycle.action?.let { action ->
            ActionButton(
                label = stringResource(action.labelRes),
                contentDescription = stringResource(action.labelRes),
                onClick = { onIntent(action.intent) },
            )
        }
    }
}

@Composable
private fun EmptyContent(lifecycle: StreamLifecycleUi) {
    val detail = if (lifecycle == StreamLifecycleUi.Idle) {
        R.string.empty_idle_detail
    } else {
        R.string.empty_streaming_detail
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = StreamSpacing.Xxxl, horizontal = StreamSpacing.Xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamSpacing.Sm),
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(detail),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StreamResultRow(row: StreamRowUi) {
    val section = stringResource(R.string.section_accessibility, row.sectionNumber)
    val item = stringResource(R.string.item_title, row.itemNumber)
    val state = rowCopy(row)
    val accessibilityState = if (row.state == StreamRowStateUi.Ready) {
        stringResource(
            R.string.result_ready_accessibility,
            row.resultValue[0],
            row.resultValue[1],
        )
    } else {
        stringResource(state.accessibilityRes)
    }
    val contentDescription = if (row.state == StreamRowStateUi.Failed) {
        stringResource(
            R.string.row_failed_accessibility,
            section,
            item,
            stringResource(R.string.result_failed),
            stringResource(state.reasonRes),
        )
    } else {
        stringResource(
            R.string.row_accessibility,
            section,
            item,
            accessibilityState,
        )
    }
    ResultRow(
        title = item,
        state = stringResource(state.visibleStateRes, row.resultValue),
        reason = stringResource(state.reasonRes),
        contentDescription = contentDescription,
        rowKind = state.kind,
    )
}

private data class LifecycleCopy(
    val titleRes: Int,
    val detailRes: Int,
    val kind: Int,
    val action: ActionCopy?,
)

private data class ActionCopy(val labelRes: Int, val intent: StreamIntent)

private fun lifecycleCopy(lifecycle: StreamLifecycleUi): LifecycleCopy = when (lifecycle) {
    StreamLifecycleUi.Idle -> LifecycleCopy(
        R.string.lifecycle_idle,
        R.string.lifecycle_idle_detail,
        StreamUiStateKind.Idle,
        ActionCopy(R.string.start_stream, StreamIntent.Start),
    )
    StreamLifecycleUi.Streaming -> LifecycleCopy(
        R.string.lifecycle_streaming,
        R.string.lifecycle_streaming_detail,
        StreamUiStateKind.Streaming,
        null,
    )
    StreamLifecycleUi.Completed -> LifecycleCopy(
        R.string.lifecycle_completed,
        R.string.lifecycle_completed_detail,
        StreamUiStateKind.Completed,
        ActionCopy(R.string.restart_stream, StreamIntent.Restart),
    )
    StreamLifecycleUi.Failed -> LifecycleCopy(
        R.string.lifecycle_failed,
        R.string.lifecycle_failed_detail,
        StreamUiStateKind.Failed,
        ActionCopy(R.string.retry, StreamIntent.Retry),
    )
}

private data class RowCopy(
    val kind: Int,
    val visibleStateRes: Int,
    val accessibilityRes: Int,
    val reasonRes: Int,
)

private fun rowCopy(row: StreamRowUi): RowCopy = when (row.state) {
    StreamRowStateUi.NotNeeded -> RowCopy(
        StreamRowKind.NotNeeded,
        R.string.result_not_needed_accessibility,
        R.string.result_not_needed_accessibility,
        R.string.result_not_needed_accessibility,
    )
    StreamRowStateUi.Pending -> RowCopy(
        StreamRowKind.Pending,
        R.string.result_pending,
        R.string.result_pending,
        R.string.result_pending,
    )
    StreamRowStateUi.Ready -> RowCopy(
        StreamRowKind.Ready,
        R.string.result_ready,
        R.string.result_ready_accessibility,
        R.string.result_ready,
    )
    StreamRowStateUi.Failed -> RowCopy(
        StreamRowKind.Failed,
        R.string.result_failed,
        R.string.result_failed,
        when (row.failure) {
            StreamFailureUi.MissingInput -> R.string.failure_missing_input
            StreamFailureUi.InvalidInput -> R.string.failure_invalid_input
            StreamFailureUi.ComputationFailed -> R.string.failure_computation
            null -> R.string.failure_computation
        },
    )
}
