package com.qwen.opsmentor.state

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 章节学习记录：首次学习日期 + 已完成的复习间隔档 */
class LearnRec(var at: String, var done: MutableList<Int>) {
    fun toJson() = JSONObject().put("at", at).put("done", JSONArray(done))
    companion object {
        fun from(o: JSONObject): LearnRec {
            val done = mutableListOf<Int>()
            o.optJSONArray("done")?.let { a -> for (i in 0 until a.length()) done += a.optInt(i) }
            return LearnRec(o.optString("at"), done)
        }
    }
}

/** 一门课程的独立数据袋（与 Web newBag 对齐） */
class Bag(var route: String = "r1", var idx: Int = 0, var day: Int = 1, var mode: String = "long",
          var pin: String = "", var status: LinkedHashMap<String, String> = LinkedHashMap(),
          var learn: LinkedHashMap<String, LearnRec> = LinkedHashMap(),
          var cases: MutableList<CaseRec> = mutableListOf(),
          var gloss: MutableList<GlossRec> = mutableListOf(),
          var marks: MutableList<Mark> = mutableListOf()) {

    fun toJson() = JSONObject().apply {
        put("route", route); put("idx", idx); put("day", day); put("mode", mode); put("pin", pin)
        put("status", JSONObject(status as Map<*, *>))
        put("learn", JSONObject().apply { learn.forEach { (k, v) -> put(k, v.toJson()) } })
        put("cases", JSONArray(cases.map { it.toJson() }))
        put("gloss", JSONArray(gloss.map { it.toJson() }))
        put("marks", JSONArray(marks.map { it.toJson() }))
    }

    companion object {
        fun from(o: JSONObject): Bag {
            val b = Bag()
            b.route = o.optString("route", "r1")
            b.idx = o.optInt("idx", 0)
            b.day = if (o.optInt("day", 1) > 0) o.optInt("day") else 1
            b.mode = if (o.optString("mode", "long") == "lite") "lite" else "long"
            b.pin = o.optString("pin", "")
            o.optJSONObject("status")?.let { s -> s.keys().forEach { k -> b.status[k] = s.optString(k) } }
            o.optJSONObject("learn")?.let { l -> l.keys().forEach { k -> l.optJSONObject(k)?.let { b.learn[k] = LearnRec.from(it) } } }
            o.optJSONArray("cases")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { b.cases += CaseRec.from(it) } }
            o.optJSONArray("gloss")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { b.gloss += GlossRec.from(it) } }
            o.optJSONArray("marks")?.let { a ->
                for (i in 0 until a.length()) a.optJSONObject(i)?.let { b.marks += Mark.from(it) }
            }
            return b
        }

        fun newBag(route: String = "r1") = Bag(route = route)
    }
}

/** 课程封面 / 名称 / 简介 / 图标的自定义覆盖 */
class CoMeta(var title: String = "", var sub: String = "", var cover: String = "", var glyph: String = "", var desc: String = "") {
    fun toJson() = JSONObject().apply {
        put("title", title); put("sub", sub); put("cover", cover); put("glyph", glyph); put("desc", desc)
    }

    companion object {
        fun from(o: JSONObject) = CoMeta(o.optString("title"), o.optString("sub"), o.optString("cover"), o.optString("glyph"), o.optString("desc"))
    }
}

/** 快捷入口（首页搜索行下方的功能块） */
class Shortcut(var id: String, var name: String, var target: String, var img: String = "", var color: String = "", var tc: String = "") {
    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("target", target); put("img", img); put("color", color); put("tc", tc)
    }

    companion object {
        fun from(o: JSONObject) = Shortcut(o.optString("id"), o.optString("name"), o.optString("target"),
            o.optString("img"), o.optString("color"), o.optString("tc"))

        fun defaults(): MutableList<Shortcut> = mutableListOf(
            Shortcut("sc_course", "课程", "course-manage"),
            Shortcut("sc_marks", "划线", "my-marks"),
            Shortcut("sc_cal", "学习日历", "calendar"),
        )
    }
}

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class StyleMode { MINIMAL, PLANET }

