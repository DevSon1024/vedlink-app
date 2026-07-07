package com.devson.vedlink.data.network.scraper.orchestrator

import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryEngine @Inject constructor() {

    fun isTransientException(throwable: Throwable): Boolean {
        return when (throwable) {
            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException -> true
            is IOException -> {
                val message = throwable.message?.lowercase() ?: ""
                message.contains("timeout") || message.contains("reset") || message.contains("canceled")
            }
            else -> {
                // If it is a custom exception holding HTTP code
                val message = throwable.message ?: ""
                message.contains("429") || message.contains("503") || message.contains("502") || message.contains("500")
            }
        }
    }

    suspend fun <T> executeWithRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                lastException = e
                if (!isTransientException(e) || attempt == maxAttempts) {
                    throw e
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        throw lastException ?: IllegalStateException("Retry failed")
    }
}
