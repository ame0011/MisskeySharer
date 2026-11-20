package com.amesingularity.misskeysharer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Misskey API由来のエラー。message はサーバー由来の文言であり、表示時は原文をそのまま使う */
open class MisskeyException(message: String) : Exception(message)

/** MiAuthセッションがまだ承認されていない（表示文言はUI側で R.string.error_not_authorized から選択する） */
class NotAuthorizedException : MisskeyException("not authorized")

/** 通信の失敗。detail は技術的な原因の原文（英語等）であり、表示文言はUI側で R.string.error_network から選択する */
class NetworkErrorException(val detail: String?) : MisskeyException(detail ?: "network error")

/** MiAuth check の結果 */
data class MiAuthResult(
    val token: String,
    val username: String?
)

/** Misskey API クライアント (MiAuth確認 / ノート作成) */
object MisskeyClient {

    private const val APP_NAME = "Misskey Sharer"
    private const val CALLBACK_URL = "misskeysharer://miauth"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * MiAuth認証ページのURLを組み立てる。
     * ブラウザで「許可する」を押すと misskeysharer://miauth に
     * ?session=<uuid>&instance=<instance> 付きでリダイレクトされる。
     */
    fun miAuthUrl(instance: String, session: String): String {
        val instanceEncoded = URLEncoder.encode(instance, "UTF-8")
        val callback = URLEncoder.encode("$CALLBACK_URL?instance=$instanceEncoded", "UTF-8")
        val name = URLEncoder.encode(APP_NAME, "UTF-8")
        return "$instance/miauth/$session?name=$name&permission=write:notes&callback=$callback"
    }

    /**
     * MiAuthセッションの承認状況を確認し、アクセストークンを返す。
     * 未承認の場合は MisskeyException を投げる。
     */
    suspend fun checkMiAuth(instance: String, session: String): MiAuthResult =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$instance/api/miauth/$session/check")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            try {
                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    val json = try {
                        JSONObject(body)
                    } catch (e: Exception) {
                        JSONObject()
                    }
                    val token = json.optString("token")
                    if (json.optBoolean("ok", false) && token.isNotEmpty()) {
                        val user = json.optJSONObject("user")
                        val username = user?.optString("username")?.takeIf { it.isNotEmpty() }
                        MiAuthResult(token = token, username = username)
                    } else {
                        throw NotAuthorizedException()
                    }
                }
            } catch (e: MisskeyException) {
                throw e
            } catch (e: Exception) {
                throw NetworkErrorException(e.message)
            }
        }

    /**
     * /api/notes/create でノートを投稿する。
     * 失敗時は MisskeyException。
     */
    suspend fun createNote(
        instance: String,
        token: String,
        text: String,
        visibility: String,
        localOnly: Boolean
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("i", token)
            put("text", text)
            put("visibility", visibility)
            if (localOnly) put("localOnly", true)
        }

        val request = Request.Builder()
            .url("$instance/api/notes/create")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (response.isSuccessful) {
                    JSONObject(body)
                } else {
                    throw MisskeyException(parseApiError(body, response.code))
                }
            }
        } catch (e: MisskeyException) {
            throw e
        } catch (e: Exception) {
            throw NetworkErrorException(e.message)
        }
    }

    internal fun parseApiError(body: String, code: Int): String = try {
        val error = JSONObject(body).optJSONObject("error")
        error?.optString("message")?.takeIf { it.isNotEmpty() } ?: "HTTP $code"
    } catch (e: Exception) {
        "HTTP $code"
    }
}
