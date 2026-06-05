package com.cleanpic.viewmodel

import com.cleanpic.di.ServiceLocator
import com.cleanpic.mock.*
import com.cleanpic.model.MediaType
import com.cleanpic.model.OperationState
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ViewerViewModelTest {

    private lateinit var vm: ViewerViewModel
    private lateinit var mockRepo: MockMediaRepository

    @BeforeTest
    fun setup() {
        val photos = TestMediaFactory.photos(30)
        val videos = TestMediaFactory.videos(20)
        mockRepo = MockMediaRepository(photos, videos)
        ServiceLocator.initialize(
            mediaRepo = mockRepo,
            settings = MockAppSettings(),
            permission = MockPermissionManager(),
            player = MockVideoPlayer()
        )
        vm = ViewerViewModel()
    }

    @Test
    fun loadMedia_photos_loads_correct_count() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        assertEquals(10, vm.items.value.size)
        assertEquals(0, vm.currentIndex.value)
        assertFalse(vm.isComplete)
    }

    @Test
    fun loadMedia_videos_loads_correct_count() = runTest {
        vm.loadMedia(MediaType.VIDEO)
        assertEquals(10, vm.items.value.size)
    }

    @Test
    fun loadMedia_empty_sets_isEmpty() = runTest {
        val emptyRepo = MockMediaRepository()
        ServiceLocator.initialize(
            emptyRepo, MockAppSettings(), MockPermissionManager(), MockVideoPlayer()
        )
        val emptyVm = ViewerViewModel()
        emptyVm.loadMedia(MediaType.PHOTO)
        assertTrue(emptyVm.isEmpty.value)
    }

    @Test
    fun markKept_updates_state_and_advances() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()
        assertEquals(OperationState.KEPT, vm.items.value[0].state)
        assertEquals(1, vm.currentIndex.value)
    }

    @Test
    fun markDelete_updates_state_and_advances() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[0].state)
        assertEquals(1, vm.currentIndex.value)
    }

    @Test
    fun isComplete_after_all_operations() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        repeat(10) { vm.markKept() }
        assertTrue(vm.isComplete)
    }

    @Test
    fun stats_are_correct() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()  // item 0
        vm.markDelete()  // item 1
        vm.markKept()    // item 2
        vm.markDelete()  // item 3
        assertEquals(3, vm.deletedCount)
        assertEquals(1, vm.keptCount)
        assertEquals(3, vm.pendingDeletes.size)
    }

    @Test
    fun cancelDelete_reverts_to_kept() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        val deletedId = vm.items.value[0].media.id
        vm.cancelDelete(deletedId)
        assertEquals(OperationState.KEPT, vm.items.value[0].state)
        assertEquals(0, vm.deletedCount)
    }

    @Test
    fun confirmDelete_calls_repo() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        vm.markDelete()
        val result = vm.confirmDelete()
        assertTrue(result.isSuccess)
        assertEquals(2, mockRepo.deletedItems.size)
    }

    @Test
    fun confirmDelete_passes_full_mediaItems() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()  // item 0
        vm.markKept()    // item 1
        vm.markDelete()  // item 2
        val result = vm.confirmDelete()
        assertTrue(result.isSuccess)
        assertEquals(2, mockRepo.deletedItems.size)
        assertTrue(mockRepo.deletedItems.all { it.type == MediaType.PHOTO })
        assertEquals(vm.pendingDeletes.map { it.media.id }.toSet(),
            mockRepo.deletedItems.map { it.id }.toSet())
    }

    @Test
    fun confirmDelete_video_passes_correct_type() = runTest {
        vm.loadMedia(MediaType.VIDEO)
        vm.markDelete()
        val result = vm.confirmDelete()
        assertTrue(result.isSuccess)
        assertEquals(1, mockRepo.deletedItems.size)
        assertEquals(MediaType.VIDEO, mockRepo.deletedItems[0].type)
    }

    @Test
    fun confirmDelete_empty_succeeds() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        repeat(10) { vm.markKept() }
        val result = vm.confirmDelete()
        assertTrue(result.isSuccess)
        assertEquals(0, mockRepo.deletedItems.size)
    }

    @Test
    fun confirmDelete_repo_failure_returns_failure() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        vm.markDelete()
        mockRepo.shouldFail = true
        val result = vm.confirmDelete()
        assertTrue(result.isFailure)
        assertEquals("mock deletion failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun next_round_excludes_shown() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        val firstRoundIds = vm.items.value.map { it.media.id }.toSet()
        repeat(10) { vm.markKept() }

        vm.loadMedia(MediaType.PHOTO)
        val secondRoundIds = vm.items.value.map { it.media.id }.toSet()
        // With 30 photos, second round should not overlap with first
        assertTrue(firstRoundIds.intersect(secondRoundIds).isEmpty())
    }

    @Test
    fun clearSession_resets_shown_ids() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        repeat(10) { vm.markKept() }
        vm.clearSession()
        vm.loadMedia(MediaType.PHOTO)
        // After clear, items from first round can appear again
        assertEquals(10, vm.items.value.size)
    }

    @Test
    fun releasedBytes_calculation() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        vm.markDelete()
        val expected = vm.pendingDeletes.sumOf { it.media.size }
        assertEquals(expected, vm.releasedBytes)
        assertTrue(vm.releasedBytes > 0)
    }

    // ===== US-CP-19 撤销上一步 =====

    @Test
    fun undo_01_canUndo_true_after_decision() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        assertFalse(vm.canUndo.value)   // 未决策不可撤
        vm.markKept()
        assertTrue(vm.canUndo.value)    // 决策后可撤
    }

    @Test
    fun undo_02_reverts_index_and_clears_state() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()                 // item0 → PENDING_DELETE，index→1
        assertEquals(1, vm.currentIndex.value)
        vm.undo()
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
    }

    @Test
    fun undo_03_canUndo_false_after_undo() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()
        vm.undo()
        assertFalse(vm.canUndo.value)
    }

    @Test
    fun undo_04_no_effect_when_no_decision() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        assertFalse(vm.canUndo.value)
        vm.undo()                       // 无决策，应无副作用
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
        assertFalse(vm.canUndo.value)
    }

    @Test
    fun undo_05_loadMedia_resets_undo_state() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()
        assertTrue(vm.canUndo.value)
        vm.loadMedia(MediaType.PHOTO)
        assertFalse(vm.canUndo.value)
    }

    @Test
    fun undo_06_multi_step_back_to_first() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()                   // item0 KEPT, index→1
        vm.markDelete()                 // item1 DELETE, index→2
        vm.markKept()                   // item2 KEPT, index→3
        assertEquals(3, vm.currentIndex.value)
        assertTrue(vm.canUndo.value)

        vm.undo()                       // 回到 item2
        assertEquals(2, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[2].state)
        assertTrue(vm.canUndo.value)    // 仍可继续回退

        vm.undo()                       // 回到 item1
        assertEquals(1, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[1].state)
        assertTrue(vm.canUndo.value)

        vm.undo()                       // 回到 item0（第一个）
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
        assertFalse(vm.canUndo.value)   // 已到第一个，不可再撤

        vm.undo()                       // 越界无副作用
        assertEquals(0, vm.currentIndex.value)
    }

    @Test
    fun undo_08_canUndo_true_until_reaching_first() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        repeat(5) { vm.markKept() }     // index→5
        repeat(5) {
            assertTrue(vm.canUndo.value)  // 每次回退前都应可撤
            vm.undo()
        }
        assertEquals(0, vm.currentIndex.value)
        assertFalse(vm.canUndo.value)
        // 前 5 项全部恢复待决策态
        assertTrue(vm.items.value.take(5).all { it.state == OperationState.PENDING })
    }

    @Test
    fun undo_09_redecide_after_partial_undo() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()                   // item0, index→1
        vm.markKept()                   // item1, index→2
        vm.undo()                       // 回到 item1
        vm.markDelete()                 // 重新决策 item1 为删除, index→2
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[1].state)
        assertEquals(2, vm.currentIndex.value)
        vm.undo()                       // 回到 item1
        vm.undo()                       // 继续回到 item0
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
        assertFalse(vm.canUndo.value)
    }

    @Test
    fun undo_07_clears_pending_delete() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        assertEquals(1, vm.deletedCount)
        vm.undo()
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
        assertEquals(0, vm.deletedCount)
    }

    // ===== US-CP-21 轮播模式左右滑动切换前后媒体 =====

    @Test
    fun nav_01_goNext_pending_defaults_to_kept_and_advances() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        // item0 尚未决策
        vm.goNext()
        assertEquals(OperationState.KEPT, vm.items.value[0].state)  // 默认保留
        assertEquals(1, vm.currentIndex.value)
    }

    @Test
    fun nav_02_goNext_preserves_existing_delete() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()          // item0 → PENDING_DELETE, index→1
        vm.goPrevious()          // 回到 item0
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[0].state)
        vm.goNext()              // 再次离开 item0，删除决策应保持，不被改成保留
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[0].state)
        assertEquals(1, vm.currentIndex.value)
    }

    @Test
    fun nav_03_goNext_preserves_existing_kept() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()            // item0 → KEPT, index→1
        vm.goPrevious()          // 回到 item0
        vm.goNext()              // 再次离开，仍是保留
        assertEquals(OperationState.KEPT, vm.items.value[0].state)
        assertEquals(1, vm.currentIndex.value)
    }

    @Test
    fun nav_04_goPrevious_navigates_back_without_changing_state() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()            // item0 KEPT, index→1
        vm.markDelete()          // item1 DELETE, index→2
        vm.goPrevious()          // 回到 item1
        assertEquals(1, vm.currentIndex.value)
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[1].state)
        assertEquals(OperationState.KEPT, vm.items.value[0].state)
    }

    @Test
    fun nav_05_goPrevious_no_effect_at_first() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.goPrevious()
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
    }

    @Test
    fun nav_06_goNext_past_last_completes_round() = runTest {
        vm.loadMedia(MediaType.PHOTO)   // 10 items
        repeat(9) { vm.goNext() }       // 到达最后一个 index 9
        assertEquals(9, vm.currentIndex.value)
        assertFalse(vm.isComplete)
        vm.goNext()                     // 越过最后一个 → 完成
        assertTrue(vm.isComplete)
    }

    @Test
    fun nav_07_goNext_then_undo_reverts_default_keep() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.goNext()                     // item0 默认保留, index→1
        assertEquals(OperationState.KEPT, vm.items.value[0].state)
        assertTrue(vm.canUndo.value)
        vm.undo()
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
    }

    @Test
    fun nav_08_back_and_forth_preserves_all_decisions() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()                 // item0 DELETE, index→1
        vm.markKept()                   // item1 KEPT, index→2
        vm.goPrevious()                 // index→1
        vm.goPrevious()                 // index→0
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[0].state)
        assertEquals(OperationState.KEPT, vm.items.value[1].state)
        vm.goNext()                     // 离开 item0（已删除，保持）
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[0].state)
        vm.goNext()                     // 离开 item1（已保留，保持）
        assertEquals(OperationState.KEPT, vm.items.value[1].state)
        assertEquals(2, vm.currentIndex.value)
    }

    @Test
    fun nav_09_goPrevious_then_redecide_via_buttons() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.goNext()                     // item0 默认保留, index→1
        vm.goPrevious()                 // 回到 item0 复查
        assertEquals(OperationState.KEPT, vm.items.value[0].state)
        vm.markDelete()                 // 改判为删除
        assertEquals(OperationState.PENDING_DELETE, vm.items.value[0].state)
    }
}
