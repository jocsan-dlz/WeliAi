package com.npcg.welliai.backend.security

class InMemoryRateLimiter(
    private val maximumRequests: Int = 20,
    private val windowMillis: Long = 60_000,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val requestTimes = ArrayDeque<Long>()

    init {
        require(maximumRequests > 0)
        require(windowMillis > 0)
    }

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = clock()
        while (requestTimes.firstOrNull()?.let { now - it >= windowMillis } == true) requestTimes.removeFirst()
        if (requestTimes.size >= maximumRequests) return false
        requestTimes.addLast(now)
        return true
    }
}
