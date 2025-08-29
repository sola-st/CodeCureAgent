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
package com.pinterest.singer.e2e;

import com.pinterest.singer.utils.SingerTestHelper;

/**
 * In this end-to-end test, we write a number of messages to log files,
 * use Singer to upload those messages to singer_test_event topic in datakafka,
 * and verify the adds-up number in the associated audit topic equals to the number of messages
 * that we wrote to the log files.
 */
public class SingerEndToEndTest {

  private static final String SINGER_TEST_BASE_DIR = "/tmp/singer_e2e_test";
  private static final String SINGER_BINARY_DIR = SINGER_TEST_BASE_DIR + "/singer";
  private static final String SINGER_DATA_DIR = SINGER_TEST_BASE_DIR + "/data";
  private static final String SINGER_CONFIG_DIR = SINGER_TEST_BASE_DIR + "/config";
  private static final String SINGER_CONFIG_CONF_DIR = SINGER_CONFIG_DIR + "/conf.d";

  private static final String TEST_TOPIC = "singer_test_event";
  private static final String AUDIT_TOPIC = "singer_test_event_audit";

  private static int rotateThresholdKilobytes;
  private static int numTestMessages;
  private static int batchSize;
  private static int waitTimeInSeconds;

  private static void printUsageAndExit() {
    System.err.println("USAGE: java " + SingerEndToEndTest.class.getName()
        + " rotateThreshholdKiloBytes  batchSize   numTestMessages  waitTimeInSeconds");
    System.exit(1);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      printUsageAndExit();
    }

    try {
      rotateThresholdKilobytes = Integer.parseInt(args[0]);
      batchSize = Integer.parseInt(args[1]);
      numTestMessages = Integer.parseInt(args[2]);
      waitTimeInSeconds = Integer.parseInt(args[3]);
    } catch (Exception e) {
      printUsageAndExit();
    }

    SingerEndToEndTest test = new SingerEndToEndTest();
    test.testLoggingWithAudit();
  }


  public void testLoggingWithAudit() throws Exception {
    System.out.println("Testing Singer logging with auditing: ");
    long testStartTimestamp = System.currentTimeMillis();

    SingerAuditMessageReader auditMessageReader = new SingerAuditMessageReader(AUDIT_TOPIC, testStartTimestamp);
    auditMessageReader.start();

    // initialize the test environment
    System.out.println("Initialize the test environment");
    SingerTestHelper.e2eTestCleanup(SINGER_TEST_BASE_DIR, true);

    LogWriter logWriter = new LogWriter("singer_test_event",
        SINGER_DATA_DIR, rotateThresholdKilobytes, numTestMessages);
    Thread logWriterThread = new Thread(logWriter);
    logWriterThread.start();

    Thread.sleep(3000);

    SingerTestHelper.createSingerConfig(
        SINGER_CONFIG_DIR, SINGER_CONFIG_CONF_DIR, SINGER_DATA_DIR, "singer_test_event",
        batchSize, TEST_TOPIC, AUDIT_TOPIC, 60, "singer_heartbeat");

    // launch singer
    Process singerProc = SingerTestHelper
        .startSingerProcess(SINGER_BINARY_DIR, SINGER_CONFIG_DIR, SingerEndToEndTest.class);

    SingerOutputRetriever outputRetriever = new SingerOutputRetriever(singerProc.getErrorStream());
    outputRetriever.start();


    logWriterThread.join();

    for (int i = 0; i < waitTimeInSeconds; i++) {
      Thread.sleep(1000);
      System.out.println(". waiting seconds : " + i);
    }
    auditMessageReader.stop();

    singerProc.destroy();
    outputRetriever.stop();
    SingerTestHelper.e2eTestCleanup(SINGER_TEST_BASE_DIR, false);
    System.out.println(
        "count = " + auditMessageReader.getAuditCount() + "  numTestMessages = " + numTestMessages);
    assert (auditMessageReader.getAuditCount() == numTestMessages);
    if (auditMessageReader.getAuditCount() == numTestMessages) {
      System.out.println("Test succeeded! :)");
    } else {
      System.out.println("Test failed! :(");
    }
    System.exit(0);
  }
}

