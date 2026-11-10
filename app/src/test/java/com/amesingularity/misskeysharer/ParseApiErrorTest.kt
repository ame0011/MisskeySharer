package com.amesingularity.misskeysharer

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * MisskeyClient.parseApiError の単体テスト。
 * Android の org.json.JSONObject を使うため Robolectric 環境で実行する。
 */
@RunWith(RobolectricTestRunner::class)
class ParseApiErrorTest {

    @Test
    fun extractsErrorMessageFromApiBody() {
        val body = """{"error":{"message":"Character count exceeded"}}"""
        assertEquals("Character count exceeded", MisskeyClient.parseApiError(body, 400))
    }

    @Test
    fun fallsBackToHttpCodeWhenNoErrorObject() {
        assertEquals("HTTP 400", MisskeyClient.parseApiError("""{"ok":false}""", 400))
    }

    @Test
    fun fallsBackToHttpCodeWhenBodyIsNotJson() {
        assertEquals("HTTP 500", MisskeyClient.parseApiError("<html>oops</html>", 500))
    }
}
