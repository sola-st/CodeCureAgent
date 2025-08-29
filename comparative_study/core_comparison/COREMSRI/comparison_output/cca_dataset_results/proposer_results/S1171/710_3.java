package com.sonalake.utah;

import com.sonalake.utah.config.Config;
import com.sonalake.utah.config.ConfigLoader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A test to load up a file and confirm it parses ok
 */
public class ExamplesTest {

  private static final List<Map<String, String>> expectedResultsSampleFile = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsExampleCiscoVersion = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsExampleCiscoBgpSurvey = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsExampleF10IPBgpSurvey = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsExampleF10Version = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsJuniperBgpVersion = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsExampleJuniperVersion = new ArrayList<>();
  private static final List<Map<String, String>> expectedResultsExampleIfcfg = new ArrayList<>();

  static {
    // Initialize expectedResultsSampleFile
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("numberField", "123");
      m1.put("someOtherNumberField", "456");
      m1.put("stringFieldA", "what'll I do?");
      m1.put("stringFieldB", "without you?");
      m1.put("startOfInterval", "10/22/2015 12:00:00 AM");
      m1.put("endOfInterval", "12/31/2015 11:59:00 PM");
      expectedResultsSampleFile.add(m1);

      Map<String, String> m2 = new TreeMap<>();
      m2.put("numberField", "987");
      m2.put("someOtherNumberField", "654");
      m2.put("stringFieldA", "some fields are missing!");
      m2.put("startOfInterval", "10/22/2015 12:00:00 AM");
      expectedResultsSampleFile.add(m2);
    }

