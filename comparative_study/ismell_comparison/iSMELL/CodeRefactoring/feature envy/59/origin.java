public void doUpdates(MetricsContext caller) {
synchronized (this) {
        this.lastUpdate = System.currentTimeMillis();

        // has the extended period for long-living stats elapsed?
        if (this.extendedPeriod > 0 &&
        this.lastUpdate - this.lastExtUpdate >= this.extendedPeriod) {
        this.lastExtUpdate = this.lastUpdate;
        this.compactionTime.resetMinMaxAvg();
        this.compactionSize.resetMinMaxAvg();
        this.flushTime.resetMinMaxAvg();
        this.flushSize.resetMinMaxAvg();
        this.resetAllMinMax();
        }

        this.stores.pushMetric(this.metricsRecord);
        this.storefiles.pushMetric(this.metricsRecord);
        this.hlogFileCount.pushMetric(this.metricsRecord);
        this.hlogFileSizeMB.pushMetric(this.metricsRecord);
        this.storefileIndexSizeMB.pushMetric(this.metricsRecord);
        this.rootIndexSizeKB.pushMetric(this.metricsRecord);
        this.totalStaticIndexSizeKB.pushMetric(this.metricsRecord);
        this.totalStaticBloomSizeKB.pushMetric(this.metricsRecord);
        this.memstoreSizeMB.pushMetric(this.metricsRecord);
        this.mbInMemoryWithoutWAL.pushMetric(this.metricsRecord);
        this.numPutsWithoutWAL.pushMetric(this.metricsRecord);
        this.readRequestsCount.pushMetric(this.metricsRecord);
        this.writeRequestsCount.pushMetric(this.metricsRecord);
        this.regions.pushMetric(this.metricsRecord);
        this.requests.pushMetric(this.metricsRecord);
        this.compactionQueueSize.pushMetric(this.metricsRecord);
        this.flushQueueSize.pushMetric(this.metricsRecord);
        this.blockCacheSize.pushMetric(this.metricsRecord);
        this.blockCacheFree.pushMetric(this.metricsRecord);
        this.blockCacheCount.pushMetric(this.metricsRecord);
        this.blockCacheHitCount.pushMetric(this.metricsRecord);
        this.blockCacheMissCount.pushMetric(this.metricsRecord);
        this.blockCacheEvictedCount.pushMetric(this.metricsRecord);
        this.blockCacheHitRatio.pushMetric(this.metricsRecord);
        this.blockCacheHitCachingRatio.pushMetric(this.metricsRecord);
        this.hdfsBlocksLocalityIndex.pushMetric(this.metricsRecord);
        this.blockCacheHitRatioPastNPeriods.pushMetric(this.metricsRecord);
        this.blockCacheHitCachingRatioPastNPeriods.pushMetric(this.metricsRecord);

        // Mix in HFile and HLog metrics
        // Be careful. Here is code for MTVR from up in hadoop:
        // public synchronized void inc(final int numOps, final long time) {
        //   currentData.numOperations += numOps;
        //   currentData.time += time;
        //   long timePerOps = time/numOps;
        //    minMax.update(timePerOps);
        // }
        // Means you can't pass a numOps of zero or get a ArithmeticException / by zero.
        // HLog metrics
        addHLogMetric(HLog.getWriteTime(), this.fsWriteLatency);
        addHLogMetric(HLog.getWriteSize(), this.fsWriteSize);
        addHLogMetric(HLog.getSyncTime(), this.fsSyncLatency);
        addHLogMetric(HLog.getSlowAppendTime(), this.slowHLogAppendTime);
        this.slowHLogAppendCount.set(HLog.getSlowAppendCount());
        // HFile metrics, sequential reads
        int ops = HFile.getReadOps();
        if (ops != 0) this.fsReadLatency.inc(ops, HFile.getReadTimeMs());
        // HFile metrics, positional reads
        ops = HFile.getPreadOps();
        if (ops != 0) this.fsPreadLatency.inc(ops, HFile.getPreadTimeMs());
        this.checksumFailuresCount.set(HFile.getChecksumFailuresCount());

        /* NOTE: removed HFile write latency.  2 reasons:
         * 1) Mixing HLog latencies are far higher priority since they're
         *      on-demand and HFile is used in background (compact/flush)
         * 2) HFile metrics are being handled at a higher level
         *      by compaction & flush metrics.
         */

        for(Long latency : HFile.getReadLatenciesNanos()) {
        this.fsReadLatencyHistogram.update(latency);
        }
        for(Long latency : HFile.getPreadLatenciesNanos()) {
        this.fsPreadLatencyHistogram.update(latency);
        }
        for(Long latency : HFile.getWriteLatenciesNanos()) {
        this.fsWriteLatencyHistogram.update(latency);
        }


        // push the result
        this.fsPreadLatency.pushMetric(this.metricsRecord);
        this.fsReadLatency.pushMetric(this.metricsRecord);
        this.fsWriteLatency.pushMetric(this.metricsRecord);
        this.fsWriteSize.pushMetric(this.metricsRecord);

        this.fsReadLatencyHistogram.pushMetric(this.metricsRecord);
        this.fsWriteLatencyHistogram.pushMetric(this.metricsRecord);
        this.fsPreadLatencyHistogram.pushMetric(this.metricsRecord);

        this.fsSyncLatency.pushMetric(this.metricsRecord);
        this.compactionTime.pushMetric(this.metricsRecord);
        this.compactionSize.pushMetric(this.metricsRecord);
        this.flushTime.pushMetric(this.metricsRecord);
        this.flushSize.pushMetric(this.metricsRecord);
        this.slowHLogAppendCount.pushMetric(this.metricsRecord);
        this.regionSplitSuccessCount.pushMetric(this.metricsRecord);
        this.regionSplitFailureCount.pushMetric(this.metricsRecord);
        this.checksumFailuresCount.pushMetric(this.metricsRecord);
        this.updatesBlockedSeconds.pushMetric(this.metricsRecord);
        this.updatesBlockedSecondsHighWater.pushMetric(this.metricsRecord);
        }
        this.metricsRecord.update();
}