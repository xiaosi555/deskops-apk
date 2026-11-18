package com.qwen.opsmentor.data

import android.content.Context
import com.qwen.opsmentor.state.Bag
import com.qwen.opsmentor.state.Bus
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.UserCourse
import org.json.JSONObject

/**
 * 课程内容包注册表：内置包从 assets 读一次；自建 / 导入课程各自持有一份包。
 * 对应 Web 的 opsPack() / packOf(co) / loadPack(id)。
 */
class PackRegistry(private val ctx: Context) {
    private val cache = HashMap<String, Pack>()
    private var builtinPack: Pack? = null

    fun of(store: Store, courseId: String): Pack {
        if (courseId == Courses.OPS || Courses.builtin.any { it.id == courseId }) return builtin(store)
        cache[courseId]?.let { return it }
        val uc = store.userCourses.firstOrNull { it.id == courseId }
        val p = if (uc != null && uc.packJson.isNotBlank()) {
            try {
                Pack.from(JSONObject(uc.packJson))
            } catch (e: Exception) {
                Pack.empty()
            }
        } else {
            Pack.empty()
        }
        cache[courseId] = p
        return p
    }

    /** 当前课程的内容包（页面渲染入口） */
    fun current(store: Store): Pack = of(store, store.cur)

    fun builtin(store: Store): Pack {
        builtinPack?.let { return it }
        val p = try {
            val text = ctx.assets.open("course_ops.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            Pack.from(JSONObject(text))
        } catch (e: Exception) {
            android.util.Log.e("PackRegistry", "内置课程数据读取失败", e)
            Pack.empty()
        }
        builtinPack = p
        return p
    }

    /** 媒体课程建课后回填包内容 */
    fun put(courseId: String, pack: Pack) {
        cache[courseId] = pack
    }

    fun evict(courseId: String) {
        cache.remove(courseId)
        if (courseId == Courses.OPS) builtinPack = null
    }

    fun saveBack(store: Store, courseId: String, pack: Pack) {
        val uc = store.userCourses.firstOrNull { it.id == courseId } ?: return
        uc.packJson = pack.toJson().toString()
        cache[courseId] = pack
        store.save()
    }
}

/** Pack 的反向序列化（供课程档案导出与本机持久化） */
fun Pack.toJson(): JSONObject = JSONObject().apply {
    put("mods", org.json.JSONArray(mods.map { JSONObject().put("k", it.k).put("n", it.n).put("tag", it.tag) }))
    put("ch", org.json.JSONArray(ch.map { c ->
        JSONObject().apply {
            put("id", c.id); put("m", c.m); put("t", c.t); put("s", c.s); put("terms", c.terms); put("gate", c.gate)
            put("plain", org.json.JSONArray(c.plain.map { p -> org.json.JSONArray(listOf(p.first, p.second)) }))
            put("cmds", org.json.JSONArray(c.cmds.map { JSONObject().put("c", it.c).put("n", it.n).put("lv", it.lv) }))
            put("pits", org.json.JSONArray(c.pits.map { JSONObject().put("w", it.w).put("c", it.c).put("r", it.r) }))
            put("quiz", JSONObject().put("q", c.quiz.q).put("a", c.quiz.a).put("hint", c.quiz.hint))
            c.media?.let { put("media", JSONObject().put("kind", it.kind).put("fname", it.fname).put("rel", it.uri).put("size", it.size).put("dur", it.dur)) }
            c.media?.let { put("media", JSONObject().put("kind", it.kind).put("fname", it.fname).put("rel", it.uri).put("size", it.size).put("dur", it.dur)) }
        }
    }))
    put("deep", JSONObject().apply {
        deep.forEach { (k, v) ->
            put(k, JSONObject().apply {
                put("deep", org.json.JSONArray(v.deep.map { org.json.JSONArray(listOf(it.first, it.second)) }))
                put("out", org.json.JSONArray(v.out.map { org.json.JSONArray(listOf(it.first, it.second)) }))
                put("flow", org.json.JSONArray(v.flow.map { org.json.JSONArray(listOf(it.first, it.second)) }))
            })
        }
    })
    put("long", JSONObject().apply {
        this@toJson.long.forEach { (k, v) -> put(k, v.toJson()) }
    })
    put("layers", org.json.JSONArray(layers.map { org.json.JSONArray(listOf(it.first, it.second)) }))
    put("cmds", org.json.JSONArray(cmds.map { org.json.JSONArray(it) }))
    put("events", org.json.JSONArray(events.map { org.json.JSONArray(it) }))
    put("scen", org.json.JSONArray(scen.map {
        JSONObject().put("t", it.t).put("p", it.p).put("steps", org.json.JSONArray(it.steps)).put("c", it.c).put("f", it.f)
    }))
    put("lab", org.json.JSONArray(lab.map { org.json.JSONArray(listOf(it.first, it.second)) }))
    put("routes", JSONObject().apply { routes.forEach { (k, v) -> put(k, org.json.JSONArray(v)) } })
    put("routeNames", JSONObject().apply { routeNames.forEach { (k, v) -> put(k, v) } })
    put("intervals", org.json.JSONArray(intervals))
    put("depText", depText)
    put("depNotes", org.json.JSONArray(depNotes.map { org.json.JSONArray(listOf(it.first, it.second)) }))
}

fun LongLecture.toJson(): JSONObject = JSONObject().apply {
    put("title", title)
    put("concept", org.json.JSONArray(concept.map { b ->
        JSONObject().apply {
            if (b.p.isNotEmpty()) put("p", b.p)
            if (b.ul.isNotEmpty()) put("ul", org.json.JSONArray(b.ul))
            if (b.isTable) put("tbl", JSONObject().put("head", org.json.JSONArray(b.head)).put("rows", org.json.JSONArray(b.rows.map { org.json.JSONArray(it) })))
            if (b.md.isNotEmpty()) put("md", b.md)
            if (b.title.isNotEmpty()) put("t", b.title)
            if (b.parts.isNotEmpty()) put("parts", org.json.JSONArray(b.parts.map { JSONObject().put("t", it.text).put("b", it.b).put("code", it.code) }))
        }
    }))
    put("practice", org.json.JSONArray(practice.map { s ->
        JSONObject().apply {
            put("t", s.t); put("d", s.d); put("cmd", s.cmd); put("out", s.out); put("read", s.read)
            if (s.md.isNotEmpty()) put("md", s.md)
            if (s.parts.isNotEmpty()) put("parts", org.json.JSONArray(s.parts.map { JSONObject().put("t", it.text).put("b", it.b).put("code", it.code) }))
        }
    }))
    put("guide", org.json.JSONArray(guide.map { g ->
        JSONObject().apply {
            if (g.w.isNotEmpty()) put("w", g.w)
            if (g.c.isNotEmpty()) put("c", g.c)
            if (g.r.isNotEmpty()) put("r", g.r)
            if (g.t.isNotEmpty()) put("t", g.t)
            if (g.d.isNotEmpty()) put("d", g.d)
        }
    }))
    put("quiz", JSONObject().put("q", quiz.q).put("a", quiz.a).put("hint", quiz.hint))
}
