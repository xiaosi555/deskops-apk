package com.qwen.opsmentor.ui.pages
import com.qwen.opsmentor.core.groupTitle

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.qwen.opsmentor.App
import com.qwen.opsmentor.ui.Bars
import com.qwen.opsmentor.core.Icons
import com.qwen.opsmentor.core.Sheets
import com.qwen.opsmentor.core.Tokens
import com.qwen.opsmentor.core.add
import com.qwen.opsmentor.core.countUp
import com.qwen.opsmentor.core.dp
import com.qwen.opsmentor.core.flat
import com.qwen.opsmentor.core.hrow
import com.qwen.opsmentor.core.iconView
import com.qwen.opsmentor.core.label
import com.qwen.opsmentor.core.muted
import com.qwen.opsmentor.core.pill
import com.qwen.opsmentor.core.typeset
import com.qwen.opsmentor.core.vcol
import com.qwen.opsmentor.state.Courses
import com.qwen.opsmentor.ui.MainActivity
import com.qwen.opsmentor.ui.Page
import com.qwen.opsmentor.ui.ProfileEditor
import com.qwen.opsmentor.ui.Rows
import java.io.File

/**
 * 我的（底部导航第二项，对应 Web renderProfileMin）：
 * 个人信息卡 + 学习空间 / 偏好设置 / 数据与隐私 / 学习概况。
 */
class PageProfile(act: Activity, app: App) : Page(act, app) {

    override val key = "profile"
    override val isTab = true

    override fun body(host: LinearLayout) {
        val ctx = act
        pageHead(host, "我的")
        val s = app.store
        val pr = Courses.prog(s, pack, s.cur)
        val pct = pr.pct()
        // 个人信息卡
        val name = s.profileName.ifBlank { "桌面运维学习者" }
        val bio = s.profileBio.ifBlank { "把每一次学习，留成自己的进度" }
        val card = if (Tokens.planet) hrow(ctx) else vcol(ctx)
        card.flat(Tokens.l2, ctx.dp(Tokens.rCard()))
        val p = ctx.dp(16f)
        card.setPadding(p, p, p, p)
        val av = avatar(name, s.profileAvatar)
        av.setOnClickListener { ProfileEditor.open(act, app) }
        card.addView(av, LinearLayout.LayoutParams(ctx.dp(66f), ctx.dp(66f)))
        val pi = vcol(ctx)
        val nmRow = hrow(ctx)
        nmRow.addView(label(ctx, name, 30f, Tokens.ink, Tokens.weightH(), 1))
        nmRow.addView(iconView(ctx, "pen", Tokens.sub, 18f), LinearLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.marginStart = ctx.dp(6f) })
        pi.addView(nmRow)
        pi.add(muted(ctx, bio, 13f, 2), t = 6)
        card.addView(pi, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(16f) })
        val edit = label(ctx, "编辑  ›", 13f, Tokens.sub)
        edit.setOnClickListener { ProfileEditor.open(act, app) }
        if (Tokens.planet) card.addView(edit)
        host.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(10f)
        })

        val l1 = groupTitle(ctx, "学习空间")
        host.addView(l1)
        l1.addView(Rows.list(act, "book", "课程", "管理课程、封面与课程资料", Courses.all(s).size.toString()) { nav("course-manage") })
        l1.addView(Rows.list(act, "highlighter", "划线", "按课程查看与整理已保存的重点", s.allMarks().size.toString()) { nav("my-marks") })
        l1.addView(Rows.list(act, "calendar", "学习日历", "查看每天的学习记录与课程进度", "") { nav("calendar") })

        val l2 = groupTitle(ctx, "偏好设置")
        host.addView(l2)
        l2.addView(Rows.list(act, "settings", "设置", "主题色、外观与夜间模式", "") { nav("settings") })

        val l3 = groupTitle(ctx, "数据与隐私")
        host.addView(l3)
        l3.addView(Rows.list(act, "info", "数据说明", "学习数据仅保存在本机", "") { dataSheet() })

        val l4 = groupTitle(ctx, "学习概况")
        host.addView(l4)
        val panel = vcol(ctx)
        panel.flat(Tokens.l2, ctx.dp(Tokens.rCard()))
        panel.setPadding(p, p, p, p)
        val line = hrow(ctx)
        val big = label(ctx, "$pct%", 30f, Tokens.ink, Tokens.weightH())
        big.countUp(pct, "%")
        line.addView(big)
        line.addView(label(ctx, "课程章节完成度 · ${pr.done}/${pr.total}", 12f, Tokens.sub),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        panel.addView(line)
        panel.addView(Bars.bar(ctx, pct / 100f), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(6f)).also { it.topMargin = ctx.dp(10f) })
        host.addView(panel)

        if (Tokens.planet) {
            val io = groupTitle(ctx, "工具箱")
            host.addView(io)
            io.addView(Rows.list(act, "database", "数据", "课程、学习手册、划线、进度、案例与术语", "") { nav("io") })
        }
    }

    private fun nav(v: String) = (act as? MainActivity)?.nav(v)

    private fun avatar(name: String, path: String): View {
        val ctx = act
        val box = FrameLayout(ctx)
        if (path.isNotEmpty() && File(path).exists()) {
            val bmp = android.graphics.BitmapFactory.decodeFile(path)
            val iv = android.widget.ImageView(ctx).apply {
                setImageBitmap(bmp)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: View, o: android.graphics.Outline) {
                        o.setOval(0, 0, v.width, v.height)
                    }
                }
            }
            box.addView(iv, FrameLayout.LayoutParams(ctx.dp(66f), ctx.dp(66f)))
        } else {
            val c = FrameLayout(ctx)
            c.flat(if (Tokens.planet) Tokens.priSoft else Tokens.pri, 33f)
            c.addView(label(ctx, name.substring(0, 1), 26f, if (Tokens.planet) Tokens.pri else Tokens.onPri, Tokens.weightH()),
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.CENTER })
            box.addView(c, FrameLayout.LayoutParams(ctx.dp(66f), ctx.dp(66f)))
        }
        return box
    }

    private fun dataSheet() {
        val body = vcol(act, padH = 16)
        body.addView(label(act, "本应用是纯本地应用：进度、划线、案例库、术语手册与个人信息保存在本机，不联网、不上传、无账号体系。", 14f, Tokens.ink))
        body.addView(muted(act, "更换设备或清理数据前，请先在「数据」页导出一份完整档案。"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = act.dp(10f) })
        val link = label(act, "查看完整数据说明与导入导出入口 ›", 13.5f, Tokens.pri, Tokens.weightM())
        link.setOnClickListener { nav("io") }
        body.addView(link, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = act.dp(12f) })
        Sheets.sheet(act, "数据说明", body, null)
    }
}
