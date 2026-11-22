package com.amesingularity.misskeysharer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.net.Inet6Address
import java.net.InetAddress
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

    private val hostLabelRegex = Regex("^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?$")

    /**
     * http/https 以外のスキームが付いた入力かどうかを判定する。
     * 正規化後に末尾スラッシュが除去されると "ftp://" が "https://ftp:/" になり
     * 有効なホストとして通ってしまうため、正規化前にチェックする。
     * "localhost:3000" のような host:port はコロン直後が数字なのでスキーム扱いしない。
     */
    private fun hasUnsupportedScheme(raw: String): Boolean {
        val colon = raw.indexOf(':')
        if (colon <= 0) return false
        val candidate = raw.substring(0, colon)
        if (!candidate.first().isLetter()) return false
        if (candidate.any { !it.isLetterOrDigit() && it !in "+-." }) return false
        if (colon + 1 < raw.length && raw[colon + 1].isDigit()) return false
        return !candidate.equals("http", ignoreCase = true) &&
            !candidate.equals("https", ignoreCase = true)
    }

    /** ASCII のホスト名、または IPv6 リテラルかどうかを判定する */
    private fun isValidHost(host: String): Boolean {
        if (host.isEmpty()) return false
        if (host.startsWith("[") && host.endsWith("]")) {
            val address = host.removePrefix("[").removeSuffix("]")
            // IPv6 リテラルは必ず ':' を含む。これにより [example.com] などの
            // ホスト名に対する DNS 参照をメインスレッドで行わないようにする。
            if (!address.contains(':')) return false
            return try {
                InetAddress.getByName(address) is Inet6Address
            } catch (e: Exception) {
                false
            }
        }
        return host.split('.').all { hostLabelRegex.matches(it) }
    }

    /**
     * 認証に使えるインスタンスURLかどうかを判定する。
     * スキームとホストが必要。クエリ・フラグメント・ユーザー情報は
     * APIパスを連結するときに壊れるため拒否する。
     */
    fun isValidInstanceUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        if (hasUnsupportedScheme(trimmed)) return false
        // "ftp://example.com" のような入力を "https://ftp://example.com" にしない
        if (trimmed.indexOf("://") != trimmed.lastIndexOf("://")) return false
        val normalized = normalizeInstanceUrl(trimmed)
        if (normalized.isEmpty()) return false
        if (normalized.indexOf("://") != normalized.lastIndexOf("://")) return false
        return try {
            val url = URL(normalized)
            (url.protocol.equals("http", ignoreCase = true) ||
                url.protocol.equals("https", ignoreCase = true)) &&
                isValidHost(url.host) &&
                (url.port == -1 || url.port in 1..65535) &&
                url.query == null &&
                url.ref == null &&
                url.userInfo == null
        } catch (e: Exception) {
            false
        }
    }
}
