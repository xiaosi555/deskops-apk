package com.qwen.opsmentor.ui
import com.qwen.opsmentor.core.surface
import android.graphics.Rect
import android.graphics.Canvas
import android.graphics.Bitmap
import java.io.FileOutputStream
import java.io.File

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.qwen.opsmentor.App
import com.qwen.opsmentor.R
import com.qwen.opsmentor.core.DecoView
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.MarkEngine
import com.qwen.opsmentor.core.Reader
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconBtn
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.state.Bus
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.ui.pages.*

import com.qwen.opsmentor.core.add
/**
 * 单 Activity 外壳：顶部装饰 + 页面容器 + 顶栏 + 底部双 Tab + 迷你继续学习条。
 * 路由模型与 Web 完全一致（当前视图 view + 返回栈 navStack + Tab 归属判定）。
 */
class MainActivity : AppCompatActivity(), ScrollOwner {

    lateinit var app: App
        private set
    lateinit var selectionBar: com.qwen.opsmentor.core.SelectionBar
        private set

    private lateinit var root: FrameLayout
    private lateinit var deco: DecoView
    private lateinit var stage: FrameLayout
    private lateinit var appbar: LinearLayout
    private lateinit var abTitleBox: LinearLayout
    private lateinit var abActions: LinearLayout
    private lateinit var abEdit: View
    private lateinit var tabbar: LinearLayout
    private lateinit var tabBarInner: LinearLayout
    private lateinit var mini: MiniBar

    private val pages = HashMap<String, Page>()
    private var view = "mine"
    private val navStack = ArrayList<String>()
    private var topPad = 0
    private var bottomPad = 0

    /** Tab 页（进入时清空返回栈、不显示顶栏） */
    val tabs = linkedMapOf("mine" to "课程", "profile" to "我的")
    private val profileSubs = setOf("calendar", "my-marks", "course-manage", "settings", "shortcuts", "player", "io", "personal-data", "course-import")

    private val titles = mapOf(
        "know" to "知识详解", "anno" to "我的划线", "io" to "数据", "review" to "复习队列",
        "case" to "案例库", "gloss" to "术语手册", "play" to "排障手册", "prog" to "学习进度",
        "calendar" to "学习日历", "my-marks" to "我的划线", "course-manage" to "课程管理",
        "settings" to "设置", "shortcuts" to "快捷入口", "course-import" to "导入课程", "player" to "",
    )

    /** 下一次渲染是否回顶部（对应 Web 的 __readTop） */
    var toTop = false

    // ---------------- 选图：GetContent → 方形裁剪 → 落盘 ----------------

