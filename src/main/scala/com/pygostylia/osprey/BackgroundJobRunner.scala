package com.pygostylia.osprey

import java.util.concurrent.PriorityBlockingQueue

object BackgroundJobRunner extends Runnable {
  case class BackgroundJob(val priority: Int, fn: () => Unit)

  private val queue = new PriorityBlockingQueue[BackgroundJob]()

  override def run(): Unit = {
    while (true) {
      val task = queue.take()
    }
  }

  def start(): Unit = new Thread(BackgroundJobRunner).start()

  def queueWithPriority(priority: Int, fn: () => Unit): Boolean = {
    queue.add(new BackgroundJob(priority, fn))
  }

  def queue(fn: () => Unit): Boolean = queueWithPriority(1, fn)

  def queueHighPriority(fn: () => Unit): Boolean = queueWithPriority(10, fn)
}