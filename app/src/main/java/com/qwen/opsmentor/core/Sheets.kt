package com.qwen.opsmentor.core

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.qwen.opsmentor.state.LineType
import com.qwen.opsmentor.state.MarkColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 弹层条目：value / 标题 / 副标题 / 图标 / 分组 / 危险 */
data class Opt(
    val value: String,
    val title: String,
    val sub: String = "",
    val icon: String = "",
    val group: String = "",
    val danger: Boolean = false,
)

object Sheets {

    private val openDialogs = mutableListOf<Dialog>()

    fun closeAll() {
        openDialogs.toList().forEach { runCatching { it.dismiss() } }
        openDialogs.clear()
    }

    private fun dialog(ctx: Context, transparent: Boolean = true): Dialog {
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.window?.apply {
            if (transparent) setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            if (!transparent) addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        d.setCanceledOnTouchOutside(true)
        openDialogs += d
        d.setOnDismissListener { openDialogs.remove(d) }
        return d
    }

    // ---------------- Toast（对齐 Web 底部 1700ms 自动消失） ----------------

    fun toast(ctx: Context, msg: String) {
        val root = (ctx as? android.app.Activity)?.findViewById<FrameLayout>(android.R.id.content) ?: return
        val tv = label(ctx, msg, 13.5f, 0xFFFFFFFF.toInt(), Tokens.weightM())
        val p = ctx.dp(12f)
        tv.setPadding(p, ctx.dp(9f), p, ctx.dp(9f))
        tv.gravity = Gravity.CENTER
        tv.flat(0xE6242B33.toInt(), ctx.dp(12f).toFloat())
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.bottomMargin = ctx.dp(96f)
        root.addView(tv, lp)
        tv.alpha = 0f
        tv.animate().alpha(1f).setDuration(140).withEndAction {
            tv.postDelayed({ tv.animate().alpha(0f).setDuration(220).withEndAction { root.removeView(tv) }.start() }, 1500)
        }.start()
    }

    /** 复制反馈：Q 弹对勾浮层，2500ms 自动隐身 */
    fun popCheck(ctx: Context, msg: String) {
        val root = (ctx as? android.app.Activity)?.findViewById<FrameLayout>(android.R.id.content) ?: return
        val row = hrow(ctx)
        row.flat(if (Tokens.dark) 0xF01F2621.toInt() else 0xF0FDFEFD.toInt(), ctx.dp(14f).toFloat())
        val p = ctx.dp(14f)
        row.setPadding(p, ctx.dp(10f), p, ctx.dp(10f))
        row.addView(iconBtn(ctx, "checkTick", "", 22f, Tokens.pri) {})
        row.addView(label(ctx, msg, 13.5f, Tokens.ink, Tokens.weightM()).also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = ctx.dp(8f)
        })
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.topMargin = ctx.dp(80f)
        root.addView(row, lp)
        row.scaleX = 0.8f; row.scaleY = 0.8f
        row.animate().scaleX(1f).scaleY(1f).setDuration(POP_D).setInterpolator(POP).withEndAction {
            row.postDelayed({ row.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(220).withEndAction { root.removeView(row) }.start() }, 2200)
        }.start()
    }

    fun copy(ctx: Context, text: String, okMsg: String = "已复制到剪贴板") {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (cm == null) { toast(ctx, "复制失败，已改为文本框，手动全选即可"); return }
        cm.setPrimaryClip(ClipData.newPlainText("ops-mentor", text))
        popCheck(ctx, okMsg)
    }

    // ---------------- 底部抽屉 Sheet ----------------

