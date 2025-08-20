...
  /**
   *  reserve some time (by default 30 seconds at most)to let AuditEventKafkaSender to send out
   *  LoggingAuditEvent in the queue and gracefully stop AuditEventKafkaSender.
   */
  public synchronized void stop() {
    LOG.warn(
        "[{}] waits up to {} seconds to let [{}] send out LoggingAuditEvents left in the queue if"
            + " any.",
        Thread.currentThread().getName(), stopGracePeriodInSeconds, name);
    int i = 0;
    int numOfRounds = stopGracePeriodInSeconds / THREAD_SLEEP_IN_SECONDS;
    while (queue.size() > 0 && this.thread != null && thread.isAlive() && i < numOfRounds) {
      i += 1;
      try {
        synchronized(queue) {
          queue.wait(THREAD_SLEEP_IN_SECONDS * 1000);
        }
        CommonUtils.reportQueueUsage(queue.size(), queue.remainingCapacity(), host, stage.toString());
        LOG.info("In {} round, [{}] waited {} seconds and the current queue size is {}", i,
            Thread.currentThread().getName(), THREAD_SLEEP_IN_SECONDS, queue.size());
      } catch (InterruptedException e) {
        LOG.warn("[{}] got interrupted while waiting for [{}] to send out LoggingAuditEvents left "
            + "in the queue.", Thread.currentThread().getName(), name, e);
      }
    }
    cancelled.set(true);
    if (this.thread != null && thread.isAlive()) {
      thread.interrupt();
    }
    try {
      this.kafkaProducer.close();
    } catch (Throwable t) {
      LOG.warn("Exception is thrown while stopping {}.", name, t);
    }
    LOG.warn("[{}] is stopped and the number of LoggingAuditEvents left in the queue is {}.", name,
        queue.size());
  }
...
