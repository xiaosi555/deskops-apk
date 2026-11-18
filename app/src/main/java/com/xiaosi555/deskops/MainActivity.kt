package com.xiaosi555.deskops

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.provider.DocumentsContract
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private data class DeskMedia(val uri: String, val name: String, val mime: String, val folder: String = "", val addedAt: Long = System.currentTimeMillis())
private data class Course(
    val id: Long,
    val title: String,
    val intro: String,
    val chapters: Int = 0,
    val progress: Int = 0,
    val chapterItems: List<Chapter> = emptyList()
)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.SIMPLIFIED_CHINESE
        }
        window.statusBarColor = android.graphics.Color.rgb(247, 248, 252)
        window.navigationBarColor = android.graphics.Color.rgb(247, 248, 252)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { DeskOpsApp(::speakText, ::availableTtsVoices) }
        Log.i("DeskOpsNativeSmoke", "NATIVE_APP_READY")
    }

    private fun speakText(text: String) {
        if (!ttsReady) {
            Toast.makeText(this, "系统语音引擎尚未就绪，请检查手机是否安装并启用 TTS 服务", Toast.LENGTH_LONG).show()
            return
        }
        val prefs = getSharedPreferences("deskops_native", 0)
        tts?.setSpeechRate(prefs.getFloat("tts_rate", 1.0f).coerceIn(0.5f, 2.0f))
        tts?.setPitch(prefs.getFloat("tts_pitch", 1.0f).coerceIn(0.5f, 2.0f))
        val voiceName = prefs.getString("tts_voice", "") ?: ""
        if (voiceName.isNotBlank()) tts?.voices?.firstOrNull { it.name == voiceName }?.let { tts?.voice = it }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "deskops-reading")
    }

    private fun availableTtsVoices(): List<Pair<String, String>> {
        return tts?.voices?.sortedBy { it.locale.displayName }?.map { voice ->
            voice.name to (voice.locale.displayName + " · " + voice.name)
        } ?: emptyList()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeskOpsApp(speak: (String) -> Unit, getTtsVoices: () -> List<Pair<String, String>>) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("deskops_native", 0) }
    val media = remember { mutableStateListOf<DeskMedia>().apply { addAll(readMedia(prefs.getString("media", "[]") ?: "[]")) } }
    val courses = remember { mutableStateListOf<Course>().apply { addAll(readCourses(prefs.getString("courses", "[]") ?: "[]")) } }
    val highlights = remember { mutableStateListOf<Highlight>().apply { addAll(LearningDataCodec.decodeHighlights(prefs.getString("highlights", "[]") ?: "[]")) } }
    val sessions = remember { mutableStateListOf<StudySession>().apply { addAll(LearningDataCodec.decodeSessions(prefs.getString("sessions", "[]") ?: "[]")) } }
    var tab by remember { mutableStateOf("学习") }
    var page by remember { mutableStateOf("root") }
    var selectedMedia by remember { mutableStateOf<DeskMedia?>(null) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var activeCourseId by remember { mutableLongStateOf(0L) }
    var activeChapterId by remember { mutableLongStateOf(0L) }
    var activeResumePosition by remember { mutableLongStateOf(0L) }
    var showAddCourse by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var deleteCourseTarget by remember { mutableStateOf<Course?>(null) }
    var showChapterEditor by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<Chapter?>(null) }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterContent by remember { mutableStateOf("") }
    var chapterMediaUri by remember { mutableStateOf("") }
    var courseTitle by remember { mutableStateOf("") }
    var courseIntro by remember { mutableStateOf("") }
    var themeChoice by remember { mutableIntStateOf(prefs.getInt("theme", 0)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var ttsRate by remember { mutableFloatStateOf(prefs.getFloat("tts_rate", 1.0f)) }
    var ttsPitch by remember { mutableFloatStateOf(prefs.getFloat("tts_pitch", 1.0f)) }
    var ttsVoice by remember { mutableStateOf(prefs.getString("tts_voice", "") ?: "") }
    val palettes = listOf(Color(0xFF6757D9), Color(0xFF2878D0), Color(0xFF168C76), Color(0xFFE18A35))
    val primary = palettes[themeChoice.coerceIn(0, 3)]
    val bg = if (darkMode) Color(0xFF11131A) else Color(0xFFF6F7FB)
    val surface = if (darkMode) Color(0xFF1B1E28) else Color.White
    val fg = if (darkMode) Color(0xFFF2F3F8) else Color(0xFF202332)
    val sub = if (darkMode) Color(0xFFAAAFBF) else Color(0xFF777D8E)

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            val backup = LearningDataCodec.encodeBackup(writeCourses(courses), writeMedia(media), LearningDataCodec.encodeHighlights(highlights), LearningDataCodec.encodeSessions(sessions))
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(backup) }
                ?: error("无法写入所选文件")
            Toast.makeText(context, "学习数据备份已导出", Toast.LENGTH_LONG).show()
        }.onFailure { Toast.makeText(context, "导出失败：" + (it.message ?: "未知错误"), Toast.LENGTH_LONG).show() }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取备份文件")
            val root = LearningDataCodec.validateBackup(raw)
            val importedCourses = readCourses(root.getJSONArray("courses").toString())
            val importedMedia = readMedia(root.optJSONArray("media")?.toString() ?: "[]")
            val importedHighlights = LearningDataCodec.decodeHighlights(root.optJSONArray("highlights")?.toString() ?: "[]")
            val importedSessions = LearningDataCodec.decodeSessions(root.optJSONArray("sessions")?.toString() ?: "[]")
            courses.clear(); courses.addAll(importedCourses)
            media.clear(); media.addAll(importedMedia)
            highlights.clear(); highlights.addAll(importedHighlights)
            sessions.clear(); sessions.addAll(importedSessions)
            prefs.edit().putString("courses", writeCourses(courses)).putString("media", writeMedia(media))
                .putString("highlights", LearningDataCodec.encodeHighlights(highlights))
                .putString("sessions", LearningDataCodec.encodeSessions(sessions)).apply()
            selectedCourse = null
            Toast.makeText(context, "已导入课程、媒体、摘录和学习记录", Toast.LENGTH_LONG).show()
        }.onFailure { Toast.makeText(context, "导入失败：" + (it.message ?: "备份文件无效"), Toast.LENGTH_LONG).show() }
    }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            val name = queryDisplayName(context, uri) ?: "媒体文件"
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            if (media.none { it.uri == uri.toString() }) media.add(DeskMedia(uri.toString(), name, mime, addedAt = queryLastModified(context, uri)))
        }
        prefs.edit().putString("media", writeMedia(media)).apply()
        if (uris.isNotEmpty()) Toast.makeText(context, "已添加 ${uris.size} 个媒体文件", Toast.LENGTH_SHORT).show()
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            try { context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: Exception) {}
            val root = DocumentFile.fromTreeUri(context, tree)
            val found = mutableListOf<DeskMedia>()
            fun scan(dir: DocumentFile, depth: Int) {
                if (depth > 5) return
                dir.listFiles().forEach { f ->
                    if (f.isDirectory) scan(f, depth + 1)
                    else {
                        val mime = f.type ?: ""
                        val name = f.name ?: "媒体文件"
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (mime.startsWith("video/") || mime.startsWith("audio/") || ext in listOf("mp4","mkv","mov","webm","3gp","avi","mp3","m4a","aac","wav","flac","ogg","opus")) {
                            found.add(DeskMedia(f.uri.toString(), name, mime, dir.name ?: "", f.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()))
                        }
                    }
                }
            }
            if (root != null) scan(root, 0)
            found.forEach { item -> if (media.none { it.uri == item.uri }) media.add(item) }
            prefs.edit().putString("media", writeMedia(media)).apply()
            Toast.makeText(context, "扫描完成，找到 ${found.size} 个媒体文件", Toast.LENGTH_LONG).show()
        }
    }

    fun persistCourse(updated: Course) {
        val index = courses.indexOfFirst { it.id == updated.id }
        if (index >= 0) courses[index] = updated
        selectedCourse = updated
        prefs.edit().putString("courses", writeCourses(courses)).apply()
    }

    BackHandler(enabled = page != "root") {
        if (page == "player") selectedMedia = null
        page = "root"
    }

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(primary = primary, background = bg, surface = surface, onSurface = fg) else lightColorScheme(primary = primary, background = bg, surface = surface, onSurface = fg)) {
        Scaffold(
            containerColor = bg,
            topBar = {
                if (page != "player") {
                    CenterAlignedTopAppBar(
                        title = { Text(if (page == "course") selectedCourse?.title ?: "课程" else when(tab) { "学习" -> "学习空间"; "课程" -> "课程管理"; "媒体" -> "媒体资料库"; else -> "我的" }, fontWeight = FontWeight.Bold, color = fg) },
                        navigationIcon = { if (page != "root") IconButton(onClick = { page = "root" }) { Icon(Icons.Rounded.ArrowBack, null, tint = fg) } },
                        actions = {
                            if (tab == "课程" && page == "root") IconButton(onClick = { showAddCourse = true }) { Icon(Icons.Rounded.Add, "新建课程", tint = primary) }
                            if (tab == "媒体" && page == "root") IconButton(onClick = { mediaPicker.launch(arrayOf("video/*", "audio/*")) }) { Icon(Icons.Rounded.Add, "导入媒体", tint = primary) }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bg)
                    )
                }
            },
            bottomBar = {
                if (page == "root") NavigationBar(containerColor = surface, tonalElevation = 0.dp) {
                    listOf("学习" to Icons.Rounded.AutoStories, "课程" to Icons.Rounded.CollectionsBookmark, "媒体" to Icons.Rounded.PlayCircle, "我的" to Icons.Rounded.Person).forEach { (label, icon) ->
                        NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Icon(icon, null) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = primary, selectedTextColor = primary))
                    }
                }
            }
        ) { padding ->
            when {
                page == "player" && selectedMedia != null -> NativePlayer(
                    selectedMedia!!, Modifier.padding(padding), fg, sub, primary, activeResumePosition,
                    onSession = { duration, position ->
                        if (activeCourseId != 0L) {
                            courses.firstOrNull { it.id == activeCourseId }?.let { course ->
                                val updated = course.copy(chapterItems = course.chapterItems.map { if (it.id == activeChapterId) it.copy(lastPositionMs = position) else it })
                                persistCourse(updated)
                            }
                        }
                        if (duration >= 5000L) {
                            val day = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                            sessions.add(StudySession(System.currentTimeMillis(), activeCourseId, activeChapterId, duration, day))
                            prefs.edit().putString("sessions", LearningDataCodec.encodeSessions(sessions)).apply()
                        }
                    }
                )
                page == "course" && selectedCourse != null -> CourseDetail(
                    selectedCourse!!, Modifier.padding(padding), fg, sub, surface, primary, speak,
                    onAddChapter = { editingChapter = null; chapterTitle = ""; chapterContent = ""; chapterMediaUri = ""; showChapterEditor = true },
                    onEditChapter = { ch -> editingChapter = ch; chapterTitle = ch.title; chapterContent = ch.content; chapterMediaUri = ch.mediaUri; showChapterEditor = true },
                    onToggleChapter = { ch ->
                        val items = selectedCourse!!.chapterItems.map { if (it.id == ch.id) it.copy(completed = !it.completed) else it }
                        val done = items.count { it.completed }
                        persistCourse(selectedCourse!!.copy(chapters = items.size, chapterItems = items, progress = if (items.isEmpty()) 0 else done * 100 / items.size))
                    },
                    onDeleteChapter = { ch -> persistCourse(selectedCourse!!.copy(chapters = (selectedCourse!!.chapterItems.size - 1).coerceAtLeast(0), chapterItems = selectedCourse!!.chapterItems.filterNot { it.id == ch.id })) },
                    onPlayMedia = { uri, chapterId -> media.firstOrNull { it.uri == uri }?.let {
                        selectedMedia = it; activeCourseId = selectedCourse!!.id; activeChapterId = chapterId
                        activeResumePosition = selectedCourse!!.chapterItems.firstOrNull { ch -> ch.id == chapterId }?.lastPositionMs ?: 0L
                        page = "player"
                    } },
                    onSaveHighlight = { ch ->
                        if (ch.content.isNotBlank()) {
                            highlights.add(Highlight(System.currentTimeMillis(), selectedCourse!!.id, ch.id, ch.content))
                            prefs.edit().putString("highlights", LearningDataCodec.encodeHighlights(highlights)).apply()
                            Toast.makeText(context, "已保存本章摘录", Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(context, "本章暂无正文可保存", Toast.LENGTH_SHORT).show()
                    },
                    highlights = highlights.filter { it.courseId == selectedCourse!!.id },
                    onDeleteHighlight = { item ->
                        highlights.removeAll { it.id == item.id }
                        prefs.edit().putString("highlights", LearningDataCodec.encodeHighlights(highlights)).apply()
                    }
                )
                page == "root" -> when(tab) {
                    "学习" -> LearningHome(courses, media, sessions, Modifier.padding(padding), fg, sub, surface, primary) { c -> selectedCourse = c; page = "course" }
                    "课程" -> CourseList(courses, Modifier.padding(padding), fg, sub, surface, primary,
                        onOpen = { selectedCourse = it; page = "course" },
                        onAdd = { editingCourse = null; courseTitle = ""; courseIntro = ""; showAddCourse = true },
                        onEdit = { editingCourse = it; courseTitle = it.title; courseIntro = it.intro; showAddCourse = true },
                        onDelete = { deleteCourseTarget = it })
                    "媒体" -> MediaLibrary(media, Modifier.padding(padding), fg, sub, surface, primary,
                        onPlay = { activeCourseId = 0L; activeChapterId = 0L; activeResumePosition = 0L; selectedMedia = it; page = "player" },
                        onImportFiles = { mediaPicker.launch(arrayOf("video/*", "audio/*")) },
                        onImportFolder = { folderPicker.launch(null) },
                        onRemove = { item -> media.remove(item); prefs.edit().putString("media", writeMedia(media)).apply() },
                        onCreateCourse = { selected, kind ->
                            if (selected.isNotEmpty()) {
                                val now = System.currentTimeMillis()
                                val chapters = selected.mapIndexed { index, item ->
                                    Chapter(now + index, item.name.substringBeforeLast('.', item.name), "", item.uri)
                                }
                                val title = (if (kind == "音频") "音频课程 · " else "视频课程 · ") + java.text.SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(now))
                                val course = Course(now, title, "由媒体资料库导入，共 " + chapters.size + " 个章节", chapters.size, 0, chapters)
                                courses.add(course); prefs.edit().putString("courses", writeCourses(courses)).apply()
                                selectedCourse = course; tab = "课程"; page = "course"
                                Toast.makeText(context, "已创建课程，章节按当前排序排列", Toast.LENGTH_LONG).show()
                            }
                        })
                    else -> SettingsScreen(themeChoice, darkMode, Modifier.padding(padding), fg, sub, surface, primary,
                        onTheme = { themeChoice = it; prefs.edit().putInt("theme", it).apply() },
                        onDark = { darkMode = it; prefs.edit().putBoolean("dark", it).apply() },
                        onExport = { exportBackupLauncher.launch("deskops-learning-backup.json") },
                        onImport = { importBackupLauncher.launch(arrayOf("application/json", "text/*")) },
                        ttsRate = ttsRate, ttsPitch = ttsPitch, ttsVoice = ttsVoice, getTtsVoices = getTtsVoices,
                        onTtsRate = { ttsRate = it; prefs.edit().putFloat("tts_rate", it).apply() },
                        onTtsPitch = { ttsPitch = it; prefs.edit().putFloat("tts_pitch", it).apply() },
                        onTtsVoice = { ttsVoice = it; prefs.edit().putString("tts_voice", it).apply() })
                }
            }
        }
    }

    if (deleteCourseTarget != null) AlertDialog(
        onDismissRequest = { deleteCourseTarget = null },
        title = { Text("删除课程？") },
        text = { Text("删除后课程章节和本地学习进度将从应用中移除，此操作无法撤销。") },
        confirmButton = {
            TextButton(onClick = {
                val target = deleteCourseTarget!!
                courses.removeAll { it.id == target.id }
                highlights.removeAll { it.courseId == target.id }
                sessions.removeAll { it.courseId == target.id }
                prefs.edit().putString("courses", writeCourses(courses))
                    .putString("highlights", LearningDataCodec.encodeHighlights(highlights))
                    .putString("sessions", LearningDataCodec.encodeSessions(sessions)).apply()
                if (selectedCourse?.id == target.id) { selectedCourse = null; page = "root" }
                deleteCourseTarget = null
            }) { Text("删除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { deleteCourseTarget = null }) { Text("取消") } }
    )

    if (showChapterEditor && selectedCourse != null) AlertDialog(
        onDismissRequest = { showChapterEditor = false },
        title = { Text(if (editingChapter == null) "新增章节" else "编辑章节") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(chapterTitle, { chapterTitle = it }, label = { Text("章节名称") }, singleLine = true)
                OutlinedTextField(chapterContent, { chapterContent = it }, label = { Text("章节正文") }, minLines = 4)
                Text("关联媒体（可选）", style = MaterialTheme.typography.labelMedium)
                var mediaExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = mediaExpanded, onExpandedChange = { mediaExpanded = !mediaExpanded }) {
                    OutlinedTextField(
                        value = media.firstOrNull { it.uri == chapterMediaUri }?.name ?: "不关联媒体",
                        onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mediaExpanded) }
                    )
                    ExposedDropdownMenu(expanded = mediaExpanded, onDismissRequest = { mediaExpanded = false }) {
                        DropdownMenuItem(text = { Text("不关联媒体") }, onClick = { chapterMediaUri = ""; mediaExpanded = false })
                        media.forEach { item -> DropdownMenuItem(text = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { chapterMediaUri = item.uri; mediaExpanded = false }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (chapterTitle.isNotBlank()) {
                    val old = selectedCourse!!
                    val ch = Chapter(editingChapter?.id ?: System.currentTimeMillis(), chapterTitle.trim(), chapterContent.trim(), chapterMediaUri, editingChapter?.completed ?: false, editingChapter?.lastPositionMs ?: 0L)
                    val items = if (editingChapter == null) old.chapterItems + ch else old.chapterItems.map { if (it.id == ch.id) ch else it }
                    persistCourse(old.copy(chapters = items.size, chapterItems = items))
                    showChapterEditor = false
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = { showChapterEditor = false }) { Text("取消") } }
    )

    if (showAddCourse) AlertDialog(
        onDismissRequest = { showAddCourse = false },
        title = { Text(if (editingCourse == null) "新建课程" else "编辑课程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(courseTitle, { courseTitle = it }, label = { Text("课程名称") }, singleLine = true)
                OutlinedTextField(courseIntro, { courseIntro = it }, label = { Text("课程简介") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (courseTitle.isNotBlank()) {
                    val editing = editingCourse
                    if (editing == null) courses.add(Course(System.currentTimeMillis(), courseTitle.trim(), courseIntro.trim()))
                    else {
                        val updated = editing.copy(title = courseTitle.trim(), intro = courseIntro.trim())
                        val index = courses.indexOfFirst { it.id == editing.id }
                        if (index >= 0) courses[index] = updated
                        if (selectedCourse?.id == editing.id) selectedCourse = updated
                    }
                    prefs.edit().putString("courses", writeCourses(courses)).apply()
                    editingCourse = null; courseTitle = ""; courseIntro = ""; showAddCourse = false
                }
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = { showAddCourse = false; editingCourse = null }) { Text("取消") } }
    )
}

@Composable
private fun LearningHome(courses: List<Course>, media: List<DeskMedia>, sessions: List<StudySession>, modifier: Modifier, fg: Color, sub: Color, surface: Color, primary: Color, open: (Course) -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(primary).padding(22.dp)) {
                Text("把知识，变成能力", color = Color.White.copy(alpha = .78f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("今天也向前一步。", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    StatText("课程", courses.size.toString())
                    StatText("媒体", media.size.toString())
                    StatText("学习天数", "1")
                }
            }
        }
        item { StudyCalendarCard(sessions, surface, fg, sub, primary) }
        item { SectionTitle("继续学习", fg) }
        if (courses.isEmpty()) item { EmptyCard("还没有课程", "创建课程，整理你的学习内容", surface, sub) }
        items(courses.take(4)) { c -> CourseCard(c, surface, fg, sub, primary) { open(c) } }
        item { SectionTitle("最近导入", fg) }
        if (media.isEmpty()) item { EmptyCard("媒体资料库为空", "导入视频或音频，开始离线学习", surface, sub) }
        items(media.take(3)) { m -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (m.mime.startsWith("audio")) Icons.Rounded.Headphones else Icons.Rounded.PlayCircle, null, tint = primary); Spacer(Modifier.width(12.dp)); Column { Text(m.name, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(m.mime, color = sub, fontSize = 12.sp) } } }
    }
}

@Composable
private fun CourseList(courses: List<Course>, modifier: Modifier, fg: Color, sub: Color, surface: Color, primary: Color, onOpen: (Course) -> Unit, onAdd: () -> Unit, onEdit: (Course) -> Unit, onDelete: (Course) -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("我的课程", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = fg); Text("${courses.size} 门课程", color = sub, fontSize = 13.sp) }; FilledTonalButton(onClick = onAdd) { Icon(Icons.Rounded.Add, null); Text("新建") } } }
        if (courses.isEmpty()) item { EmptyCard("还没有课程", "点击右上角新建课程", surface, sub) }
        items(courses, key = { it.id }) { course ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { CourseCard(course, surface, fg, sub, primary) { onOpen(course) } }
                Column {
                    IconButton(onClick = { onEdit(course) }) { Icon(Icons.Rounded.Edit, "编辑课程", tint = primary) }
                    IconButton(onClick = { onDelete(course) }) { Icon(Icons.Rounded.DeleteOutline, "删除课程", tint = sub) }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, surface: Color, fg: Color, sub: Color, primary: Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(surface).clickable(onClick = onClick).padding(17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MenuBook, null, tint = primary) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(course.title, fontWeight = FontWeight.SemiBold, color = fg, fontSize = 16.sp); Text(course.intro.ifBlank { "尚未添加课程简介" }, color = sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Icon(Icons.Rounded.ChevronRight, null, tint = sub)
        }
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("${course.chapters} 个章节", color = sub, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text("${course.progress}%", color = primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(progress = { course.progress / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = primary, trackColor = primary.copy(alpha = .12f))
    }
}

@Composable
private fun CourseDetail(
    course: Course, modifier: Modifier, fg: Color, sub: Color, surface: Color, primary: Color,
    speak: (String) -> Unit, onAddChapter: () -> Unit, onEditChapter: (Chapter) -> Unit,
    onToggleChapter: (Chapter) -> Unit, onDeleteChapter: (Chapter) -> Unit, onPlayMedia: (String, Long) -> Unit,
    onSaveHighlight: (Chapter) -> Unit, highlights: List<Highlight>, onDeleteHighlight: (Highlight) -> Unit
) {
    var expandedChapter by remember { mutableStateOf<Long?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(primary.copy(alpha = .12f)).padding(20.dp)) {
                Text("课程介绍", color = primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(course.title, color = fg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(course.intro.ifBlank { "为这门课程添加简介与学习目标。" }, color = sub, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(course.chapterItems.count { it.completed }.toString() + "/" + course.chapterItems.size + " 章节完成", color = sub, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(course.progress.toString() + "%", color = primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(progress = { course.progress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = primary, trackColor = primary.copy(alpha = .16f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("课程章节", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onAddChapter) { Icon(Icons.Rounded.Add, null); Text("新增章节") }
            }
        }
        if (course.chapterItems.isEmpty()) item { EmptyCard("还没有章节", "新增章节后可填写正文并关联媒体文件", surface, sub) }
        items(course.chapterItems, key = { it.id }) { chapter ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = chapter.completed, onCheckedChange = { onToggleChapter(chapter) })
                    Column(Modifier.weight(1f).clickable { expandedChapter = if (expandedChapter == chapter.id) null else chapter.id }) {
                        Text(chapter.title, color = fg, fontWeight = FontWeight.SemiBold)
                        Text(if (chapter.completed) "已完成" else "未完成", color = sub, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onEditChapter(chapter) }) { Icon(Icons.Rounded.Edit, "编辑章节", tint = primary) }
                    IconButton(onClick = { onDeleteChapter(chapter) }) { Icon(Icons.Rounded.DeleteOutline, "删除章节", tint = sub) }
                }
                if (expandedChapter == chapter.id) {
                    if (chapter.content.isNotBlank()) {
                        Text(chapter.content, color = fg, lineHeight = 23.sp, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
                        TextButton(onClick = { speak(chapter.content) }) { Icon(Icons.Rounded.VolumeUp, null); Text("朗读本章") }
                    } else Text("本章还没有正文内容", color = sub, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                    if (chapter.content.isNotBlank()) OutlinedButton(onClick = { onSaveHighlight(chapter) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text("收藏本章为摘录") }
                    if (chapter.mediaUri.isNotBlank()) OutlinedButton(onClick = { onPlayMedia(chapter.mediaUri, chapter.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PlayCircle, null); Spacer(Modifier.width(6.dp)); Text("播放关联媒体")
                    }
                }
            }
        }
        if (highlights.isNotEmpty()) {
            item {
                Text("本课程摘录", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            items(highlights.sortedByDescending { it.createdAt }, key = { it.id }) { highlight ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(highlight.text, color = fg, maxLines = 5, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                        Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(highlight.createdAt)), color = sub, fontSize = 11.sp)
                    }
                    IconButton(onClick = { onDeleteHighlight(highlight) }) { Icon(Icons.Rounded.DeleteOutline, "删除摘录", tint = sub) }
                }
            }
        }
    }
}

@Composable
private fun MediaLibrary(media: List<DeskMedia>, modifier: Modifier, fg: Color, sub: Color, surface: Color, primary: Color, onPlay: (DeskMedia) -> Unit, onImportFiles: () -> Unit, onImportFolder: () -> Unit, onRemove: (DeskMedia) -> Unit, onCreateCourse: (List<DeskMedia>, String) -> Unit) {
    var sortByTime by remember { mutableStateOf(false) }
    var mediaKind by remember { mutableStateOf("视频") }
    val sortedMedia = if (sortByTime) media.sortedByDescending { it.addedAt } else media.sortedBy { it.name.lowercase() }
    val filteredMedia = sortedMedia.filter { if (mediaKind == "音频") isAudioMedia(it) else !isAudioMedia(it) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(surface).padding(17.dp)) {
                Text("媒体资料库", color = fg, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("直接读取手机本地视频与音频文件", color = sub, fontSize = 13.sp)
                Spacer(Modifier.height(15.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(onClick = onImportFiles, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.FileOpen, null); Spacer(Modifier.width(5.dp)); Text("选择文件") }
                    OutlinedButton(onClick = onImportFolder, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(5.dp)); Text("导入文件夹") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("已导入 · " + media.size, color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { sortByTime = false }) { Text("按名称", color = if (!sortByTime) primary else sub) }
                TextButton(onClick = { sortByTime = true }) { Text("按时间", color = if (sortByTime) primary else sub) }
            }
        }
        if (media.isEmpty()) item { EmptyCard("还没有媒体文件", "选择文件或文件夹导入视频、音频", surface, sub) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("课程导入类型", color = fg, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { mediaKind = "视频" }) { Text("视频", color = if (mediaKind == "视频") primary else sub) }
                    TextButton(onClick = { mediaKind = "音频" }) { Text("音频", color = if (mediaKind == "音频") primary else sub) }
                }
                Text("当前筛选 " + filteredMedia.size + " 个文件，创建后会按上方排序生成章节。", color = sub, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onCreateCourse(filteredMedia, mediaKind) }, enabled = filteredMedia.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.LibraryAdd, null); Spacer(Modifier.width(7.dp)); Text("将这些文件生成" + mediaKind + "课程")
                }
            }
        }
        items(filteredMedia, key = { it.uri }) { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(surface).clickable { onPlay(item) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(if (item.mime.startsWith("audio")) Icons.Rounded.Headphones else Icons.Rounded.PlayArrow, null, tint = primary) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(item.name, color = fg, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.folder.ifBlank { item.mime }, color = sub, fontSize = 11.sp) }
                IconButton(onClick = { onRemove(item) }) { Icon(Icons.Rounded.Close, "移除", tint = sub) }
                Icon(Icons.Rounded.ChevronRight, null, tint = sub)
            }
        }
    }
}

@Composable
private fun NativePlayer(media: DeskMedia, modifier: Modifier, fg: Color, sub: Color, primary: Color, initialPositionMs: Long, onSession: (Long, Long) -> Unit) {
    val context = LocalContext.current
    val startedAt = remember(media.uri) { android.os.SystemClock.elapsedRealtime() }
    val player = remember(media.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(Uri.parse(media.uri)))
            prepare()
            if (initialPositionMs > 0L) seekTo(initialPositionMs)
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Toast.makeText(context, "媒体播放失败：" + (error.message ?: "格式不受支持或文件无法读取"), Toast.LENGTH_LONG).show()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            onSession((android.os.SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L), player.currentPosition)
            player.release()
        }
    }
    Column(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } }, update = { it.player = player }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Column(Modifier.fillMaxWidth().background(Color(0xFF171923)).padding(18.dp)) {
            Text(media.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Text("Android 原生播放器 · Media3", color = Color(0xFFB6BAC8), fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { player.seekTo((player.currentPosition - 15000).coerceAtLeast(0)) }) { Icon(Icons.Rounded.Replay10, null); Text("-15秒") }
                FilledTonalButton(onClick = { player.seekTo((player.currentPosition + 15000).coerceAtMost(player.duration.coerceAtLeast(0L))) }) { Icon(Icons.Rounded.Forward10, null); Text("+15秒") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val selected = kotlin.math.abs(player.playbackParameters.speed - speed) < 0.01f
                    TextButton(onClick = { player.playbackParameters = player.playbackParameters.withSpeed(speed) }, modifier = Modifier.weight(1f)) {
                        Text(speed.toString() + "×", color = if (selected) primary else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(theme: Int, dark: Boolean, modifier: Modifier, fg: Color, sub: Color, surface: Color, primary: Color, onTheme: (Int) -> Unit, onDark: (Boolean) -> Unit, onExport: () -> Unit, onImport: () -> Unit, ttsRate: Float, ttsPitch: Float, ttsVoice: String, getTtsVoices: () -> List<Pair<String, String>>, onTtsRate: (Float) -> Unit, onTtsPitch: (Float) -> Unit, onTtsVoice: (String) -> Unit) {
    val colors = listOf(Color(0xFF6757D9), Color(0xFF2878D0), Color(0xFF168C76), Color(0xFFE18A35))
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("个性化", color = fg, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(17.dp)) {
                Text("主题颜色", color = fg, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) { colors.forEachIndexed { i, c -> Box(Modifier.size(38.dp).clip(CircleShape).background(c).clickable { onTheme(i) }, contentAlignment = Alignment.Center) { if (theme == i) Icon(Icons.Rounded.Check, null, tint = Color.White) } } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).clickable { onDark(!dark) }.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, tint = primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("深色模式", color = fg, fontWeight = FontWeight.SemiBold); Text("根据环境切换阅读外观", color = sub, fontSize = 12.sp) }
                Switch(checked = dark, onCheckedChange = onDark)
            }
        }
        item {
            var voiceExpanded by remember { mutableStateOf(false) }
            var voices by remember { mutableStateOf(getTtsVoices()) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("朗读设置", color = fg, fontWeight = FontWeight.SemiBold)
                Text("语速  " + String.format(Locale.getDefault(), "%.1f×", ttsRate), color = sub, fontSize = 12.sp)
                Slider(value = ttsRate, onValueChange = onTtsRate, valueRange = 0.5f..2.0f)
                Text("音调  " + String.format(Locale.getDefault(), "%.1f", ttsPitch), color = sub, fontSize = 12.sp)
                Slider(value = ttsPitch, onValueChange = onTtsPitch, valueRange = 0.5f..2.0f)
                ExposedDropdownMenuBox(expanded = voiceExpanded, onExpandedChange = { voiceExpanded = !voiceExpanded }) {
                    OutlinedTextField(
                        value = voices.firstOrNull { it.first == ttsVoice }?.second ?: if (ttsVoice.isBlank()) "系统默认音色" else ttsVoice,
                        onValueChange = {}, readOnly = true, label = { Text("系统语音音色") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(voiceExpanded) }
                    )
                    ExposedDropdownMenu(expanded = voiceExpanded, onDismissRequest = { voiceExpanded = false }) {
                        DropdownMenuItem(text = { Text("系统默认音色") }, onClick = { onTtsVoice(""); voiceExpanded = false })
                        voices.forEach { voice -> DropdownMenuItem(text = { Text(voice.second, maxLines = 2) }, onClick = { onTtsVoice(voice.first); voiceExpanded = false }) }
                    }
                }
                OutlinedButton(onClick = { voices = getTtsVoices() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(6.dp)); Text("重新读取手机已安装音色")
                }
                if (voices.isEmpty()) Text("暂未读取到音色。请先在手机系统设置中安装或启用语音服务，再点击刷新。", color = sub, fontSize = 12.sp)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("数据管理", color = fg, fontWeight = FontWeight.SemiBold)
                Text("完整备份课程章节、媒体索引、学习摘录与学习记录；更换手机前建议先导出备份。", color = sub, fontSize = 12.sp)
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("导出完整学习数据") }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("导入学习数据备份") }
            }
        }
        item { Text("桌面运维学习 · Android 原生版 2.0", color = sub, fontSize = 12.sp) }
    }
}

@Composable
private fun StudyCalendarCard(sessions: List<StudySession>, surface: Color, fg: Color, sub: Color, primary: Color) {
    val calendar = java.util.Calendar.getInstance()
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val days = (6 downTo 0).map { offset ->
        val day = calendar.clone() as java.util.Calendar
        day.add(java.util.Calendar.DAY_OF_YEAR, -offset)
        format.format(day.time)
    }
    val minutes = days.map { day -> sessions.filter { it.day == day }.sumOf { it.durationMs } / 60000L }
    val maxValue = (minutes.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val totalMinutes = sessions.sumOf { it.durationMs } / 60000L
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(surface).padding(17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("学习日历", color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("近 7 天的媒体学习时长", color = sub, fontSize = 12.sp)
            }
            Text(if (totalMinutes >= 60) (totalMinutes / 60).toString() + "小时" + (totalMinutes % 60) + "分" else totalMinutes.toString() + "分钟", color = primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            days.forEachIndexed { index, day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (minutes[index] > 0) minutes[index].toString() else "", color = sub, fontSize = 9.sp)
                    Box(Modifier.fillMaxWidth().height((8 + 48f * minutes[index] / maxValue).dp).clip(RoundedCornerShape(5.dp)).background(if (minutes[index] > 0) primary else primary.copy(alpha = .10f)))
                    Text(day.takeLast(5).replace("-", "/"), color = sub, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
        Text("累计学习记录 " + sessions.size + " 次", color = sub, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun SectionTitle(title: String, fg: Color) { Text(title, color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
@Composable
private fun StatText(label: String, value: String) { Column { Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(label, color = Color.White.copy(alpha = .75f), fontSize = 12.sp) } }
@Composable
private fun EmptyCard(title: String, detail: String, surface: Color, sub: Color) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.Inbox, null, tint = sub, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(9.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(detail, color = sub, fontSize = 12.sp) } }

private fun isAudioMedia(item: DeskMedia): Boolean {
    val ext = item.name.substringAfterLast('.', "").lowercase()
    return item.mime.startsWith("audio/") || ext in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "wma")
}

private fun queryLastModified(context: android.content.Context, uri: Uri): Long {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else System.currentTimeMillis()
        } ?: System.currentTimeMillis()
    }.getOrDefault(System.currentTimeMillis())
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return try { context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } } catch (_: Exception) { null }
}
private fun writeMedia(list: List<DeskMedia>): String = JSONArray().apply {
    list.forEach { put(JSONObject().put("uri", it.uri).put("name", it.name).put("mime", it.mime).put("folder", it.folder).put("addedAt", it.addedAt)) }
}.toString()
private fun readMedia(raw: String): List<DeskMedia> = try {
    val a = JSONArray(raw)
    (0 until a.length()).map { val o = a.getJSONObject(it); DeskMedia(o.optString("uri"), o.optString("name"), o.optString("mime"), o.optString("folder"), o.optLong("addedAt", System.currentTimeMillis())) }
} catch (_: Exception) { emptyList() }
private fun writeCourses(list: List<Course>): String = JSONArray().apply {
    list.forEach { course ->
        val chapters = JSONArray().apply {
            course.chapterItems.forEach { chapter ->
                put(JSONObject().put("id", chapter.id).put("title", chapter.title).put("content", chapter.content)
                    .put("mediaUri", chapter.mediaUri).put("completed", chapter.completed).put("lastPositionMs", chapter.lastPositionMs))
            }
        }
        put(JSONObject().put("id", course.id).put("title", course.title).put("intro", course.intro)
            .put("chapters", course.chapterItems.size.coerceAtLeast(course.chapters)).put("progress", course.progress).put("chapterItems", chapters))
    }
}.toString()
private fun readCourses(raw: String): List<Course> = try {
    val a = JSONArray(raw)
    (0 until a.length()).map { index ->
        val o = a.getJSONObject(index)
        val chapterArray = o.optJSONArray("chapterItems") ?: JSONArray()
        val chapterItems = (0 until chapterArray.length()).map { ci ->
            val ch = chapterArray.getJSONObject(ci)
            Chapter(ch.optLong("id"), ch.optString("title"), ch.optString("content"), ch.optString("mediaUri"),
                ch.optBoolean("completed"), ch.optLong("lastPositionMs"))
        }
        Course(o.optLong("id"), o.optString("title"), o.optString("intro"), chapterItems.size.coerceAtLeast(o.optInt("chapters")),
            o.optInt("progress"), chapterItems)
    }
} catch (_: Exception) { emptyList() }
