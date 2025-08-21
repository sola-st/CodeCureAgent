/**
 * Copyright 2019 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.singer.monitor;

import com.pinterest.singer.common.LogMonitor;
import com.pinterest.singer.common.errors.LogMonitorException;
import com.pinterest.singer.common.LogStream;
import com.pinterest.singer.common.LogStreamProcessor;
import com.pinterest.singer.common.LogStreamReader;
import com.pinterest.singer.common.errors.LogStreamReaderException;
import com.pinterest.singer.common.LogStreamWriter;
import com.pinterest.singer.common.errors.LogStreamWriterException;
import com.pinterest.singer.common.SingerMetrics;
import com.pinterest.singer.metrics.OpenTsdbMetricConverter;
import com.pinterest.singer.processor.DefaultLogStreamProcessor;
import com.pinterest.singer.processor.MemoryEfficientLogStreamProcessor;
import com.pinterest.singer.reader.DefaultLogStreamReader;
import com.pinterest.singer.reader.TextLogFileReaderFactory;
import com.pinterest.singer.reader.ThriftLogFileReaderFactory;
import com.pinterest.singer.thrift.configuration.NoOpWriteConfig;
import com.pinterest.singer.thrift.configuration.KafkaProducerConfig;
import com.pinterest.singer.thrift.configuration.KafkaWriterConfig;
import com.pinterest.singer.thrift.configuration.LogStreamProcessorConfig;
import com.pinterest.singer.thrift.configuration.LogStreamReaderConfig;
import com.pinterest.singer.thrift.configuration.LogStreamWriterConfig;
import com.pinterest.singer.thrift.configuration.SingerConfig;
import com.pinterest.singer.thrift.configuration.SingerLogConfig;
import com.pinterest.singer.thrift.configuration.SingerRestartConfig;
import com.pinterest.singer.thrift.configuration.TextReaderConfig;
import com.pinterest.singer.thrift.configuration.ThriftReaderConfig;
import com.pinterest.singer.utils.SingerUtils;
import com.pinterest.singer.writer.NoOpLogStreamWriter;
import com.pinterest.singer.writer.KafkaWriter;
import com.pinterest.singer.writer.kafka.CommittableKafkaWriter;
import com.pinterest.singer.writer.pulsar.PulsarWriter;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.twitter.ostrich.stats.Stats;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The default implementation of LogMonitor.
 * <p>
 * This default implementation of LogMonitor monitors for LogStreams in all configured SingerLogs.
 * It periodically wakes up and discovers all LogStreams in one SingerLog. If any LogStream is not
 * processed, it will start a DefaultLogStreamProcessor to process the LogStream.
 * <p>
 * This class is not thread-safe. monitorLogs() method does all the monitoring tasks and should
 * only be called from one thread at any time. The start() and stop() methods can be called in other
 * threads to start and stop the monitor.
 * <p>
 * TODO(wangxd): close processors for LogStreams in those logs that are no longer monitored.
 */
