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
    var accentIndex = 0
    var reducedMotion = false

    /** 9 色板：苔绿 海盐蓝 雾紫 陶土 灰蓝 橄榄 浅棕 石墨 樱粉 */
    val PALETTE = listOf(
        0xFF628B72.toInt(), 0xFF6689A5.toInt(), 0xFF8B7CA8.toInt(), 0xFFB47E68.toInt(),
        0xFF718B91.toInt(), 0xFF87916A.toInt(), 0xFFA58B70.toInt(), 0xFF78817C.toInt(),
        0xFFB98591.toInt(),
    )
    val PALETTE_NAMES = listOf("苔绿", "海盐蓝", "雾紫", "陶土", "灰蓝", "橄榄", "浅棕", "石墨", "樱粉")

    private const val L0_L = 0xFFEFF3F0.toInt()
    private const val L1_L = 0xFFF3F6F4.toInt()
    private const val L2_L = 0xFFF9FBFA.toInt()
    private const val L3_L = 0xFFFDFEFD.toInt()
    private const val L0_D = 0xFF111512.toInt()
    private const val L1_D = 0xFF151A16.toInt()
    private const val L2_D = 0xFF1A201B.toInt()
    private const val L3_D = 0xFF1F2621.toInt()

    fun refresh(store: Store, systemDark: Boolean) {
        dark = when (store.appearance) {
            Appearance.LIGHT -> false
            Appearance.DARK -> true
            Appearance.SYSTEM -> systemDark
        }
        planet = store.style == StyleMode.PLANET
        accentIndex = store.accent.coerceIn(0, PALETTE.size - 1)
        val base = PALETTE[accentIndex]
        theme = if (dark) "dark" else "light"
        pri = if (dark) mix(base, 0xFFFFFF, 0.14f) else base
        onPri = if (ratioOn(pri) < 2.2f) 0xFF1B1D22.toInt() else 0xFFFFFFFF.toInt()
        l0 = if (dark) L0_D else L0_L
        l1 = if (dark) L1_D else L1_L
        l2 = if (dark) L2_D else L2_L
        l3 = if (dark) L3_D else L3_L
        priSoft = mix(if (dark) L2_D else L2_L, pri, if (dark) 0.20f else 0.12f)
        accentWash = mix(if (dark) L2_D else L2_L, pri, if (dark) 0.16f else 0.09f)
        ink = if (dark) 0xFFDFE5EE.toInt() else 0xFF1E242C.toInt()
        sub = if (dark) 0xFF8F9AA8.toInt() else 0xFF69737F.toInt()
        ok = if (dark) 0xFF3FB37F.toInt() else 0xFF188A5C.toInt()
        warn = if (dark) 0xFFD9A441.toInt() else 0xFFB8770F.toInt()
        bad = if (dark) 0xFFE2776D.toInt() else 0xFFC4483F.toInt()
        hairc = if (dark) 0x1AFFFFFF else 0x141E242C
        scrim = if (dark) 0xCC000000.toInt() else 0x731B2228.toInt()
        codeBg = if (dark) mix(L3_D, 0xFFFFFF, 0.05f) else 0xFFF5F7FA.toInt()
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
