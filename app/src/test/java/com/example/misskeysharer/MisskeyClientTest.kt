package com.example.misskeysharer

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.net.URLDecoder

/** MisskeyClient.miAuthUrl の単体テスト（純粋関数・Android API非依存） */
class MisskeyClientTest {

    @Test
    fun buildsUrlWithInstanceAndSession() {
        val url = MisskeyClient.miAuthUrl("https://misskey.io", "abc-123")
        assertTrue(url.startsWith("https://misskey.io/miauth/abc-123?"))
        assertTrue(url.contains("permission=write:notes"))
    }

    @Test
    fun encodesAppName() {
        // Java の URLEncoder はスペースを「+」にエンコードする（%20 ではない）
        val url = MisskeyClient.miAuthUrl("https://misskey.io", "abc-123")
        assertTrue(url.contains("name=Misskey+Sharer"))
    }

    @Test
    fun callbackIsSingleEncodedParam() {
        // コールバック全体が 1 つのクエリパラメータになるよう、
        // スキーム・ホスト・クエリ名がエンコードされる
        val url = MisskeyClient.miAuthUrl("https://misskey.io", "abc-123")
        assertTrue(url.contains("callback=misskeysharer%3A%2F%2Fmiauth%3Finstance%3D"))
    }

    @Test
    fun callbackInstanceSurvivesRoundTrip() {
        val instance = "https://misskey.io"
        val url = MisskeyClient.miAuthUrl(instance, "abc-123")

        // MiAuth サーバーが callback を 1 回デコードしてリダイレクト URL を生成する
        val redirectUrl = URLDecoder.decode(url.substringAfter("callback="), "UTF-8")
        // Android の Uri が instance をさらに 1 回デコードする
        val rawInstance = redirectUrl.substringAfter("instance=")

        assertEquals(instance, URLDecoder.decode(rawInstance, "UTF-8"))
    }
}
