package dev.huntdex.core.common

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual class LocaleProvider actual constructor() {
    actual fun languageCode(): String =
        NSLocale.currentLocale.languageCode
            .substringBefore('-')
            .ifEmpty { "en" }
}
