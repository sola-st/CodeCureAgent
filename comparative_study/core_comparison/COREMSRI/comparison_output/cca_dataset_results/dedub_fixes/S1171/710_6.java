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
    // expectedResultsSampleFile initialization
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("numberField", "123");
      map1.put("someOtherNumberField", "456");
      map1.put("stringFieldA", "what'll I do?");
      map1.put("stringFieldB", "without you?");
      map1.put("startOfInterval", "10/22/2015 12:00:00 AM");
      map1.put("endOfInterval", "12/31/2015 11:59:00 PM");
      expectedResultsSampleFile.add(map1);
      Map<String, String> map2 = new TreeMap<>();
      map2.put("numberField", "987");
      map2.put("someOtherNumberField", "654");
      map2.put("stringFieldA", "some fields are missing!");
      map2.put("startOfInterval", "10/22/2015 12:00:00 AM");
      expectedResultsSampleFile.add(map2);
    }
    // expectedResultsExampleCiscoVersion initialization
    {
      Map<String, String> map = new TreeMap<>();
      map.put("version", "12.2(31)SGA1");
      map.put("uptime", "3 days, 13 hours, 53 minutes");
      map.put("reloadReason", "reload");
      map.put("reloadTime", "05:09:09 PDT Wed Apr 2 2008");
      map.put("imageFile", "bootflash:cat4500-entservicesk9-mz.122-31.SGA1.bin");
      map.put("model", "WS-C4948-10GE");
      map.put("memory", "262144K");
      map.put("configRegister", "x2102");
      expectedResultsExampleCiscoVersion.add(map);
    }
    // expectedResultsExampleCiscoBgpSurvey initialization
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("localAS", "65550");
      map1.put("remoteAS", "65551");
      map1.put("remoteIp", "192.0.2.77");
      map1.put("routerId", "192.0.2.70");
      map1.put("status", "1");
      map1.put("uptime", "5w4d");
      expectedResultsExampleCiscoBgpSurvey.add(map1);
      Map<String, String> map2 = new TreeMap<>();
      map2.put("localAS", "65550");
      map2.put("remoteAS", "65552");
      map2.put("remoteIp", "192.0.2.78");
      map2.put("routerId", "192.0.2.70");
      map2.put("status", "10");
      map2.put("uptime", "5w4d");
      expectedResultsExampleCiscoBgpSurvey.add(map2);
    }
    // expectedResultsExampleF10IPBgpSurvey initialization
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("localAS", "65551");
      map1.put("remoteAS", "65551");
      map1.put("remoteIp", "10.10.10.10");
      map1.put("routerId", "192.0.2.1");
      map1.put("status", "5");
      map1.put("uptime", "10:37:12");
      expectedResultsExampleF10IPBgpSurvey.add(map1);
      Map<String, String> map2 = new TreeMap<>();
      map2.put("localAS", "65551");
      map2.put("remoteAS", "65552");
      map2.put("remoteIp", "10.10.100.1");
      map2.put("routerId", "192.0.2.1");
      map2.put("status", "0");
      map2.put("uptime", "10:38:27");
      expectedResultsExampleF10IPBgpSurvey.add(map2);
      Map<String, String> map3 = new TreeMap<>();
      map3.put("localAS", "65551");
      map3.put("remoteAS", "65553");
      map3.put("remoteIp", "10.100.10.9");
      map3.put("routerId", "192.0.2.1");
      map3.put("status", "1");
      map3.put("uptime", "07:55:38");
      expectedResultsExampleF10IPBgpSurvey.add(map3);
    }
    // expectedResultsExampleF10Version initialization
    {
      Map<String, String> map = new TreeMap<>();
      map.put("software", "7.7.1.1");
      map.put("chassis", "E1200");
      map.put("model", "E1200");
      map.put("imageFile", "flash://FTOS-EF-7.7.1.1.bin");
      expectedResultsExampleF10Version.add(map);
    }
    // expectedResultsJuniperBgpVersion initialization
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("remoteIp", "10.247.68.182");
      map1.put("uptime", "6w3d17h");
      map1.put("activeV4", "4");
      map1.put("receivedV4", "5");
      map1.put("accepted_V4", "1");
      map1.put("activeV6", "0");
      map1.put("receivedV6", "0");
      map1.put("accepted_V6", "0");
      expectedResultsJuniperBgpVersion.add(map1);
      Map<String, String> map2 = new TreeMap<>();
      map2.put("remoteIp", "10.254.166.246");
      map2.put("uptime", "6w5d6h");
      map2.put("activeV4", "0");
      map2.put("receivedV4", "0");
      map2.put("accepted_V4", "0");
      map2.put("activeV6", "7");
      map2.put("receivedV6", "8");
      map2.put("accepted_V6", "1");
      expectedResultsJuniperBgpVersion.add(map2);
      Map<String, String> map3 = new TreeMap<>();
      map3.put("remoteIp", "192.0.2.100");
      map3.put("uptime", "9w5d6h");
      map3.put("activeV4", "1");
      map3.put("receivedV4", "2");
      map3.put("accepted_V4", "3");
      map3.put("activeV6", "4");
      map3.put("receivedV6", "5");
      map3.put("accepted_V6", "6");
      expectedResultsJuniperBgpVersion.add(map3);
    }
    // expectedResultsExampleJuniperVersion initialization
    {
      Map<String, String> map = new TreeMap<>();
      map.put("model", "mx960");
      map.put("junosOsBoot", "9.1S3.5");
      map.put("junosOsSoftware", "9.1S3.5");
      map.put("junosKernelSoftware", "9.1S3.5");
      map.put("junosCryptoSoftware", "9.1S3.5");
      map.put("junosPacketForwardMTCommon", "9.1S3.5");
      map.put("junosPacketForwardMXCommon", "9.1S3.5");
      map.put("junosOnlineDoc", "9.1S3.5");
      map.put("junosRoutingSoftware", "9.1S3.5");
      expectedResultsExampleJuniperVersion.add(map);
    }
    // expectedResultsExampleIfcfg initialization
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("interface", "lo0");
      map1.put("mtu", "16384");
      map1.put("inet6", "::1");
      map1.put("prefixlen", "128");
      map1.put("inet4", "127.0.0.1");
      map1.put("netmask", "0xff000000");
      expectedResultsExampleIfcfg.add(map1);
      Map<String, String> map2 = new TreeMap<>();
      map2.put("interface", "en0");
      map2.put("ether", "34:15:9e:27:45:e3");
      map2.put("mtu", "1500");
      map2.put("inet6", "2001:db8::3615:9eff:fe27:45e3");
      map2.put("prefixlen", "64");
      map2.put("inet4", "192.0.2.215");
      map2.put("netmask", "0xfffffe00");
      expectedResultsExampleIfcfg.add(map2);
      Map<String, String> map3 = new TreeMap<>();
      map3.put("interface", "en1");
      map3.put("ether", "90:84:0d:f6:d1:55");
      map3.put("mtu", "1500");
      expectedResultsExampleIfcfg.add(map3);
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
  public void testSampleFile() throws IOException {
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
  public void testExampleCiscoVersion() throws IOException {
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
  public void testExampleCiscoBgpSurvey() throws IOException {
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
  public void testExampleF10IPBgpSurvey() throws IOException {
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
  public void testExampleF10Version() throws IOException {
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
  public void testJuniperBgpVersion() throws IOException {
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
  public void testExampleJuniperVersion() throws IOException {
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
  public void testExampleIfcfg() throws IOException {
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
    expectedResults) throws IOException {
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