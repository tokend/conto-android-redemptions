package org.tokend.contoredemptions.features.nfc.logic

import android.util.Log
import java.util.concurrent.*

/**
 * Holds threads used for async communication with NFC device
 * over [NfcConnection]. Performs cleanup of stuck threads.
 *
 * Do not forget to call [shutdown] when it is no more needed.
 *
 * @see submit
 */
class NfcCommunicationThreadsHolder(
        threadsPoolSize: Int = 4,
        private val threadTimeoutMs: Int = 10000
) {
    private class ActiveCommunicationThreadData(
            val connection: NfcConnection,
            val future: Future<*>,
            val startedAt: Long = System.currentTimeMillis()
    )

    private val executorService = Executors.newScheduledThreadPool(threadsPoolSize, object : ThreadFactory {
        private var i = 1
        override fun newThread(r: Runnable?) =
                Thread(r).apply { name = "NfcCommunicationThread-${i++}" }
    })

    private val activeCommunicationThreads = mutableSetOf<ActiveCommunicationThreadData>()

    init {
        scheduleThreadsCleanup()
    }

    /**
     * Submits given [communication] to async execution.
     */
    fun submit(relatedConnection: NfcConnection,
               communication: () -> Unit) {
        val future = executorService.submit {
            val connectionHash = Integer.toHexString(relatedConnection.hashCode())
            Log.i(LOG_TAG, "Begin communication over $connectionHash in ${Thread.currentThread().name}")
            communication()
        }
        activeCommunicationThreads.add(ActiveCommunicationThreadData(relatedConnection, future))
    }

    /**
     * Shuts down underlying [ExecutorService] and so all active
     * communication threads
     */
    fun shutdown() {
        executorService.shutdownNow()
    }

    private fun scheduleThreadsCleanup() {
        executorService.scheduleAtFixedRate({
            cleanUpThreads()
        }, 0, 2, TimeUnit.SECONDS)
    }

    /**
     * Frees threads which are held by closed connection or just running for too long.
     */
    private fun cleanUpThreads() {
        val now = System.currentTimeMillis()
        val iterator = activeCommunicationThreads.iterator()
        while (iterator.hasNext()) {
            val current = iterator.next()

            val isExpired = now - current.startedAt >= threadTimeoutMs
            val isJustStarted = now == current.startedAt
            val connectionIsClosed = !current.connection.isActive
            val connectionHash = Integer.toHexString(current.connection.hashCode())

            if (!isJustStarted && (connectionIsClosed || isExpired)) {
                Log.i(LOG_TAG, "Cancel future for $connectionHash")
                current.future.cancel(true)
            }
            if (current.future.isCancelled || current.future.isDone) {
                Log.i(LOG_TAG, "Remove future for $connectionHash")
                iterator.remove()
            }
        }
    }

    private companion object {
        private const val LOG_TAG = "NfcThreads"
    }
}