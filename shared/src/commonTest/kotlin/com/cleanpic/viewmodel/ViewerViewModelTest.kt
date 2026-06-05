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
    fun undo_06_single_step_redecide_then_undo_again() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markKept()                   // index→1
        vm.undo()                       // 回到 0，canUndo=false
        assertFalse(vm.canUndo.value)
        vm.markDelete()                 // 重新决策 item0，index→1
        assertTrue(vm.canUndo.value)
        vm.undo()                       // 再次可撤回到 0
        assertEquals(0, vm.currentIndex.value)
        assertEquals(OperationState.PENDING, vm.items.value[0].state)
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
}
