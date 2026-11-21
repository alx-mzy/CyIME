package com.kingzcheung.xime.ui.render

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingzcheung.xime.ui.keyboard.CANDIDATE_ITEM_PADDING
import com.kingzcheung.xime.ui.keyboard.COMMENT_MAX_WIDTH
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
 * 注释上置（用户需求：小字注释在上、主词在下，项内居中，两行各自统一 baseline，空注释保留槽位；
 * 长注释限宽省略不挤主词；关闭时保持原来的左右排列）。
 * 设了 CYIME_RENDER_OUT=<目录> 时出图 candidate-comment-above.png。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w600dp-h400dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CandidateCommentAboveRenderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Composable
    private fun item(tag: String, text: String, comment: String, above: Boolean) = CandidateItem(
        text, 0, {}, Color.Black,
        comment = comment,
        fontSize = 18.sp,
        commentAbove = above,
        modifier = Modifier.testTag(tag),
    )

    @Test fun abovePutsCommentOverPrimaryKeepsSlotsAndCapsLongComments() {
        rule.setContent {
            Column(Modifier.background(Color.White).padding(8.dp).testTag("strip")) {
                Row {
                    item("above-empty", "好", "", above = true)
                    item("above-short", "字", "hao", above = true)
                    item("above-long", "键", "up-comment-over-cap-unique", above = true)
                }
                Row { item("side-long", "盘", "side-comment-over-cap-unique", above = false) }
            }
        }
        val density = rule.activity.resources.displayMetrics.density
        val empty = rule.onNodeWithTag("above-empty").fetchSemanticsNode().boundsInRoot
        val short = rule.onNodeWithTag("above-short").fetchSemanticsNode().boundsInRoot
        val long = rule.onNodeWithTag("above-long").fetchSemanticsNode().boundsInRoot
        val side = rule.onNodeWithTag("side-long").fetchSemanticsNode().boundsInRoot

        // 空注释保留槽位：高度与有注释时一致。
        assertEquals("空注释保留槽位", short.height, empty.height, 0.5f)
        assertEquals("长注释不改条目高度", short.height, long.height, 0.5f)
        // 同字号条目顶对齐、同高：两行槽高固定，跨条目基线不会错开。
        assertEquals(empty.top, short.top, 0.5f)
        assertEquals(short.top, long.top, 0.5f)

        // 注释在上、主词在下：注释文字的顶在主词文字顶的上面，且都在自己条目里。
        val commentBounds = rule.onNodeWithText("hao", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val primaryBounds = rule.onNodeWithText("字", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("注释(${commentBounds.top}) 应在主词(${primaryBounds.top}) 上方",
            commentBounds.top < primaryBounds.top)
        assertTrue("注释和主词都应在条目里", commentBounds.top >= short.top - 0.5f &&
            primaryBounds.bottom <= short.bottom + 0.5f)

        // 关闭时保持原来的左右排列：注释和主词同一行，注释在主词右边。
        val sideComment = rule.onNodeWithText("side-comment-over-cap-unique", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val sidePrimary = rule.onNodeWithText("盘", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("关闭时注释与主词同一水平线", sidePrimary.top, sideComment.top, 2f)
        assertTrue("关闭时注释在主词右边", sideComment.left > sidePrimary.left)

        // 长注释限宽：条目不宽于限宽值 + 左右留白；左右布局则一路叠宽。
        val cap = COMMENT_MAX_WIDTH.value * density
        val padding = CANDIDATE_ITEM_PADDING.value * density
        assertTrue("上置长注释条目(${long.width}) 应被限到 ${cap + 2 * padding} 内",
            long.width <= cap + 2 * padding + 0.5f)
        assertTrue("左右布局长注释(${side.width}) 应宽于上置(${long.width})", side.width > long.width)
        // 限宽不挤主词：条目仍至少和纯主词条目一样宽。
        assertTrue("限宽不挤主词", long.width >= empty.width - 0.5f)

        System.getenv("CYIME_RENDER_OUT")?.let { out ->
            val root = rule.activity.window.decorView
            val full = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            rule.runOnIdle { root.draw(Canvas(full)) }
            val box = rule.onNodeWithTag("strip").fetchSemanticsNode().boundsInWindow
            val bmp = Bitmap.createBitmap(full, box.left.toInt(), box.top.toInt(), box.width.toInt(), box.height.toInt())
            File(out).mkdirs()
            File(out, "candidate-comment-above.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
