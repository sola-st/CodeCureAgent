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
        this.stores.pushMetric(this.metricsRecord);
        //... all other pushMetric calls
    }

    private void addHLogAndHFileMetrics() {
        addHLogMetric(HLog.getWriteTime(), this.fsWriteLatency);
        //... all other addHLogMetric calls
    }

    private void pushLatencyMetrics() {
        this.fsPreadLatency.pushMetric(this.metricsRecord);
        //... all other pushMetric calls
    }
}
    public void doUpdates(MetricsContext caller) {
        MetricsUpdater updater = new MetricsUpdater(caller, this.metricsRecord);
        updater.updateMetrics();
    }