package com.amesingularity.misskeysharer

import kotlin.random.Random

/**
 * ファジングテスト共通の入力生成ヘルパー。
 * シードを固定して呼び出すことで、失敗時に同じ入力を再現できるようにする。
 */
object FuzzGenerators {

    /** 1プロパティあたりの反復回数。CIでの実行時間を数秒程度に抑えるための値 */
    const val ITERATIONS = 5000

    /**
     * 印刷可能ASCII・JSON記号・制御文字・非ASCII・孤立サロゲートを含む文字プール。
     * 絵文字は UTF-16 のサロゲートペアに分解されるため、孤立サロゲートも生成される。
     */
    private val charPool: CharArray = buildList {
        addAll("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray().toList())
        addAll(" \t\n\r{}[]\":,.\\/=?&%$#@!-+_~^|<>'".toCharArray().toList())
        addAll(listOf(0x00, 0x01, 0x1B, 0x7F, 0xFFFD, 0xFEFF).map(Int::toChar))
        // 孤立サロゲート (絵文字由来のサロゲートハーフもこれと合わせて混入する)
        addAll(listOf(0xD800, 0xDC00, 0xD83D).map(Int::toChar))
        addAll("あいうえお日本語👁🍄".toCharArray().toList())
    }.toCharArray()

    /** APIで実際に使われるキーを混ぜることで、パース経路に食いやすい入力を生成する */
    private val interestingKeys = listOf(
        "ok", "token", "user", "username", "error", "code", "message",
        "name", "permission", "callback", "text", "visibility", "localOnly", "i",
    )

    fun randomString(rng: Random, maxLen: Int): String {
        val len = rng.nextInt(maxLen + 1)
        return buildString(len) {
            repeat(len) { append(charPool[rng.nextInt(charPool.size)]) }
        }
    }

    /** 正常なHTTPステータスコードに加え、範囲外・負値・MAXもたまに生成する */
    fun randomStatus(rng: Random): Int = when (rng.nextInt(10)) {
        0 -> rng.nextInt(-100, 100)
        1 -> rng.nextInt(600, 1000)
        2 -> Int.MAX_VALUE
        else -> rng.nextInt(100, 600)
    }

    /** JSON文字列として妥当な形式にエスケープしたランダム文字列 */
    private fun randomJsonString(rng: Random): String {
        val s = randomString(rng, 30)
        return buildString {
            append('"')
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c.code < 32) append("\\u%04x".format(c.code)) else append(c)
                }
            }
            append('"')
        }
    }

    private fun randomJsonValue(rng: Random, depth: Int): String {
        if (depth <= 0) {
            return when (rng.nextInt(5)) {
                0 -> "true"
                1 -> "false"
                2 -> "null"
                3 -> rng.nextInt(-1000000, 1000000).toString()
                else -> randomJsonString(rng)
            }
        }
        return when (rng.nextInt(8)) {
            0 -> "true"
            1 -> "false"
            2 -> "null"
            3 -> rng.nextDouble().toString()
            4 -> randomJsonString(rng)
            5 -> randomObject(rng, depth - 1)
            6 -> randomArray(rng, depth - 1)
            else -> randomJsonString(rng)
        }
    }

    private fun randomObject(rng: Random, depth: Int): String {
        val entries = rng.nextInt(7)
        return buildString {
            append('{')
            for (j in 0 until entries) {
                if (j > 0) append(',')
                append(
                    if (rng.nextInt(2) == 0) "\"${interestingKeys[rng.nextInt(interestingKeys.size)]}\""
                    else randomJsonString(rng)
                )
                append(':')
                append(randomJsonValue(rng, depth))
            }
            append('}')
        }
    }

    private fun randomArray(rng: Random, depth: Int): String {
        val items = rng.nextInt(5)
        return buildString {
            append('[')
            for (j in 0 until items) {
                if (j > 0) append(',')
                append(randomJsonValue(rng, depth))
            }
            append(']')
        }
    }

    /**
     * APIレスポンスボディを想定したランダム入力。
     * 完全ランダム文字列・妥当なJSON・切り捨て/破損したJSON・型崩れしたMiAuthチェック風JSONを混ぜる。
     */
    fun randomBody(rng: Random): String {
        return when (rng.nextInt(6)) {
            0 -> randomString(rng, 2000)
            1 -> randomJsonValue(rng, 4)
            2 -> { // 途中で切り捨てたJSON
                val json = randomJsonValue(rng, 4)
                json.substring(0, rng.nextInt(json.length + 1))
            }
            3 -> { // 数文字を破損させたJSON
                val chars = randomJsonValue(rng, 4).toCharArray()
                repeat(rng.nextInt(5)) {
                    if (chars.isNotEmpty()) chars[rng.nextInt(chars.size)] = charPool[rng.nextInt(charPool.size)]
                }
                String(chars)
            }
            4 -> { // 型崩れしたMiAuthチェック風JSON (token が数値、error が文字列 など)
                buildString {
                    append('{')
                    append("\"ok\":${randomJsonValue(rng, 1)},")
                    append("\"token\":${randomJsonValue(rng, 1)},")
                    append("\"user\":${randomJsonValue(rng, 2)},")
                    append("\"error\":${randomJsonValue(rng, 2)}")
                    append('}')
                }
            }
            else -> randomString(rng, 200)
        }
    }

    /** 失敗レポート用に入力をエスケープする。制御文字とサロゲートは \uXXXX 形式に置換 */
    fun escape(s: String): String = buildString {
        for (c in s) {
            when {
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c.code < 32 || c.code in 0xD800..0xDFFF -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
    }

    /**
     * 固定シードで iterations 回 body を実行する。
     * テスト本体での AssertionError はそのまま、それ以外の例外は
     * シードと反復番号を添えて AssertionError にラップして投げる。
     */
    inline fun forEachFuzz(seed: Long, iterations: Int, body: (rng: Random, iteration: Int) -> Unit) {
        val rng = Random(seed)
        for (i in 0 until iterations) {
            try {
                body(rng, i)
            } catch (e: AssertionError) {
                throw e
            } catch (e: Throwable) {
                throw AssertionError("fuzz失敗 (seed=$seed, iteration=$i): ${e::class.java.name}: ${e.message}", e)
            }
        }
    }
}
