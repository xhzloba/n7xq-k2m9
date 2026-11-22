package com.example.movie.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Не больше [permits] запросов к каталогу сразу.
 * [priority] 0 обгоняет очередь полок: слайдер и открытая страница не ждут все жанры.
 */
internal class VokinoSlots(
    private val permits: Int = 4,
) {
    private val lock = Mutex()
    private var free = permits
    private val waiters = ArrayDeque<Waiter>()

    private class Waiter(
        val priority: Int,
        val ready: CompletableDeferred<Unit>,
    )

    suspend fun <T> use(priority: Int = 0, block: suspend () -> T): T {
        acquire(priority)
        return try {
            block()
        } finally {
            release()
        }
    }

    private suspend fun acquire(priority: Int) {
        val me = Waiter(priority, CompletableDeferred())
        lock.withLock {
            if (free > 0 && waiters.none { it.priority < priority }) {
                free -= 1
                me.ready.complete(Unit)
            } else {
                val at = waiters.indexOfFirst { it.priority > priority }
                if (at < 0) waiters.addLast(me) else waiters.add(at, me)
            }
        }
        try {
            me.ready.await()
        } catch (cancelled: CancellationException) {
            val stillWaiting = lock.withLock { waiters.remove(me) }
            if (!stillWaiting) release()
            throw cancelled
        }
    }

    private suspend fun release() {
        val next = lock.withLock {
            val next = waiters.removeFirstOrNull()
            if (next == null) {
                free += 1
                null
            } else {
                next
            }
        }
        next?.ready?.complete(Unit)
    }
}
