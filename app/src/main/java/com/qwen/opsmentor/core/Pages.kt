package com.qwen.opsmentor.core

import com.qwen.opsmentor.App
import com.qwen.opsmentor.state.Mark

/**
 * 页面定位服务：MainActivity 注册当前页面的划线块与定位函数，
 * 供浮层微调与「定位到原文」使用（对应 Web 的 blocksBySig / segBlock）。
 */
object Pages {
    fun currentBlocks(): MutableList<AnnotatedText> = App.inst.blocks
    var locator: ((Mark) -> Boolean)? = null
    fun locate(m: Mark): Boolean = locator?.invoke(m) ?: false
}
