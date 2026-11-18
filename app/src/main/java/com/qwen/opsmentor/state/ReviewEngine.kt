package com.qwen.opsmentor.state

import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.data.Chapter

/** 间隔提取复习（§7.4） */
class DueItem(val id: String, val ch: Chapter?, val iv: Int, val diff: Int)

object ReviewEngine {

    /** 到期队列：learn.at 距今 >= 档位 且该档位未完成 */
    fun due(store: Store, pack: Pack): List<DueItem> {
        val out = ArrayList<DueItem>()
        store.learn.forEach { (id, rec) ->
            val diff = Store.dayDiff(rec.at)
            pack.intervals.forEach { iv ->
                if (rec.done.contains(iv)) return@forEach
                if (diff >= iv) {
                    val ch = pack.chById(id)
                    if (ch != null) out.add(DueItem(id, ch, iv, diff))
                }
            }
        }
        out.sortBy { it.iv }
        return out
    }

    /** 全部已学（按已学天数倒序） */
    fun allLearned(store: Store, pack: Pack): List<DueItem> =
        store.learn.keys.mapNotNull { id ->
            val ch = pack.chById(id) ?: return@mapNotNull null
            DueItem(id, ch, 0, Store.dayDiff(store.learn[id]?.at))
        }.sortedByDescending { it.diff }

    /** 「还记得」：推进该档位；走完最后一档退回第 2 天节奏重走 */
    fun markReviewed(store: Store, pack: Pack, id: String, iv: Int) {
        val rec = store.learn.getOrPut(id) { LearnRec(Store.today(), ArrayList()) }
        if (!rec.done.contains(iv)) rec.done.add(iv)
        val last = pack.intervals.lastOrNull() ?: 30
        if (iv >= last) {
            rec.at = Store.today()
            rec.done.clear()
        }
        store.save()
    }
}