    private var pendingPick: Pair<Int, (String) -> Unit>? = null
    private val imgLauncher by lazy {
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            val job = pendingPick
            pendingPick = null
            if (uri == null || job == null) return@registerForActivityResult
            try {
                val bmp = contentResolver.openInputStream(uri).use { ins ->
                    android.graphics.BitmapFactory.decodeStream(ins)
                } ?: throw IllegalStateException("decode")
                CropEditor.open(this, bmp, job.first) { path -> job.second(path) }
            } catch (e: Exception) {
                Sheets.toast(this, "这张图片无法读取")
            }
        }
    }

    // ---------------- 文本文件 / 文件夹 / 媒体文件选择 ----------------

    private var pendingText: ((String, String) -> Unit)? = null
    private val textLauncher by lazy {
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            val job = pendingText
            pendingText = null
            if (uri == null) { Sheets.toast(this, "已取消选择"); return@registerForActivityResult }
            val name = queryName(uri)
            runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { job?.invoke(name, it.readText()) }
            }.onFailure { Sheets.toast(this, "文件读取失败") }
        }
    }

    fun pickText(cb: (name: String, text: String) -> Unit) {
        pendingText = cb
        runCatching { textLauncher.launch(arrayOf("application/json", "text/plain", "text/markdown", "text/csv", "*/*")) }
            .onFailure { Sheets.toast(this, "系统文件选择器不可用") }
    }

    private var pendingTree: ((Uri?) -> Unit)? = null
    private val treeLauncher by lazy {
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
            val job = pendingTree
            pendingTree = null
            if (uri != null) runCatching { contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            job?.invoke(uri)
        }
    }

    fun pickTree(cb: (Uri?) -> Unit) {
        pendingTree = cb
        runCatching { treeLauncher.launch(null) }.onFailure { Sheets.toast(this, "系统文件夹选择器不可用") }
    }

    private var pendingMedia: ((List<Uri>?) -> Unit)? = null
    private val mediaLauncher by lazy {
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val job = pendingMedia
            pendingMedia = null
            uris?.let { list ->
                list.forEach { u -> runCatching { contentResolver.takePersistableUriPermission(u, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
            }
            job?.invoke(uris)
        }
    }

    fun pickMedia(kind: String, cb: (List<Uri>?) -> Unit) {
        pendingMedia = cb
        val mime = if (kind == "video") arrayOf("video/*") else arrayOf("audio/*")
        runCatching { mediaLauncher.launch(mime) }.onFailure { Sheets.toast(this, "系统文件选择器不可用") }
    }

    private fun queryName(uri: Uri): String {
        runCatching {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0) ?: uri.lastPathSegment ?: "存档.json"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "存档.json"
    }

    /** 顶栏 / 页面外部刷新入口（供设置页等直接调用） */
    fun refresh() = render()

    fun pickImage(size: Int, cb: (String) -> Unit) {
        pendingPick = size to cb
        runCatching { imgLauncher.launch("image/*") }.onFailure { Sheets.toast(this, "系统相册不可用") }
    }

    fun saveImage(bmp: android.graphics.Bitmap, size: Int): String? {
        return try {
            val dir = File(filesDir, "img").apply { mkdirs() }
            val f = File(dir, "i" + System.currentTimeMillis() + ".jpg")
            val s = Math.min(bmp.width, bmp.height)
            val sx = (bmp.width - s) / 2
            val sy = (bmp.height - s) / 2
            val o = Math.min(size, s)
            val out = Bitmap.createBitmap(o, o, android.graphics.Bitmap.Config.ARGB_8888)
            val cv = Canvas(out)
            cv.drawColor(Color.WHITE)
            cv.drawBitmap(bmp, Rect(sx, sy, sx + s, sy + s), Rect(0, 0, o, o), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
            FileOutputStream(f).use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 84, it) }
            out.recycle()
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as App
        app.currentActivity = this
        app.reader = Reader(this)
        selectionBar = com.qwen.opsmentor.core.SelectionBar(this, app)
        MarkEngine.migrate(app.store.marks)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        root = FrameLayout(this)
        deco = DecoView(this, decoKey(), dp(240f))
        root.addView(deco, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240f)))
        stage = FrameLayout(this)
        root.addView(stage, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        appbar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val abin = hrow(this)
        abin.setPadding(dp(8f), 0, dp(8f), 0)
        abin.addView(iconBtn(this, "back", getString(R.string.cd_back), 36f) { goBack() })
        abTitleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = dp(6f) }
        }
        abin.addView(abTitleBox)
        abActions = hrow(this)
        abin.addView(abActions)
        abEdit = iconBtn(this, "more", getString(R.string.cd_edit), 36f) { CourseEditor.open(this, app, currentCourse()) }
        abin.addView(abEdit)
        appbar.addView(abin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52f)))
        root.addView(appbar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.TOP })

        tabbar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabBarInner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabbar.addView(tabBarInner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58f)))
        root.addView(tabbar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.BOTTOM })

        mini = MiniBar(this, app) { nav(it) }
        root.addView(mini, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58f)).also {
            it.gravity = Gravity.BOTTOM; it.bottomMargin = dp(58f)
        })

        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topPad = if (app.store.topInset) bars.top else 0
            bottomPad = bars.bottom
            appbar.setPadding(0, topPad, 0, 0)
            tabbar.setPadding(0, 0, 0, bottomPad)
            (mini.layoutParams as? FrameLayout.LayoutParams)?.bottomMargin = dp(58f) + bottomPad
            layoutStage()
            insets
        }

        Bus.navigator = { nav(it) }
        Bus.back = { goBack() }
        Bus.toast = { msg -> Sheets.toast(this, msg) }
        Bus.invalidate = { render() }
        app.rerender = { render() }
        app.reader?.onState = { updateReaderPill() }

        nav("mine")
    }

    private fun decoKey(): String = app.store.deco.ifBlank { if (Tokens.planet) "lines" else "none" }

    private fun currentCourse() = Courses.find(app.store, app.store.cur)

    private fun layoutStage() {
        val onTab = tabs.containsKey(view)
        val lp = stage.layoutParams as FrameLayout.LayoutParams
        lp.topMargin = if (onTab) 0 else topPad + dp(52f)
        lp.bottomMargin = dp(58f) + bottomPad
        stage.layoutParams = lp
    }

    // ---------------- 路由 ----------------

    private fun leavePlayer() {
        (pages["player"] as? com.qwen.opsmentor.ui.pages.PagePlayer)?.onLeave()
    }

    fun nav(v: String) {
        if (v in listOf("today", "know", "review", "prog", "play") && pack().ch.isEmpty()) {
            Sheets.toast(this, "这门课程还没有章节内容，可通过「课程 → 导入」补充")
            return
        }
        if (v == view) { render(); return }
        if (view == "player" && v != "player") leavePlayer()
        selectionBar.close()
        app.reader?.let { if (it.on && v != "today") it.stop() }
        if (tabs.containsKey(v)) navStack.clear() else navStack.add(view)
        view = v
        render()
    }

    fun goBack() {
        if (selectionBar.isOpen()) { selectionBar.close(); return }
        (pages["my-marks"] as? PageMarks)?.let { if (it.inCourse()) { it.popCourse(); render(); return } }
        (pages["io"] as? PageIo)?.let { if (it.inDetail()) { it.popDetail(); render(); return } }
        selectionBar.close()
        view = if (navStack.isNotEmpty()) navStack.removeAt(navStack.size - 1) else "mine"
        layoutStage()
        render()
    }

    fun currentView(): String = view
    fun stackBase(): String = navStack.firstOrNull() ?: "mine"
    fun pack() = app.currentPack()

    /** 打开某门课程：切课程 + 进课程详情（Web courseCard 点击等价） */
    fun onCourseOpened(id: String) {
        if (app.store.cur != id) {
            app.store.setCur(id)
            app.store.save()
        }
        PageCourse.seg = "chs"
        nav("course")
    }

    /** 章节跳转（Web openChapter 等价） */
    fun openChapter(id: String) {
        app.store.bag().pin = id
        app.store.save()
        toTop = true
        nav("today")
        Sheets.toast(this, "已展开 $id 的完整详解")
    }

    /** 定位到当前页面上的某条划线（对应 Web「定位到原文」） */
    fun locateCurrent(m: com.qwen.opsmentor.state.Mark): Boolean = pages[view]?.locate(m) ?: false

    override fun onPageScroll(key: String, y: Int, threshold: Int) {
        if (tabs.containsKey(view)) return
        val solid = y > dp(threshold.toFloat())
        appbar.setBackgroundColor(if (solid) Tokens.withAlpha(Tokens.l1, 0.92f) else Color.TRANSPARENT)
        val planetCourse = Tokens.planet && view == "course"
        abTitleBox.alpha = if (planetCourse && y <= dp(150f)) 0f else 1f
        val p = pages[view] ?: return
        if (p.learning) {
            p.postponeSave(y)
        }
    }

    /** 阅读位置防抖写入（Web 用 250ms debounce） */
    private fun Page.postponeSave(y: Int) {
        handler.removeMessages(0)
        handler.postDelayed({
            app.store.readPos["${app.store.cur}:${posKey()}"] = y
            app.store.save()
        }, 250)
    }

    private val Page.handler: android.os.Handler get() = mainHandler

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // ---------------- 渲染 ----------------

    private fun make(key: String): Page = when (key) {
        "mine" -> PageMine(this, app)
        "profile" -> PageProfile(this, app)
        "course" -> PageCourse(this, app)
        "today" -> PageToday(this, app)
        "know" -> PageKnow(this, app)
        "anno" -> PageMarksCourse(this, app)
        "my-marks" -> PageMarks(this, app)
        "io" -> PageIo(this, app)
        "review" -> PageReview(this, app)
        "case" -> PageCases(this, app)
        "gloss" -> PageGloss(this, app)
        "play" -> PagePlaybook(this, app)
        "prog" -> PageProgress(this, app)
        "calendar" -> PageCalendar(this, app)
        "course-manage" -> PageCourseManage(this, app)
        "course-import" -> PageCourseImport(this, app)
        "settings" -> PageSettings(this, app)
        "shortcuts" -> PageShortcuts(this, app)
        "personal-data" -> PagePersonalData(this, app)
        "player" -> PagePlayer(this, app)
        else -> PageMine(this, app)
    }

    fun render() {
        Tokens.refresh(app.store, resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        root.setBackgroundColor(Tokens.l0)
        if (Build.VERSION.SDK_INT >= 28) window.decorView.setBackgroundColor(Tokens.l0)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !Tokens.dark
            isAppearanceLightNavigationBars = !Tokens.dark
        }
        deco.update(decoKey(), dp(240f))

        val onTab = tabs.containsKey(view)
        val page = pages.getOrPut(view) { make(view) }
        val sv = page.ensure()
        if (stage.childCount != 1 || stage.getChildAt(0) !== sv) {
            selectionBar.close()
            stage.removeAllViews()
            stage.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        layoutStage()
        appbar.visibility = if (onTab) View.GONE else View.VISIBLE
        if (!onTab) {
            abTitleBox.removeAllViews()
            val t = page.title().ifEmpty { titles[view] ?: "" }
            abTitleBox.addView(label(this, t, 16.5f, Tokens.ink, Tokens.weightH(), 1))
            abEdit.visibility = if (view == "course") View.VISIBLE else View.GONE
            abActions.removeAllViews()
            page.appbarActions(abActions)
        }
        renderTabbar(onTab)
        com.qwen.opsmentor.core.Pages.locator = { m -> page.locate(m) }
        app.timer.onPage(view, page.learning)
        page.rerender(toTop)
        toTop = false
        mini.render(view)
        updateReaderPill()
    }

    private fun renderTabbar(onTab: Boolean) {
        tabBarInner.removeAllViews()
        tabbar.flat(Tokens.l1, 0f)
        tabs.forEach { (k, name) ->
            val sub = !onTab && view in profileSubs
            val on = k == view || (k == "profile" && sub) || (k == "profile" && onTab && view == "profile")
            val planet = Tokens.planet
            val icon = when {
                !planet -> if (k == "mine") "layers" else "user"
                k == "mine" -> if (on) "orbitFill" else "orbit"
                else -> if (on) "userFill" else "user"
            }
            val b = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                layoutParams = LinearLayout.LayoutParams(0, dp(58f), 1f)
            }
            b.addView(ImageView(this).apply {
                setImageDrawable(Icons.get(this@MainActivity, icon, if (on) Tokens.pri else Tokens.sub))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(23f), dp(23f)))
            b.add(label(this, name, 10.5f, if (on) Tokens.pri else Tokens.sub, if (on) Tokens.weightM() else Tokens.weightB()), t = 2)
            b.setOnClickListener { nav(k) }
            tabBarInner.addView(b)
        }
    }

    // ---------------- 朗读悬浮条 ----------------

    private var readerPill: View? = null

    private fun updateReaderPill() {
        val reader = app.reader
        val show = reader != null && view == "today"
        if (!show) {
            readerPill?.let { root.removeView(it) }
            readerPill = null
            return
        }
        val r = reader ?: return
        if (readerPill == null) {
            val pill = hrow(this)
            pill.surface(Tokens.l2, 24f, 4f, Tokens.planet)
            pill.setPadding(dp(6f), dp(4f), dp(10f), dp(4f))
            val play = iconBtn(this, "play", "朗读", 32f) { r.toggle(); updateReaderPill() }
            val stop = iconBtn(this, "close", "停止朗读", 28f) { r.stop(); updateReaderPill() }
            val rate = label(this, "${app.store.ttsRate}×", 12f, Tokens.pri, Tokens.weightM())
            rate.setOnClickListener {
                Sheets.picker(this, "朗读语速", listOf(
                    com.qwen.opsmentor.core.Opt("0.75", "慢 · 0.75×"), com.qwen.opsmentor.core.Opt("1", "标准 · 1×"),
                    com.qwen.opsmentor.core.Opt("1.25", "较快 · 1.25×"), com.qwen.opsmentor.core.Opt("1.5", "快 · 1.5×"),
                    com.qwen.opsmentor.core.Opt("1.75", "很快 · 1.75×"),
                ), app.store.ttsRate.toString()) { v, _ ->
                    r.setRate(v.toFloatOrNull() ?: 1f); rate.text = "${v}×"
                }
            }
            pill.addView(play)
            pill.addView(rate, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = dp(4f) })
            pill.addView(stop, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = dp(4f) })
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            lp.bottomMargin = dp(70f) + bottomPad
            root.addView(pill, lp)
            readerPill = pill
            (play as ImageView).setImageDrawable(Icons.get(this, if (reader.on) "pause" else "play", Tokens.pri))
        }
        ((readerPill as LinearLayout).getChildAt(0) as ImageView)
            .setImageDrawable(Icons.get(this, if (reader.on) "pause" else "play", Tokens.pri))
        readerPill?.alpha = if (reader.segsSize() > 0) 1f else 0.4f
    }

    override fun onResume() {
        super.onResume()
        app.timer.onResume()
        mini.reappearOnResume()
        render()
    }

    override fun onPause() {
        super.onPause()
        app.timer.onPause()
        app.store.save()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        app.updateSystemDark(newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        app.retheme()
    }

    override fun onBackPressed() {
        if (selectionBar.isOpen() || view != "mine" || navStack.isNotEmpty()) goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (app.currentActivity === this) app.currentActivity = null
        app.reader?.shutdown()
        app.reader = null
        Sheets.closeAll()
    }
}
