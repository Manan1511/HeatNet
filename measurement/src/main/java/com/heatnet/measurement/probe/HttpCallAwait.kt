package com.heatnet.measurement.probe

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.resumeWithException

/** Awaits and consumes the response inside OkHttp's callback so cancellation closes active body reads. */
internal suspend fun <T> Call.awaitResponse(
    deadlineNanos: Long,
    consume: (Response) -> T,
): T = suspendCancellableCoroutine { continuation ->
    if (deadlineNanos <= System.nanoTime()) {
        continuation.resumeWithException(SocketTimeoutException("Measurement deadline has expired"))
        return@suspendCancellableCoroutine
    }

    timeout().deadlineNanoTime(deadlineNanos)
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (!continuation.isActive) {
                response.close()
                return
            }
            val result = try {
                response.use(consume)
            } catch (error: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(error)
                return
            }
            if (continuation.isActive) continuation.resumeWith(kotlin.Result.success(result))
        }
    })
}
