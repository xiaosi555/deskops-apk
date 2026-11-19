package com.qwen.opsmentor.media
import com.qwen.opsmentor.data.toJson

import android.content.Context
import android.util.Base64
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.qwen.opsmentor.App
import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.data.LongLecture
import com.qwen.opsmentor.data.Module
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.data.Quiz
import com.qwen.opsmentor.data.MediaRef
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.UserCourse
import java.io.File
import java.io.FileOutputStream

/** 媒体文件条目（对应 Web 的 toItem 结果） */
data class MediaItem(val uri: Uri, val name: String, val size: Long, val time: Long, val kind: String)

/**
 * 视频 / 语音课程：SAF 文件夹扫描 → 建课 → 副本持久化 → 首帧封面。
 * 与 Web v13–v18 的 IndexedDB + File System Access 方案对应，落盘换成应用目录。
 */
object MediaCourses {

    private val VIDEO_EXT = listOf("mp4", "m4v", "mov", "webm", "mkv", "avi", "ogv", "3gp", "flv", "wmv", "mpg", "mpeg")
    private val AUDIO_EXT = listOf("mp3", "m4a", "aac", "wav", "ogg", "oga", "flac", "opus", "amr", "wma", "m4b", "aiff", "mka")

    fun ext(name: String) = name.substringAfterLast('.', "").lowercase()

    fun isMedia(name: String, kind: String): Boolean =
        ext(name) in (if (kind == "video") VIDEO_EXT else AUDIO_EXT)

