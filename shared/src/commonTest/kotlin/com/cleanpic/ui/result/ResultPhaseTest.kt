package com.cleanpic.ui.result

import kotlin.test.Test
import kotlin.test.assertEquals

class ResultPhaseTest {

    // U-PHASE-01：有待删除项且尚未删除成功 → 待确认态
    @Test fun confirm_when_pending_and_not_confirmed() {
        assertEquals(ResultPhase.CONFIRM, resolveResultPhase(pendingCount = 4, deleteConfirmed = false))
    }

    // U-PHASE-02：删除成功后 → 完成态
    @Test fun done_after_delete_confirmed() {
        assertEquals(ResultPhase.DONE, resolveResultPhase(pendingCount = 4, deleteConfirmed = true))
    }

    // U-PHASE-03：无待删除项（全部保留）→ 直接完成态
    @Test fun done_when_no_pending() {
        assertEquals(ResultPhase.DONE, resolveResultPhase(pendingCount = 0, deleteConfirmed = false))
    }
}
