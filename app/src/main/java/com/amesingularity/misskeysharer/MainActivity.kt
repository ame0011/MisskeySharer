package com.amesingularity.misskeysharer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.amesingularity.misskeysharer.ui.AppTheme
import com.amesingularity.misskeysharer.ui.MainScreen

/** 設定・認証を行うメイン画面 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
