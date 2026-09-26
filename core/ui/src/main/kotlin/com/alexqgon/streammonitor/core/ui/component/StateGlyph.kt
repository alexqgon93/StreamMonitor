package com.alexqgon.streammonitor.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alexqgon.streammonitor.core.ui.theme.LocalStreamColors

object StreamUiStateKind {
    const val Idle = 0
    const val Streaming = 1
    const val Completed = 2
    const val Failed = 3
}

@Composable
fun StateGlyph(
    stateKind: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint = when (stateKind) {
        StreamUiStateKind.Completed,
        StreamRowKind.Ready,
        -> LocalStreamColors.current.success
        StreamUiStateKind.Failed,
        StreamRowKind.Failed,
        -> colors.error
        StreamRowKind.Pending -> LocalStreamColors.current.warning
        else -> colors.primary
    }
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        when (stateKind) {
            StreamUiStateKind.Completed,
            StreamRowKind.Ready,
            -> {
                drawLine(tint, center.copy(x = size.width * .2f, y = size.height * .55f), center.copy(x = size.width * .43f, y = size.height * .75f), stroke.width, StrokeCap.Round)
                drawLine(tint, center.copy(x = size.width * .43f, y = size.height * .75f), center.copy(x = size.width * .8f, y = size.height * .28f), stroke.width, StrokeCap.Round)
            }

            StreamUiStateKind.Failed,
            StreamRowKind.Failed,
            -> {
                drawLine(tint, center.copy(y = size.height * .24f), center.copy(y = size.height * .62f), stroke.width, StrokeCap.Round)
                drawCircle(tint, radius = stroke.width / 2, center = center.copy(y = size.height * .79f))
            }

            else -> drawCircle(tint, radius = size.minDimension * .36f, style = stroke)
        }
    }
}
