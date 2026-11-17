package com.amesingularity.misskeysharer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                MainScreen()
            }
        }
    }
}
