package com.cleanpic.ui.navigation

import com.cleanpic.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 系统返回键导航：非首页逐级返回、首页放行退出（[consumesBack] + [AppRouter.popBackStack]）。
 */
class AppRouterTest {

    @Test
    fun home_and_splash_let_system_handle_back() {
        // 首页/启动页不拦截返回 → 交系统处理（退出 App）
        assertFalse(Route.Home.consumesBack())
        assertFalse(Route.Splash.consumesBack())
    }

    @Test
    fun other_screens_consume_back() {
        assertTrue(Route.Settings.consumesBack())
        assertTrue(Route.Stats.consumesBack())
        assertTrue(Route.Result.consumesBack())
        assertTrue(Route.Viewer(MediaType.PHOTO).consumesBack())
    }

    @Test
    fun back_from_settings_returns_to_home() {
        // Splash → Home → Settings，返回应回到 Home
        val router = AppRouter()
        router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
        router.navigate(Route.Settings)
        assertEquals(Route.Settings, router.currentRoute)

        assertTrue(router.popBackStack())
        assertEquals(Route.Home, router.currentRoute)
    }

    @Test
    fun back_from_viewer_returns_to_home() {
        val router = AppRouter()
        router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
        router.navigate(Route.Viewer(MediaType.PHOTO))
        assertEquals(Route.Viewer(MediaType.PHOTO), router.currentRoute)

        assertTrue(router.popBackStack())
        assertEquals(Route.Home, router.currentRoute)
    }

    @Test
    fun back_from_result_returns_to_home() {
        // Home → Viewer →(完成)→ Result：Viewer 跳 Result 时 inclusive 清掉自身，
        // 返回栈仅剩 Home，返回应回到 Home。
        val router = AppRouter()
        router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
        router.navigate(Route.Viewer(MediaType.PHOTO))
        router.navigate(Route.Result, clearBackStackUpTo = Route.Viewer(MediaType.PHOTO), inclusive = true)
        assertEquals(Route.Result, router.currentRoute)

        assertTrue(router.popBackStack())
        assertEquals(Route.Home, router.currentRoute)
    }

    @Test
    fun pop_on_empty_backstack_returns_false() {
        // 空返回栈（如刚到首页）popBackStack 返回 false，调用方据此放行给系统
        val router = AppRouter()
        router.navigate(Route.Home, clearBackStackUpTo = Route.Splash, inclusive = true)
        assertFalse(router.popBackStack())
    }
}
