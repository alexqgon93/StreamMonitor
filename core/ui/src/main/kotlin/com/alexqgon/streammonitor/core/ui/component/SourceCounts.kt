package com.alexqgon.streammonitor.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alexqgon.streammonitor.core.ui.theme.StreamSpacing

@Composable
fun SourceCounts(
    numbersText: String,
    inputsText: String,
    numbersContentDescription: String,
    inputsContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val countStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .drawBehind {
                drawLine(
                    color = outline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        horizontalArrangement = Arrangement.spacedBy(StreamSpacing.Sm),
    ) {
        Text(
            numbersText,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = numbersContentDescription },
            color = MaterialTheme.colorScheme.onSurface,
            style = countStyle,
        )
        Text(
            inputsText,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = inputsContentDescription },
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            style = countStyle,
        )
    }
}