/** 全局状态：与 Web 版 localStorage `dlmentor.v3` 的 JSON 结构逐字段对齐 */
class Store(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.getSharedPreferences(KEY, Context.MODE_PRIVATE)

    var cur: String = COURSE_OPS
        private set
    var theme: String = "light"           // 兼容旧字段（亮/暗）
    var bags: LinkedHashMap<String, Bag> = LinkedHashMap()
    // ---- ui ----
    var style: StyleMode = StyleMode.PLANET
    var accent: Int = 4
    var accentCustom: String = ""
    var accentPaletteVersion: Int = 1
    var appearance: Appearance = Appearance.SYSTEM
    var deco: String = "lines"
    var star: LinkedHashMap<String, Int> = LinkedHashMap()
    var co: LinkedHashMap<String, CoMeta> = LinkedHashMap()
    var profileName: String = ""
    var profileBio: String = ""
    var profileAvatar: String = ""        // filesDir 内的相对路径
    var userCourses: MutableList<UserCourse> = mutableListOf()
    var deletedCourses: MutableList<String> = mutableListOf()
    var shortcuts: MutableList<Shortcut> = Shortcut.defaults()
    var readPos: LinkedHashMap<String, Int> = LinkedHashMap()
    var studyLog: LinkedHashMap<String, Int> = LinkedHashMap()
    var mediaPos: LinkedHashMap<String, Long> = LinkedHashMap()
    var cmTab: String = "text"
    var barX: Float = -1f
    var barY: Float = -1f
    var mediaKeep: Boolean = true
    var mediaTrees: LinkedHashMap<String, String> = LinkedHashMap()
    var topInset: Boolean = true
    var ttsRate: Float = 1f
    var ttsVoice: String = ""

    var dirty: Boolean = false
    val listeners = mutableListOf<() -> Unit>()

    init {
        load()
    }

    fun bag(id: String = cur): Bag = bags.getOrPut(id) { Bag.newBag() }

    /** 当前课程袋的便捷访问（对应 Web 的 S.route / idx / marks …） */
    val route: String get() = bag().route
    val idx: Int get() = bag().idx
    val day: Int get() = bag().day
    val mode: String get() = bag().mode
    val pin: String get() = bag().pin
    val status: LinkedHashMap<String, String> get() = bag().status
    val learn: LinkedHashMap<String, LearnRec> get() = bag().learn
    val cases: MutableList<CaseRec> get() = bag().cases
    val gloss: MutableList<GlossRec> get() = bag().gloss
    val marks: MutableList<Mark> get() = bag().marks

    fun setCur(id: String) {
        if (id == cur) return
        cur = id; bag(id); dirty = true
    }

    /** 全课程划线（跨数据袋），用于「我的划线」两层视图与导出统计 */
    fun allMarks(): List<Mark> {
        val out = mutableListOf<Mark>()
        bags.forEach { (id, b) ->
            b.marks.forEach { m -> if (m.courseId.isEmpty()) m.courseId = id; out += m }
        }
        return out
    }

    fun load() {
        val raw = sp.getString(KEY, null) ?: return
        try {
            val o = JSONObject(raw)
            cur = o.optString("cur", COURSE_OPS)
            if (cur.isEmpty()) cur = COURSE_OPS
            theme = o.optString("theme", "light")
            o.optJSONObject("cp")?.let { cp ->
                cp.keys().forEach { k -> cp.optJSONObject(k)?.let { bags[k] = Bag.from(it) } }
            }
            if (!bags.containsKey(cur)) bags[cur] = Bag.newBag()
            // 旧版单课程存档迁移
            if (o.has("status") || o.has("marks")) {
                val legacy = Bag.from(o)
                val b0 = bags.getOrPut(COURSE_OPS) { Bag.newBag() }
                if (b0.status.isEmpty()) b0.status.putAll(legacy.status)
                if (b0.learn.isEmpty()) b0.learn.putAll(legacy.learn)
                if (b0.cases.isEmpty()) b0.cases += legacy.cases
                if (b0.gloss.isEmpty()) b0.gloss += legacy.gloss
                if (b0.marks.isEmpty()) {
                    legacy.marks.forEach { it.courseId = COURSE_OPS; b0.marks += it }
                }
                if (b0.day == 1) b0.day = legacy.day
            }
            o.optJSONObject("ui")?.let { u ->
                style = if (u.optString("style", "planet") == "minimal") StyleMode.MINIMAL else StyleMode.PLANET
                val savedAccent = u.optInt("accent", 0)
                val savedPaletteVersion = u.optInt("accentPaletteVersion", 0)
                accent = if (savedPaletteVersion == 0) when (savedAccent) {
                    0 -> 4; 1 -> 1; 2 -> 2; 3 -> 3; 4 -> 5
                    5 -> 4; 6 -> 3; 7 -> 4; 8 -> 6; else -> 4
                } else savedAccent
                accentCustom = u.optString("accentCustom", "")
                accentPaletteVersion = 1
                appearance = when (u.optString("appearance", "")) {
                    "light" -> Appearance.LIGHT; "dark" -> Appearance.DARK; else -> Appearance.SYSTEM
                }
                deco = u.optString("deco", "")
                u.optJSONObject("star")?.let { s -> s.keys().forEach { k -> star[k] = s.optInt(k, 1) } }
                u.optJSONObject("co")?.let { c -> c.keys().forEach { k -> c.optJSONObject(k)?.let { co[k] = CoMeta.from(it) } } }
                u.optJSONObject("profile")?.let { p ->
                    profileName = p.optString("name", ""); profileBio = p.optString("bio", ""); profileAvatar = p.optString("avatar", "")
                }
                u.optJSONArray("userCourses")?.let { a ->
                    for (i in 0 until a.length()) a.optJSONObject(i)?.let { userCourses += UserCourse.from(it) }
                }
                u.optJSONArray("deletedCourses")?.let { a -> for (i in 0 until a.length()) deletedCourses += a.optString(i) }
                u.optJSONArray("shortcuts")?.let { a ->
                    val list = mutableListOf<Shortcut>()
                    for (i in 0 until a.length()) a.optJSONObject(i)?.let { list += Shortcut.from(it) }
                    if (list.isNotEmpty()) shortcuts = list
                }
                u.optJSONObject("readPos")?.let { r -> r.keys().forEach { k -> readPos[k] = r.optInt(k) } }
                u.optJSONObject("studyLog")?.let { l -> l.keys().forEach { k -> studyLog[k] = l.optInt(k) } }
                u.optJSONObject("mediaPos")?.let { m -> m.keys().forEach { k -> mediaPos[k] = m.optLong(k) } }
                cmTab = u.optString("cmTab", "text")
                u.optJSONObject("bar")?.let { b -> barX = b.optDouble("x", -1.0).toFloat(); barY = b.optDouble("y", -1.0).toFloat() }
                mediaKeep = u.optBoolean("mediaKeep", true)
                u.optJSONObject("mediaTrees")?.let { t -> t.keys().forEach { k -> mediaTrees[k] = t.optString(k) } }
                topInset = u.optBoolean("topInset", true)
                ttsRate = u.optDouble("ttsRate", 1.0).toFloat()
                ttsVoice = u.optString("ttsVoice", "")
            }
        } catch (e: Exception) {
            // 存档损坏：保留可默认项，不清空用户数据之外的东西
            android.util.Log.w("Store", "存档解析失败，回退默认", e)
        }
    }

    fun save() {
        val o = JSONObject()
        o.put("cur", cur)
        o.put("theme", theme)
        val u = JSONObject()
        u.put("style", if (style == StyleMode.MINIMAL) "minimal" else "planet")
        u.put("accent", accent)
        u.put("accentCustom", accentCustom)
        u.put("accentPaletteVersion", accentPaletteVersion)
        u.put("appearance", when (appearance) { Appearance.LIGHT -> "light"; Appearance.DARK -> "dark"; else -> "system" })
        u.put("deco", deco)
        u.put("star", JSONObject(star as Map<*, *>))
        u.put("co", JSONObject().apply { co.forEach { (k, v) -> put(k, v.toJson()) } })
        u.put("profile", JSONObject().put("name", profileName).put("bio", profileBio).put("avatar", profileAvatar))
        u.put("userCourses", JSONArray(userCourses.map { it.toJson() }))
        u.put("deletedCourses", JSONArray(deletedCourses))
        u.put("shortcuts", JSONArray(shortcuts.map { it.toJson() }))
        u.put("readPos", JSONObject(readPos as Map<*, *>))
        u.put("studyLog", JSONObject(studyLog as Map<*, *>))
        u.put("mediaPos", JSONObject(mediaPos as Map<*, *>))
        u.put("cmTab", cmTab)
        u.put("bar", JSONObject().put("x", barX.toDouble()).put("y", barY.toDouble()))
        u.put("mediaKeep", mediaKeep)
        u.put("mediaTrees", JSONObject(mediaTrees as Map<*, *>))
        u.put("topInset", topInset)
        u.put("ttsRate", ttsRate.toDouble())
        u.put("ttsVoice", ttsVoice)
        o.put("ui", u)
        o.put("cp", JSONObject().apply { bags.forEach { (k, v) -> put(k, v.toJson()) } })
        try {
            sp.edit().putString(KEY, o.toString()).apply()
            dirty = false
        } catch (e: Exception) {
            Bus.toast("存储空间不足：请先导出备份，再删除不用的课程")
        }
    }

    fun wipe() {
        sp.edit().remove(KEY).apply()
    }

    fun touch() {
        dirty = true
        save()
        listeners.forEach { it() }
    }

    companion object {
        const val KEY = "***"
        const val COURSE_OPS = "ops"
        const val AR_APP = "desktop-it-ops-mentor"
        const val AR_VER = 7
        private val DF = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        fun today(): String = DF.format(Date())
        fun ymd(d: Date): String = DF.format(d)
        fun dayDiff(s: String?): Int {
            if (s.isNullOrEmpty()) return 0
            return try {
                val a = DF.parse(s)?.time ?: return 0
                val b = DF.parse(today())?.time ?: return 0
                Math.round((b - a) / 86400000.0).toInt()
            } catch (e: Exception) {
                0
            }
        }
    }
}

/** 自建 / 导入的课程：pack 内容序列化在本机，进度独立成袋 */
class UserCourse(
    val id: String, var title: String, var sub: String, var desc: String,
    var glyph: String = "book", var media: String = "", var packJson: String = "",
    var imported: JSONObject? = null,
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("sub", sub); put("desc", desc)
        put("glyph", glyph); put("media", media); put("pack", packJson)
        imported?.let { put("importedData", it) }
    }

    companion object {
        fun from(o: JSONObject) = UserCourse(
            o.optString("id"), o.optString("title"), o.optString("sub"),
            o.optString("desc", o.optString("sub")), o.optString("glyph", "book"),
            o.optString("media"), o.optString("pack"), o.optJSONObject("importedData"),
        )
    }
}

/** 全局事件总线：toast / 重绘 / 抽屉 */
object Bus {
    var toast: (String) -> Unit = {}
    var invalidate: () -> Unit = {}
    var navigator: (String) -> Unit = {}
    var back: () -> Unit = {}
}
