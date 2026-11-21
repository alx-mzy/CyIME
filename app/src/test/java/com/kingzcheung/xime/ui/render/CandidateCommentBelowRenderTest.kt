package com.kingzcheung.xime.ui.render

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingzcheung.xime.ui.keyboard.BELOW_COMMENT_MAX_WIDTH
import com.kingzcheung.xime.ui.keyboard.CANDIDATE_ITEM_PADDING
import com.kingzcheung.xime.ui.keyboard.CandidateItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 注释下置（用户需求：主词上注释下，项内居中，主词／注释各自统一 baseline，空注释保留槽位；
 * 长注释限宽省略不挤主词；见 docs/development/candidate-comment-below.md）。
 * 设了 CYIME_RENDER_OUT=<目录> 时出图 candidate-comment-below.png。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w600dp-h400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CandidateCommentBelowRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Composable
    private fun item(tag: String, text: String, comment: String, below: Boolean) = CandidateItem(
        text, 0, {}, Color.Black,
        comment = comment,
        fontSize = 18.sp,
        commentBelow = below,
        modifier = Modifier.testTag(tag),
    )

    @Test fun belowKeepsSlotsAlignsBaselinesAndCapsLongComments() {
        rule.setContent {
            Column(Modifier.background(Color.White).padding(8.dp).testTag("strip")) {
                Row {
                    item("below-empty", "好", "", below = true)
                    item("below-short", "好", "hao", below = true)
                    item("below-long", "好", "very-long-comment-that-exceeds-the-cap-width-for-sure", below = true)
                }
                Row { item("side-long", "好", "very-long-comment-that-exceeds-the-cap-width-for-sure", below = false) }
            }
        }
        val density = rule.activity.resources.displayMetrics.density
        val empty = rule.onNodeWithTag("below-empty").fetchSemanticsNode().boundsInRoot
        val short = rule.onNodeWithTag("below-short").fetchSemanticsNode().boundsInRoot
        val long = rule.onNodeWithTag("below-long").fetchSemanticsNode().boundsInRoot
        val side = rule.onNodeWithTag("side-long").fetchSemanticsNode().boundsInRoot

        // 空注释保留槽位：高度与有注释时一致。
        assertEquals("空注释保留槽位", short.height, empty.height, 0.5f)
        assertEquals("长注释不改条目高度", short.height, long.height, 0.5f)
        // 主词统一 baseline：同字号条目顶对齐、同高。
        assertEquals(empty.top, short.top, 0.5f)
        assertEquals(short.top, long.top, 0.5f)

        // 长注释限宽：条目不宽于限宽值 + 左右留白；左右布局则一路叠宽。
        val cap = BELOW_COMMENT_MAX_WIDTH.value * density
        val padding = CANDIDATE_ITEM_PADDING.value * density
        assertTrue("下置长注释条目(${long.width}) 应被限到 ${cap + 2 * padding} 内",
            long.width <= cap + 2 * padding + 0.5f)
        assertTrue("左右布局长注释(${side.width}) 应宽于下置(${long.width})", side.width > long.width)
        // 限宽不挤主词：条目仍至少和纯主词条目一样宽。
        assertTrue("限宽不挤主词", long.width >= empty.width - 0.5f)

        System.getenv("CYIME_RENDER_OUT")?.let { out ->
            val root = rule.activity.window.decorView
            val full = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { root.draw(Canvas(full)) }
            val box = rule.onNodeWithTag("strip").fetchSemanticsNode().boundsInWindow
            val bmp = Bitmap.createBitmap(full, box.left.toInt(), box.top.toInt(), box.width.toInt(), box.height.toInt())
            File(out).mkdirs()
            File(out, "candidate-comment-below.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
