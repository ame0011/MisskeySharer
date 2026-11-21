package com.amesingularity.misskeysharer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amesingularity.misskeysharer.ui.AboutScreen
import com.amesingularity.misskeysharer.ui.AppTheme
import com.amesingularity.misskeysharer.ui.MainScreen

/** 設定・認証を行うメイン画面 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // システムバーを透明にし、ライト/ダークテーマに応じてアイコン色を自動調整する
        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainApp()
            }
        }
    }
}

/** Main(設定) と About の遷移を管理する NavHost */
@Composable
private fun MainApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("main") {
            MainScreen(onAbout = {
                navController.navigate("about") {
                    // ボタン2連続タップなどでAbout画面が複数積み重なるの(多重遷移)を防ぐ
                    launchSingleTop = true
                }
            })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
