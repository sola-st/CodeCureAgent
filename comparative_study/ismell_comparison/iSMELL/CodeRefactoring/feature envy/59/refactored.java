public class MetricsUpdater {
    private MetricsContext caller;
    private MetricsRecord metricsRecord;

    public MetricsUpdater(MetricsContext caller, MetricsRecord metricsRecord) {
        this.caller = caller;
        this.metricsRecord = metricsRecord;
    }

    public void updateMetrics() {
        synchronized (this) {
            this.lastUpdate = System.currentTimeMillis();

            if (this.extendedPeriod > 0 &&
                    this.lastUpdate - this.lastExtUpdate >= this.extendedPeriod) {
                resetMetrics();
            }

            pushMetrics();

            addHLogAndHFileMetrics();

            pushLatencyMetrics();
        }
        this.metricsRecord.update();
    }

    private void resetMetrics() {
        this.lastExtUpdate = this.lastUpdate;
        this.compactionTime.resetMinMaxAvg();
        this.compactionSize.resetMinMaxAvg();
        this.flushTime.resetMinMaxAvg();
        this.flushSize.resetMinMaxAvg();
        this.resetAllMinMax();
    }

    private void pushMetrics() {
        //... all other pushMetric calls
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
    }

    private void addHLogAndHFileMetrics() {
        addHLogMetric(HLog.getWriteTime(), this.fsWriteLatency);
        //... all other addHLogMetric calls
        addHLogMetric(HLog.getWriteSize(), this.fsWriteSize);
        addHLogMetric(HLog.getSyncTime(), this.fsSyncLatency);
        addHLogMetric(HLog.getSlowAppendTime(), this.slowHLogAppendTime);
    }

    private void pushLatencyMetrics() {
        //... all other pushMetric calls
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
}
    public void doUpdates(MetricsContext caller) {
        MetricsUpdater updater = new MetricsUpdater(caller, this.metricsRecord);
        updater.updateMetrics();
    }