    /**
     * @param cancelText 左侧按钮文案（编辑课程时为「删除」）
     * @return 打开的 Dialog
     */
    fun sheet(
        ctx: Context, title: String, body: View,
        saveText: String? = null, cancelText: String = "取消",
        danger: Boolean = false,
        onSave: (() -> Boolean?)? = null, onCancel: (() -> Unit)? = null,
        maxHeightPx: Int = 0,
    ): Dialog {
        val d = dialog(ctx)
        val wrap = FrameLayout(ctx)
        val sh = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            flat(Tokens.l2, Tokens.rSheet())
            val p = ctx.dp(4f)
            setPadding(0, p, 0, 0)
        }
        // 拖拽指示条（星球版）
        if (Tokens.planet) {
            val bar = View(ctx).apply {
                flat(Tokens.hairc, 4f)
                layoutParams = LinearLayout.LayoutParams(ctx.dp(40f), ctx.dp(4f)).also { it.gravity = Gravity.CENTER_HORIZONTAL; it.topMargin = ctx.dp(4f) }
            }
            sh.addView(bar)
        }
        val hd = hrow(ctx)
        hd.addView(label(ctx, title, 16f, Tokens.ink, Tokens.weightH()).also {
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); it.layoutParams = lp
        })
        hd.addView(iconBtn(ctx, "close", "关闭", 34f) { d.dismiss() })
        hd.setPadding(ctx.dp(16f), ctx.dp(10f), ctx.dp(12f), ctx.dp(6f))
        sh.addView(hd)
        val scroll = NestedScrollView(ctx).apply {
            isVerticalScrollBarEnabled = false
            addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            if (maxHeightPx > 0) layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeightPx)
        }
        sh.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if (maxHeightPx > 0) ViewGroup.LayoutParams.WRAP_CONTENT else 0, 1f))
        if (onSave != null) {
            val ft = hrow(ctx)
            ft.setPadding(ctx.dp(14f), ctx.dp(10f), ctx.dp(14f), ctx.dp(14f) + insetBottom(ctx))
            val c = btn(ctx, cancelText, if (danger) "danger" else "n")
            val s = btn(ctx, saveText, "pri")
            ft.addView(c)
            ft.addView(s, LinearLayout.LayoutParams(0, ctx.dp(44f), 1f).also { it.leftMargin = ctx.dp(8f) })
            c.setOnClickListener {
                if (onCancel != null) onCancel() else d.dismiss()
            }
            s.setOnClickListener { if (onSave() != false) d.dismiss() }
            sh.addView(ft)
        } else {
            sh.addView(View(ctx), LinearLayout.LayoutParams(1, insetBottom(ctx)))
        }
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.BOTTOM
        wrap.addView(sh, lp)
        d.setContentView(wrap)
        d.show()
        sh.translationY = sh.height.toFloat() + ctx.dp(240f)
        sh.animate().translationY(0f).setDuration(260).setInterpolator(SNAP).start()
        return d
    }

    private fun insetBottom(ctx: Context): Int = 0

    /** 表单字段：label + 控件 */
    fun field(ctx: Context, labelText: String, control: View): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, ctx.dp(6f), 0, ctx.dp(6f))
        if (labelText.isNotEmpty()) addView(label(context, labelText, 12.5f, Tokens.sub, Tokens.weightM()))
        control.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(control)
    }

    fun input(ctx: Context, value: String = "", hint: String = "", maxLen: Int = 0, multiLine: Boolean = false): android.widget.EditText =
        android.widget.EditText(ctx).apply {
            setText(value)
            setHint(hint)
            setHintTextColor(Tokens.withAlpha(Tokens.sub, 0.7f))
            setTextColor(Tokens.ink)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = typefaceFor(Tokens.weightB())
            if (maxLen > 0) filters = arrayOf(android.text.InputFilter.LengthFilter(maxLen))
            if (multiLine) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 3
                gravity = Gravity.TOP
            }
            background = null
            val p = ctx.dp(11f)
            setPadding(p, p, p, p)
            flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.04f), ctx.dp(10f).toFloat())
        }

    // ---------------- 选项弹层 Picker ----------------

    fun picker(ctx: Context, title: String, items: List<Opt>, value: String?, onPick: (String, String) -> Unit) {
        val list = vcol(ctx, padH = 0, padV = 0)
        items.forEach { o ->
            if (o.group.isNotEmpty()) {
                list.addView(label(ctx, o.group, 12f, Tokens.sub, Tokens.weightM()).also {
                    (it.layoutParams as LinearLayout.LayoutParams).setMargins(ctx.dp(16f), ctx.dp(14f), ctx.dp(16f), ctx.dp(6f))
                })
                return@forEach
            }
            val row = hrow(ctx)
            row.setPadding(ctx.dp(16f), 0, ctx.dp(16f), 0)
            val on = o.value == value
            row.addView(iconBtn(ctx, o.icon.ifEmpty { if (on) "check" else "dot" }, o.title, 30f,
                if (o.danger) Tokens.bad else (if (on) Tokens.pri else Tokens.sub)) {})
            val copy = vcol(ctx)
            copy.addView(label(ctx, o.title, 15f, if (o.danger) Tokens.bad else Tokens.ink, Tokens.weightM()))
            if (o.sub.isNotEmpty()) copy.addView(muted(ctx, o.sub, 12f), t = 2)
            row.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.leftMargin = ctx.dp(10f) })
            if (on) row.addView(iconBtn(ctx, "checkTick", "", 22f, Tokens.pri) {})
            row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(56f))
            val wrap = FrameLayout(ctx)
            val div = View(ctx).apply { flat(Tokens.hairc, 1f) }
            wrap.addView(row, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            list.addView(wrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(56f)))
            row.isClickable = true
            row.setOnClickListener {
                Sheets.findDialog(row)?.dismiss()
                onPick(o.value, o.title)
            }
        }
        sheet(ctx, title, list, null)
    }

    private fun findDialog(v: View): Dialog? {
        var p = v.parent
        while (p != null) {
            if (p is Dialog) return p
            p = (p as? View)?.parent
        }
        return null
    }

    // ---------------- 确认 / 输入 ----------------

    fun confirm(ctx: Context, msg: String, title: String = "请确认", ok: String = "确定", danger: Boolean = false, onOk: () -> Unit) {
        val d = dialog(ctx)
        val box = vcol(ctx)
        box.surface(Tokens.l2, ctx.dp(if (Tokens.planet) 14f else 18f), 6f, Tokens.planet)
        box.setPadding(ctx.dp(20f), ctx.dp(18f), ctx.dp(20f), ctx.dp(16f))
        box.addView(label(ctx, if (danger) "确认操作" else title, 16.5f, Tokens.ink, Tokens.weightH()))
        box.addView(label(ctx, msg, 14f, Tokens.sub), t = 10)
        val ft = hrow(ctx)
        (ft.layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(18f)
        ft.addView(btn(ctx, "取消", "n"))
        ft.addView(btn(ctx, ok, if (danger) "danger" else "pri").also {
            (it.layoutParams as LinearLayout.LayoutParams).leftMargin = ctx.dp(8f)
        })
        box.addView(ft, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        ft.getChildAt(0).setOnClickListener { d.dismiss() }
        ft.getChildAt(1).setOnClickListener { d.dismiss(); onOk() }
        val wrap = FrameLayout(ctx)
        val lp = FrameLayout.LayoutParams(ctx.dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        wrap.addView(box, lp)
        d.setContentView(wrap)
        d.show()
        box.alpha = 0f; box.scaleX = 0.94f; box.scaleY = 0.94f
        box.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(SNAP).start()
    }

    fun prompt(ctx: Context, title: String, value: String, maxLen: Int = 120, placeholder: String = "", ok: String = "保存", onOk: (String) -> Unit) {
        val inp = input(ctx, value, placeholder, maxLen)
        val col = vcol(ctx)
        col.surface(Tokens.l2, ctx.dp(if (Tokens.planet) 14f else 18f), 6f, Tokens.planet)
        col.setPadding(ctx.dp(20f), ctx.dp(18f), ctx.dp(20f), ctx.dp(16f))
        col.addView(label(ctx, title, 16.5f, Tokens.ink, Tokens.weightH()))
        col.addView(field(ctx, "", inp), t = 12)
        val ft = hrow(ctx)
        (ft.layoutParams as LinearLayout.LayoutParams).topMargin = ctx.dp(14f)
        ft.addView(btn(ctx, "取消", "n"))
        ft.addView(btn(ctx, ok, "pri").also { (it.layoutParams as LinearLayout.LayoutParams).leftMargin = ctx.dp(8f) })
        col.addView(ft)
        val d = dialog(ctx)
        val wrap = FrameLayout(ctx)
        val lp = FrameLayout.LayoutParams(ctx.dp(320f), ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        wrap.addView(col, lp)
        d.setContentView(wrap)
        d.show()
        inp.requestFocus()
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        ft.getChildAt(0).setOnClickListener { d.dismiss() }
        ft.getChildAt(1).setOnClickListener {
            val v = inp.text.toString().trim()
            if (v.isEmpty()) { toast(ctx, "名称不能为空"); return@setOnClickListener }
            d.dismiss(); onOk(v)
        }
    }

    // ---------------- 导出预览（先看后存，绝不自动落盘） ----------------

    /**
     * 导出预览弹层：标题 + 文件名 · 体积 · "仅在本机生成，未上传"
     * + 预览/源码切换（md） + 底部动作（复制全文 / 保存 / 打印或分享 / 关闭）
     */
    fun export(
        ctx: Context, title: String, filename: String, bytes: Int, text: String,
        kind: String, hint: String = "", onSave: () -> Unit, onPrint: (() -> Unit)? = null,
    ) {
        val d = dialog(ctx)
        val sheet = vcol(ctx)
        sheet.surface(Tokens.l2, ctx.dp(if (Tokens.planet) 18f else 14f), 10f, Tokens.planet)
        val hd = hrow(ctx)
        val meta = vcol(ctx)
        meta.addView(label(ctx, title, 15.5f, Tokens.ink, Tokens.weightH()))
        meta.addView(muted(ctx, "$filename · ${fmtSize(bytes)} · 仅在本机生成，未上传", 11.5f), t = 2)
        hd.addView(meta, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hd.addView(iconBtn(ctx, "close", "关闭", 34f) { d.dismiss() })
        hd.setPadding(ctx.dp(16f), ctx.dp(14f), ctx.dp(12f), ctx.dp(8f))
        sheet.addView(hd)
        val tabs = hrow(ctx)
        tabs.setPadding(ctx.dp(16f), 0, ctx.dp(16f), ctx.dp(6f))
        val body = ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = false
            flat(if (Tokens.planet) Tokens.l3 else Tokens.withAlpha(Tokens.ink, 0.03f), ctx.dp(10f).toFloat())
        }
        val content = vcol(ctx, padH = 12, padV = 12)
        body.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        sheet.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).also {
            it.setMargins(ctx.dp(16f), 0, ctx.dp(16f), 0)
            it.height = ctx.dp(340f)
        })
        var source = false
        fun paint() {
            content.removeAllViews()
            if (kind == "md" && !source) {
                MarkdownView(ctx, text).also { content.addView(it) }
            } else {
                content.addView(mono(ctx, text))
            }
        }
        if (kind == "md") {
            listOf("渲染效果" to false, "源码" to true).forEachIndexed { i, pair ->
                tabs.addView(chip(ctx, pair.first, !source, null, 0) { source = pair.second; paint(); refreshTabs(tabs, !source, i) })
            }
        } else {
            tabs.addView(pill(ctx, if (kind == "json") "JSON 存档" else kind.uppercase() + " 文本", true))
        }
        paint()
        val ft = hrow(ctx)
        ft.setPadding(ctx.dp(16f), ctx.dp(10f), ctx.dp(16f), ctx.dp(12f))
        val cp = btn(ctx, "复制全文", "pri"); cp.setOnClickListener { copy(ctx, text, "已复制全文，可直接粘进笔记") }
        val sv = btn(ctx, "保存到本机", "n"); sv.setOnClickListener { onSave() }
        ft.addView(cp)
        ft.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.leftMargin = ctx.dp(8f) })
        if (onPrint != null) {
            val pr = btn(ctx, "打印 / 存为 PDF", "n"); pr.setOnClickListener { onPrint() }
            ft.addView(pr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.leftMargin = ctx.dp(8f) })
        }
        val sh = btn(ctx, "分享", "ghost"); sh.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text.take(20000)) }
            try { ctx.startActivity(Intent.createChooser(i, title)) } catch (_: Exception) { toast(ctx, "没有可用的分享目标") }
        }
        ft.addView(sh, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.leftMargin = ctx.dp(8f) })
        sheet.addView(ft)
        sheet.addView(noteBox(ctx, "这里只是预览：点「保存到本机」才会生成文件，直接关闭不会触发任何下载。", hint), t = 0)
        ((sheet.getChildAt(sheet.childCount - 1).layoutParams) as? LinearLayout.LayoutParams)?.setMargins(ctx.dp(16f), ctx.dp(6f), ctx.dp(16f), ctx.dp(16f))
        val wrap = FrameLayout(ctx)
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(520f))
        lp.gravity = Gravity.CENTER
        wrap.addView(sheet, lp)
        d.setContentView(wrap)
        d.show()
    }

    private fun refreshTabs(tabs: LinearLayout, source: Boolean, active: Int) {
        for (i in 0 until tabs.childCount) {
            val c = tabs.getChildAt(i)
            (c as? LinearLayout)?.let { chip ->
                val on = i == if (source) 1 else 0
                chip.flat(if (on) Tokens.priSoft else Tokens.l3, chip.context.dp(12f).toFloat())
            }
        }
    }

    fun fmtSize(n: Int): String = when {
        n < 1024 -> "$n B"
        n < 1048576 -> String.format(Locale.CHINA, "%.1f KB", n / 1024.0)
        else -> String.format(Locale.CHINA, "%.2f MB", n / 1048576.0)
    }

    fun fmtSizeLong(n: Long): String = when {
        n >= 1073741824 -> String.format(Locale.CHINA, "%.1f GB", n / 1073741824.0)
        n >= 1048576 -> (n / 1048576).toString() + " MB"
        else -> (Math.max(1L, n / 1024)).toString() + " KB"
    }

    val TIME_FMT = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA)
    fun date(ms: Long): String = TIME_FMT.format(Date(ms))
}

/** 线型 × 颜色的展示名（浮层、导出、列表标签共用） */
fun lineLabel(ty: String) = LineType.of(ty).label
fun colorLabel(co: String) = MarkColor.of(co).label
