package com.cleanpic.ui.navigation

import androidx.compose.runtime.*
import com.cleanpic.model.MediaType

/**
 * Simple state-based navigation for KuiklyUI Compose.
 * Replaces NavHostController which is not available in KuiklyUI.
 */
sealed class Route {
    data object Splash : Route()
    data object Home : Route()
    data class Viewer(val type: MediaType) : Route()
    data object Result : Route()
    data object Settings : Route()
    data object Stats : Route()
}

class AppRouter {
    var currentRoute: Route by mutableStateOf(Route.Splash)
        private set

    private val backStack = mutableListOf<Route>()

    fun navigate(route: Route, clearBackStackUpTo: Route? = null, inclusive: Boolean = false) {
        if (clearBackStackUpTo != null) {
            val index = backStack.indexOfLast { it::class == clearBackStackUpTo::class }
            if (index >= 0) {
                val removeFrom = if (inclusive) index else index + 1
                if (removeFrom < backStack.size) {
                    backStack.subList(removeFrom, backStack.size).clear()
                }
            }
        } else {
            backStack.add(currentRoute)
        }
        currentRoute = route
    }

    fun popBackStack(): Boolean {
        if (backStack.isEmpty()) return false
        currentRoute = backStack.removeLast()
        return true
    }
}

/**
 * App 内是否应拦截系统返回键/手势：
 * 首页与启动页放行给系统（→ 退出 App），其余界面逐级返回上一级。
 */
fun Route.consumesBack(): Boolean = this !is Route.Home && this !is Route.Splash

@Composable
fun rememberAppRouter(): AppRouter {
    return remember { AppRouter() }
}
