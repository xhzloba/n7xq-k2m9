package com.example.movie.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class VokinoSlotsTest {
    @Test
    fun foregroundJumpsAheadOfQueuedShelves() = runBlocking {
        val slots = VokinoSlots(permits = 1)
        val hold = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        val started = CompletableDeferred<Unit>()
        val holder = launch {
            slots.use(priority = 1) {
                started.complete(Unit)
                hold.await()
                order += "hold"
            }
        }
        started.await()
        val queued = AtomicInteger()
        val shelf = async {
            queued.incrementAndGet()
            slots.use(priority = 1) { order += "shelf" }
        }
        val hero = async {
            queued.incrementAndGet()
            slots.use(priority = 0) { order += "hero" }
        }
        while (queued.get() < 2) yield()
        repeat(8) { yield() }
        hold.complete(Unit)
        awaitAll(shelf, hero)
        holder.join()
        assertEquals(listOf("hold", "hero", "shelf"), order)
    }

    @Test
    fun cancelledWaiterDoesNotKeepTheSlot() = runBlocking {
        val slots = VokinoSlots(permits = 1)
        val hold = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val holder = launch {
            slots.use {
                started.complete(Unit)
                hold.await()
            }
        }
        started.await()
        val waiter = launch { slots.use(priority = 1) { } }
        yield()
        waiter.cancel()
        waiter.join()
        hold.complete(Unit)
        holder.join()
        val entered = CompletableDeferred<Unit>()
        slots.use { entered.complete(Unit) }
        entered.await()
    }

    @Test
    fun onlyFourRunTogether() = runBlocking {
        val slots = VokinoSlots(permits = 4)
        val running = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        coroutineScope {
            repeat(12) {
                launch {
                    slots.use {
                        running.incrementAndGet()
                        gate.await()
                        running.decrementAndGet()
                    }
                }
            }
            while (running.get() < 4) yield()
            repeat(20) { yield() }
            assertEquals(4, running.get())
            gate.complete(Unit)
        }
        assertEquals(0, running.get())
    }
}
