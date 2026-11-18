package com.qwen.opsmentor.ui

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.qwen.opsmentor.App
import com.qwen.opsmentor.core.AnnotatedText
import com.qwen.opsmentor.core.MarkEditor
import com.qwen.opsmentor.core.MarkEngine
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.surface
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.itemIn
import com.qwen.opsmentor.core.installHtmlPressFeedbackRecursively
import com.qwen.opsmentor.data.Chapter
import com.qwen.opsmentor.data.Pack
import com.qwen.opsmentor.state.Mark
import com.qwen.opsmentor.state.Store

/** 页面滚动回调（MainActivity 实现，用于顶栏实底切换） */
interface ScrollOwner {
    fun onPageScroll(key: String, y: Int, threshold: Int)
}

/**
 * 页面基类：一个页面 = 一个 body() 渲染函数（对应 Web 的 renderXxx()）。
 * 重绘时整棵子树重建，保证主题 / 状态变更后与 Web 行为一致。
 */
abstract class Page(val act: Activity, val app: App) {

    abstract val key: String
    open val isTab: Boolean get() = false
    open fun title(): String = ""
    open val scrollSolidThreshold: Int get() = 6
    open val learning: Boolean get() = false

    val blocks = ArrayList<AnnotatedText>()
    var scroll: NestedScrollView? = null
        private set
    var content: LinearLayout? = null
        private set
    private var lastY = 0

    val ctx: Activity get() = act
    val store: Store get() = app.store
    val pack: Pack get() = app.currentPack()

    abstract fun body(host: LinearLayout)

    /** 顶栏动作区（课程管理页的「导入」图标等） */
    open fun appbarActions(host: LinearLayout) {}

    open fun posKey(): String = key
    open fun readingSegs(): List<String> = emptyList()

