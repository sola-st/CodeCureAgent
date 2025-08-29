```java
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
package com.pinterest.singer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.pinterest.singer.common.SingerSettings;
import com.pinterest.singer.config.ConfigFileServerSet;
import com.pinterest.singer.config.DirectorySingerConfigurator;
import com.pinterest.singer.thrift.LogMessage;
import com.pinterest.singer.thrift.LogMessageAndPosition;
import com.pinterest.singer.thrift.LogPosition;
import com.pinterest.singer.thrift.configuration.FileNameMatchMode;
import com.pinterest.singer.thrift.configuration.SingerLogConfig;
import com.pinterest.singer.utils.TextLogger;
import com.pinterest.singer.utils.SimpleThriftLogger;

import junit.framework.TestCase;

/**
 * Base class for Singer tests.
 */
public class SingerTestBase extends TestCase {

  private static String OS = System.getProperty("os.name").toLowerCase();

  static {
    ConfigFileServerSet.SERVERSET_DIR = "target";
  }
  
  private static boolean isMac() {
    return (OS.indexOf("mac") >= 0);
  }

  public static long FILE_EVENT_WAIT_TIME_MS = isMac() ? 3000 : 200;

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();
  protected File logConfigDir;
  private Random random;

  private byte[] getRandomByteArray(int size) {
    byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return bytes;
  }

  protected LogMessageAndPosition writeThriftLogMessage(
      SimpleThriftLogger<LogMessage> logger, int keySize, int messageSize) throws Exception {
    LogMessage message = new LogMessage(ByteBuffer.wrap(getRandomByteArray(messageSize)));
    message.setKey(getRandomByteArray(keySize));
    logger.logThrift(message);
    logger.flush();
    return new LogMessageAndPosition(
            message, new LogPosition(logger.getLogFile(), logger.getByteOffset()));
  }

  protected LogMessageAndPosition writeThriftLogMessage(
          SimpleThriftLogger<LogMessage> logger, int messageSize) throws Exception {
    LogMessage message = new LogMessage(ByteBuffer.wrap(getRandomByteArray(messageSize)));
    logger.logThrift(message);
    logger.flush();
    return new LogMessageAndPosition(
            message, new LogPosition(logger.getLogFile(), logger.getByteOffset()));
  }

  protected List<LogMessageAndPosition> writeThriftLogMessages(
      SimpleThriftLogger<LogMessage> logger, int n, int keySize, int messageSize) throws Exception {
    List<LogMessageAndPosition> logMessages = Lists.newArrayListWithExpectedSize(n);
    for (int j = 0; j < n; ++j) {
      logMessages.add(writeThriftLogMessage(logger, keySize, messageSize));
    }
    return logMessages;
  }

  protected List<LogMessageAndPosition> writeThriftLogMessages(
          SimpleThriftLogger<LogMessage> logger, int n, int messageSize) throws Exception {
    List<LogMessageAndPosition> logMessages = Lists.newArrayListWithExpectedSize(n);
    for (int j = 0; j < n; ++j) {
      logMessages.add(writeThriftLogMessage(logger, messageSize));
    }
    return logMessages;
  }

  protected long writeTextLog(
      TextLogger logger, String text) throws Exception {
    long byteOffset = logger.getByteOffset();
    logger.logText(text);
    return byteOffset;
  }

  protected void rotateWithDelay(SimpleThriftLogger<LogMessage> logger, long delayInMillis)
      throws InterruptedException, IOException {
    // Sleep for a given period of time as file's lastModified timestamp is up to seconds on some platforms.
    Thread.sleep(delayInMillis);
    logger.rotate();
  }

  protected SingerLogConfig createSingerLogConfig(String name, String path) {
    SingerLogConfig config = new SingerLogConfig();
    config.setName(name);
    config.setLogDir(path);
    config.setFilenameMatchMode(FileNameMatchMode.PREFIX);
    return config;
  }

  /**
   * Create a list of test log files, in the formm like
   *     test.tmp.9, test.tmp.8, test.tmp.7, ..., test.tmp.2, test.tmp.1, test.tmp
   */
  protected File[] createTestLogStreamFiles(File testDir, String fileNamePrefix, int numFiles)
      throws IOException, InterruptedException {
    File[] created = new File[numFiles];
    for (int i = 0; i < numFiles - 1; i++) {
      created[i] = new File(testDir + "/" + fileNamePrefix + "." + (numFiles - i - 1));
      if (!created[i].createNewFile()) {
        System.out.println("Failed to create new file : " + created[i]);
        fail();
      }
      byte[] bytes = new String("This is a test string").getBytes();
      Files.write(created[i].toPath(), bytes);
      Thread.sleep(FILE_EVENT_WAIT_TIME_MS);
    }
    Thread.sleep(FILE_EVENT_WAIT_TIME_MS);
    created[numFiles - 1] = new File(testDir + "/" + fileNamePrefix);
    if (!created[numFiles - 1].createNewFile()) {
      System.out.println("Failed to create new file : " + created[numFiles - 1]);
      fail();
    }
    byte[] bytes = new String("This is a test string").getBytes();
    Files.write(created[numFiles - 1].toPath(), bytes);
    Thread.sleep(FILE_EVENT_WAIT_TIME_MS);
    return created;
  }

  @Before
  public void setUp() throws Exception {
    tempDir.create();
    logConfigDir = tempDir.newFolder(DirectorySingerConfigurator.SINGER_LOG_CONFIG_DIR);
    random = new Random();
  }

  @After
  public void tearDown() throws IOException {
    File rootDir = tempDir.getRoot();

    File[] files = logConfigDir.listFiles();
    for (File f : files) {
      f.delete();
    }

    File f = new File(FilenameUtils.concat(rootDir.getPath(),
        DirectorySingerConfigurator.DATAPIPELINES_CONFIG));
    if (f.exists()) {
      f.delete();
    }
    
    SingerSettings.reset();
  }

  protected String getTempPath() {
    return tempDir.getRoot().getPath();
  }

  /**
   * Create a properties files with given name and key-value property map.
   */
  protected File createLogConfigPropertiesFile(String name) throws IOException {
    return createLogConfigPropertiesFile(name, ImmutableMap.of());
  }

  protected File createLogConfigPropertiesFile(String name, Map<String, String> overrides)
      throws IOException {
    Map<String, String> propertyMap = makeCommonLogConfigProperties();
    propertyMap.putAll(overrides);
    return createPropertyFile(FilenameUtils.concat(logConfigDir.getPath(), name), propertyMap);
  }

  protected String createNewLogConfigString() {
    return createPropertyString(makeCommonNewLogConfigProperties());
  }

  protected File createSingerConfigFile(Map<String, String> keyValues)
      throws IOException {
    return createPropertyFile(FilenameUtils.concat(tempDir.getRoot().getPath(),
        DirectorySingerConfigurator.SINGER_CONFIGURATION_FILE), keyValues);
  }

  protected File createPropertyFile(String fullPath, Map<String, String> keyValues) throws
                                                                                    IOException {
    File properties = new File(fullPath);
    BufferedWriter out = new BufferedWriter(new FileWriter(properties));
    for (String key : keyValues.keySet()) {
      out.write(String.format("%s = %s\n", key, keyValues.get(key)));
    }
    out.close();
    return properties;
  }

  protected Map<String, String> makeCommonLogConfigProperties() {
    Map<String, String> map = new TreeMap<String, String>();
    map.put("logDir", "/mnt/log/singer/");
    map.put("logStreamRegex", "mohawk_(\\\\w+)");

    map.put("processor.batchSize", "200");
    map.put("processor.processingIntervalInSeconds", "10");

    map.put("reader.type", "thrift");
    map.put("reader.thrift.readerBufferSize", "16384");
    map.put("reader.thrift.maxMessageSize", "16484");

    map.put("writer.type", "kafka");
    map.put("writer.kafka.topic", "singer_\\\\1");
    map.put("writer.kafka.auditingEnabled", "false");
    // we need this line to cheat the producer parser, otherwise it tries to resolve server set.
    map.put("writer.kafka.producerConfig.metadata.broker.list", "broken_list_for_testing");
    map.put("writer.kafka.producerConfig.metadata.broker.serverset", "/discovery/m10nkafka/prod");
    map.put("writer.kafka.producerConfig.broker.serverset", "/discovery/m10nkafka/prod");
    map.put("writer.kafka.producerConfig.acks", "1");
    map.put("writer.kafka.producerConfig.partitioner.class",
        "com.pinterest.singer.writer.Crc32ByteArrayPartitioner");
    return map;
  }

  protected Map<String, String> makeCommonNewLogConfigProperties () {
    Map<String, String> map = new TreeMap<String, String>();
    map.put("topic_names", "topic1, topic2");

    map.put("topic1.name", "topic1");
    map.put("topic1.owner", "owner1");
    map.put("topic1.type", "JSON");
    map.put("topic1.daily_datavolume", "1024");
    // we need this line to cheat the producer parser, otherwise it tries to resolve server set.
    map.put("topic1.writer.kafka.producerConfig.bootstrap.servers", "bootstrap_brokers");
    map.put("topic1.processor.batchSize", "200");

    map.put("topic2.name", "topic2");
    map.put("topic2.owner", "owner2");
    map.put("topic2.type", "JSON");
    map.put("topic2.daily_datavolume", "2048");
    // we need this line to cheat the producer parser, otherwise it tries to resolve server set.
    map.put("topic2.writer.kafka.producerConfig.bootstrap.servers", "broken_list_for_testing");
    map.put("topic2.processor.batchSize", "200");

    map.put("singer.default.processor.processingIntervalInSeconds", "1");
    map.put("singer.default.reader.type", "thrift");
    map.put("singer.default.reader.thrift.readerBufferSize", "524288");
    map.put("singer.default.reader.thrift.maxMessageSize", "524288");
    map.put("singer.default.writer.type", "kafka");
    map.put("singer.default.writer.kafka.producerConfig.acks", "1");
    map.put("singer.default.writer.kafka.producerConfig.ssl.enabled", "true");
    map.put("singer.default.writer.kafka.producerConfig.ssl.client.auth", "required");
    map.put("singer.default.writer.kafka.producerConfig.ssl.enabled.protocols", "TLSv1.2,TLSv1.1,TLSv1");
    map.put("singer.default.writer.kafka.producerConfig.ssl.endpoint.identification.algorithm", "HTTPS");
    map.put("singer.default.writer.kafka.producerConfig.ssl.key.password", "key_password");
    map.put("singer.default.writer.kafka.producerConfig.ssl.keystore.location", "keystore_path");
    map.put("singer.default.writer.kafka.producerConfig.ssl.keystore.password", "keystore_password");
    map.put("singer.default.writer.kafka.producerConfig.ssl.keystore.type", "JKS");
    map.put("singer.default.writer.kafka.producerConfig.ssl.secure.random.implementation", "SHA1PRNG");
    map.put("singer.default.writer.kafka.producerConfig.ssl.truststore.location", "truststore_path");
    map.put("singer.default.writer.kafka.producerConfig.ssl.truststore.password", "truststore_password");
    map.put("singer.default.writer.kafka.producerConfig.ssl.truststore.type", "JKS");
    map.put("singer.default.logDir", "/mnt/thrift_logger/");
    return map;
  }

  private static String createPropertyString (Map<String, String> properties) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      sb.append(String.format("%s=%s\n", key, value));
    }
    return sb.toString();
  }

  protected void dumpServerSetFiles() throws IOException {
    PrintWriter pr = new PrintWriter("target/server_set_files");
    pr.println("127.0.0.1:9092");
    pr.close();
  }
}

