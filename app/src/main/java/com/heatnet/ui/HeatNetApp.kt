package com.heatnet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.ui.screens.HomeScreen
import com.heatnet.ui.screens.MappingScreen
import com.heatnet.ui.screens.NewSessionScreen
import com.heatnet.ui.screens.OutlineScreen
import com.heatnet.ui.screens.SessionListScreen
import com.heatnet.ui.screens.SummaryScreen

/**
 * Route names. Member 3's compare screen can be added to [HeatNetApp]'s NavHost, for example as
 * "compare/{before}/{after}", reached from the session list.
 */
object Routes {
    const val HOME = "home"
    const val NEW_SESSION = "new"
    const val OUTLINE = "outline/{type}"
    const val MAP = "map/{sessionId}"
    const val SUMMARY = "summary/{sessionId}"
    const val SESSIONS = "sessions"

    fun outline(type: ConnectionType) = "outline/${type.name}"
    fun map(sessionId: Long) = "map/$sessionId"
    fun summary(sessionId: Long) = "summary/$sessionId"
}

@Composable
fun HeatNetApp(container: AppContainer) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colors) {
        val nav = rememberNavController()
        NavHost(nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onNewSession = { nav.navigate(Routes.NEW_SESSION) },
                    onSavedSessions = { nav.navigate(Routes.SESSIONS) },
                )
            }
            composable(Routes.NEW_SESSION) {
                NewSessionScreen(
                    container = container,
                    onBack = { nav.popBackStack() },
                    onContinue = { type -> nav.navigate(Routes.outline(type)) },
                )
            }
            composable(Routes.OUTLINE, listOf(navArgument("type") { type = NavType.StringType })) { entry ->
                val type = ConnectionType.valueOf(entry.arguments?.getString("type") ?: ConnectionType.WIFI.name)
                OutlineScreen(
                    container = container,
                    connectionType = type,
                    onBack = { nav.popBackStack() },
                    onSessionCreated = { id ->
                        nav.navigate(Routes.map(id)) { popUpTo(Routes.HOME) }
                    },
                )
            }
            composable(Routes.MAP, listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("sessionId") ?: return@composable
                MappingScreen(
                    container = container,
                    sessionId = id,
                    onBack = { nav.popBackStack() },
                    onFinish = { nav.navigate(Routes.summary(id)) },
                )
            }
            composable(Routes.SUMMARY, listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("sessionId") ?: return@composable
                SummaryScreen(
                    container = container,
                    sessionId = id,
                    onBackToMap = { nav.popBackStack() },
                    onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                )
            }
            composable(Routes.SESSIONS) {
                SessionListScreen(
                    container = container,
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate(Routes.map(id)) },
                )
            }
        }
    }
}
