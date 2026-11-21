package com.kingzcheung.xime.ui.keyboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import kotlin.math.roundToInt

internal val BELOW_COMMENT_MAX_WIDTH = 120.dp
internal const val BELOW_LINE_HEIGHT = 1.35f

@Composable
internal fun candidateBarContentHeight(fontSize: TextUnit, below: Boolean) =
    if (below) maxOf(44.dp, candidateItemHeight(fontSize, true) + 4.dp) else 44.dp

/** Allocate fixed baseline slots, including an empty annotation, before async candidates arrive. */
internal fun Modifier.belowCandidateBaseline(fontSize: TextUnit): Modifier = layout { measurable, constraints ->
    val child = measurable.measure(constraints.copy(minHeight = 0))
    val targetBaseline = (fontSize.toPx() * 1.05f).roundToInt()
    val baseline = child[FirstBaseline].takeUnless { it == AlignmentLine.Unspecified } ?: targetBaseline
    val top = (targetBaseline - baseline).coerceAtLeast(0)
    val slotHeight = (fontSize.toPx() * BELOW_LINE_HEIGHT).roundToInt()
    val height = constraints.constrainHeight(maxOf(slotHeight, top + child.height))
    layout(child.width, height, mapOf(FirstBaseline to top + baseline)) {
        child.placeRelative(0, top)
    }
}

/** Children: primary, optional zero-width learned marker, secondary. Inline path stays a Row. */
@Composable
internal fun CandidateCommentLayout(below: Boolean, fontSize: TextUnit, learned: Boolean = false,
    secondaryRatio: Float = 11f / 19f,
    content: @Composable () -> Unit) {
    if (!below) {
        Row(verticalAlignment = Alignment.CenterVertically) { content() }
        return
    }
    val cap = with(LocalDensity.current) { BELOW_COMMENT_MAX_WIDTH.roundToPx() }
    val height = with(LocalDensity.current) { (fontSize.value * BELOW_LINE_HEIGHT * (1f + secondaryRatio)).sp.toDp() }
    val policy = remember(cap, learned) {
        object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val primary = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                val secondary = measurables.last().measure(constraints.copy(minWidth = 0, minHeight = 0,
                    maxWidth = minOf(constraints.maxWidth, cap)))
                val marker = if (learned) measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0)) else null
                val width = constraints.constrainWidth(belowCandidateWidth(primary.width, secondary.width, cap))
                val totalHeight = constraints.constrainHeight(primary.height + secondary.height)
                return layout(width, totalHeight) {
                    val top = (totalHeight - primary.height - secondary.height) / 2
                    primary.placeRelative((width - primary.width) / 2, top)
                    marker?.placeRelative((width + primary.width) / 2, top)
                    secondary.placeRelative((width - secondary.width) / 2, top + primary.height)
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
                belowCandidateWidth(measurables.first().maxIntrinsicWidth(height),
                    measurables.last().maxIntrinsicWidth(height), cap)

            override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
                maxIntrinsicWidth(measurables, height)
        }
    }
    Layout(content = content, modifier = Modifier.heightIn(min = height), measurePolicy = policy)
}
