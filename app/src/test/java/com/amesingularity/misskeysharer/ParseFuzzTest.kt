package com.amesingularity.misskeysharer

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * ファジングテスト: MisskeyClient.parseCheckResponse / parseApiError と
 * errorDetailForDisplay の堅牢性を検証する。
 * Android の org.json を使うため Robolectric 環境で実行する。
 *
 * シードは固定しており、失敗時にシード・反復番号・入力を印字するので再現可能。
 */
@RunWith(RobolectricTestRunner::class)
class ParseFuzzTest {

    /** parseCheckResponse は MisskeyException 系のみを投げ、正常返却時の token は非空でなければならない */
    @Test
    fun parseCheckResponseThrowsOnlyMisskeyException() {
        val seed = 2026112401L
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val code = FuzzGenerators.randomStatus(rng)
            val body = FuzzGenerators.randomBody(rng)
            val ctx = "seed=$seed iteration=$i code=$code body=${FuzzGenerators.escape(body.take(200))}"
            try {
                val result = MisskeyClient.parseCheckResponse(code, body)
                assertTrue("token が空 ($ctx)", result.token.isNotEmpty())
            } catch (e: MisskeyException) {
                // 想定される例外はすべて MisskeyException 系
            } catch (e: Throwable) {
                throw AssertionError(
                    "MisskeyException 以外の例外 ($ctx): ${e::class.java.name}: ${e.message}",
                    e,
                )
            }
        }
    }

    /** parseApiError は例外を投げず、常に非空の文言を返さなければならない */
    @Test
    fun parseApiErrorNeverThrowsAndReturnsNonEmpty() {
        val seed = 2026112402L
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val code = FuzzGenerators.randomStatus(rng)
            val body = FuzzGenerators.randomBody(rng)
            val ctx = "seed=$seed iteration=$i code=$code body=${FuzzGenerators.escape(body.take(200))}"
            val message = try {
                MisskeyClient.parseApiError(body, code)
            } catch (e: Throwable) {
                throw AssertionError("parseApiError が例外を投げた ($ctx): ${e::class.java.name}: ${e.message}", e)
            }
            assertTrue("エラー文言が空 ($ctx)", message.isNotEmpty())
        }
    }

    /** errorDetailForDisplay は例外を投げず、常に非空の表示文言を返さなければならない */
    @Test
    fun errorDetailForDisplayNeverThrowsAndReturnsNonEmpty() {
        val seed = 2026112403L
        val context = RuntimeEnvironment.getApplication()
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val detail = FuzzGenerators.randomString(rng, 200)
            val e: Throwable = when (rng.nextInt(6)) {
                0 -> NotAuthorizedException()
                1 -> NetworkErrorException(detail)
                2 -> NetworkErrorException(null)
                3 -> NoSuchSessionException(detail)
                4 -> MisskeyException(detail)
                else -> Exception(if (rng.nextBoolean()) detail else null)
            }
            val ctx = "seed=$seed iteration=$i type=${e::class.java.simpleName} " +
                "msg=${FuzzGenerators.escape((e.message ?: "").take(200))}"
            val text = try {
                errorDetailForDisplay(context, e)
            } catch (ex: Throwable) {
                throw AssertionError(
                    "errorDetailForDisplay が例外を投げた ($ctx): ${ex::class.java.name}: ${ex.message}",
                    ex,
                )
            }
            assertTrue("表示文言が空 ($ctx)", text.isNotEmpty())
        }
    }

    /** レスポンス解析の決定論的なエッジケースコーパス (毎回必ず実行される) */
    @Test
    fun parseHandlesDeterministicCorpus() {
        val codes = listOf(0, 200, 204, 301, 400, 404, 500, 999, Int.MAX_VALUE)
        for (body in corpusBodies()) {
            val ctx = "body=${FuzzGenerators.escape(body.take(120))} len=${body.length}"
            for (code in codes) {
                try {
                    val result = MisskeyClient.parseCheckResponse(code, body)
                    assertTrue("token が空 ($ctx code=$code)", result.token.isNotEmpty())
                } catch (e: MisskeyException) {
                    // OK
                } catch (e: Throwable) {
                    throw AssertionError(
                        "parseCheckResponse が想定外の例外 ($ctx code=$code): ${e::class.java.name}: ${e.message}",
                        e,
                    )
                }
            }
            val message = try {
                MisskeyClient.parseApiError(body, 500)
            } catch (e: Throwable) {
                throw AssertionError(
                    "parseApiError が例外を投げた ($ctx): ${e::class.java.name}: ${e.message}",
                    e,
                )
            }
            assertTrue("parseApiError の文言が空 ($ctx)", message.isNotEmpty())
        }
    }

    /** errorDetailForDisplay の決定論的なエッジケース */
    @Test
    fun errorDetailForDisplayHandlesDeterministicCorpus() {
        val context = RuntimeEnvironment.getApplication()
        val throwables: List<Throwable> = listOf(
            NotAuthorizedException(),
            NetworkErrorException(null),
            NetworkErrorException(""),
            NetworkErrorException("%s %1\$d %n"),
            NoSuchSessionException(""),
            NoSuchSessionException("%s"),
            MisskeyException(""),
            Exception(null as String?),
            Exception("Database failure"),
        )
        for (e in throwables) {
            val text = try {
                errorDetailForDisplay(context, e)
            } catch (ex: Throwable) {
                throw AssertionError(
                    "errorDetailForDisplay が例外を投げた (${e::class.java.simpleName}): ${ex::class.java.name}",
                    ex,
                )
            }
            assertTrue("表示文言が空 (${e::class.java.simpleName} msg=${e.message})", text.isNotEmpty())
        }
    }

    private fun corpusBodies(): List<String> = listOf(
        "",
        " ",
        0.toChar().toString(), // NUL 文字
        "{}",
        "[]",
        "{",
        "{\"ok\":true,\"token\":123}",
        "{\"ok\":\"true\",\"token\":\"\"}",
        "{\"ok\":true,\"token\":\"t\"}",
        "{\"ok\":true,\"token\":\"t\",\"user\":\"alice\"}",
        "{\"ok\":true,\"token\":\"t\",\"user\":{\"username\":123}}",
        "{\"error\":\"plain string\"}",
        "{\"error\":{\"code\":123,\"message\":null}}",
        "{\"error\":{\"code\":\"NO_SUCH_SESSION\",\"message\":\"\"}}",
        "<html><body>500</body></html>",
        "%s %1\$d %n",
        "👁🍄",
        0xD800.toChar().toString(), // 孤立サロゲート
        "a".repeat(1_000_000),
        // 深さ 10000 の入れ子オブジェクト (org.json の再帰パースの耐性探り)
        "{\"a\":".repeat(10_000) + "1" + "}".repeat(10_000),
    )
}
