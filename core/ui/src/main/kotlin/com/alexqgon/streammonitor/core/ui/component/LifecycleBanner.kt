package com.alexqgon.streammonitor.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alexqgon.streammonitor.core.ui.theme.LocalStreamColors
import com.alexqgon.streammonitor.core.ui.theme.StreamSpacing

@Composable
fun LifecycleBanner(
    title: String,
    detail: String,
    stateKind: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background = when (stateKind) {
        StreamUiStateKind.Completed -> LocalStreamColors.current.successContainer
        StreamUiStateKind.Failed -> colors.errorContainer
        else -> colors.primaryContainer
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        shape = RoundedCornerShape(14.dp),
        color = background,
    ) {
        Row(
            modifier = Modifier.padding(StreamSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateGlyph(stateKind)
            Column {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(detail, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
