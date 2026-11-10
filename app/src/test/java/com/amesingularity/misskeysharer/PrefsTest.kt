package com.amesingularity.misskeysharer

import org.junit.Test
import org.junit.Assert.assertEquals

/** Prefs.normalizeInstanceUrl の単体テスト */
class PrefsTest {

    @Test
    fun addsHttpsWhenSchemeMissing() {
        assertEquals("https://misskey.io", Prefs.normalizeInstanceUrl("misskey.io"))
    }

    @Test
    fun keepsExistingScheme() {
        assertEquals("https://misskey.io", Prefs.normalizeInstanceUrl("https://misskey.io"))
        assertEquals("http://localhost:3000", Prefs.normalizeInstanceUrl("http://localhost:3000"))
    }

    @Test
    fun trimsWhitespaceAndTrailingSlash() {
        assertEquals("https://misskey.io", Prefs.normalizeInstanceUrl("  https://misskey.io/  "))
    }

    @Test
    fun returnsEmptyForBlankInput() {
        assertEquals("", Prefs.normalizeInstanceUrl("   "))
    }
}
