package com.heatnet.ui.screens

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class CompareLoadBenchmarkTest {

    private suspend fun mockLoad(id: Long): String {
        delay(100)
        return "Session_$id"
    }

    private suspend fun sequentialLoad(beforeId: Long, afterId: Long): Pair<String, String> {
        val before = mockLoad(beforeId)
        val after = mockLoad(afterId)
        return before to after
    }

    private suspend fun concurrentLoad(beforeId: Long, afterId: Long): Pair<String, String> {
        return coroutineScope {
            val (before, after) = awaitAll(
                async { mockLoad(beforeId) },
                async { mockLoad(afterId) },
            )
            before to after
        }
    }

    @Test
    fun testConcurrentLoadIsFasterThanSequentialLoad() = runBlocking {
        val sequentialTime = measureTimeMillis {
            sequentialLoad(1L, 2L)
        }
        val concurrentTime = measureTimeMillis {
            concurrentLoad(1L, 2L)
        }

        println("Sequential load time: ${sequentialTime}ms")
        println("Concurrent load time: ${concurrentTime}ms")

        assertTrue(
            "Concurrent load ($concurrentTime ms) should be faster than sequential load ($sequentialTime ms)",
            concurrentTime < sequentialTime
        )
    }
}
