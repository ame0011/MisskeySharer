package com.amesingularity.misskeysharer

import android.content.Context

/**
 * Misskey通信で発生した例外を、ユーザーに表示する説明文へ変換する。
 *
 * - [NotAuthorizedException] / [NetworkErrorException] はローカライズ済みの文言を返す
 * - その他の [MisskeyException] はサーバー由来のメッセージを原文のまま返す
 */
fun errorDetailForDisplay(context: Context, e: Throwable): String = when (e) {
    is NotAuthorizedException -> context.getString(R.string.error_not_authorized)
    is NetworkErrorException -> context.getString(
        R.string.error_network,
        e.detail ?: context.getString(R.string.error_detail_unknown)
    )
    else -> e.message?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.error_unknown)
}
