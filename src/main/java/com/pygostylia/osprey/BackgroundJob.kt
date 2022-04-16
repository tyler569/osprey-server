package com.pygostylia.osprey

import java.util.concurrent.PriorityBlockingQueue

data class BackgroundJob(val f: () -> Unit, val priority: Int) : Comparable<BackgroundJob> {
    companion object : Runnable {
        private val backgroundQueue: PriorityBlockingQueue<BackgroundJob> = PriorityBlockingQueue()

        private fun queueWithPriority(f: () -> Unit, priority: Int) {
            backgroundQueue.add(BackgroundJob(f, priority))
        }

        fun queue(f: () -> Unit) = queueWithPriority(f, 1)
        fun queueHighPriority(f: () -> Unit) = queueWithPriority(f, 10)

        override fun run() {
            while (true) {
                val job = backgroundQueue.take()
                job()
            }
        }
    }

    override fun compareTo(other: BackgroundJob): Int {
        return priority.compareTo(other.priority)
    }

    operator fun invoke() = f()
}