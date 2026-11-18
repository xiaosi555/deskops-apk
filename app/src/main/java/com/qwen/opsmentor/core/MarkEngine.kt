package com.qwen.opsmentor.core

import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.Seg
import kotlin.math.abs
import kotlin.math.min

/**
 * 划线定位引擎（与 Web annotation v4 同一套算法，保证存档双向可定位）。
 *
 * - 块文本变化后仍能找到原处：先用块签名 + 区间精确命中，失败再按"前后文 + 位置偏移"打分选最近候选。
 * - 同一处允许多条划线并存，绘制时按创建顺序分层。
 */
object MarkEngine {

    /** FNV-1a 32 位 → base36 + 长度 */
    fun sigOf(t: String?): String {
        val s = norm(t)
        var h = -0x79996300 // 2166136261 as signed int
        for (ch in s) {
            h = h xor ch.code
            h *= 16777619
        }
        return "s" + (h.toLong() and 0xFFFFFFFFL).toString(36) + "_" + s.length
    }

    fun norm(t: String?): String =
        (t ?: "").replace("\uFFFC", "").replace(Regex("\\s+"), " ")

    /** 迁移旧格式记录（无 segs 时用 blk/a/b/t 合成） */
    fun migrate(marks: MutableList<Mark>) {
        marks.forEach { m ->
            if (m.segs.isEmpty()) m.segs.add(Seg(sig = "@" + m.ch, s = 0, e = m.t.length, txt = m.t))
            if (m.up == 0L) m.up = m.at
        }
    }

    fun commonEnd(a: String, b: String): Int {
        var n = 0
        while (n < a.length && n < b.length && a[a.length - 1 - n] == b[b.length - 1 - n]) n++
        return n
    }

    fun commonStart(a: String, b: String): Int {
        var n = 0
        while (n < a.length && n < b.length && a[n] == b[n]) n++
        return n
    }

    /** 同一句话重复出现时，用前后文挑出正确的那一处 */
    fun pickByContext(text: String, sg: Seg): IntRange? {
        val t = sg.txt
        if (t.isEmpty()) return null
        val cands = mutableListOf<Int>()
        var i = text.indexOf(t)
        while (i >= 0) {
            cands.add(i)
            if (cands.size > 60) break
            i = text.indexOf(t, i + 1)
        }
        if (cands.isEmpty()) return null
        var best = cands[0]
        var bestScore = -Double.MAX_VALUE
        cands.forEach { a ->
            var sc = 0.0
            if (sg.pre.isNotEmpty()) sc += commonEnd(text.substring(max0(a - 18), a), sg.pre) * 2.0
            if (sg.post.isNotEmpty()) sc += commonStart(text.substring(a + t.length, min(text.length, a + t.length + 18)), sg.post) * 2.0
            sc -= min(6.0, abs(a - sg.s).toDouble() / 120.0)
            if (sc > bestScore) {
                bestScore = sc; best = a
            }
        }
        return best until (best + t.length)
    }

    private fun max0(v: Int) = if (v < 0) 0 else v

    /**
     * 命中某块文本上的所有划线。
     * @return 按创建时间排序的 (划线, 区间) 列表；同时把纠偏结果写回 seg，避免每帧重算。
     */
    fun resolve(text: String, sig: String, marks: List<Mark>, chId: String): List<Pair<Mark, IntRange>> {
        val out = ArrayList<Pair<Mark, IntRange>>()
        marks.forEach { m ->
            if (m.ch != chId) return@forEach
            m.segs.forEach { sg ->
                var hit: IntRange? = null
                if (sg.sig == sig && sg.sig.isNotEmpty() && sg.txt.isNotEmpty()) {
                    val a = max0(min(sg.s, text.length))
                    val b = max0(min(sg.e, text.length))
                    if (b > a && text.substring(a, b) == sg.txt) hit = a until b
                }
                if (hit == null && sg.txt.length > 1) hit = pickByContext(text, sg)
                if (hit != null) {
                    sg.sig = sig
                    sg.s = hit.first
                    sg.e = hit.last + 1
                    sg.txt = text.substring(hit.first, hit.last + 1)
                    out.add(m to hit)
                }
            }
        }
        out.sortWith(compareBy({ it.first.at }, { it.second.first }))
        return out
    }

    /** 分层下划线偏移：与 Web text-underline-offset 0.14em + 0.24em × 剩余层数同源 */
    fun layersFor(hits: List<Pair<Mark, IntRange>>): Map<Mark, Int> {
        val map = HashMap<Mark, Int>()
        val total = hits.size
        hits.forEachIndexed { idx, pair ->
            // 后创建的划线层数更低（更靠近文字），与 Web 的嵌套顺序一致
            map[pair.first] = total - 1 - idx
        }
        return map
    }

    /** 该章的划线条数（章节行徽标用） */
    fun countOf(marks: List<Mark>, chId: String): Int = marks.count { it.ch == chId }

    /** 微调区间：edge = "s" / "e"，delta = ±1；保证 end > start，并刷新前后文 */
    fun adjust(m: Mark, text: String, sig: String, edge: String, delta: Int): Boolean {
        if (m.segs.isEmpty()) return false
        val idx = if (edge == "s") 0 else m.segs.size - 1
        val sg = m.segs[idx]
        var a = sg.s
        var b = sg.e
        if (edge == "s") {
            a = max0(min(a + delta, b - 1))
            if (idx == 0 && m.segs.size > 1 && a >= b) return false
        } else {
            b = min(text.length, max0(b + delta))
        }
        if (b - a < 1) return false
        sg.sig = sig
        sg.s = a
        sg.e = b
        sg.txt = text.substring(a, b)
        sg.pre = text.substring(max0(a - 14), a)
        sg.post = text.substring(b, min(text.length, b + 14))
        m.t = m.segs.joinToString("") { it.txt }
        m.up = System.currentTimeMillis()
        return true
    }

    /** 扩展到整段 */
    fun expandToBlock(m: Mark, text: String, sig: String) {
        if (m.segs.isEmpty()) return
        val first = m.segs[0]
        first.sig = sig; first.s = 0; first.e = text.length; first.txt = text; first.pre = ""; first.post = ""
        if (m.segs.size > 1) {
            val last = m.segs[m.segs.size - 1]
            last.sig = sig; last.s = 0; last.e = text.length; last.txt = text; last.pre = ""; last.post = ""
        }
        m.t = m.segs.joinToString("") { it.txt }
        m.up = System.currentTimeMillis()
    }

    /** 新建划线（来自选区）；跨段合并为一条记录 */
    fun create(chId: String, courseId: String, ty: String, co: String, segs: List<Seg>): Mark {
        val m = Mark(
            id = Mark.uid(), ch = chId, courseId = courseId, ty = ty, co = co,
            tags = mutableListOf(), n = "", at = System.currentTimeMillis(),
            up = System.currentTimeMillis(), segs = segs.toMutableList(),
            t = segs.joinToString("") { it.txt },
        )
        m.segs.forEach { it.pre = ""; it.post = "" }
        return m
    }

    fun fingerprint(m: Mark): String =
        (m.ch ?: "") + "|" + m.segs.joinToString(",") { sigOf(it.txt) }
}
