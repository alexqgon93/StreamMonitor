package com.alexqgon.streammonitor.core.ui

import androidx.compose.foundation.BorderStroke
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
import com.alexqgon.streammonitor.core.ui.theme.StreamResultTextStyle
import com.alexqgon.streammonitor.core.ui.theme.StreamSpacing

object StreamRowKind {
    const val NotNeeded = 10
    const val Pending = 11
    const val Ready = 12
    const val Failed = 13
}

@Composable
fun ResultRow(
    title: String,
    state: String,
    reason: String,
    contentDescription: String,
    rowKind: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val stateColor = when (rowKind) {
        StreamRowKind.Ready -> LocalStreamColors.current.success
        StreamRowKind.Pending -> LocalStreamColors.current.warning
        StreamRowKind.Failed -> colors.error
        else -> colors.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (rowKind == StreamRowKind.Failed) 74.dp else 62.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        shape = RoundedCornerShape(10.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StreamSpacing.Sm),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (rowKind != StreamRowKind.NotNeeded) {
                    StateGlyph(rowKind)
                    Text(
                        text = state,
                        color = stateColor,
                        style = if (rowKind == StreamRowKind.Ready) {
                            StreamResultTextStyle
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                    )
                }
            }
            if (rowKind == StreamRowKind.Failed) {
                Text(
                    text = reason,
                    modifier = Modifier.padding(top = StreamSpacing.Xs),
                    color = stateColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
