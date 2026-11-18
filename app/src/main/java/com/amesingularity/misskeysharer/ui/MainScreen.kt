package com.amesingularity.misskeysharer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amesingularity.misskeysharer.MisskeyClient
import com.amesingularity.misskeysharer.Prefs
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf(Prefs.account(context)) }
    var pending by remember { mutableStateOf(Prefs.pendingAuth(context)) }
    var instanceInput by rememberSaveable {
        mutableStateOf(account?.instance ?: "https://misskey.io")
    }
    var checking by remember { mutableStateOf(false) }
    var visibility by rememberSaveable { mutableStateOf(Prefs.defaultVisibility(context)) }
    var localOnly by rememberSaveable { mutableStateOf(Prefs.defaultLocalOnly(context)) }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun startAuth() {
        val instance = Prefs.normalizeInstanceUrl(instanceInput)
        if (instance.isEmpty()) {
            toast("インスタンスのURLを入力してください")
            return
        }
        val session = UUID.randomUUID().toString()
        Prefs.setPendingAuth(context, instance, session)
        pending = Prefs.pendingAuth(context)
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, MisskeyClient.miAuthUrl(instance, session).toUri())
            )
        } catch (e: ActivityNotFoundException) {
            Prefs.clearPendingAuth(context)
            pending = null
            toast("ブラウザが見つかりませんでした")
        }
    }

    fun checkAuth() {
        val p = pending ?: return
        checking = true
        scope.launch {
            try {
                val result = MisskeyClient.checkMiAuth(p.first, p.second)
                Prefs.saveAccount(context, p.first, result.token, result.username)
                account = Prefs.account(context)
                toast("ログインしました")
            } catch (e: Exception) {
                toast("認証を確認できませんでした: ${e.message ?: "エラー"}")
            } finally {
                checking = false
                pending = Prefs.pendingAuth(context)
            }
        }
    }

    val visibilityOptions = listOf(
        "公開" to Prefs.VISIBILITY_PUBLIC,
        "ホーム" to Prefs.VISIBILITY_HOME,
        "フォロワー" to Prefs.VISIBILITY_FOLLOWERS
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Misskey Sharer 設定") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // edge-to-edgeではIMEがコンテンツの後ろに重なるため、スクロール前にIME分 padding を適用する
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= アカウント =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "アカウント",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val acc = account
                    if (acc == null) {
                        Text(
                            "共有で投稿するには、まずMisskeyアカウントを連携してください。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = instanceInput,
                            onValueChange = { instanceInput = it },
                            label = { Text("インスタンスのURL") },
                            placeholder = { Text("https://misskey.io") },
                            singleLine = true,
                            enabled = !checking,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { startAuth() },
                            enabled = !checking && instanceInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ブラウザで認証する")
                        }

                        if (pending != null) {
                            Text(
                                "ブラウザで「許可する」を押すと自動的にこの画面に戻ります。\n" +
                                    "戻らない場合は下のボタンで認証状況を確認できます。",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = { checkAuth() },
                                enabled = !checking,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (checking) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text("認証状況を確認")
                            }
                            TextButton(
                                onClick = {
                                    Prefs.clearPendingAuth(context)
                                    pending = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("認証をやめる")
                            }
                        }
                    } else {
                        Text(
                            "@${acc.username ?: "?"}@${acc.instance.removePrefix("https://").removePrefix("http://")}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "連携済みです。他のアプリから共有すると投稿できます。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("連携を解除")
                        }
                    }
                }
            }

            // ================= 投稿のデフォルト設定 =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "投稿のデフォルト設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text("公開範囲", style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        visibilityOptions.forEachIndexed { index, (label, value) ->
                            SegmentedButton(
                                selected = visibility == value,
                                onClick = {
                                    visibility = value
                                    Prefs.setDefaultVisibility(context, value)
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = visibilityOptions.size
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ローカルのみ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "他のインスタンスに連合させない",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = localOnly,
                            onCheckedChange = {
                                localOnly = it
                                Prefs.setDefaultLocalOnly(context, it)
                            }
                        )
                    }
                }
            }

            // ================= 使い方 =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "使い方",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "1. 「ブラウザで認証する」からMisskeyにログインし、「許可する」を押します。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "2. 他のアプリで文字列を選んで「共有」→「Misskeyに投稿」を選びます。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "3. 確認画面で内容と公開範囲を確かめて「投稿する」を押します。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("連携を解除しますか？") },
            text = { Text("保存されているアカウント連携情報を削除します。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        Prefs.clearAccount(context)
                        account = null
                        showClearConfirm = false
                    }
                ) {
                    Text(
                        "解除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
