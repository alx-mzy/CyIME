package com.kingzcheung.xime.ui.keyboard

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** 注释字号 = 候选字号 × 这个比例（11/19，参照微信）。 */
internal const val COMMENT_SIZE_RATIO = 11f / 19f

/** 行槽高 = 字号 × 这个比例；和 [candidateBaseline] 的 1.35 一致，主词行与注释行共用。 */
internal const val COMMENT_SLOT_FACTOR = 1.35f

/** 注释最长多宽：更长的注释在限宽处省略，不把候选撑到小半屏。 */
internal val COMMENT_MAX_WIDTH = 120.dp

/** 注释的字号。 */
internal fun commentTextUnit(fontSize: TextUnit): TextUnit =
    (fontSize.value * COMMENT_SIZE_RATIO).sp

/** 一行字的槽高像素。主词行用候选字号，注释行用 [commentTextUnit]：同一套取整，上下两行加起来就是条目高度。 */
internal fun candidateSlotPx(fontSize: TextUnit, density: Density): Int =
    with(density) { (fontSize.toPx() * COMMENT_SLOT_FACTOR).roundToInt() }

/**
 * 注释上置的条目宽度：主词和注释上下排，宽度取较宽的一个，注释再长也只到 [commentCap]。
 * 真实测量、候选栏排窗、展开页行宽估算和硬件键盘卡片宽度共用这一个公式。
 */
internal fun commentStackWidth(primary: Int, comment: Int, commentCap: Int): Int =
    maxOf(primary, minOf(comment, commentCap))
