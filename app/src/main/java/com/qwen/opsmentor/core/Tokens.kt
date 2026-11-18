package com.qwen.opsmentor.core

import android.graphics.Color
import com.qwen.opsmentor.state.Appearance
import com.qwen.opsmentor.state.Store
import com.qwen.opsmentor.state.StyleMode
import kotlin.math.pow

/**
 * 设计令牌引擎（对齐 Web 版 CSS 变量）。
 * 4 层表面只靠 1~3% 明度差 + 柔和阴影拉开层级；星球版改用 1px 细描边。
 * 所有页面渲染前必须调用 [Tokens.refresh]。
 */
object Tokens {
    /** L0 页面 / L1 顶栏 / L2 卡片 / L3 卡内元素 */
    var l0 = 0xFFEFF3F0.toInt()
    var l1 = 0xFFF3F6F4.toInt()
    var l2 = 0xFFF9FBFA.toInt()
    var l3 = 0xFFFDFEFD.toInt()
    var ink = 0xFF1E242C.toInt()
    var sub = 0xFF69737F.toInt()
    var pri = 0xFF628B72.toInt()
    var priSoft = 0xFFE3EFE7.toInt()
    var onPri = 0xFFFFFFFF.toInt()
    var ok = 0xFF188A5C.toInt()
    var warn = 0xFFB8770F.toInt()
    var bad = 0xFFC4483F.toInt()
    var hairc = 0x141E242C          // 细描边（星球版）
    var accentWash = 0              // 当前行主色底纹
    var scrim = 0x731B2228.toInt()  // 遮罩
    var codeBg = 0xFFF5F7FA.toInt() // 命令块底色
    var dark = false
    var planet = true
    var accentIndex = 4
    var accentLine = 0
    var reducedMotion = false

    /** 与 HTML ACCENT_PALETTE 一致的九色板，默认第 5 项翡翠绿。 */
    val PALETTE = listOf(
        0xFFF5304A.toInt(), 0xFF1A6DFF.toInt(), 0xFF7A3CFF.toInt(),
        0xFFFF7A00.toInt(), 0xFF00B56A.toInt(), 0xFF00A6C7.toInt(),
        0xFFF23D97.toInt(), 0xFFF5B800.toInt(), 0xFF3B4BFF.toInt(),
    )
    val PALETTE_NAMES = listOf("珊瑚红", "海洋蓝", "电光紫", "活力橙", "翡翠绿", "湖水青", "玫瑰粉", "柠檬黄", "靛蓝")

