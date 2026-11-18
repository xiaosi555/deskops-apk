package com.qwen.opsmentor.state

import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.data.PackRegistry



/** 课程注册表：负责把内置课与用户课合成一份可排序、可星标、可删除的列表 */
object Courses {

    /** 课程目录条目：内置课程 + 自建 / 导入课程（与 Web COURSES + syncCourses 对齐） */
    data class Entry(
        val id: String,
        val title: String,
        val sub: String,
        val desc: String,
        val glyph: String,
        val coverClass: String,
        val custom: Boolean,
        val media: String = "",
    )

    const val OPS = Store.COURSE_OPS

    /** 内置课程元信息（course_ops.json 顶层字段） */
    val builtin = listOf(
        Entry(
            OPS, "桌面运维全能学习导师",
            "零基础到独当一面：Windows、AD 域与策略、网络、终端外设、排障方法论一条龙。",
            "本课程把桌面运维拆成 7 个模块、30 章：先打地基（Windows / 网络 / 硬件），再攻 AD 域与策略主线，穿插 Helpdesk 高频场景与分层排障方法论。每章都配双轨对照、命令与预期输出、避坑清单和互动题，学完直接沉淀进案例库。",
            "monitor", "cv-a", false,
        ),
    )

    fun all(store: Store): List<Entry> {
        val out = mutableListOf<Entry>()
        builtin.forEach { if (!store.deletedCourses.contains(it.id)) out += it }
        store.userCourses.forEach { uc ->
            if (out.none { x -> x.id == uc.id }) {
                out += Entry(uc.id, uc.title, uc.sub, uc.desc.ifBlank { uc.sub },
                    uc.glyph.ifBlank { "book" }, "cv-a", true, uc.media)
            }
        }
        if (out.isEmpty()) out += builtin.first()
        return out
    }

    /** 星标优先排序 */
    fun sorted(store: Store): List<Entry> =
        all(store).sortedByDescending { (store.star[it.id] ?: 0) > 0 }

    fun find(store: Store, id: String?): Entry =
        all(store).firstOrNull { it.id == id } ?: all(store).first()

    /** 自定义信息覆盖在默认值之上（名称 / 简介 / 封面） */
    fun meta(store: Store, co: Entry): Triple<String, String, String> {
        val m = store.co[co.id]
        val title = m?.title?.takeIf { it.isNotBlank() } ?: co.title
        val sub = m?.sub?.takeIf { it.isNotBlank() } ?: co.sub
        val cover = m?.cover ?: ""
        return Triple(title, sub, cover)
    }

    fun glyph(store: Store, co: Entry): String =
        store.co[co.id]?.glyph?.takeIf { it.isNotBlank() } ?: co.glyph

    fun descOf(store: Store, co: Entry, pack: Pack): String {
        val m = store.co[co.id]
        return (m?.desc?.takeIf { it.isNotBlank() } ?: co.desc).ifBlank { pack.title }
    }

    fun typeOf(store: Store, co: Entry): String =
        if (co.media == "video") "video" else if (co.media == "audio") "audio" else "text"

    /** 课程进度：按该课程数据袋所选路线统计 */
    fun prog(store: Store, pack: Pack, courseId: String): Prog {
        val b = store.bags[courseId] ?: Bag.newBag()
        val routeKey = if (pack.routes.containsKey(b.route)) b.route else pack.routes.keys.firstOrNull() ?: "all"
        val q = pack.route(routeKey)
        return Prog(
            done = q.count { b.status[it] == "green" },
            total = q.size,
            started = b.learn.isNotEmpty(),
            mods = pack.mods.size,
            chs = pack.ch.size,
        )
    }

    data class Prog(val done: Int, val total: Int, val started: Boolean, val mods: Int, val chs: Int) {
        fun pct(): Int = if (total == 0) 0 else Math.round(done * 100f / total)
    }

    fun delete(store: Store, id: String, packs: PackRegistry): Boolean {
        val list = all(store)
        if (list.size <= 1) {
            Bus.toast("至少需要保留一门课程")
            return false
        }
        val co = list.firstOrNull { it.id == id } ?: return false
        if (co.custom) {
            store.userCourses.removeAll { it.id == id }
        } else if (!store.deletedCourses.contains(id)) {
            store.deletedCourses += id
        }
        store.co.remove(id)
        store.star.remove(id)
        store.shortcuts.removeAll { it.target == "course:$id" }
        store.bags.remove(id)
        packs.evict(id)
        if (store.cur == id) {
            val next = all(store).firstOrNull { it.id != id }
            store.setCur(next?.id ?: OPS)
        }
        store.save()
        return true
    }
}
