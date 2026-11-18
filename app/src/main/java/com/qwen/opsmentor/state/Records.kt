package com.qwen.opsmentor.state

import org.json.JSONArray
import org.json.JSONObject

/** 划线线型（与 Web LINE_TYPES 对齐） */
enum class LineType(val id: String, val label: String) {
    SOLID("solid", "横线"), WAVY("wavy", "波浪线"), DASH("dash", "虚线");

    companion object {
        fun of(id: String?) = values().firstOrNull { it.id == id } ?: SOLID
    }
}

/** 划线颜色（与 Web LINE_COLORS 对齐：b/g/a/r） */
enum class MarkColor(val id: String, val label: String, val hex: String) {
    BLUE("b", "蓝", "#2f6fed"), GREEN("g", "绿", "#188a5c"), AMBER("a", "琥珀", "#b8770f"), RED("r", "红", "#c4483f");

    companion object {
        fun of(id: String?) = values().firstOrNull { it.id == id } ?: BLUE
    }
}

val TAG_PRESET = listOf("易错", "面试", "命令", "流程")

/**
 * 一段划线的定位信息：块签名 + 区间 + 前后文，
 * 与 Web 的 segs 逐字段一致，保证存档可双向导入。
 */
data class Seg(
    var sig: String = "",
    var s: Int = 0,
    var e: Int = 0,
    var txt: String = "",
    var pre: String = "",
    var post: String = "",
) {
    fun toJson() = JSONObject().apply {
        put("sig", sig); put("s", s); put("e", e); put("txt", txt)
        if (pre.isNotEmpty()) put("pre", pre)
        if (post.isNotEmpty()) put("post", post)
    }

    companion object {
        fun from(o: JSONObject) = Seg(
            o.optString("sig"), o.optInt("s"), o.optInt("e"),
            o.optString("txt"), o.optString("pre"), o.optString("post"),
        )
    }
}

data class Mark(
    val id: String,
    var ch: String,
    var courseId: String,
    var ty: String,
    var co: String,
    var tags: MutableList<String>,
    var n: String,
    var at: Long,
    var up: Long,
    var segs: MutableList<Seg>,
    var t: String,
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("ch", ch); put("courseId", courseId)
        put("ty", ty); put("co", co)
        put("tags", JSONArray(tags)); put("n", n)
        put("at", at); put("up", up)
        put("segs", JSONArray(segs.map { it.toJson() }))
        put("t", t)
    }

    /** 跨段落时导出用 " ／ " 连接（与 Web marksText 一致） */
    fun joinedText(): String = segs.joinString(" ／ ") { it.txt }.ifBlank { t }

    companion object {
        fun from(o: JSONObject): Mark {
            val segs = mutableListOf<Seg>()
            o.optJSONArray("segs")?.let { a ->
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    segs += Seg.from(s)
                }
            }
            var at = o.optLong("at")
            if (at == 0L) at = System.currentTimeMillis()
            val up = if (o.optLong("up") > 0) o.optLong("up") else at
            val tags = mutableListOf<String>()
            o.optJSONArray("tags")?.let { a -> for (i in 0 until a.length()) tags += a.optString(i) }
            var m = Mark(
                id = if (o.optString("id").isNotEmpty()) o.optString("id") else Mark.uid(),
                ch = o.optString("ch"), courseId = o.optString("courseId"),
                ty = o.optString("ty", "solid"), co = o.optString("co", "b"),
                tags = tags, n = o.optString("n"), at = at, up = up,
                segs = segs, t = o.optString("t"),
            )
            if (m.segs.isEmpty()) {
                // 旧格式迁移：blk/a/b/t → 单段 segs
                val a = o.optInt("a"); val b = o.optInt("b")
                m.segs += Seg(sig = "@" + o.optString("blk"), s = a, e = b, txt = m.t)
            }
            val joined = m.segs.joinToString("") { it.txt }
            if (joined.isNotEmpty()) m = m.copy(t = joined)
            return m
        }

        fun uid(): String = "m" + System.currentTimeMillis().toString(36) +
            ('a'..'z').random() + ('a'..'z').random() + ('a'..'z').random()
    }
}

/** 故障案例（五栏固定 + 关联章节） */
data class CaseRec(
    var no: String, var sym: String, var path: String,
    var cause: String, var lesson: String, var ch: String,
) {
    fun toJson() = JSONObject().apply {
        put("no", no); put("sym", sym); put("path", path)
        put("cause", cause); put("lesson", lesson); put("ch", ch)
    }

    companion object {
        val MODULE_CODES = listOf(
            "AD" to "AD 域与策略", "NET" to "网络", "SYS" to "Windows 系统", "PRN" to "打印机",
            "HW" to "硬件", "O365" to "Office 365", "MAIL" to "邮件", "FILE" to "文件共享",
            "SEC" to "终端防护", "CONF" to "会议外设", "MAC" to "macOS", "LINUX" to "Linux",
            "TICKET" to "工单流程", "OTHER" to "其他",
        )

        /** 编号自动生成：优先取未使用的类别前缀，序号为该前缀已有条数 +1，三位补零 */
        fun nextCode(cases: List<CaseRec>): String {
            val used = cases.mapNotNull { it.no.takeIf { n -> n.contains('-') }?.substringBefore('-') }.toSet()
            val pick = MODULE_CODES.firstOrNull { it.first !in used } ?: MODULE_CODES.last()
            val n = cases.count { it.no.startsWith(pick.first + "-") } + 1
            return pick.first + "-" + n.toString().padStart(3, '0')
        }

        fun from(o: JSONObject) = CaseRec(
            o.optString("no"), o.optString("sym"), o.optString("path"),
            o.optString("cause"), o.optString("lesson"), o.optString("ch"),
        )
    }
}

/** 自填术语 */
data class GlossRec(var term: String, var full: String, var say: String, val src: String = "user") {
    fun toJson() = JSONObject().put("term", term).put("full", full).put("say", say).put("src", src)

    companion object {
        fun from(o: JSONObject) = GlossRec(o.optString("term"), o.optString("full"), o.optString("say"), o.optString("src", "user"))
    }
}

fun <T> List<T>.joinString(sep: String, transform: (T) -> String): String = joinToString(sep, transform = transform)