    fun ensure(): NestedScrollView {
        scroll?.let { return it }
        val c = act
        val box = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            // HTML 最终版两种风格的 stage 均为 20px；窄屏媒体查询收窄为 17px。
            val widthDp = c.resources.displayMetrics.widthPixels / c.resources.displayMetrics.density
            val pad = c.dp(if (widthDp <= 420f) 17f else 20f)
            // HTML stage 底部预留 122px；原生容器已避开 60px 底栏，因此内容补足 62px。
            setPadding(pad, 0, pad, c.dp(62f))
        }
        val owner = (c as? ScrollOwner)
        val sv = object : NestedScrollView(c) {
            override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
                super.onScrollChanged(l, t, oldl, oldt)
                lastY = t
                owner?.onPageScroll(key, t, scrollSolidThreshold)
                app.timer.poke(store)
            }
        }
        sv.isFillViewport = true
        sv.clipToPadding = false
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        sv.addView(box, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        scroll = sv
        content = box
        return sv
    }

    /** 整页重绘；keepScroll=false 时回顶部（对应 Web 的 __readTop） */
    fun rerender(toTop: Boolean = false, animateItems: Boolean = false) {
        val box = content ?: return
        // 搜索框等输入控件会在 TextWatcher 中触发重绘。记录焦点路径与光标，
        // 避免每输入一个字符就销毁输入框，导致键盘失焦、搜索无法连续输入。
        val focusedInput = box.findFocus() as? android.widget.EditText
        val focusPath = focusedInput?.let { childPath(box, it) }
        val selectionStart = focusedInput?.selectionStart ?: -1
        val selectionEnd = focusedInput?.selectionEnd ?: -1
        val imm = act.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        val restoreIme = focusedInput != null && imm?.isAcceptingText == true

        box.removeAllViews()
        blocks.clear()
        body(box)
        // HTML 按压反馈：递归为可点击行 / 卡片补上主色水波，不改写原点击回调。
        box.installHtmlPressFeedbackRecursively()
        // HTML .stagger：页面进入时，一级内容按 30ms 起步错峰浮现。
        if (animateItems && !Tokens.reducedMotion) {
            for (index in 0 until box.childCount) {
                box.getChildAt(index).itemIn((30L + index * 50L).coerceAtMost(390L))
            }
        }
        app.registerBlocks(blocks)
        app.reader?.load(readingSegs())
        val key = "$CUR:${posKey()}"
        val y = if (toTop) {
            store.readPos[key] = 0
            0
        } else (store.readPos[key] ?: lastY)
        box.post {
            scroll?.scrollTo(0, y.coerceAtLeast(0))
            val target = focusPath?.let { viewAtPath(box, it) } as? android.widget.EditText
            if (target != null) {
                target.requestFocus()
                val end = target.text?.length ?: 0
                if (selectionStart >= 0 && selectionEnd >= 0) {
                    target.setSelection(selectionStart.coerceAtMost(end), selectionEnd.coerceAtMost(end))
                }
                if (restoreIme) target.post { imm?.showSoftInput(target, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) }
            }
        }
    }

    private fun childPath(root: ViewGroup, target: View): IntArray? {
        val reversed = ArrayList<Int>()
        var current: View = target
        while (current !== root) {
            val parent = current.parent as? ViewGroup ?: return null
            val index = parent.indexOfChild(current)
            if (index < 0) return null
            reversed.add(index)
            current = parent
        }
        return reversed.asReversed().toIntArray()
    }

    private fun viewAtPath(root: ViewGroup, path: IntArray): View? {
        var current: View = root
        for (index in path) {
            val parent = current as? ViewGroup ?: return null
            if (index !in 0 until parent.childCount) return null
            current = parent.getChildAt(index)
        }
        return current
    }

    val CUR: String get() = store.cur

    /** 划线块注册：所有页面共用同一套选择 / 编辑 / 定位逻辑 */
    fun block(t: AnnotatedText): AnnotatedText {
        t.marksProvider = { store.marks }
        t.onSelect = { v, a, b -> (act as? MainActivity)?.selectionBar?.open(v, a, b) }
        t.onPick = { m -> MarkEditor.open(act, app, m, { v -> if (blocks.contains(v)) v else null }, { mm -> locate(mm) }) }
        blocks.add(t)
        t.refresh()
        return t
    }

    /** 定位到原文：滚动到该划线所在块并短暂高亮 */
    open fun locate(m: Mark): Boolean {
        val target = blocks.firstOrNull { it.spec.chId == m.ch } ?: return false
        target.post {
            var y = target.top
            var p = target.parent as? View
            while (p != null && p != scroll) {
                y += p.top
                p = p.parent as? View
            }
            scroll?.scrollTo(0, (y - act.dp(72f)).coerceAtLeast(0))
            val sg = m.segs.firstOrNull()
            if (sg != null) {
                target.setSelection(sg.s, sg.e)
                target.postDelayed({ target.clearSelection() }, 1800)
            }
        }
        return true
    }

    /** 大标题页头（Tab 页 / 子页均可用） */
    fun pageHead(host: LinearLayout, title: String, right: View? = null, big: Boolean = isTab): View {
        val c = act
        val hd = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, c.dp(if (big) 26f else 12f), 0, c.dp(4f))
        }
        hd.addView(label(c, title, if (big) 29f else 20f, Tokens.ink, Tokens.weightH()).also {
            it.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (right != null) hd.addView(right)
        host.addView(hd)
        return hd
    }

    /** 卡片容器 */
    fun card(host: LinearLayout, padDp: Float = 16f, top: Int = 14): LinearLayout {
        val c = act
        val box = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(c.dp(padDp), c.dp(padDp), c.dp(padDp), c.dp(padDp))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = c.dp(top.toFloat())
            }
            surface(Tokens.l2, Tokens.rCard(), if (Tokens.planet) 0f else 2f, true)
        }
        host.addView(box)
        return box
    }

    /** 文本变更防抖监听 */
    protected fun watcher(fn: (String) -> Unit): android.text.TextWatcher = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) = fn(s?.toString()?.trim() ?: "")
    }

    /** 搜索框 */
    protected fun searchField(host: View, hint: String, initial: String, onChange: (String) -> Unit): android.widget.EditText {
        val c = act
        val inp = android.widget.EditText(c).apply {
            this.hint = hint
            setHintTextColor(Tokens.withAlpha(Tokens.sub, 0.75f))
            setTextColor(Tokens.ink)
            setText(initial)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14.5f)
            isSingleLine = true
            val p = c.dp(11f)
            setPadding(p, p, p, p)
            flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.04f), c.dp(10f).toFloat())
            addTextChangedListener(watcher(onChange))
        }
        (host as android.view.ViewGroup).addView(inp, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = c.dp(10f)
        })
        return inp
    }

    /** 工具页头部卡：图标 + 标题 + 说明 */
    protected fun toolHeader(host: LinearLayout, ico: String, head: String, intro: String): LinearLayout {
        val c = act
        val card = card(host, 16f, 12)
        val hd = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val box = FrameLayout(c).apply {
            flat(Tokens.priSoft, c.dp(8f).toFloat())
            layoutParams = LinearLayout.LayoutParams(c.dp(30f), c.dp(30f))
        }
        box.addView(com.qwen.opsmentor.core.iconView(c, ico, Tokens.pri, 17f))
        hd.addView(box)
        hd.addView(label(c, head, 17f, Tokens.ink, Tokens.weightH()), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = c.dp(10f) })
        card.addView(hd)
        if (intro.isNotEmpty()) card.addView(label(c, intro, 13f, Tokens.sub).also {
            it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { lp -> lp.topMargin = c.dp(8f) }
        })
        return card
    }

    /** 当前路线的当前章节 id */
    protected fun currentChapterId(): String {
        val b = store.bag()
        if (b.pin.isNotEmpty() && pack.chById(b.pin) != null) return b.pin
        val q = pack.route(b.route)
        return if (q.isEmpty()) "" else q[b.idx.coerceIn(0, q.size - 1)]
    }

    /** 当前章节对象 */
    protected fun currentChapter(): Chapter? = pack.chById(currentChapterId()) ?: pack.ch.firstOrNull()

    fun gap(host: LinearLayout, dp: Int) {
        host.addView(View(act), LinearLayout.LayoutParams(1, act.dp(dp.toFloat())))
    }

    /** 刷新划线（数据变化后） */
    fun repaintMarks() {
        blocks.forEach { it.refresh() }
    }

    companion object {
        /** 供外部（浮层）按签名找回块 */
        fun findBySig(blocks: List<AnnotatedText>, sig: String): AnnotatedText? =
            blocks.firstOrNull { it.sigId == sig }

        fun resolveHost(blocks: List<AnnotatedText>, m: Mark): AnnotatedText? {
            val sg = m.segs.firstOrNull() ?: return null
            val exact = blocks.firstOrNull { it.sigId == sg.sig && it.spec.chId == m.ch }
            if (exact != null) return exact
            val cands = blocks.filter { it.spec.chId == m.ch && sg.txt.isNotEmpty() && it.textOf().contains(sg.txt) }
            return if (cands.size == 1) cands[0] else cands.firstOrNull()
        }

        fun adjustOnHost(blocks: List<AnnotatedText>, m: Mark, edge: String, delta: Int): Boolean {
            val host = resolveHost(blocks, m) ?: return false
            val ok = MarkEngine.adjust(m, host.textOf(), host.sigId, edge, delta)
            if (ok) {
                val sg = m.segs[if (edge == "s") 0 else m.segs.size - 1]
                host.setSelection(sg.s, sg.e)
            }
            return ok
        }

        fun expandOnHost(blocks: List<AnnotatedText>, m: Mark): Boolean {
            val host = resolveHost(blocks, m) ?: return false
            MarkEngine.expandToBlock(m, host.textOf(), host.sigId)
            return true
        }
    }
}
