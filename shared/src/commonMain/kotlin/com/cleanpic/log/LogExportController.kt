package com.cleanpic.log

/**
 * 导出触发的平台无关入口。Android MainActivity 注册 onRequestExport（弹 SAF 保存框）；
 * 设置页「导出诊断日志」按钮调用 export()。iOS/Harmony 未注册时 export() 为 no-op。
 */
object LogExportController {
    var onRequestExport: (() -> Unit)? = null
    fun export() { onRequestExport?.invoke() }
}
