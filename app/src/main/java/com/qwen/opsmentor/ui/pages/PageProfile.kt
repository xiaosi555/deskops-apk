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
        val s = app.store
        val name = s.profileName.ifBlank { "桌面运维学习者" }
        val bio = s.profileBio.ifBlank { "还没有设置签名" }
        val course = Courses.find(s, s.cur)
        val courseName = Courses.meta(s, course).first
        val pr = Courses.prog(s, pack, s.cur)
        val pct = pr.pct().coerceIn(0, 100)

        // HTML: #v-profile 从星球顶部装饰带下沿开始，头像与个人资料横向排列。
        host.setPadding(host.paddingLeft, ctx.dp(if (Tokens.planet) 48f else 22f), host.paddingRight, host.paddingBottom)
        val head = hrow(ctx).apply { setPadding(0, ctx.dp(8f), 0, ctx.dp(18f)) }
        val info = vcol(ctx)
        val nameRow = hrow(ctx)
        nameRow.addView(label(ctx, name, 30f, Tokens.ink, Tokens.weightH(), 1))
        nameRow.addView(iconView(ctx, "chev", Tokens.sub, 18f),
            LinearLayout.LayoutParams(ctx.dp(18f), ctx.dp(18f)).also { it.marginStart = ctx.dp(6f) })
        info.addView(nameRow)
        info.addView(muted(ctx, bio, 13f, 2), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = ctx.dp(6f) })
        head.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(avatar(name, s.profileAvatar), LinearLayout.LayoutParams(ctx.dp(66f), ctx.dp(66f)))
        head.isClickable = true
        head.setOnClickListener { ProfileEditor.open(act, app) }
        host.addView(head)

        // HTML: 当前课程进度横幅，点击后直接继续今日学习。
        val banner = FrameLayout(ctx)
        val c1 = Tokens.mix(Tokens.l2, Tokens.pri, 0.06f)
        val c2 = Tokens.mix(Tokens.l2, Tokens.pri, 0.12f)
        val c3 = Tokens.mix(Tokens.l2, Tokens.pri, 0.05f)
        banner.background = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(c1, c2, c3)
        ).apply { cornerRadius = ctx.dp(14f).toFloat() }
        val bannerContent = hrow(ctx).apply {
            setPadding(ctx.dp(20f), ctx.dp(16f), ctx.dp(20f), ctx.dp(20f))
        }
        val bannerLeft = vcol(ctx)
        val bannerTitle = hrow(ctx)
        bannerTitle.addView(iconView(ctx, "sparkle", Tokens.pri, 19f))
        bannerTitle.addView(label(ctx, courseName, 16f, Tokens.pri, Tokens.weightH(), 1),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(8f) })
        bannerLeft.addView(bannerTitle)
        bannerLeft.addView(muted(ctx,
            "已过关 ${pr.done} / ${pr.total} 章 · 累计学习 Day ${s.bag().day}",
            12f, 2
        ), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.topMargin = ctx.dp(6f)
        })
        bannerContent.addView(bannerLeft, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val action = hrow(ctx)
        action.addView(label(ctx, if (s.bag().learn.isNotEmpty()) "继续学习" else "开始学习", 14f, Tokens.pri, Tokens.weightM()))
        action.addView(iconView(ctx, "chev", Tokens.pri, 16f),
            LinearLayout.LayoutParams(ctx.dp(16f), ctx.dp(16f)).also { it.marginStart = ctx.dp(2f) })
        bannerContent.addView(action)
        banner.addView(bannerContent, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        val track = FrameLayout(ctx).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = ctx.dp(3f).toFloat()
                setColor(Tokens.withAlpha(Tokens.pri, 0.16f))
            }
        }
        val fill = View(ctx).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = ctx.dp(3f).toFloat()
                setColor(Tokens.pri)
            }
        }
        track.addView(fill, FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT))
        banner.addView(track, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(3f), Gravity.BOTTOM
        ).also {
            it.leftMargin = ctx.dp(20f); it.rightMargin = ctx.dp(20f); it.bottomMargin = ctx.dp(8f)
        })
        track.post {
            val lp = fill.layoutParams as FrameLayout.LayoutParams
            lp.width = (track.width * pct / 100f).toInt()
            fill.layoutParams = lp
        }
        banner.isClickable = true
        banner.setOnClickListener { nav("today") }
        host.addView(banner, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = ctx.dp(2f); it.bottomMargin = ctx.dp(14f) })

        val first = vcol(ctx)
        profileRow(first, "book", "课程", Courses.all(s).size.toString()) { nav("course-manage") }
        profileRow(first, "highlighter", "划线", s.marks.size.toString()) { nav("my-marks") }
        profileRow(first, "calendar", "学习日历", "") { nav("calendar") }
        host.addView(first)

        val divider = View(ctx).apply { flat(Tokens.hairc, 1f) }
        host.addView(divider, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(1f)
        ).also { it.setMargins(ctx.dp(6f), ctx.dp(10f), ctx.dp(6f), ctx.dp(10f)) })

        val second = vcol(ctx)
        profileRow(second, "settings", "设置", "") { nav("settings") }
        profileRow(second, "layers", "快捷入口", "") { nav("shortcuts") }
        host.addView(second)
    }

    private fun profileRow(host: LinearLayout, icon: String, title: String, tail: String, onClick: () -> Unit) {
        val ctx = act
        val row = hrow(ctx).apply {
            setPadding(ctx.dp(6f), 0, ctx.dp(6f), 0)
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(iconView(ctx, icon, Tokens.pri, 26f),
            LinearLayout.LayoutParams(ctx.dp(26f), ctx.dp(26f)))
        row.addView(label(ctx, title, 18.5f, Tokens.ink, Tokens.weightM()),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = ctx.dp(16f) })
        if (tail.isNotEmpty()) row.addView(label(ctx, tail, 15f, Tokens.sub))
        row.addView(iconView(ctx, "chev", Tokens.withAlpha(Tokens.sub, 0.6f), 20f),
            LinearLayout.LayoutParams(ctx.dp(20f), ctx.dp(20f)).also { it.marginStart = ctx.dp(8f) })
        host.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(64f)
        ))
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
