@file:JvmName("HttpClientFactoryAndroid")

package dev.huntdex.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

actual fun createHttpClient(): HttpClient = HttpClient(Android)
