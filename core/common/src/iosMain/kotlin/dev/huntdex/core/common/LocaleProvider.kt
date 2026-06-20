package dev.huntdex.core.common

import platform.Foundation.NSLocale

actual class LocaleProvider actual constructor() {
    actual fun languageCode(): String =
        NSLocale.preferredLanguages.firstOrNull()
            ?.substringBefore('-')
            ?: "en"
}