    // Initialize expectedResultsExampleCiscoVersion
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("version", "12.2(31)SGA1");
      m1.put("uptime", "3 days, 13 hours, 53 minutes");
      m1.put("reloadReason", "reload");
      m1.put("reloadTime", "05:09:09 PDT Wed Apr 2 2008");
      m1.put("imageFile", "bootflash:cat4500-entservicesk9-mz.122-31.SGA1.bin");
      m1.put("model", "WS-C4948-10GE");
      m1.put("memory", "262144K");
      m1.put("configRegister", "x2102");
      expectedResultsExampleCiscoVersion.add(m1);
    }

    // Initialize expectedResultsExampleCiscoBgpSurvey
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("localAS", "65550");
      m1.put("remoteAS", "65551");
      m1.put("remoteIp", "192.0.2.77");
      m1.put("routerId", "192.0.2.70");
      m1.put("status", "1");
      m1.put("uptime", "5w4d");
      expectedResultsExampleCiscoBgpSurvey.add(m1);

      Map<String, String> m2 = new TreeMap<>();
      m2.put("localAS", "65550");
      m2.put("remoteAS", "65552");
      m2.put("remoteIp", "192.0.2.78");
      m2.put("routerId", "192.0.2.70");
      m2.put("status", "10");
      m2.put("uptime", "5w4d");
      expectedResultsExampleCiscoBgpSurvey.add(m2);
    }

    // Initialize expectedResultsExampleF10IPBgpSurvey
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("localAS", "65551");
      m1.put("remoteAS", "65551");
      m1.put("remoteIp", "10.10.10.10");
      m1.put("routerId", "192.0.2.1");
      m1.put("status", "5");
      m1.put("uptime", "10:37:12");
      expectedResultsExampleF10IPBgpSurvey.add(m1);

      Map<String, String> m2 = new TreeMap<>();
      m2.put("localAS", "65551");
      m2.put("remoteAS", "65552");
      m2.put("remoteIp", "10.10.100.1");
      m2.put("routerId", "192.0.2.1");
      m2.put("status", "0");
      m2.put("uptime", "10:38:27");
      expectedResultsExampleF10IPBgpSurvey.add(m2);

      Map<String, String> m3 = new TreeMap<>();
      m3.put("localAS", "65551");
      m3.put("remoteAS", "65553");
      m3.put("remoteIp", "10.100.10.9");
      m3.put("routerId", "192.0.2.1");
      m3.put("status", "1");
      m3.put("uptime", "07:55:38");
      expectedResultsExampleF10IPBgpSurvey.add(m3);
    }

    // Initialize expectedResultsExampleF10Version
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("software", "7.7.1.1");
      m1.put("chassis", "E1200");
      m1.put("model", "E1200");
      m1.put("imageFile", "flash://FTOS-EF-7.7.1.1.bin");
      expectedResultsExampleF10Version.add(m1);
    }

    // Initialize expectedResultsJuniperBgpVersion
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("remoteIp", "10.247.68.182");
      m1.put("uptime", "6w3d17h");
      m1.put("activeV4", "4");
      m1.put("receivedV4", "5");
      m1.put("accepted_V4", "1");
      m1.put("activeV6", "0");
      m1.put("receivedV6", "0");
      m1.put("accepted_V6", "0");
      expectedResultsJuniperBgpVersion.add(m1);

      Map<String, String> m2 = new TreeMap<>();
      m2.put("remoteIp", "10.254.166.246");
      m2.put("uptime", "6w5d6h");
      m2.put("activeV4", "0");
      m2.put("receivedV4", "0");
      m2.put("accepted_V4", "0");
      m2.put("activeV6", "7");
      m2.put("receivedV6", "8");
      m2.put("accepted_V6", "1");
      expectedResultsJuniperBgpVersion.add(m2);

      Map<String, String> m3 = new TreeMap<>();
      m3.put("remoteIp", "192.0.2.100");
      m3.put("uptime", "9w5d6h");
      m3.put("activeV4", "1");
      m3.put("receivedV4", "2");
      m3.put("accepted_V4", "3");
      m3.put("activeV6", "4");
      m3.put("receivedV6", "5");
      m3.put("accepted_V6", "6");
      expectedResultsJuniperBgpVersion.add(m3);
    }

    // Initialize expectedResultsExampleJuniperVersion
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("model", "mx960");
      m1.put("junosOsBoot", "9.1S3.5");
      m1.put("junosOsSoftware", "9.1S3.5");
      m1.put("junosKernelSoftware", "9.1S3.5");
      m1.put("junosCryptoSoftware", "9.1S3.5");
      m1.put("junosPacketForwardMTCommon", "9.1S3.5");
      m1.put("junosPacketForwardMXCommon", "9.1S3.5");
      m1.put("junosOnlineDoc", "9.1S3.5");
      m1.put("junosRoutingSoftware", "9.1S3.5");
      expectedResultsExampleJuniperVersion.add(m1);
    }

    // Initialize expectedResultsExampleIfcfg
    {
      Map<String, String> m1 = new TreeMap<>();
      m1.put("interface", "lo0");
      m1.put("mtu", "16384");
      m1.put("inet6", "::1");
      m1.put("prefixlen", "128");
      m1.put("inet4", "127.0.0.1");
      m1.put("netmask", "0xff000000");
      expectedResultsExampleIfcfg.add(m1);

      Map<String, String> m2 = new TreeMap<>();
      m2.put("interface", "en0");
      m2.put("ether", "34:15:9e:27:45:e3");
      m2.put("mtu", "1500");
      m2.put("inet6", "2001:db8::3615:9eff:fe27:45e3");
      m2.put("prefixlen", "64");
      m2.put("inet4", "192.0.2.215");
      m2.put("netmask", "0xfffffe00");
      expectedResultsExampleIfcfg.add(m2);

      Map<String, String> m3 = new TreeMap<>();
      m3.put("interface", "en1");
      m3.put("ether", "90:84:0d:f6:d1:55");
      m3.put("mtu", "1500");
      expectedResultsExampleIfcfg.add(m3);
    }
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testSampleFile() throws IOException, IOException {
    String configResource = "sample.config.xml";
    String fileResource = "sample.import.txt";

    testFileProcessing(configResource, fileResource, expectedResultsSampleFile);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testExampleCiscoVersion() throws IOException, IOException {
    String configResource = "examples/cisco_version_template.xml";
    String fileResource = "examples/cisco_version_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsExampleCiscoVersion);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testExampleCiscoBgpSurvey() throws IOException, IOException {
    String configResource = "examples/cisco_bgp_summary_template.xml";
    String fileResource = "examples/cisco_bgp_summary_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsExampleCiscoBgpSurvey);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testExampleF10IPBgpSurvey() throws IOException, IOException {
    String configResource = "examples/f10_ip_bgp_summary_template.xml";
    String fileResource = "examples/f10_ip_bgp_summary_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsExampleF10IPBgpSurvey);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testExampleF10Version() throws IOException, IOException {
    String configResource = "examples/f10_version_template.xml";
    String fileResource = "examples/f10_version_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsExampleF10Version);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testJuniperBgpVersion() throws IOException, IOException {
    String configResource = "examples/juniper_bgp_summary_template.xml";
    String fileResource = "examples/juniper_bgp_summary_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsJuniperBgpVersion);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testExampleJuniperVersion() throws IOException, IOException {
    String configResource = "examples/juniper_version_template.xml";
    String fileResource = "examples/juniper_version_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsExampleJuniperVersion);
  }

  /**
   * In this test we load up the sample config and then process the sample input file, and then confirm the
   * data records are as expected.
   *
   * @throws IOException
   * 
   */
  @Test
  public void testExampleIfcfg() throws IOException, IOException {
    String configResource = "examples/unix_ifcfg_template.xml";
    String fileResource = "examples/unix_ifcfg_example.txt";

    testFileProcessing(configResource, fileResource, expectedResultsExampleIfcfg);
  }

  /**
   * Test file processing
   *
   * @param configResource  the resource name for a config
   * @param fileResource    the resource name for a file that is expected to match the config
   * @param expectedResults the json results expected from the processing
   * 
   * @throws IOException
   */
  private void testFileProcessing(String configResource, String fileResource, List<Map<String, String>>
    expectedResults) throws IOException, IOException {
    // load the config
    URL configURL = Thread.currentThread().getContextClassLoader().getResource(configResource);
    Config config = new ConfigLoader().loadConfig(configURL);

    // load a real file
    List<Map<String, String>> observedValues = new ArrayList<>();
    try (Reader in = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream
      (fileResource))) {
      Parser parser = Parser.parse(config, in);
      while (true) {
        Map<String, String> record = parser.next();
        if (null == record) {
          break;
        } else {
          observedValues.add(record);
        }
      }
    }

    Assert.assertEquals(expectedResults, observedValues);
  }

}