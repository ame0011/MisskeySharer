package com.example.misskeysharer

import android.content.Context
import android.content.SharedPreferences

/** 連携済みのMisskeyアカウント情報 */
data class Account(
    val instance: String,
    val token: String,
    val username: String?
)

/** SharedPreferences への読み書きをまとめたオブジェクト */
object Prefs {
    const val VISIBILITY_PUBLIC = "public"
    const val VISIBILITY_HOME = "home"
    const val VISIBILITY_FOLLOWERS = "followers"

    private const val FILE = "misskey_sharer_prefs"
    private const val KEY_INSTANCE = "instance"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_PENDING_INSTANCE = "pending_instance"
    private const val KEY_PENDING_SESSION = "pending_session"
    private const val KEY_DEFAULT_VISIBILITY = "default_visibility"
    private const val KEY_DEFAULT_LOCAL_ONLY = "default_local_only"

    private fun p(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun account(context: Context): Account? {
        val prefs = p(context)
        val instance = prefs.getString(KEY_INSTANCE, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Account(
            instance = normalizeInstanceUrl(instance),
            token = token,
            username = prefs.getString(KEY_USERNAME, null)
        )
    }

    fun saveAccount(context: Context, instance: String, token: String, username: String?) {
        p(context).edit()
            .putString(KEY_INSTANCE, normalizeInstanceUrl(instance))
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            // 認証が完了したので保留状態はクリア
            .remove(KEY_PENDING_INSTANCE)
            .remove(KEY_PENDING_SESSION)
            .apply()
    }

    fun clearAccount(context: Context) {
        p(context).edit()
            .remove(KEY_INSTANCE)
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .apply()
    }

    /** 認証待ちの (instance, session) ペア。ブラウザからの復帰に使う */
    fun pendingAuth(context: Context): Pair<String, String>? {
        val prefs = p(context)
        val instance = prefs.getString(KEY_PENDING_INSTANCE, null) ?: return null
        val session = prefs.getString(KEY_PENDING_SESSION, null) ?: return null
        return normalizeInstanceUrl(instance) to session
    }

    fun setPendingAuth(context: Context, instance: String, session: String) {
        p(context).edit()
            .putString(KEY_PENDING_INSTANCE, normalizeInstanceUrl(instance))
            .putString(KEY_PENDING_SESSION, session)
            .apply()
    }

    fun clearPendingAuth(context: Context) {
        p(context).edit()
            .remove(KEY_PENDING_INSTANCE)
            .remove(KEY_PENDING_SESSION)
            .apply()
    }

    fun defaultVisibility(context: Context): String =
        p(context).getString(KEY_DEFAULT_VISIBILITY, VISIBILITY_HOME) ?: VISIBILITY_HOME

    fun setDefaultVisibility(context: Context, value: String) {
        p(context).edit().putString(KEY_DEFAULT_VISIBILITY, value).apply()
    }

    fun defaultLocalOnly(context: Context): Boolean =
        p(context).getBoolean(KEY_DEFAULT_LOCAL_ONLY, false)

    fun setDefaultLocalOnly(context: Context, value: Boolean) {
        p(context).edit().putBoolean(KEY_DEFAULT_LOCAL_ONLY, value).apply()
    }

    /** "misskey.io" のような入力を "https://misskey.io" に正規化する */
    fun normalizeInstanceUrl(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://$s"
        }
        return s.trimEnd('/')
    }
}
