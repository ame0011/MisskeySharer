package com.amesingularity.misskeysharer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.amesingularity.misskeysharer.ui.AppTheme
import com.amesingularity.misskeysharer.ui.ShareScreen

/**
 * 他アプリからの共有 (ACTION_SEND, text/plain) を受け取り、
 * 確認画面を経由してMisskeyに投稿するアクティビティ。
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textExtra = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val subjectExtra = intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        // ブラウザ共有では「タイトルがSUBJECT、URLがTEXT」に入ることが多いため結合する
        val sharedText = when {
            !textExtra.isNullOrBlank() && !subjectExtra.isNullOrBlank() &&
                !textExtra.contains(subjectExtra) -> subjectExtra + "\n" + textExtra

            !textExtra.isNullOrBlank() -> textExtra
            !subjectExtra.isNullOrBlank() -> subjectExtra
            else -> ""
        }

        setContent {
            AppTheme {
                ShareScreen(initialText = sharedText, onFinish = { finish() })
            }
        }
    }
}
