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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amesingularity.misskeysharer.MisskeyClient
import com.amesingularity.misskeysharer.NoSuchSessionException
import com.amesingularity.misskeysharer.Prefs
import com.amesingularity.misskeysharer.R
import com.amesingularity.misskeysharer.errorDetailForDisplay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onAbout: () -> Unit = {}) {
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
    var pureBlack by rememberSaveable { mutableStateOf(Prefs.pureBlackMode(context)) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 通知・エラー文言は合成中に取得しておく（locale 変更時に再計算され、LocaleContext の警告を回避）
    val toastInstanceUrlRequired = stringResource(R.string.toast_instance_url_required)
    val toastBrowserNotFound = stringResource(R.string.toast_browser_not_found)
    val toastLoggedIn = stringResource(R.string.toast_logged_in)
    val errorAuthCheckFormat = stringResource(R.string.error_auth_check)

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun startAuth() {
        val instance = Prefs.normalizeInstanceUrl(instanceInput)
        if (instance.isEmpty()) {
            toast(toastInstanceUrlRequired)
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
            toast(toastBrowserNotFound)
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
                toast(toastLoggedIn)
            } catch (e: Exception) {
                // 存在しないセッションは再確認しても成功しないので pending をクリアしておく
                if (e is NoSuchSessionException) Prefs.clearPendingAuth(context)
                toast(String.format(errorAuthCheckFormat, errorDetailForDisplay(context, e)))
            } finally {
                checking = false
                pending = Prefs.pendingAuth(context)
            }
        }
    }

    val visibilityOptions = listOf(
        stringResource(R.string.visibility_public) to Prefs.VISIBILITY_PUBLIC,
        stringResource(R.string.visibility_home) to Prefs.VISIBILITY_HOME,
        stringResource(R.string.visibility_followers) to Prefs.VISIBILITY_FOLLOWERS
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
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
                        stringResource(R.string.account_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val acc = account
                    if (acc == null) {
                        Text(
                            stringResource(R.string.account_not_connected),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = instanceInput,
                            onValueChange = { instanceInput = it },
                            label = { Text(stringResource(R.string.instance_url_label)) },
                            placeholder = { Text(stringResource(R.string.instance_url_placeholder)) },
                            singleLine = true,
                            enabled = !checking,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { startAuth() },
                            enabled = !checking && instanceInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.auth_button))
                        }

                        if (pending != null) {
                            Text(
                                stringResource(R.string.auth_pending_hint),
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
                                Text(stringResource(R.string.check_auth_button))
                            }
                            TextButton(
                                onClick = {
                                    Prefs.clearPendingAuth(context)
                                    pending = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.cancel_auth_button))
                            }
                        }
                    } else {
                        Text(
                            "@${acc.username ?: "?"}@${acc.instance.removePrefix("https://").removePrefix("http://")}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.account_connected),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.unlink_button))
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
                        stringResource(R.string.defaults_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(stringResource(R.string.visibility_label), style = MaterialTheme.typography.bodyMedium)
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
                            Text(stringResource(R.string.local_only_label), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.local_only_sub),
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
                        stringResource(R.string.howto_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.howto_step1),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.howto_step2),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.howto_step3),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ================= 外観 =================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.appearance_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.pure_black_label), style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = pureBlack,
                            onCheckedChange = {
                                pureBlack = it
                                Prefs.setPureBlackMode(context, it)
                            }
                        )
                    }
                }
            }

            TextButton(
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.about_button))
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.unlink_dialog_title)) },
            text = { Text(stringResource(R.string.unlink_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        Prefs.clearAccount(context)
                        account = null
                        showClearConfirm = false
                    }
                ) {
                    Text(
                        stringResource(R.string.unlink_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
