package com.aicode.feature.agent.presentation.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.aicode.core.util.FileLogger
import com.aicode.feature.workspace.domain.FileAccessProvider
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Markdown 图片 transformer 的 CompositionLocal。
 * 默认空实现（返回 null）不渲染图片节点，与引入本功能前的行为一致。
 */
internal val LocalMarkdownImageTransformer = staticCompositionLocalOf<ImageTransformer> { NoOpImageTransformer }

private object NoOpImageTransformer : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData? = null
}

/**
 * 只加载本地图片的 markdown 图片渲染器。
 *
 * 支持 AI 容器视角的本地路径（`~/workspace/...`、容器绝对路径、相对路径）与 `file://`，
 * 经 [FileAccessProvider.copyToLocal] 落到宿主文件（远程模式自动 SFTP 下载到临时文件）；
 * 网络协议（http/https/data/content 等）一律不渲染。
 *
 * 解码用 [BitmapFactory]（GIF 只显示首帧），大图按最大边采样防 OOM，显示时限制高度等比缩放。
 */
internal class MarkdownImageTransformer(
    private val fileAccess: FileAccessProvider
) : ImageTransformer {

    @Composable
    override fun transform(link: String): ImageData? {
        if (!isLocalPath(link)) return null
        val painter by produceState<Painter?>(initialValue = null, key1 = link) {
            value = withContext(Dispatchers.IO) { decode(link) }
        }
        val p = painter ?: return null
        return ImageData(
            painter = p,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = MAX_HEIGHT_DP)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }

    private fun decode(url: String): Painter? {
        return try {
            val file = fileAccess.copyToLocal(url)
            if (!file.isFile || file.length() <= 0L) return null
            val bounds = decodeBounds(file) ?: return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.first, bounds.second, MAX_EDGE)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            FileLogger.w(TAG, "markdown 本地图片解码失败: $url", e)
            null
        }
    }

    private fun decodeBounds(file: File): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= maxEdge && height / (sample * 2) >= maxEdge) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun isLocalPath(url: String): Boolean {
        val u = url.trim()
        if (u.isEmpty()) return false
        if (u.startsWith("file://")) return true
        // 带 :// 的其它协议（http/https/data/content/ftp...）一律视为非本地
        return !u.contains("://")
    }

    private companion object {
        const val TAG = "MarkdownImage"
        /** 解码采样最大边（像素），防止大图撑爆内存。 */
        const val MAX_EDGE = 1024
        /** 显示最大高度（dp），超长图等比缩放。 */
        val MAX_HEIGHT_DP = 320.dp
    }
}
