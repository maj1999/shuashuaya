package com.cleanpic.ui.result

/**
 * 结果页的两个语义阶段。
 *
 * 修复前结果页一进入就显示"本轮清理完成"，即使尚未删除任何文件，造成"已经清理完"的误导。
 * 现按是否真正删除成功区分两态，删除前明确处于待确认状态。
 */
enum class ResultPhase {
    /** 待确认态：有待删除项且尚未删除成功，标题"即将删除 N 项"，展示确认按钮 */
    CONFIRM,

    /** 完成态：删除成功后、或本轮无待删除项，标题"本轮清理完成" */
    DONE
}

/**
 * 由待删除数量与删除是否成功，派生结果页当前阶段。
 *
 * - 有待删除项且未确认成功 → CONFIRM（删除前的待确认态）
 * - 否则（删除成功 / 本来就无待删除项）→ DONE
 */
fun resolveResultPhase(pendingCount: Int, deleteConfirmed: Boolean): ResultPhase =
    if (pendingCount > 0 && !deleteConfirmed) ResultPhase.CONFIRM else ResultPhase.DONE
