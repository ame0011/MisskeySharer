package com.amesingularity.misskeysharer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.net.URL

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
    private const val KEY_PURE_BLACK_MODE = "pure_black_mode"

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
        p(context).edit {
            putString(KEY_INSTANCE, normalizeInstanceUrl(instance))
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, username)
            // 認証が完了したので保留状態はクリア
            remove(KEY_PENDING_INSTANCE)
            remove(KEY_PENDING_SESSION)
        }
    }

    fun clearAccount(context: Context) {
        p(context).edit {
            remove(KEY_INSTANCE)
            remove(KEY_TOKEN)
            remove(KEY_USERNAME)
        }
    }

    /** 認証待ちの (instance, session) ペア。ブラウザからの復帰に使う */
    fun pendingAuth(context: Context): Pair<String, String>? {
        val prefs = p(context)
        val instance = prefs.getString(KEY_PENDING_INSTANCE, null) ?: return null
        val session = prefs.getString(KEY_PENDING_SESSION, null) ?: return null
        return normalizeInstanceUrl(instance) to session
    }

    fun setPendingAuth(context: Context, instance: String, session: String) {
        p(context).edit {
            putString(KEY_PENDING_INSTANCE, normalizeInstanceUrl(instance))
            putString(KEY_PENDING_SESSION, session)
        }
    }

    fun clearPendingAuth(context: Context) {
        p(context).edit {
            remove(KEY_PENDING_INSTANCE)
            remove(KEY_PENDING_SESSION)
        }
    }

    fun defaultVisibility(context: Context): String =
        p(context).getString(KEY_DEFAULT_VISIBILITY, VISIBILITY_HOME) ?: VISIBILITY_HOME

    fun setDefaultVisibility(context: Context, value: String) {
        p(context).edit { putString(KEY_DEFAULT_VISIBILITY, value) }
    }

    fun defaultLocalOnly(context: Context): Boolean =
        p(context).getBoolean(KEY_DEFAULT_LOCAL_ONLY, false)

    fun setDefaultLocalOnly(context: Context, value: Boolean) {
        p(context).edit { putBoolean(KEY_DEFAULT_LOCAL_ONLY, value) }
    }

    /** ピュアブラックモードの可否（ダークモード時のみ有効） */
    fun pureBlackMode(context: Context): Boolean =
        p(context).getBoolean(KEY_PURE_BLACK_MODE, false)

    fun setPureBlackMode(context: Context, value: Boolean) {
        p(context).edit { putBoolean(KEY_PURE_BLACK_MODE, value) }
    }

    /** ピュアブラックモード変更のリスナーを登録し、解除関数を返す */
    fun registerPureBlackModeListener(context: Context, onChange: () -> Unit): () -> Unit {
        val prefs = p(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PURE_BLACK_MODE) onChange()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** "misskey.io" のような入力を "https://misskey.io" に正規化する */
    fun normalizeInstanceUrl(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (!s.startsWith("http://", ignoreCase = true) &&
            !s.startsWith("https://", ignoreCase = true)
        ) {
            s = "https://$s"
        }
        return s.trimEnd('/')
    }

    /**
     * 認証に使えるインスタンスURLかどうかを判定する。
     * スキームとホストが必要。クエリ・フラグメント・ユーザー情報は
     * APIパスを連結するときに壊れるため拒否する。
     */
    fun isValidInstanceUrl(raw: String): Boolean {
        val normalized = normalizeInstanceUrl(raw)
        if (normalized.isEmpty()) return false
        // "ftp://example.com" のように別スキームが付いた入力は
        // 正規化で "https://ftp://example.com" にならないよう拒否する
        if (normalized.indexOf("://") != normalized.lastIndexOf("://")) return false
        return try {
            val url = URL(normalized)
            (url.protocol.equals("http", ignoreCase = true) ||
                url.protocol.equals("https", ignoreCase = true)) &&
                url.host.isNotEmpty() &&
                url.query == null &&
                url.ref == null &&
                url.userInfo == null
        } catch (e: Exception) {
            false
        }
    }
}
