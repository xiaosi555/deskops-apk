package com.qwen.opsmentor.io

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * 导出落盘（对应 Web 的 saveBlob：只有用户点「保存到本机」才写文件）。
 * API 29+ 走 MediaStore Downloads（无需存储权限），低版本写应用外部目录。
 */
object Saver {

    fun mime(kind: String) = when (kind) {
        "json" -> "application/json"
        "csv" -> "text/csv"
        "md" -> "text/markdown"
        "html" -> "text/html"
        else -> "text/plain"
    }

    /** 写入公共下载目录；返回展示路径 */
    fun write(ctx: Context, name: String, kind: String, text: String): String {
        val bom = if (kind == "csv" || kind == "txt") "\uFEFF" else ""
        val bytes = (bom + text).toByteArray(Charsets.UTF_8)
        return if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime(kind))
                put(MediaStore.Downloads.SIZE, bytes.size)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("无法写入下载目录")
            ctx.contentResolver.openOutputStream(uri).use { it?.write(bytes) }
            "下载/$name"
        } else {
            val dir = File(ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: ctx.filesDir, "exports")
                .apply { mkdirs() }
            val f = File(dir, name)
            f.writeBytes(bytes)
            f.absolutePath
        }
    }

    /** 导出预览 → 保存：内部先弹预览层，确认后才落盘 */
    fun export(ctx: Context, title: String, name: String, kind: String, text: String, onSaved: (String) -> Unit = {}) {
        com.qwen.opsmentor.core.Sheets.export(
            ctx, title, name, text.toByteArray(Charsets.UTF_8).size, text, kind,
            onSave = {
                try {
                    val path = write(ctx, name, kind, text)
                    com.qwen.opsmentor.core.Sheets.popCheck(ctx, "已保存 $path")
                    onSaved(path)
                } catch (e: Exception) {
                    com.qwen.opsmentor.core.Sheets.toast(ctx, "保存失败：${e.message?.take(40) ?: "请改用复制全文"}")
                }
            },
        )
    }

    fun export(ctx: Context, name: String, kind: String, text: String) =
        export(ctx, name.substringBeforeLast('.'), name, kind, text)
}
