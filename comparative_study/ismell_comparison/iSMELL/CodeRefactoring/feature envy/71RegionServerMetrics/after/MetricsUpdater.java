package org.apache.hadoop.hbase.regionserver.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.io.hfile.HFile;
import org.apache.hadoop.hbase.metrics.HBaseInfo;
import org.apache.hadoop.hbase.metrics.PersistentMetricsTimeVaryingRate;
import org.apache.hadoop.hbase.regionserver.wal.HLog;
import org.apache.hadoop.hbase.util.Pair;
import org.apache.hadoop.hbase.util.Strings;
import org.apache.hadoop.metrics.ContextFactory;
import org.apache.hadoop.metrics.MetricsContext;
import org.apache.hadoop.metrics.MetricsRecord;
import org.apache.hadoop.metrics.MetricsUtil;
import org.apache.hadoop.metrics.Updater;
import org.apache.hadoop.metrics.jvm.JvmMetrics;
import org.apache.hadoop.metrics.util.MetricsIntValue;
import org.apache.hadoop.metrics.util.MetricsLongValue;
import org.apache.hadoop.metrics.util.MetricsRegistry;
import org.apache.hadoop.metrics.util.MetricsTimeVaryingRate;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;

public class MetricsUpdater {
    private MetricsContext metricsContext;

    public MetricsUpdater(MetricsContext metricsContext) {
        this.metricsContext = metricsContext;
    }

    public void resetAllMinMax() {
        this.metricsContext.compactionTime.resetMinMaxAvg();
        this.metricsContext.compactionSize.resetMinMaxAvg();
        this.metricsContext.flushTime.resetMinMaxAvg();
        this.metricsContext.flushSize.resetMinMaxAvg();
    }

    public void doUpdates() {
        synchronized (this) {
            this.metricsContext.lastUpdate = System.currentTimeMillis();

            if (this.metricsContext.extendedPeriod > 0 &&
                this.metricsContext.lastUpdate - this.metricsContext.lastExtUpdate >= this.metricsContext.extendedPeriod) {
                this.metricsContext.lastExtUpdate = this.metricsContext.lastUpdate;
                this.metricsContext.compactionTime.resetMinMaxAvg();
                this.metricsContext.compactionSize.resetMinMaxAvg();
                this.metricsContext.flushTime.resetMinMaxAvg();
                this.metricsContext.flushSize.resetMinMaxAvg();
                resetAllMinMax();
            }

            this.metricsContext.stores.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.storefiles.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.storefileIndexSizeMB.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.memstoreSizeMB.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.regions.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.requests.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.compactionQueueSize.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.flushQueueSize.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheSize.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheFree.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheCount.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheHitCount.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheMissCount.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheEvictedCount.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheHitRatio.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.blockCacheHitCachingRatio.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.compactionTime.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.compactionSize.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.flushTime.pushMetric(this.metricsContext.metricsRecord);
            this.metricsContext.flushSize.pushMetric(this.metricsContext.metricsRecord);
        }
        this.metricsContext.metricsRecord.update();
    }
}

