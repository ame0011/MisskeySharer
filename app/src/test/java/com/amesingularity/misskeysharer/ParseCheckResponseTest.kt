package com.amesingularity.misskeysharer

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * MisskeyClient.parseCheckResponse の単体テスト。
 * Android の org.json.JSONObject を使うため Robolectric 環境で実行する。
 */
@RunWith(RobolectricTestRunner::class)
class ParseCheckResponseTest {

    @Test
    fun authorizedOn200ReturnsTokenAndUsername() {
        val body = """{"ok":true,"token":"tk-1","user":{"username":"alice"}}"""
        val result = MisskeyClient.parseCheckResponse(200, body)
        assertEquals("tk-1", result.token)
        assertEquals("alice", result.username)
    }

    @Test
    fun okFalseOn200IsNotAuthorized() {
        assertThrows(NotAuthorizedException::class.java) {
            MisskeyClient.parseCheckResponse(200, """{"ok":false}""")
        }
    }

    @Test
    fun okTrueWithoutTokenOn200IsNotAuthorized() {
        assertThrows(NotAuthorizedException::class.java) {
            MisskeyClient.parseCheckResponse(200, """{"ok":true}""")
        }
    }

    @Test
    fun nonJsonBodyOn200IsNetworkError() {
        assertThrows(NetworkErrorException::class.java) {
            MisskeyClient.parseCheckResponse(200, "<html>gateway page</html>")
        }
    }

    @Test
    fun missingSessionOn404IsNoSuchSession() {
        val body = """{"error":{"code":"NO_SUCH_SESSION","message":"No such session"}}"""
        val e = assertThrows(NoSuchSessionException::class.java) {
            MisskeyClient.parseCheckResponse(404, body)
        }
        assertEquals("No such session", e.message)
    }

    @Test
    fun nonJsonBodyOn404IsNoSuchSessionWithHttpCode() {
        val e = assertThrows(NoSuchSessionException::class.java) {
            MisskeyClient.parseCheckResponse(404, "<html>not found</html>")
        }
        assertEquals("HTTP 404", e.message)
    }

    @Test
    fun noSuchSessionCodeOnOtherStatusIsAlsoNoSuchSession() {
        val body = """{"error":{"code":"NO_SUCH_SESSION","message":"No such session"}}"""
        assertThrows(NoSuchSessionException::class.java) {
            MisskeyClient.parseCheckResponse(400, body)
        }
    }

    @Test
    fun serverErrorKeepsOriginalMessage() {
        val body = """{"error":{"code":"INTERNAL_ERROR","message":"Database failure"}}"""
        val e = assertThrows(MisskeyException::class.java) {
            MisskeyClient.parseCheckResponse(500, body)
        }
        assertEquals("Database failure", e.message)
        assertFalse(e is NoSuchSessionException)
    }

    @Test
    fun serverErrorWithoutJsonFallsBackToHttpCode() {
        val e = assertThrows(MisskeyException::class.java) {
            MisskeyClient.parseCheckResponse(502, "<html>Bad Gateway</html>")
        }
        assertEquals("HTTP 502", e.message)
        assertFalse(e is NoSuchSessionException)
    }
}
