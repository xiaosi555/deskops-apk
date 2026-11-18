package com.qwen.opsmentor.core

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.qwen.opsmentor.R

/** 图标名 → drawable（由源应用 SVG path 自动生成，共 87 个） */
object Icons {
    private val map: HashMap<String, Int> = HashMap()

    init {
        put("back", R.drawable.ic_back); put("moon", R.drawable.ic_moon); put("sun", R.drawable.ic_sun)
        put("search", R.drawable.ic_search); put("compass", R.drawable.ic_compass); put("layers", R.drawable.ic_layers)
        put("grid", R.drawable.ic_grid); put("user", R.drawable.ic_user); put("play", R.drawable.ic_play)
        put("refresh", R.drawable.ic_refresh); put("pen", R.drawable.ic_pen); put("book", R.drawable.ic_book)
        put("aA", R.drawable.ic_aa); put("folder", R.drawable.ic_folder); put("wrench", R.drawable.ic_wrench)
        put("chart", R.drawable.ic_chart); put("share", R.drawable.ic_share); put("chev", R.drawable.ic_chev)
        put("star", R.drawable.ic_star); put("starFill", R.drawable.ic_starfill); put("monitor", R.drawable.ic_monitor)
        put("trash", R.drawable.ic_trash); put("calendar", R.drawable.ic_calendar); put("settings", R.drawable.ic_settings)
        put("close", R.drawable.ic_close); put("image", R.drawable.ic_image); put("info", R.drawable.ic_info)
        put("importCourse", R.drawable.ic_importcourse); put("resetArrow", R.drawable.ic_resetarrow)
        put("chevDown", R.drawable.ic_chevdown); put("checkTick", R.drawable.ic_checktick); put("check", R.drawable.ic_check)
        put("route", R.drawable.ic_route); put("queue", R.drawable.ic_queue); put("more", R.drawable.ic_more)
        put("highlighter", R.drawable.ic_highlighter); put("database", R.drawable.ic_database)
        put("bookOpen", R.drawable.ic_bookopen); put("copy", R.drawable.ic_copy); put("quote", R.drawable.ic_quote)
        put("sort", R.drawable.ic_sort); put("sparkle", R.drawable.ic_sparkle); put("leaf", R.drawable.ic_leaf)
        put("orbit", R.drawable.ic_orbit); put("orbitFill", R.drawable.ic_orbitfill); put("userFill", R.drawable.ic_userfill)
        put("speaker", R.drawable.ic_speaker); put("upload", R.drawable.ic_upload); put("download", R.drawable.ic_download)
        put("filter", R.drawable.ic_filter); put("locate", R.drawable.ic_locate); put("note", R.drawable.ic_note)
        put("tune", R.drawable.ic_tune); put("pause", R.drawable.ic_pause); put("json", R.drawable.ic_json)
        put("deco", R.drawable.ic_deco); put("crop", R.drawable.ic_crop); put("rotateCw", R.drawable.ic_rotatecw)
        put("zoomIn", R.drawable.ic_zoomin); put("zoomOut", R.drawable.ic_zoomout); put("clock", R.drawable.ic_clock)
        put("dot", R.drawable.ic_dot); put("plus", R.drawable.ic_share); put("bookmark", R.drawable.ic_bookmark)
        put("checklist", R.drawable.ic_checklist)
        put("g_code", R.drawable.ic_g_code); put("g_terminal", R.drawable.ic_g_terminal)
        put("g_cloud", R.drawable.ic_g_cloud); put("g_shield", R.drawable.ic_g_shield)
        put("g_cpu", R.drawable.ic_g_cpu); put("g_globe", R.drawable.ic_g_globe)
        put("g_bulb", R.drawable.ic_g_bulb); put("g_flask", R.drawable.ic_g_flask)
        put("g_music", R.drawable.ic_g_music); put("g_video", R.drawable.ic_g_video)
        put("g_camera", R.drawable.ic_g_camera); put("g_rocket", R.drawable.ic_g_rocket)
        put("g_heart", R.drawable.ic_g_heart); put("g_bolt", R.drawable.ic_g_bolt)
        put("g_target", R.drawable.ic_g_target); put("g_puzzle", R.drawable.ic_g_puzzle)
        put("g_server", R.drawable.ic_g_server); put("g_wifi", R.drawable.ic_g_wifi)
        put("g_mic", R.drawable.ic_g_mic); put("g_headset", R.drawable.ic_g_headset)
        put("g_clock", R.drawable.ic_g_clock); put("g_mail", R.drawable.ic_g_mail)
    }

    private fun put(k: String, v: Int) { map[k] = v }

    /** 课程 / 入口图标选择器用的图标池（与 Web COURSE_GLYPHS / ENTRY_GLYPHS 对齐） */
    val COURSE_GLYPHS = listOf(
        "monitor", "book", "g_code", "g_terminal", "g_server", "g_cpu", "g_wifi", "g_shield", "g_cloud",
        "g_globe", "g_bulb", "g_flask", "g_rocket", "g_target", "g_puzzle", "g_bolt", "g_heart", "star",
        "g_video", "g_music", "g_mic", "g_headset", "g_camera", "folder", "wrench", "chart", "layers", "compass",
    )

    val ENTRY_GLYPHS = listOf(
        "layers", "book", "highlighter", "calendar", "play", "refresh", "aA", "folder", "wrench", "chart",
        "share", "settings", "database", "compass", "star", "g_bolt", "g_heart", "g_rocket", "g_target",
        "g_bulb", "g_flask", "g_code", "g_terminal", "g_globe", "g_cloud", "g_music", "g_video", "g_mic",
        "g_headset", "g_camera", "g_puzzle", "g_shield", "g_cpu", "g_server", "g_wifi", "g_clock",
    )

    fun get(ctx: Context, name: String?, tint: Int? = null): Drawable? {
        val id = map[name] ?: map["layers"] ?: return null
        val d = ContextCompat.getDrawable(ctx, id)?.mutate() ?: return null
        if (tint != null) d.setTint(tint)
        return d
    }

    fun resId(name: String?): Int = map[name] ?: map["layers"] ?: 0

    /** 便捷：直接拿到带 tint 的 ImageView */
    fun iconView(ctx: android.content.Context, name: String?, tint: Int, sizeDp: Float = 20f): android.widget.ImageView =
        com.qwen.opsmentor.core.iconView(ctx, name, tint, sizeDp)
}
