package com.kingzcheung.xime.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** 注释上置时候选栏整条内容的高度：装得下「注释一行 + 主词一行」，比原来的 44dp 高一点。关闭时仍是 44dp。 */
@Composable
internal fun candidateBarContentHeight(fontSize: TextUnit, commentAbove: Boolean): Dp =
    if (commentAbove) maxOf(44.dp, candidateItemHeight(fontSize, true) + 4.dp) else 44.dp

/**
 * 把一行字做成等高的一槽：槽高 [COMMENT_SLOT_FACTOR]×字号，基线固定在 1.05×字号。
 * 上下两行各占一槽，字号不同也能对齐，取值和 [candidateBaseline] 一致。
 */
internal fun Modifier.candidateSlot(fontSize: TextUnit): Modifier = layout { measurable, constraints ->
    val child = measurable.measure(constraints.copy(minHeight = 0))
    val targetBaseline = (fontSize.toPx() * 1.05f).roundToInt()
    val baseline = child[FirstBaseline].takeUnless { it == AlignmentLine.Unspecified } ?: targetBaseline
    val top = (targetBaseline - baseline).coerceAtLeast(0)
    val slot = candidateSlotPx(fontSize, this)
    val height = constraints.constrainHeight(maxOf(slot, top + child.height))
    layout(child.width, height, mapOf(FirstBaseline to top + baseline)) { child.placeRelative(0, top) }
}

/**
 * 注释上置的候选条目：注释（或纠错读音）一行小字在上，主词一行在下，各自居中，宽度取较宽的一行。
 * 子节点顺序按原本的左右排列给出（主词、角标、注释），这里只改摆放位置。
 * 关闭时直接把 [content] 发出来，不添任何布局节点，候选和上游完全一样。
 */
@Composable
internal fun CandidateCommentLayout(commentAbove: Boolean, fontSize: TextUnit, learned: Boolean = false,
    content: @Composable () -> Unit) {
    if (!commentAbove) {
        content()
        return
    }
    val cap = with(LocalDensity.current) { COMMENT_MAX_WIDTH.roundToPx() }
    val policy = remember(cap, learned) {
        object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                val primary = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                val comment = measurables.last().measure(constraints.copy(minWidth = 0, minHeight = 0,
                    maxWidth = minOf(constraints.maxWidth, cap)))
                val marker = if (learned) measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0)) else null
                val width = constraints.constrainWidth(commentStackWidth(primary.width, comment.width, cap))
                val height = constraints.constrainHeight(comment.height + primary.height)
                return layout(width, height) {
                    comment.placeRelative((width - comment.width) / 2, 0)
                    primary.placeRelative((width - primary.width) / 2, comment.height)
                    marker?.placeRelative((width + primary.width) / 2, comment.height)
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
                commentStackWidth(measurables.first().maxIntrinsicWidth(height),
                    measurables.last().maxIntrinsicWidth(height), cap)

            override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
                maxIntrinsicWidth(measurables, height)
        }
    }
    Layout(content = content, measurePolicy = policy)
}
