package com.cleanpic.viewmodel

import com.cleanpic.di.ServiceLocator
import com.cleanpic.mock.*
import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import com.cleanpic.model.OperationState
import com.cleanpic.stats.InMemoryStatsStore
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

    // ===== US-CP-22/23 随机算法增强（持久化浏览记忆）=====

    // I-VM-01 连续三轮互不重复（洗牌袋）
    @Test
    fun ivm_01_three_rounds_no_overlap() = runTest {
        vm.loadMedia(MediaType.PHOTO); val r1 = vm.items.value.map { it.media.id }.toSet()
        vm.loadMedia(MediaType.PHOTO); val r2 = vm.items.value.map { it.media.id }.toSet()
        vm.loadMedia(MediaType.PHOTO); val r3 = vm.items.value.map { it.media.id }.toSet()
        assertEquals(30, (r1 + r2 + r3).size)  // 30 张全覆盖且互不重复
    }

    // I-VM-02 跨重启记忆（同一 store，重建 VM）
    @Test
    fun ivm_02_persists_across_restart() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        val firstRound = vm.items.value.map { it.media.id }.toSet()
        val restarted = ViewerViewModel()   // 共用 ServiceLocator.pickStateStore，模拟重启
        restarted.loadMedia(MediaType.PHOTO)
        val secondRound = restarted.items.value.map { it.media.id }.toSet()
        assertTrue(firstRound.intersect(secondRound).isEmpty())
    }

    // I-VM-03 回首页不丢记忆（不再 clearSession）→ 第二轮不重复第一轮
    @Test
    fun ivm_03_go_home_keeps_memory() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        val first = vm.items.value.map { it.media.id }.toSet()
        // 模拟回首页（不调用 clearSession）后再开始
        vm.loadMedia(MediaType.PHOTO)
        val second = vm.items.value.map { it.media.id }.toSet()
        assertTrue(first.intersect(second).isEmpty())
    }

    // I-VM-04 已保留过的沉底
    @Test
    fun ivm_04_kept_items_sink() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        val kept = vm.items.value.map { it.media.id }.toSet()
        repeat(10) { vm.markKept() }
        vm.loadMedia(MediaType.PHOTO); val r2 = vm.items.value.map { it.media.id }.toSet()
        vm.loadMedia(MediaType.PHOTO); val r3 = vm.items.value.map { it.media.id }.toSet()
        // 30 张里 20 张非保留，两轮刚好用完；保留的 10 张不应出现
        assertTrue((r2 + r3).intersect(kept).isEmpty())
    }

    // I-VM-05 删除后从记忆移除（自愈）
    @Test
    fun ivm_05_confirm_delete_forgets_record() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        vm.markDelete()
        val deletedId = vm.items.value[0].media.id
        repeat(9) { vm.markKept() }
        val result = vm.confirmDelete()
        assertTrue(result.isSuccess)
        val records = ServiceLocator.pickStateStore.load(MediaType.PHOTO).records
        assertFalse(records.containsKey(deletedId))
    }

    // I-VM-06 重置浏览记录后清空
    @Test
    fun ivm_06_clear_session_resets_store() = runTest {
        vm.loadMedia(MediaType.PHOTO)
        repeat(10) { vm.markKept() }
        assertTrue(ServiceLocator.pickStateStore.load(MediaType.PHOTO).records.isNotEmpty())
        vm.clearSession()
        assertTrue(ServiceLocator.pickStateStore.load(MediaType.PHOTO).records.isEmpty())
        vm.loadMedia(MediaType.PHOTO)
        assertEquals(10, vm.items.value.size)
    }

    // ===== Task 9: 清理统计埋点 =====

    @Test
    fun confirm_delete_records_amount_into_stats() = runTest {
        val stats = InMemoryStatsStore()
        val photo1 = MediaItem(
            id = "p1", type = MediaType.PHOTO, name = "a.jpg",
            size = 100L, date = 1700000000000L, width = 100, height = 100
        )
        val photo2 = MediaItem(
            id = "p2", type = MediaType.PHOTO, name = "b.jpg",
            size = 200L, date = 1700000000000L, width = 100, height = 100
        )
        val repo = MockMediaRepository(photos = listOf(photo1, photo2))
        ServiceLocator.initialize(
            mediaRepo = repo,
            settings = MockAppSettings(),
            permission = MockPermissionManager(),
            player = MockVideoPlayer(),
            statsStore = stats
        )
        val localVm = ViewerViewModel()
        localVm.loadMedia(MediaType.PHOTO)
        // 两张都标记删除
        localVm.markDelete()
        localVm.markDelete()
        localVm.confirmDelete()
        val lifetime = stats.load().lifetime
        assertEquals(2, lifetime.photo.count)
        assertEquals(300L, lifetime.photo.bytes)
    }

    @Test
    fun record_round_reached_is_idempotent_per_round() = runTest {
        val stats = InMemoryStatsStore()
        val repo = MockMediaRepository(photos = TestMediaFactory.photos(10))
        ServiceLocator.initialize(
            mediaRepo = repo,
            settings = MockAppSettings(),
            permission = MockPermissionManager(),
            player = MockVideoPlayer(),
            statsStore = stats
        )
        val localVm = ViewerViewModel()
        localVm.loadMedia(MediaType.PHOTO)
        // 同一轮调用两次，只应记一次
        localVm.recordRoundReached()
        localVm.recordRoundReached()
        assertEquals(1, stats.load().lifetime.photo.rounds)
    }
}
