package dev.huntdex.core.common

import kotlin.test.Test
import kotlin.test.assertTrue

class LocaleProviderTest {

    @Test
    fun `languageCode returns non-empty string`() {
        val code = LocaleProvider().languageCode()
        assertTrue(code.isNotEmpty(), "Expected non-empty language code, got: '$code'")
    }

    @Test
    fun `languageCode contains only lowercase letters`() {
        val code = LocaleProvider().languageCode()
        assertTrue(code.all { it.isLetter() && it.isLowerCase() },
            "Expected lowercase letters only, got: '$code'")
    }
}
