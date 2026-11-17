package com.amesingularity.misskeysharer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * MiAuthのリダイレクト (misskeysharer://miauth?session=...&instance=...)
 * を受け取り、トークンを取得して保存する透過アクティビティ。
 */
class MiAuthCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UIは表示しないが、他Activityと同じくedge-to-edge設定を適用する
        enableEdgeToEdge()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        val session = data?.getQueryParameter("session")
        val instanceFromUrl = data?.getQueryParameter("instance")
        // pending（本アプリが生成したセッションに紐づくinstance）を優先し、
        // 改変されたディープリンクのinstanceパラメータに過信しない
        val pending = Prefs.pendingAuth(this)
        val instance = Prefs.normalizeInstanceUrl(
            pending?.first ?: instanceFromUrl.orEmpty()
        )

        if (session.isNullOrEmpty() || instance.isEmpty()) {
            Toast.makeText(this, "認証情報を読み取れませんでした", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val result = MisskeyClient.checkMiAuth(instance, session)
                Prefs.saveAccount(
                    this@MiAuthCallbackActivity,
                    instance,
                    result.token,
                    result.username
                )
                Toast.makeText(this@MiAuthCallbackActivity, "ログインしました", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // 失敗時も設定画面を開き、pendingが残っていれば「認証状況を確認」で再試行できる
                Toast.makeText(
                    this@MiAuthCallbackActivity,
                    "認証を確認できませんでした: ${e.message ?: "エラー"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                startActivity(
                    Intent(this@MiAuthCallbackActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                finish()
            }
        }
    }
}