    /** 扫描文件夹：读取根目录及其下一层常见子目录，避免深层目录造成超长等待。 */
    fun scan(ctx: Context, tree: Uri, kind: String): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val visited = HashSet<String>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        fun scanDirectory(parentId: String, depth: Int) {
            if (!visited.add(parentId)) return
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            runCatching {
                ctx.contentResolver.query(children, projection, null, null, null)?.use { c ->
                    val iId = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val iName = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val iSize = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val iTime = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val iMime = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    if (iId < 0 || iName < 0 || iMime < 0) return@use

                    while (c.moveToNext() && out.size < 5000) {
                        val documentId = c.getString(iId) ?: continue
                        val name = c.getString(iName) ?: continue
                        val mime = if (iMime >= 0) c.getString(iMime).orEmpty() else ""
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            // 根目录下的一层子目录也纳入扫描；不无限递归，避免误扫大型媒体库。
                            if (depth < 8 && out.size < 5000) scanDirectory(documentId, depth + 1)
                            continue
                        }
                        val matchesMime = (kind == "video" && mime.startsWith("video/")) ||
                            (kind == "audio" && mime.startsWith("audio/"))
                        if (!isMedia(name, kind) && !matchesMime) continue
                        out.add(MediaItem(
                            DocumentsContract.buildDocumentUriUsingTree(tree, documentId),
                            name,
                            if (iSize >= 0) c.getLong(iSize) else 0L,
                            if (iTime >= 0) c.getLong(iTime) else 0L,
                            kind,
                        ))
                    }
                }
            }
        }

        runCatching { scanDirectory(DocumentsContract.getTreeDocumentId(tree), 0) }
        return sortItems(out.distinctBy { it.uri.toString() }, "name", true)
    }

    fun fromUris(ctx: Context, uris: List<Uri>, kind: String): List<MediaItem> =
        sortItems(uris.mapNotNull { u ->
            val name = queryName(ctx, u) ?: u.lastPathSegment?.substringAfterLast('/') ?: return@mapNotNull null
            if (!isMedia(name, kind)) return@mapNotNull null
            MediaItem(u, name, querySize(ctx, u), 0L, kind)
        }, "name", true)

    fun displayName(ctx: Context, tree: Uri): String {
        val id = DocumentsContract.getTreeDocumentId(tree)
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        return queryName(ctx, uri) ?: ""
    }

    /** 自然排序（1、2、10 排在正确位置） */
    fun sortItems(items: List<MediaItem>, by: String, asc: Boolean): List<MediaItem> {
        val cmp = if (by == "time") compareBy<MediaItem>({ it.time }, { naturalKey(it.name) }) else compareBy<MediaItem>({ naturalKey(it.name) }, { it.time })
        val sorted = items.sortedWith(cmp)
        return if (asc) sorted else sorted.reversed()
    }

    private fun naturalKey(name: String): String {
        val m = Regex("(\\d+)").findAll(name.substringBeforeLast('.')).map { it.value.toInt() }.toList()
        val p = if (m.isEmpty()) intArrayOf() else m.toIntArray()
        return p.joinToString("-") { it.toString().padStart(6, '0') } + "#" + name.lowercase()
    }

    private fun queryName(ctx: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) return c.getString(0)
                }
            }
            runCatching {
                ctx.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) return c.getString(0)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun querySize(ctx: Context, uri: Uri): Long = runCatching {
        ctx.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else 0L
        } ?: 0L
    }.getOrElse { 0L }

    /** 建课：一个文件一章，路线 all = 全部集 */
    fun createCourse(app: App, kind: String, title: String, items: List<MediaItem>, glyph: String): String? {
        if (items.isEmpty()) return null
        val id = "media_${kind}_" + System.currentTimeMillis()
        val wd = maxOf(2, items.size.toString().length)
        val chs = items.mapIndexed { i, it ->
            Chapter(
                id = (i + 1).toString().padStart(wd, '0'), m = "A",
                t = it.name.substringBeforeLast('.'), s = fmtSize(it.size),
                terms = "", plain = emptyList(), cmds = emptyList(), pits = emptyList(),
                gate = "能用自己的话讲清本节内容",
                quiz = Quiz("用自己的话复述本节最重要的三点。", "对照本节内容自查：能讲出来才算过关。", ""),
                media = MediaRef(kind, it.name, it.uri.toString(), it.size),
            )
        }
        val pack = Pack(
            mods = listOf(Module("A", "模块 A · ${if (kind == "video") "视频" else "音频"}内容", "")),
            ch = chs,
            deep = emptyMap(), long = emptyMap(),
            layers = emptyList(), cmds = emptyList(), events = emptyList(), scen = emptyList(), lab = emptyList(),
            routes = linkedMapOf("all" to chs.map { it.id }),
            routeNames = linkedMapOf("all" to "全章节顺序"),
            intervals = listOf(2, 4, 7, 15, 30), depText = "", depNotes = emptyList(),
        )
        app.packs.put(id, pack)
        val uc = UserCourse(id, title.ifBlank { if (kind == "video") "视频课程" else "语音课程" },
            "${chs.size} 集 · 本地${if (kind == "video") "视频" else "音频"}课程", "", glyph, kind)
        uc.packJson = pack.toJson().toString()
        app.store.userCourses.add(uc)
        app.store.bags[id] = com.qwen.opsmentor.state.Bag.newBag("all")
        // 记住文件夹 uri（下次免重选）
        app.store.mediaTrees[id] = trimTree(items.first().uri.toString())
        app.store.save()
        return id
    }

    fun fmtSize(n: Long): String = when {
        n >= 1073741824 -> String.format(java.util.Locale.CHINA, "%.1f GB", n / 1073741824.0)
        n >= 1048576 -> (n / 1048576).toString() + " MB"
        else -> Math.max(1L, n / 1024).toString() + " KB"
    }

    /** 应用内副本：filesDir/media/<courseId>/<chapterId>.<ext> */
    fun copyInto(ctx: Context, app: App, courseId: String): Int {
        val pack = app.packs.of(app.store, courseId)
        var done = 0
        pack.ch.forEach { c ->
            val m = c.media ?: return@forEach
            val dst = File(dir(ctx, courseId), c.id + "." + ext(m.fname))
            if (dst.isFile && dst.length() > 0L && (m.size <= 0L || dst.length() == m.size)) {
                done++
                return@forEach
            }
            // 使用同目录临时文件，避免应用中断后留下一个被误判为完整的半截媒体文件。
            dst.delete()
            val temp = File(dst.parentFile, dst.name + ".part")
            temp.delete()
            val copied = runCatching {
                val input = ctx.contentResolver.openInputStream(Uri.parse(m.uri))
                    ?: throw IllegalStateException("无法打开媒体文件")
                input.use { ins -> temp.outputStream().use { out -> ins.copyTo(out) } }
                if (!temp.isFile || temp.length() <= 0L) throw IllegalStateException("媒体文件为空")
                if (!temp.renameTo(dst)) {
                    temp.copyTo(dst, overwrite = true)
                    temp.delete()
                }
                dst.isFile && dst.length() > 0L
            }.getOrDefault(false)
            if (copied) done++ else {
                temp.delete()
                dst.delete()
            }
        }
        return done
    }

    private fun courseFolderId(courseId: String): String {
        return if (courseId.matches(Regex("[A-Za-z0-9._-]{1,120}")) && courseId != "." && courseId != "..") courseId
        else "id_" + Base64.encodeToString(courseId.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun dir(ctx: Context, courseId: String): File = File(File(ctx.filesDir, "media"), courseFolderId(courseId)).apply { mkdirs() }

    /** 删除课程时只清理本应用私有副本，不触碰用户原始 SAF 文件。 */
    fun deleteLocal(ctx: Context, store: Store, courseId: String) {
        runCatching {
            val root = File(ctx.filesDir, "media").canonicalFile
            val target = File(root, courseFolderId(courseId)).canonicalFile
            if (target.parentFile == root && target.exists()) target.deleteRecursively()
        }
        store.mediaTrees.remove(courseId)
        val prefix = "$courseId:"
        store.mediaPos.keys.filter { it.startsWith(prefix) }.toList().forEach { store.mediaPos.remove(it) }
        store.save()
    }


    fun clearLocalCopies(ctx: Context, courseIds: Collection<String>, treeUris: Collection<String>) {
        courseIds.forEach { id ->
            runCatching {
                val root = File(ctx.filesDir, "media").canonicalFile
                val target = File(root, courseFolderId(id)).canonicalFile
                if (target.parentFile == root && target.exists()) target.deleteRecursively()
            }
        }
        treeUris.forEach { raw ->
            runCatching {
                ctx.contentResolver.releasePersistableUriPermission(
                    Uri.parse(raw),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }

    /** 播放地址：优先应用内副本，其次原始 uri */
    fun resolve(ctx: Context, courseId: String, ch: Chapter): String {
        val m = ch.media ?: return ""
        val f = File(dir(ctx, courseId), ch.id + "." + ext(m.fname))
        return if (f.exists()) f.absolutePath else m.uri
    }

    /** 首帧封面 */
    fun poster(ctx: Context, courseId: String, ch: Chapter): File? {
        val m = ch.media ?: return null
        if (m.kind != "video") return null
        val f = File(dir(ctx, courseId), "p_${ch.id}.jpg")
        if (f.exists()) return f
        val src = resolve(ctx, courseId, ch)
        return runCatching {
            val mmr = android.media.MediaMetadataRetriever()
            var bmp: Bitmap? = null
            var scaled: Bitmap? = null
            try {
                if (src.startsWith("/")) mmr.setDataSource(src) else mmr.setDataSource(ctx, Uri.parse(src))
                bmp = mmr.getFrameAtTime(1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                val frame = bmp ?: return@runCatching null
                scaled = Bitmap.createScaledBitmap(frame, 360, (360f * frame.height / frame.width).toInt().coerceAtLeast(1), true)
                FileOutputStream(f).use { scaled!!.compress(Bitmap.CompressFormat.JPEG, 72, it) }
                f
            } finally {
                runCatching { mmr.release() }
                if (scaled !== bmp) runCatching { scaled?.recycle() }
                runCatching { bmp?.recycle() }
            }
        }.getOrNull()
    }

    /** 时长探测（后台逐个读取元数据） */
    fun probeDuration(ctx: Context, app: App, courseId: String, onDone: () -> Unit) {
        val pack = app.packs.of(app.store, courseId)
        Thread {
            var changed = false
            pack.ch.forEach { c ->
                val m = c.media ?: return@forEach
                if (m.dur > 0) return@forEach
                val src = resolve(ctx, courseId, c)
                runCatching {
                    val mmr = android.media.MediaMetadataRetriever()
                    val d = try {
                        if (src.startsWith("/")) mmr.setDataSource(src) else mmr.setDataSource(ctx, Uri.parse(src))
                        mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    } finally {
                        runCatching { mmr.release() }
                    }
                    if (d > 0) { c.media = m.copy(dur = d / 1000); changed = true }
                }
            }
            if (changed) {
                val uc = app.store.userCourses.firstOrNull { it.id == courseId }
                if (uc != null) { uc.packJson = pack.toJson().toString(); app.store.save() }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post(onDone)
        }.start()
    }

    /** tree uri → document uri（只保留文件夹根，便于下次免重选时定位） */
    private fun trimTree(u: String): String {
        val doc = u.indexOf("/document/")
        if (doc < 0) return u
        return u.substring(0, doc)
    }

    /** 启动时恢复：媒体课程的内容包从本机 packJson 直接装载，不重扫文件夹 */
    fun restore(app: App, store: Store) {
        Courses.all(store).filter { it.media.isNotEmpty() }.forEach { co ->
            app.packs.of(store, co.id)
        }
    }
}

