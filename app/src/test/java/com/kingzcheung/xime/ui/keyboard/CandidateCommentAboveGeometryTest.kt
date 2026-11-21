package com.kingzcheung.xime.ui.keyboard

import androidx.compose.ui.unit.sp
import com.kingzcheung.xime.rime.RimeCandidate
import com.kingzcheung.xime.service.ExpandedCandidatePager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 注释上置（candidate_comment_above）的几何口径：条目宽是主词与（限宽）注释的较大者，
 * 长注释被 [COMMENT_MAX_WIDTH] 限宽省略，不叠加宽度也不挤主词。
 * 与 CandidateCommentLayout 的测量策略、FixedCandidateStrip 的 widthAt、
 * HardwareKeyboardCandidateBar 的卡片宽估算共用同一套公式。
 */
class CandidateCommentAboveGeometryTest {

    private fun candidate(text: String, comment: String = "") = RimeCandidate(text = text, comment = comment)

    // ── commentStackWidth ──

    @Test fun `条目宽取主词与注释的较大者`() {
        assertEquals(30, commentStackWidth(primary = 30, comment = 20, commentCap = 120))
        assertEquals(40, commentStackWidth(primary = 30, comment = 40, commentCap = 120))
    }

    @Test fun `长注释被限宽省略不叠加宽度`() {
        assertEquals(120, commentStackWidth(primary = 30, comment = 400, commentCap = 120))
        // 注释没有时只留主词（空注释行只占高度，不占宽度）
        assertEquals(30, commentStackWidth(primary = 30, comment = 0, commentCap = 120))
        assertEquals(240, commentStackWidth(primary = 240, comment = 400, commentCap = 120))
    }

    // ── 注释字号与行槽 ──

    @Test fun `注释字号是候选字号的 11 比 19`() {
        assertEquals(11f, commentTextUnit(19.sp).value, 0.0001f)
        assertEquals(18f * 11f / 19f, commentTextUnit(18.sp).value, 0.0001f)
    }

    // ── ExpandedCandidatePager.estimateItemUnits / flowRows（展开页）──

    @Test fun `注释上置时估算宽度不叠加注释`() {
        val short = candidate("你", "ni")
        val stacked = ExpandedCandidatePager.estimateItemUnits(short, commentAbove = true, commentCapUnits = 10f)
        val side = ExpandedCandidatePager.estimateItemUnits(short)
        assertTrue("上置($stacked) 应小于左右叠放($side)", stacked < side)
        var primary = 0f
        var secondary = 0f
        short.text.forEach { primary += ExpandedCandidatePager.charUnits(it) }
        short.comment.forEach { secondary += ExpandedCandidatePager.charUnits(it) * ExpandedCandidatePager.COMMENT_ABOVE_SIZE_RATIO }
        assertEquals(ExpandedCandidatePager.ITEM_FIXED_UNITS + maxOf(primary, minOf(secondary, 10f)), stacked, 0.0001f)
    }

    @Test fun `注释上置时空注释与纯主词宽度一致`() {
        val plain = candidate("你")
        val empty = candidate("你", "")
        assertEquals(ExpandedCandidatePager.estimateItemUnits(plain, commentAbove = true, commentCapUnits = 10f),
            ExpandedCandidatePager.estimateItemUnits(empty, commentAbove = true, commentCapUnits = 10f), 0f)
    }

    @Test fun `长注释限宽后上置条目宽有上限`() {
        val long = candidate("你", "很长".repeat(30))
        val capped = ExpandedCandidatePager.estimateItemUnits(long, commentAbove = true, commentCapUnits = 6.67f)
        assertEquals(ExpandedCandidatePager.ITEM_FIXED_UNITS + 6.67f, capped, 0.0001f)
    }

    @Test fun `注释上置时同一行装得下更多候选`() {
        val all = List(10) { candidate("那", "narly-comment-${it}") }
        val capUnits = 6.67f
        val side = ExpandedCandidatePager.flowRows(all.indices.toList(), all, 15f)
        val stacked = ExpandedCandidatePager.flowRows(all.indices.toList(), all, 15f,
            commentAbove = true, commentCapUnits = capUnits)
        assertTrue("上置每行应装更多条目：${stacked.first().size} vs ${side.first().size}",
            stacked.first().size > side.first().size)
        assertEquals(all.indices.toList(), stacked.flatten())
    }

    @Test fun `默认参数保持原有左右布局行为`() {
        val all = listOf(candidate("超".repeat(50)), candidate("字"))
        assertEquals(ExpandedCandidatePager.flowRows(listOf(0, 1), all, 5f),
            ExpandedCandidatePager.flowRows(listOf(0, 1), all, 5f, commentAbove = false))
    }

    @Test fun `关闭上置时保持原来的浮点累加结果`() {
        for (length in 0..60) {
            val entry = candidate("hello你好".repeat(length), "pin yin释义".repeat(length))
            var original = ExpandedCandidatePager.ITEM_FIXED_UNITS
            entry.text.forEach { original += ExpandedCandidatePager.charUnits(it) }
            entry.comment.forEach { original += ExpandedCandidatePager.charUnits(it) * ExpandedCandidatePager.COMMENT_SIZE_RATIO }
            assertEquals(original, ExpandedCandidatePager.estimateItemUnits(entry), 0f)
        }
    }

    // ── KeyboardSquareSnap：候选栏变高后方形吸附按实际栏高算 ──

    @Test fun `候选栏变高时方形点整体上移（需要更高面板）`() {
        val base = KeyboardSquareSnap(10f, false, false)
        val taller = base.copy(candidateBarBaseHeightDp = 60f)
        val rect = ResizeRect(100f, 100f, 1000f, 800f)
        val spacing = 6f to 10f
        val baseHeight = base.squareTotalHeightPx(rect, 1f, 0f, false, spacing) ?: error("基础几何应有方形点")
        val tallHeight = taller.squareTotalHeightPx(rect, 1f, 0f, false, spacing) ?: error("加高后几何应有方形点")
        assertTrue("栏高 $baseHeight → ${tallHeight}：方形点应在更高的面板位置", tallHeight > baseHeight + 8f)
    }

    @Test fun `候选栏基础高度不写死 44dp`() {
        // capDifference 用 candidateBarBaseHeightDp，不再直接调 candidateBarHeightDp(44*scale)
        val rect = ResizeRect(100f, 100f, 1000f, 800f)
        val spacing = 6f to 10f
        val base = KeyboardSquareSnap(10f, false, false).capDifference(rect, 1f, 0f, false, spacing)
        val taller = KeyboardSquareSnap(10f, false, false, candidateBarBaseHeightDp = 60f)
            .capDifference(rect, 1f, 0f, false, spacing)
        assertTrue(taller > base)
    }
}
