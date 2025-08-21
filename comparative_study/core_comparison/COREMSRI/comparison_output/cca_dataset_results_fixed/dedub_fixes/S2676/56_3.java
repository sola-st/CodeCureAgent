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
package com.pinterest.singer.processor;

import com.pinterest.singer.common.LogStream;
import com.pinterest.singer.common.LogStreamProcessor;
import com.pinterest.singer.common.errors.LogStreamProcessorException;
import com.pinterest.singer.common.LogStreamReader;
import com.pinterest.singer.common.errors.LogStreamReaderException;
import com.pinterest.singer.common.LogStreamWriter;
import com.pinterest.singer.common.errors.LogStreamWriterException;
import com.pinterest.singer.common.SingerMetrics;
import com.pinterest.singer.common.SingerSettings;
import com.pinterest.singer.config.Decider;
import com.pinterest.singer.metrics.OpenTsdbMetricConverter;
import com.pinterest.singer.thrift.LogFileAndPath;
import com.pinterest.singer.thrift.LogMessage;
import com.pinterest.singer.thrift.LogMessageAndPosition;
import com.pinterest.singer.thrift.LogPosition;
import com.pinterest.singer.utils.LogConfigUtils;
import com.pinterest.singer.utils.WatermarkUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.twitter.ostrich.stats.Stats;
import org.apache.commons.io.FilenameUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of LogStreamProcessor which periodically wakes up and processes the
 * LogStream till the current end of it.
 * <p/>
 * This class is not thread-safe. processLogStream() method does all the processing job and
 * should only be called from one thread at any time. The start() and stop() methods can be called
 * in other threads to start and stop the processor .
 */
public class DefaultLogStreamProcessor implements LogStreamProcessor, Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLogStreamProcessor.class);

  // Decider for the log stream.
  private final String logDecider;

  // LogStream to be processed.
  protected final LogStream logStream;

  // Reader for the LogStream.
  protected final LogStreamReader reader;

  // Writer for the LogStream.
  protected final LogStreamWriter writer;

  // Processor batch size.
  protected int batchSize;
  private final int batchSizeOriginal;

  // Randomizer for initial processing delay.
  private final Random random;

  // Processor process interval in milliseconds.
  private long processingIntervalInMillis;

  // Processor process interval in milliseconds.
  private final long processingIntervalInMillisMin;

  // Processor process interval in milliseconds.
  private final long processingIntervalInMillisMax;

  // Processor process time slice in milliseconds.
  private final long processingTimeSliceInMilliseconds;

  // a boolean flag on whether LogStremaProcess uses up the time slice or not
  private boolean exceedTimeSliceLimit;

  // the log retention time in seconds
  private final int logRetentionInSecs;

  // Executor which executes processing tasks.
  private final ScheduledExecutorService executorService;

  // Whether this processor is stopped.
```java
        long initialDelay = random.nextLong() == Long.MIN_VALUE ? 0 : Math.abs(random.nextLong()) % processingIntervalInMillis;

   */
  private void writeLogMessages(List<LogMessageAndPosition> logMessagesRead)
      throws LogStreamWriterException {
    int numMessages = logMessagesRead.size();
    if (numMessages <= 0) {
      return;
    }
    List<LogMessage> logMessagesToWrite = Lists.newArrayListWithExpectedSize(numMessages);
    for (LogMessageAndPosition logMessageRead : logMessagesRead) {
      LogMessage logMessage = logMessageRead.getLogMessage();
      logMessagesToWrite.add(logMessage);
      emitMessageSizeMetrics(logStream, logMessage);
    }
    writer.writeLogMessages(logMessagesToWrite);
    LogMessage lastMessage = logMessagesToWrite.get(numMessages - 1);
    if (lastMessage.isSetTimestampInNanos()) {
      logStream.setLatestProcessedMessageTime(lastMessage.getTimestampInNanos() / 1000000);
    }
  }

  /**
   * Commit the specified LogPosition.
   *
   * @param position   LogPosition to be committed.
   * @param persistent Whether the position should be saved to watermark file.
   * @throws Exception when fail to commit the LogPosition.
   */
  protected void commitLogPosition(LogPosition position, boolean persistent)
      throws IOException, TException {
    this.committedPosition = position;
    if (persistent) {
      WatermarkUtils.saveCommittedPositionToWatermark(
          getWatermarkFilename(this.logStream), this.committedPosition);
    }
  }
}