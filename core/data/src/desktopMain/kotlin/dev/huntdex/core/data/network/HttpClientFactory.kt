@file:JvmName("HttpClientFactoryDesktop")

package dev.huntdex.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java

actual fun createHttpClient(): HttpClient = HttpClient(Java)
