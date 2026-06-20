package dev.huntdex.core.common

actual class LocaleProvider actual constructor() {
    actual fun languageCode(): String = java.util.Locale.getDefault().language
}
