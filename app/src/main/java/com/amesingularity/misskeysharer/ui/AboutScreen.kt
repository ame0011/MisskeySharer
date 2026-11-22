package com.amesingularity.misskeysharer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.amesingularity.misskeysharer.R
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/** アプリ情報とサードパーティライセンスを表示するAbout画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // アプリのバージョン名
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }
    val libraries by produceLibraries(R.raw.aboutlibraries)

    // clickable の lambda 内では composable 関数を使えないため、文言は合成中に取得しておく
    val repositoryUrl = stringResource(R.string.about_repository_url)
    val licenseUrl = stringResource(R.string.about_license_url)

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            // ブラウザが無い場合は何もしない
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        // カードとタイトルも LazyColumn のアイテムとして描画し、トップバー以外を全てスクロール可能にする
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            header = {
                item {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            versionName?.let {
                                Text(
                                    stringResource(R.string.about_version, it),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                stringResource(R.string.about_license_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.about_license_text),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            // カード最下部のリポジトリとライセンスのボタン
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                TextButton(
                                    onClick = { openUrl(repositoryUrl) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        bottomStart = 12.dp,
                                        topEnd = 0.dp,
                                        bottomEnd = 0.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.about_repository),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                TextButton(
                                    onClick = { openUrl(licenseUrl) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(
                                        topStart = 0.dp,
                                        bottomStart = 0.dp,
                                        topEnd = 12.dp,
                                        bottomEnd = 12.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.about_license_link),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.about_oss_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        )
    }
}
