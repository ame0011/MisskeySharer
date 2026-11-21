package com.amesingularity.misskeysharer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
                var showAbout by rememberSaveable { mutableStateOf(false) }
                if (showAbout) {
                    AboutScreen(onBack = { showAbout = false })
                } else {
                    MainScreen(onAbout = { showAbout = true })
                }
            }
        }
    }
}