public class DefaultLogMonitor implements LogMonitor, Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLogMonitor.class);
  public static final String HOSTNAME = SingerUtils.getHostname();

  protected static LogMonitor INSTANCE;

  // If a stream hasn't been processed for this long time, it is considered stuck. Currently 1 min.
  private static final long MINIMUM_STUCK_STREAM_AGE_SECS = 60 * 10;

  // Monitoring interval in seconds.
  private final int monitorIntervalInSecs;

  // Map from LogStreams to their processors.
  private final Map<LogStream, LogStreamProcessor> processedLogStreams;

  // Whether this monitor is stopped. This should be accessed under the lock on "isStopped".
  private Boolean isStopped;

  // Handle to the next monitor run. This should be accessed under the lock on "isStopped".
  private ScheduledFuture<?> scheduledFuture;
  
  /**
   * The executor service for executing logMonitor thread. This needs to be independent of the
   * log processor. Otherwise, under heavy workload, logMonitor may not get cycle to run.
   */
  private ScheduledExecutorService logMonitorExecutor;

  private boolean dailyRestart = false;

  private long restartTimeInMillis = Long.MAX_VALUE;

  /**
   * Constructor.
   *
   * @param monitorIntervalInSecs monitor interval in seconds.
   * @param singerConfig          the SingerConfig.
   */
  protected DefaultLogMonitor(int monitorIntervalInSecs,
                              SingerConfig singerConfig)
      throws ConfigurationException {
    Preconditions.checkArgument(monitorIntervalInSecs > 0);
    this.monitorIntervalInSecs = monitorIntervalInSecs;
    this.processedLogStreams = Maps.newHashMap();
    this.isStopped = true;
    this.scheduledFuture = null;
    this.logMonitorExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("LogMonitor").build());
    if (singerConfig.isSetSingerRestartConfig() && singerConfig.singerRestartConfig.restartDaily) {
      dailyRestart = true;
      setDailyRestartTime(singerConfig.singerRestartConfig);
    }
  }

  /**
   * The configuration sets the daily restart time range. This method randomly picks up a time
   * during that range within the next 24 hours. By using randomly selected time in a range,
   * we can avoid singer on all hosts restart at the same time.
   *
   * @param restartConfig  the restart configuration
   * @throws ParseException
   */
  private void setDailyRestartTime(SingerRestartConfig restartConfig)
      throws ConfigurationException {
    Calendar c = new GregorianCalendar();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    Date date = c.getTime();
    Date startTime = SingerUtils.convertToDate(restartConfig.dailyRestartUtcTimeRangeBegin);
    Date endTime = SingerUtils.convertToDate(restartConfig.dailyRestartUtcTimeRangeEnd);
    Random rand = new Random(SingerUtils.getHostname().hashCode());
    long randomMillis = rand.nextInt((int) endTime.getTime() - (int) startTime.getTime() + 1)
        + startTime.getTime();
    restartTimeInMillis = date.getTime() + randomMillis;
    if (restartTimeInMillis < System.currentTimeMillis()) {
      restartTimeInMillis += 86400 * 1000;
    }
  }

  public static LogMonitor getInstance(int monitorIntervalInSecs, 
                                       SingerConfig singerConfig)
      throws ConfigurationException {
    if (INSTANCE == null) {
      synchronized (DefaultLogMonitor.class) {
        if (INSTANCE == null) {
          INSTANCE = new DefaultLogMonitor(monitorIntervalInSecs, singerConfig);
        }
      }
    }
    return INSTANCE;
  }

  /**
   * Monitor all SingerLogs configured.
   * <p>
  private final Object stopLock = new Object();

  /**
   * Start to periodically monitor LogStreams in all configured logs.
   * <p>
   * This method is thread safe.
   */
  @Override
  public void start() {
    synchronized (stopLock) {
      if (isStopped) {
        scheduledFuture = logMonitorExecutor.scheduleAtFixedRate(
            this, 0, monitorIntervalInSecs, TimeUnit.SECONDS);
        isStopped = false;
        LOG.info("Start log monitor which monitor logs every {} seconds.", monitorIntervalInSecs);
      } else {
        LOG.warn("LogMonitor already started when asked to start.");
      }
    }
  }

  /**
   * Stop monitoring and processing of LogStreams in all configured logs.
   * <p>
   * This method is thread-safe.
   */
  public void stop() {
    synchronized (stopLock) {
      if (!isStopped) {
        // Stop processing of current LogStreams.
        stopMonitoredLogs();

        Preconditions.checkNotNull(scheduledFuture, "LogMonitor is not running");
        // Do not interrupt since the log monitor is running now.
        scheduledFuture.cancel(false);

        // Wait until last run is done.
        try {
          scheduledFuture.get();
        } catch (InterruptedException e) {
          LOG.error("Interrupted while waiting", e);
        } catch (ExecutionException e) {
          // Ignore any exception from the scheduled run.
          LOG.error("Caught exception from the last monitoring cycle", e);
        } catch (CancellationException e) {
          // this is an expected exception
        } catch (Exception e) {
          LOG.error("Caught unexpected exception", e);
          Stats.incr("singer.monitor.unexpected_exception");
        }
        isStopped = true;
        
        // stop the thread pool
        logMonitorExecutor.shutdown();
        
        LOG.info("Stopped log monitor.");
      } else {
        LOG.warn("LogMonitor already stopped when asked to stop.");
      }
    }
  }

      LogStreamProcessor processor = entry.getValue();
      processor.stop();
      try {
        processor.close();
        LOG.info("Stop and close processor for log stream: {}", logStream);
      } catch (IOException e) {
        LOG.error("Failed to close processor for log stream: {}", logStream);
      }
      // Remove the LogStream from processed LogStreams.
      it.remove();
    }
  }

  /**
   * Expand a name by replacing placeholder (such as \1, \2) in the name with captured group
   * from LogStream name.
   *
   * @param logStreamName the LogStream name this string will be expanded in.
   * @param streamRegex   the stream name regex.
   * @param name          the name to be expanded.
   * @return expanded name with placeholder replaced.
   * @throws ConfigurationException
   */
  public static String extractTopicNameFromLogStreamName(
      String logStreamName, String streamRegex, String name) throws ConfigurationException {
    try {
      Pattern pattern = Pattern.compile(streamRegex);
      Matcher logStreamNameMatcher = pattern.matcher(logStreamName);
      Preconditions.checkState(logStreamNameMatcher.matches());

      // Replace all group numbers in "name" with the groups from logStreamName.
      Pattern p = Pattern.compile("\\\\(\\d{1})");
      Matcher groupMatcher = p.matcher(name);
      StringBuffer sb = new StringBuffer();
      while (groupMatcher.find()) {
        groupMatcher.appendReplacement(
            sb,
            logStreamNameMatcher.group(Integer.parseInt(groupMatcher.group(1))));
      }
      groupMatcher.appendTail(sb);
      return sb.toString();
    } catch (NumberFormatException e) {
      throw new ConfigurationException("Cannot expand " + name + " in log stream " + logStreamName,
          e);
    }
  }
}