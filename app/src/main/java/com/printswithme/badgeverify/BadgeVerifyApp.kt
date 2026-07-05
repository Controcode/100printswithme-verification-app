package com.printswithme.badgeverify

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.printswithme.badgeverify.data.api.ApiClient
import com.printswithme.badgeverify.data.storage.HistoryStorage
import com.printswithme.badgeverify.data.storage.SecretsStorage
import com.printswithme.badgeverify.ui.screens.HistoryScreen
import com.printswithme.badgeverify.ui.screens.HomeScreen
import com.printswithme.badgeverify.ui.screens.ResultScreen
import com.printswithme.badgeverify.ui.screens.ScannerScreen
import com.printswithme.badgeverify.ui.screens.SecretsScreen

object Routes {
    const val HOME = "home"
    const val SCANNER = "scanner?mode={mode}"
    const val RESULT = "result/{id}?error={error}&needsSecret={needsSecret}"
    const val HISTORY = "history"
    const val SECRETS = "secrets"

    fun scanner(mode: String = "badge") = "scanner?mode=$mode"
    fun result(id: String) = "result/$id"
    fun resultError(id: String, error: String, needsSecret: Boolean) =
        "result/$id?error=${java.net.URLEncoder.encode(error, "UTF-8")}&needsSecret=$needsSecret"
}

@Composable
fun BadgeVerifyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val historyStorage = HistoryStorage(context)
    val secretsStorage = SecretsStorage(context)
    val api = ApiClient.api

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                historyStorage = historyStorage,
                secretsStorage = secretsStorage,
                api = api
            )
        }

        composable(
            route = Routes.SCANNER,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "badge"
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "badge"
            ScannerScreen(
                navController = navController,
                mode = mode,
                historyStorage = historyStorage,
                secretsStorage = secretsStorage,
                api = api
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("error") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("needsSecret") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val error = backStackEntry.arguments?.getString("error")
            val needsSecret = backStackEntry.arguments?.getBoolean("needsSecret") ?: false
            ResultScreen(
                navController = navController,
                verificationId = id,
                initialError = error,
                initialNeedsSecret = needsSecret,
                historyStorage = historyStorage,
                secretsStorage = secretsStorage,
                api = api
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                navController = navController,
                historyStorage = historyStorage
            )
        }

        composable(Routes.SECRETS) {
            SecretsScreen(
                navController = navController,
                secretsStorage = secretsStorage
            )
        }
    }
}