    fun refresh(store: Store, systemDark: Boolean) {
        dark = when (store.appearance) {
            Appearance.LIGHT -> false
            Appearance.DARK -> true
            Appearance.SYSTEM -> systemDark
        }
        planet = store.style == StyleMode.PLANET
        accentIndex = store.accent
        val custom = if (store.accent == -1) runCatching { Color.parseColor(store.accentCustom) }.getOrNull() else null
        val base = custom ?: PALETTE[if (store.accent == -1) 4 else store.accent.coerceIn(0, PALETTE.size - 1)]
        // HTML applyAccent：暗色主色仅提亮 14%，亮色保持原始色值。
        pri = if (dark) mix(base, 0xFFFFFFFF.toInt(), 0.14f) else base
        onPri = if (ratioOn(pri) < 2.2f) 0xFF1B1D22.toInt() else 0xFFFFFFFF.toInt()

        // 与 HTML 最终级联后的 data-style / data-theme 变量逐项对应。
        if (planet && !dark) {
            l0 = 0xFFFFFFFF.toInt()
            l1 = 0xFFFFFFFF.toInt()
            l2 = 0xFFFFFFFF.toInt()
            l3 = mix(0xFFFFFFFF.toInt(), pri, 0.06f)
            ink = mix(0xFF0B1511.toInt(), pri, 0.16f)
            sub = mix(0xFF6B7873.toInt(), pri, 0.30f)
            priSoft = mix(0xFFFFFFFF.toInt(), pri, 0.11f)
            accentWash = mix(0xFFFFFFFF.toInt(), pri, 0.07f)
            accentLine = mix(0xFFFFFFFF.toInt(), pri, 0.34f)
            hairc = mix(0xFFFFFFFF.toInt(), pri, 0.13f)
        } else if (planet && dark) {
            l0 = 0xFF0E1011.toInt()
            l1 = 0xFF0E1011.toInt()
            l2 = 0xFF141718.toInt()
            l3 = mix(0xFF1A1D1E.toInt(), pri, 0.09f)
            ink = mix(0xFFEEF1F5.toInt(), pri, 0.08f)
            sub = mix(0xFF98A1AB.toInt(), pri, 0.22f)
            priSoft = mix(0xFF12161A.toInt(), pri, 0.17f)
            accentWash = mix(0xFF12161A.toInt(), pri, 0.11f)
            accentLine = mix(0xFF12161A.toInt(), pri, 0.62f)
            hairc = withAlpha(pri, 0.14f)
        } else if (!dark) {
            // HTML 极简版浅色主题
            l0 = 0xFFF7F7F7.toInt()
            l1 = 0xFFF7F7F7.toInt()
            l2 = 0xFFFFFFFF.toInt()
            l3 = 0xFFF2F2F2.toInt()
            ink = 0xFF1C1D1F.toInt()
            sub = 0xFF898C91.toInt()
            priSoft = mix(0xFFFFFFFF.toInt(), pri, 0.06f)
            accentWash = 0xFFF3F3F3.toInt()
            accentLine = mix(0xFFFFFFFF.toInt(), pri, 0.34f)
            hairc = 0x12000000
        } else {
            // HTML 极简版深色主题
            l0 = 0xFF101112.toInt()
            l1 = 0xFF131415.toInt()
            l2 = 0xFF17181A.toInt()
            l3 = 0xFF1F2022.toInt()
            ink = 0xFFECEDED.toInt()
            sub = 0xFF8E9196.toInt()
            priSoft = mix(0xFF17181A.toInt(), pri, 0.12f)
            accentWash = 0xFF1D1E20.toInt()
            accentLine = mix(0xFF17181A.toInt(), pri, 0.62f)
            hairc = 0x14FFFFFF
        }

        // HTML v7 单色主题状态色：同样由主色派生。
        ok = pri
        warn = mix(0xFFFFFFFF.toInt(), pri, 0.70f)
        bad = mix(0xFF1D2026.toInt(), pri, 0.74f)
        scrim = if (planet) {
            // HTML 星球版弹层遮罩：主色 12% 混入深色底，整体透明度约 49%。
            withAlpha(mix(0xFF0A0E12.toInt(), pri, 0.245f), 0.49f)
        } else 0x4719201B
        codeBg = l3
    }

    var theme = "light"

    /** 暗色字重整体下调 40，抵消人眼光晕造成的"看起来更粗" */
    fun weightH() = if (dark) 620 else 660
    fun weightM() = if (dark) 560 else 600
    fun weightS() = if (dark) 660 else 700
    fun weightB() = if (dark) 360 else 400

    /** 圆角：极简版卡片 18dp / 星球版卡片 10dp、卡内元素 8dp */
    fun rCard() = if (planet) 10f else 18f
    fun rInner() = if (planet) 8f else 12f
    fun rBtn() = if (planet) 8f else 12f
    fun rCover() = if (planet) 6f else 17f
    fun rPill() = if (planet) 6f else 999f
    fun rSheet() = if (planet) 18f else 16f

    // ---------------- 颜色工具 ----------------

    /** 与 CSS color-mix(in srgb, A t%, B) 同口径：sRGB 分量线性插值 */
    fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt().coerceIn(0, 255),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt().coerceIn(0, 255),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt().coerceIn(0, 255),
    )

    fun withAlpha(c: Int, a: Float): Int =
        Color.argb((a.coerceIn(0f, 1f) * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))

    /** WCAG 相对亮度 */
    fun lum(color: Int): Float {
        fun f(x: Int): Float {
            val v = x / 255f
            return if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
        }
        return 0.2126f * f(Color.red(color)) + 0.7152f * f(Color.green(color)) + 0.0722f * f(Color.blue(color))
    }

    /** 主色上的文字色判定（与 Web 1.05/(L+0.05) < 2.2 同一公式） */
    fun ratioOn(bg: Int): Float = 1.05f / (lum(bg) + 0.05f)

    /** 快捷入口文字色：底纹亮 → 黑字，底纹暗 → 白字 */
    fun autoTextColor(bg: Int): Int = if (lum(bg) > 0.36f) 0xFF161A18.toInt() else 0xFFFFFFFF.toInt()

    /** 状态色（🟢🟡🔴） */
    fun statusColor(v: String?): Int = when (v) {
        "green" -> ok
        "yellow" -> warn
        "red" -> bad
        else -> sub
    }

    fun statusLabel(v: String?): String = when (v) {
        "green" -> "🟢 过关"
        "yellow" -> "🟡 半过"
        "red" -> "🔴 未过关"
        else -> "未标记"
    }
}
