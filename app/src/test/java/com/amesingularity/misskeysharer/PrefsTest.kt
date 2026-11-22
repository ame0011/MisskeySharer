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

    @Test
    fun acceptsNormalizedInstanceUrls() {
        assertEquals(true, Prefs.isValidInstanceUrl("misskey.io"))
        assertEquals(true, Prefs.isValidInstanceUrl("https://misskey.io/"))
        assertEquals(true, Prefs.isValidInstanceUrl("http://localhost:3000"))
        assertEquals(true, Prefs.isValidInstanceUrl("https://example.com/misskey"))
    }

    @Test
    fun rejectsUrlsWithoutHostOrWithExtraParts() {
        assertEquals(false, Prefs.isValidInstanceUrl(""))
        assertEquals(false, Prefs.isValidInstanceUrl("   "))
        assertEquals(false, Prefs.isValidInstanceUrl("あ"))
        assertEquals(false, Prefs.isValidInstanceUrl("https://"))
        assertEquals(false, Prefs.isValidInstanceUrl("ftp://"))
        assertEquals(false, Prefs.isValidInstanceUrl("ftp://misskey.io"))
        assertEquals(false, Prefs.isValidInstanceUrl("javascript:alert(1)"))
        assertEquals(false, Prefs.isValidInstanceUrl("https://misskey.io/?foo=bar"))
        assertEquals(false, Prefs.isValidInstanceUrl("https://misskey.io/#top"))
        assertEquals(false, Prefs.isValidInstanceUrl("https://user@misskey.io"))
    }
}
