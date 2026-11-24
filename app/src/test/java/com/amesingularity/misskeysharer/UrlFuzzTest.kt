package com.amesingularity.misskeysharer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.net.URLDecoder

/**
 * ファジングテスト: MisskeyClient.miAuthUrl と Prefs のURL正規化・検証。
 * 純粋関数のため Robolectric なしで実行する (PrefsTest / MisskeyClientTest と同じ)。
 *
 * シードは固定しており、失敗時にシード・反復番号・入力を印字するので再現可能。
 */
class UrlFuzzTest {

    /** miAuthUrl は任意の instance / session でも例外を投げない */
    @Test
    fun miAuthUrlNeverThrows() {
        val seed = 2026112411L
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val instance = FuzzGenerators.randomString(rng, 100)
            val session = FuzzGenerators.randomString(rng, 50)
            val ctx = "seed=$seed iteration=$i " +
                "instance=${FuzzGenerators.escape(instance.take(120))} " +
                "session=${FuzzGenerators.escape(session.take(120))}"
            try {
                MisskeyClient.miAuthUrl(instance, session)
            } catch (e: Throwable) {
                throw AssertionError("miAuthUrl が例外を投げた ($ctx): ${e::class.java.name}: ${e.message}", e)
            }
        }
    }

    /**
     * 有効な instance と UUID 風の session (本番では UUID.randomUUID() 由来) では、
     * 生成されたURLがパースでき、callback クエリがコールバックURLに復元できる。
     */
    @Test
    fun miAuthUrlCallbackRoundTripsForValidInstanceAndUuidSession() {
        val seed = 2026112412L
        val instances = listOf("https://misskey.io", "http://localhost:3000", "https://xn--r8jz45g.jp")
        val uuidChars = "0123456789abcdef-"
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val instance = instances[rng.nextInt(instances.size)]
            val session = buildString {
                repeat(rng.nextInt(1, 40)) { append(uuidChars[rng.nextInt(uuidChars.length)]) }
            }
            val url = MisskeyClient.miAuthUrl(instance, session)
            val ctx = "seed=$seed iteration=$i url=${FuzzGenerators.escape(url.take(300))}"
            val parsed = try {
                URL(url)
            } catch (e: Throwable) {
                throw AssertionError("生成URLがパースできない ($ctx): ${e::class.java.name}: ${e.message}", e)
            }
            assertTrue("パスに /miauth/ を含まない ($ctx)", parsed.path.contains("/miauth/"))
            val callback = queryParam(parsed.query, "callback")
            assertNotNull("callback クエリが無い ($ctx)", callback)
            assertTrue(
                "callback がコールバックURLに復元できない ($ctx)",
                callback!!.startsWith("misskeysharer://miauth?instance="),
            )
        }
    }

    /** normalizeInstanceUrl は例外を投げず、空白のみ入力で空文字を返し、有効URLでは / で終わらず冪等 */
    @Test
    fun normalizeInstanceUrlKeepsInvariants() {
        val seed = 2026112413L
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val raw = FuzzGenerators.randomString(rng, 120)
            val ctx = "seed=$seed iteration=$i raw=${FuzzGenerators.escape(raw.take(120))}"
            val normalized = try {
                Prefs.normalizeInstanceUrl(raw)
            } catch (e: Throwable) {
                throw AssertionError("normalizeInstanceUrl が例外を投げた ($ctx): ${e::class.java.name}: ${e.message}", e)
            }
            if (raw.isBlank()) {
                assertEquals("空白のみの入力は空文字 ($ctx)", "", normalized)
            } else {
                assertTrue("正規化結果が / で終わる ($ctx normalized=$normalized)", !normalized.endsWith("/"))
                // 無効な入力の再正規化はスキームが伸びる (例: "//" → "https:" → "https://https:") ため、
                // アプリが実際に使う有効な入力のみ冪等性を検証する
                if (Prefs.isValidInstanceUrl(raw)) {
                    assertEquals("正規化が冪等でない ($ctx)", normalized, Prefs.normalizeInstanceUrl(normalized))
                }
            }
        }
    }

    /** isValidInstanceUrl は例外を投げず、true のときの正規化結果は http/https で余分な成分が無い */
    @Test
    fun isValidInstanceUrlNeverThrowsAndValidUrlsAreWellFormed() {
        val seed = 2026112414L
        FuzzGenerators.forEachFuzz(seed, FuzzGenerators.ITERATIONS) { rng, i ->
            val raw = FuzzGenerators.randomString(rng, 120)
            val ctx = "seed=$seed iteration=$i raw=${FuzzGenerators.escape(raw.take(120))}"
            val valid = try {
                Prefs.isValidInstanceUrl(raw)
            } catch (e: Throwable) {
                throw AssertionError("isValidInstanceUrl が例外を投げた ($ctx): ${e::class.java.name}: ${e.message}", e)
            }
            if (!valid) return@forEachFuzz
            val normalized = Prefs.normalizeInstanceUrl(raw)
            val url = try {
                URL(normalized)
            } catch (e: Throwable) {
                throw AssertionError("有効判定なのにURLがパースできない ($ctx normalized=$normalized)", e)
            }
            assertTrue(
                "スキームが http/https でない ($ctx normalized=$normalized)",
                url.protocol.equals("http", ignoreCase = true) || url.protocol.equals("https", ignoreCase = true),
            )
            assertTrue("ホストが空 ($ctx normalized=$normalized)", url.host.isNotEmpty())
            assertTrue("ポートが範囲外 ($ctx normalized=$normalized)", url.port == -1 || url.port in 1..65535)
            assertTrue("クエリを含む ($ctx normalized=$normalized)", url.query == null)
            assertTrue("フラグメントを含む ($ctx normalized=$normalized)", url.ref == null)
            assertTrue("ユーザー情報を含む ($ctx normalized=$normalized)", url.userInfo == null)
            assertTrue("正規化後も有効でない ($ctx normalized=$normalized)", Prefs.isValidInstanceUrl(normalized))
        }
    }

    /** isValidInstanceUrl の期待値を明示した決定論的コーパス */
    @Test
    fun isValidInstanceUrlCorpus() {
        val expected = listOf(
            "misskey.io" to true,
            "https://misskey.io/" to true,
            "http://localhost:3000" to true,
            "https://example.com/misskey" to true,
            "HTTPS://A" to true,
            "https://a//" to true,
            "[::1]" to true,
            "https://[::1]:3000" to true,
            " https://a.com" to true, // 前後の空白はトリムされる
            "" to false,
            "   " to false,
            "ftp://example.com" to false,
            "https://a?q=1" to false,
            "https://a#f" to false,
            "https://user:pass@a" to false,
            "[example.com]" to false,
            "https://a.com:0" to false,
            "https://a.com:99999" to false,
            "https://-a.com" to false,
            "https://a-.com" to false,
            "https://.a.com" to false,
            "https://a b.com" to false,
            "https://".plus(0xD800.toChar()).plus(".com") to false, // 孤立サロゲート
            "https://👁🍄.com" to false,
            0.toChar().toString().plus("https://a.com") to false, // NUL 文字が混入した入力
            "https://a.com?q" to false,
            "https://a.com#q" to false,
        )
        for ((raw, want) in expected) {
            assertEquals("isValidInstanceUrl(${FuzzGenerators.escape(raw)})", want, Prefs.isValidInstanceUrl(raw))
        }
    }

    /** normalizeInstanceUrl の期待値を明示した決定論的コーパス */
    @Test
    fun normalizeInstanceUrlCorpus() {
        assertEquals("https://misskey.io", Prefs.normalizeInstanceUrl("misskey.io"))
        assertEquals("https://misskey.io", Prefs.normalizeInstanceUrl("  https://misskey.io/  "))
        assertEquals("https://a", Prefs.normalizeInstanceUrl("https://a//"))
        assertEquals("HTTPS://A", Prefs.normalizeInstanceUrl("HTTPS://A"))
        assertEquals("", Prefs.normalizeInstanceUrl(""))
        assertEquals("", Prefs.normalizeInstanceUrl("   "))
    }

    private fun queryParam(query: String?, name: String): String? {
        query ?: return null
        for (pair in query.split('&')) {
            val idx = pair.indexOf('=')
            if (idx < 0) continue
            if (URLDecoder.decode(pair.substring(0, idx), "UTF-8") == name) {
                return URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }
        }
        return null
    }
}
