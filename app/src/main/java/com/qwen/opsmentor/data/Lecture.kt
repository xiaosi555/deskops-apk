package com.qwen.opsmentor.data

import com.qwen.opsmentor.state.LineType

/**
 * 长讲义解析：优先取内置 long，缺失时用 synth() 实时合成（§7.3）。
 * 页面渲染与学习手册导出共用，保证两端内容一致。
 */
object Lecture {

    fun of(pack: Pack, c: Chapter): LongLecture {
        pack.long[c.id]?.let { return it }
        return synth(pack, c)
    }

    private val LV_DESC = mapOf(
        "r" to "只读查看，可在自己办公机上运行",
        "w" to "会改动本机配置，先征得同意或只在实验环境做",
        "s" to "需要管理员或服务器权限，仅限实验环境，生产走变更审批",
    )
    private val LV_READ = mapOf(
        "r" to "只读命令，可在自己办公机上跑；结论写进工单。",
        "w" to "会改动本机配置：先征得同意，或在实验虚拟机里做。",
        "s" to "需要管理员或服务器权限：只在实验环境跑，生产走变更审批。",
    )

    private fun synth(pack: Pack, c: Chapter): LongLecture {
        val dp = pack.deep[c.id] ?: Deep(emptyList(), emptyList(), emptyList())
        val concept = ArrayList<ConceptBlock>()
        concept.add(ConceptBlock.p("本章要解决的问题：" + c.s + "。先用下面的双轨对照把术语钉住，再看后面的细节条目。"))
        concept.add(
            ConceptBlock(
                head = listOf("专业术语", "大白话（面试和文档里怎么翻译给别人）"),
                rows = c.plain.map { listOf(it.first, it.second) },
            )
        )
        dp.deep.forEach { concept.add(ConceptBlock.p(it.first + "。" + it.second)) }
        val practice = ArrayList<StepBlock>()
        c.cmds.forEach { x ->
            practice.add(
                StepBlock(
                    x.n, "档位：" + (LV_DESC[x.lv] ?: LV_DESC["r"]), x.c, "",
                    LV_READ[x.lv] ?: LV_READ["r"]!!,
                )
            )
        }
        dp.out.forEach { o ->
            practice.add(StepBlock("看输出样例：" + o.first, "先看清正常输出长什么样，现场才有对照物。", o.first, o.second, ""))
        }
        dp.flow.forEach { f ->
            practice.add(
                StepBlock(
                    f.first, "把这条链路当成操作清单，逐项做完再进下一项。",
                    "顺序：" + f.second.split("→").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" → "),
                    "", "",
                )
            )
        }
        return LongLecture(
            title = c.t,
            concept = concept,
            practice = if (practice.isEmpty()) listOf(
                StepBlock("本章无命令行操作", "用图形界面完成，重点是流程与留痕。", "（图形界面为主）参考指南条目的操作路径", "", "")
            ) else practice,
            guide = c.pits.map { GuideBlock(it.w, it.c, it.r, "", "") },
            quiz = c.quiz,
        )
    }

    /** 复制本章要点（对应 Web chapterDigest） */
    fun digest(pack: Pack, c: Chapter): String {
        val d = of(pack, c)
        val l = ArrayList<String>()
        l.add("【" + c.id + " " + c.t + "】")
        l.add("")
        l.add("本节要解决：" + c.s)
        l.add("")
        l.add("■ 双轨对照")
        c.plain.forEach { l.add("· " + it.first + " = " + it.second) }
        l.add("")
        l.add("■ 专业清单")
        l.add(c.terms.split("｜").joinToString(" / "))
        d.concept.forEach { b ->
            l.add("")
            l.add("■ " + (if (b.title.isNotEmpty()) b.title else "概念"))
            if (b.p.isNotEmpty()) l.add(b.p)
            b.ul.forEach { l.add("- " + it) }
            if (b.isTable) {
                l.add(b.head.joinToString(" | "))
                b.rows.forEach { l.add(it.joinToString(" | ")) }
            }
            if (b.md.isNotEmpty()) l.add(b.md)
        }
        l.add("")
        l.add("■ 命令与样例")
        c.cmds.forEach { l.add("\$ " + it.c + "  (" + it.n + ")") }
        d.practice.forEach { s ->
            if (s.cmd.isNotEmpty()) l.add("\$ " + s.cmd)
            if (s.out.isNotEmpty()) {
                l.add("预期输出：")
                l.add(s.out)
            }
        }
        l.add("")
        l.add("■ 避坑")
        c.pits.forEach {
            l.add("错误：" + it.w)
            l.add("  后果：" + it.c)
            l.add("  正确：" + it.r)
        }
        l.add("")
        l.add("■ 过关标准")
        l.add(c.gate)
        l.add("")
        l.add("■ 练习题")
        l.add(c.quiz.q)
        l.add("")
        l.add("参考答案")
        l.add(c.quiz.a)
        return l.joinToString("\n")
    }
}
