package com.amesingularity.misskeysharer.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amesingularity.misskeysharer.MainActivity
import com.amesingularity.misskeysharer.MisskeyClient
import com.amesingularity.misskeysharer.Prefs
import com.amesingularity.misskeysharer.R
import com.amesingularity.misskeysharer.errorDetailForDisplay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(initialText: String, onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf(Prefs.account(context)) }
    var text by rememberSaveable { mutableStateOf(initialText) }
    var visibility by rememberSaveable { mutableStateOf(Prefs.defaultVisibility(context)) }
    var localOnly by rememberSaveable { mutableStateOf(Prefs.defaultLocalOnly(context)) }
    var posting by remember { mutableStateOf(false) }

    // 通知・エラー文言は合成中に取得しておく（locale 変更時に再計算され、LocaleContext の警告を回避）
    val toastPosted = stringResource(R.string.toast_posted)
    val errorPostFailedFormat = stringResource(R.string.error_post_failed)

    val acc = account
    if (acc == null) {
        NoAccountScreen()
        return
    }

    val visibilityOptions = listOf(
        stringResource(R.string.visibility_public) to Prefs.VISIBILITY_PUBLIC,
        stringResource(R.string.visibility_home) to Prefs.VISIBILITY_HOME,
        stringResource(R.string.visibility_followers) to Prefs.VISIBILITY_FOLLOWERS
    )

    fun post() {
        posting = true
        scope.launch {
            try {
                MisskeyClient.createNote(
                    instance = acc.instance,
                    token = acc.token,
                    text = text.trim(),
                    visibility = visibility,
                    localOnly = localOnly
                )
                Toast.makeText(context, toastPosted, Toast.LENGTH_SHORT).show()
                onFinish()
            } catch (e: Exception) {
                posting = false
                // MainActivity と同じく、エラーも Toast で表示する（通知手段を統一）
                Toast.makeText(
                    context,
                    String.format(errorPostFailedFormat, errorDetailForDisplay(context, e)),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.share_to)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // IME表示時はウィンドウを縮めず（adjustNothing）、下部要素はキーボードの下に隠れる
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.post_destination,
                    "@${acc.username ?: "?"}@${acc.instance.removePrefix("https://").removePrefix("http://")}"
                ),
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text(stringResource(R.string.note_label)) },
                supportingText = { if (text.isBlank()) Text(stringResource(R.string.note_required)) },
                enabled = !posting
            )

            // Misskey サーバーはUnicodeコードポイントで数える（サロゲートペアを1文字）。
            // 投稿時はtrimされるため、そのままの長さではなくtrim後の長さで表示する。
            val submittedText = text.trim()
            val charCount = submittedText.codePointCount(0, submittedText.length)
            Text(
                text = pluralStringResource(R.plurals.char_count, charCount, charCount),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Text(stringResource(R.string.visibility_label), style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                visibilityOptions.forEachIndexed { index, (label, value) ->
                    SegmentedButton(
                        selected = visibility == value,
                        onClick = { visibility = value },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = visibilityOptions.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.local_only_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.local_only_sub),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = localOnly,
                    onCheckedChange = { localOnly = it },
                    enabled = !posting
                )
            }

            if (posting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { post() },
                        modifier = Modifier.weight(1f),
                        enabled = text.isNotBlank()
                    ) {
                        Text(stringResource(R.string.post_button))
                    }
                }
            }
        }
    }
}

/** 未連携のときに表示する案内画面 */
@Composable
fun NoAccountScreen() {
    val context = LocalContext.current
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.no_account_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.no_account_body),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                (context as? ComponentActivity)?.finish()
            }) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}
