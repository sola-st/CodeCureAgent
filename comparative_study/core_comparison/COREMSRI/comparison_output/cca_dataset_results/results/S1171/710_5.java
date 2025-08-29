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

  private static final List<Map<String, String>> EXPECTED_RESULTS_SAMPLE_FILE = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_CISCO_VERSION = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_CISCO_BGP_SURVEY = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_F10_IP_BGP_SURVEY = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_F10_VERSION = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_JUNIPER_BGP_VERSION = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_JUNIPER_VERSION = new ArrayList<>();
  private static final List<Map<String, String>> EXPECTED_RESULTS_IFCFG = new ArrayList<>();

  static {
    // Initialize EXPECTED_RESULTS_SAMPLE_FILE
    Map<String, String> map1 = new TreeMap<>();
    map1.put("numberField", "123");
    map1.put("someOtherNumberField", "456");
    map1.put("stringFieldA", "what'll I do?");
    map1.put("stringFieldB", "without you?");
    map1.put("startOfInterval", "10/22/2015 12:00:00 AM");
    map1.put("endOfInterval", "12/31/2015 11:59:00 PM");
    EXPECTED_RESULTS_SAMPLE_FILE.add(map1);

    Map<String, String> map2 = new TreeMap<>();
    map2.put("numberField", "987");
    map2.put("someOtherNumberField", "654");
    map2.put("stringFieldA", "some fields are missing!");
    map2.put("startOfInterval", "10/22/2015 12:00:00 AM");
    EXPECTED_RESULTS_SAMPLE_FILE.add(map2);

    // Initialize EXPECTED_RESULTS_CISCO_VERSION
    Map<String, String> ciscoVerMap = new TreeMap<>();
    ciscoVerMap.put("version", "12.2(31)SGA1");
    ciscoVerMap.put("uptime", "3 days, 13 hours, 53 minutes");
    ciscoVerMap.put("reloadReason", "reload");
    ciscoVerMap.put("reloadTime", "05:09:09 PDT Wed Apr 2 2008");
    ciscoVerMap.put("imageFile", "bootflash:cat4500-entservicesk9-mz.122-31.SGA1.bin");
    ciscoVerMap.put("model", "WS-C4948-10GE");
    ciscoVerMap.put("memory", "262144K");
    ciscoVerMap.put("configRegister", "x2102");
    EXPECTED_RESULTS_CISCO_VERSION.add(ciscoVerMap);

    // Initialize EXPECTED_RESULTS_CISCO_BGP_SURVEY
    Map<String, String> bgpMap1 = new TreeMap<>();
    bgpMap1.put("localAS", "65550");
    bgpMap1.put("remoteAS", "65551");
    bgpMap1.put("remoteIp", "192.0.2.77");
    bgpMap1.put("routerId", "192.0.2.70");
    bgpMap1.put("status", "1");
    bgpMap1.put("uptime", "5w4d");
    EXPECTED_RESULTS_CISCO_BGP_SURVEY.add(bgpMap1);

    Map<String, String> bgpMap2 = new TreeMap<>();
    bgpMap2.put("localAS", "65550");
    bgpMap2.put("remoteAS", "65552");
    bgpMap2.put("remoteIp", "192.0.2.78");
    bgpMap2.put("routerId", "192.0.2.70");
    bgpMap2.put("status", "10");
    bgpMap2.put("uptime", "5w4d");
    EXPECTED_RESULTS_CISCO_BGP_SURVEY.add(bgpMap2);

    // Initialize EXPECTED_RESULTS_F10_IP_BGP_SURVEY
    Map<String, String> f10Map1 = new TreeMap<>();
    f10Map1.put("localAS", "65551");
    f10Map1.put("remoteAS", "65551");
    f10Map1.put("remoteIp", "10.10.10.10");
    f10Map1.put("routerId", "192.0.2.1");
    f10Map1.put("status", "5");
    f10Map1.put("uptime", "10:37:12");
    EXPECTED_RESULTS_F10_IP_BGP_SURVEY.add(f10Map1);

    Map<String, String> f10Map2 = new TreeMap<>();
    f10Map2.put("localAS", "65551");
    f10Map2.put("remoteAS", "65552");
    f10Map2.put("remoteIp", "10.10.100.1");
    f10Map2.put("routerId", "192.0.2.1");
    f10Map2.put("status", "0");
    f10Map2.put("uptime", "10:38:27");
    EXPECTED_RESULTS_F10_IP_BGP_SURVEY.add(f10Map2);

    Map<String, String> f10Map3 = new TreeMap<>();
    f10Map3.put("localAS", "65551");
    f10Map3.put("remoteAS", "65553");
    f10Map3.put("remoteIp", "10.100.10.9");
    f10Map3.put("routerId", "192.0.2.1");
    f10Map3.put("status", "1");
    f10Map3.put("uptime", "07:55:38");
    EXPECTED_RESULTS_F10_IP_BGP_SURVEY.add(f10Map3);

    // Initialize EXPECTED_RESULTS_F10_VERSION
    Map<String, String> f10VerMap = new TreeMap<>();
    f10VerMap.put("software", "7.7.1.1");
    f10VerMap.put("chassis", "E1200");
    f10VerMap.put("model", "E1200");
    f10VerMap.put("imageFile", "flash://FTOS-EF-7.7.1.1.bin");
    EXPECTED_RESULTS_F10_VERSION.add(f10VerMap);

    // Initialize EXPECTED_RESULTS_JUNIPER_BGP_VERSION
    Map<String, String> juniperBgpMap1 = new TreeMap<>();
    juniperBgpMap1.put("remoteIp", "10.247.68.182");
    juniperBgpMap1.put("uptime", "6w3d17h");
    juniperBgpMap1.put("activeV4", "4");
    juniperBgpMap1.put("receivedV4", "5");
    juniperBgpMap1.put("accepted_V4", "1");
    juniperBgpMap1.put("activeV6", "0");
    juniperBgpMap1.put("receivedV6", "0");
    juniperBgpMap1.put("accepted_V6", "0");
    EXPECTED_RESULTS_JUNIPER_BGP_VERSION.add(juniperBgpMap1);

    Map<String, String> juniperBgpMap2 = new TreeMap<>();
    juniperBgpMap2.put("remoteIp", "10.254.166.246");
    juniperBgpMap2.put("uptime", "6w5d6h");
    juniperBgpMap2.put("activeV4", "0");
    juniperBgpMap2.put("receivedV4", "0");
    juniperBgpMap2.put("accepted_V4", "0");
    juniperBgpMap2.put("activeV6", "7");
    juniperBgpMap2.put("receivedV6", "8");
    juniperBgpMap2.put("accepted_V6", "1");
    EXPECTED_RESULTS_JUNIPER_BGP_VERSION.add(juniperBgpMap2);

    Map<String, String> juniperBgpMap3 = new TreeMap<>();
    juniperBgpMap3.put("remoteIp", "192.0.2.100");
    juniperBgpMap3.put("uptime", "9w5d6h");
    juniperBgpMap3.put("activeV4", "1");
    juniperBgpMap3.put("receivedV4", "2");
    juniperBgpMap3.put("accepted_V4", "3");
    juniperBgpMap3.put("activeV6", "4");
    juniperBgpMap3.put("receivedV6", "5");
    juniperBgpMap3.put("accepted_V6", "6");
    EXPECTED_RESULTS_JUNIPER_BGP_VERSION.add(juniperBgpMap3);

    // Initialize EXPECTED_RESULTS_JUNIPER_VERSION
    Map<String, String> juniperVerMap = new TreeMap<>();
    juniperVerMap.put("model", "mx960");
    juniperVerMap.put("junosOsBoot", "9.1S3.5");
    juniperVerMap.put("junosOsSoftware", "9.1S3.5");
    juniperVerMap.put("junosKernelSoftware", "9.1S3.5");
    juniperVerMap.put("junosCryptoSoftware", "9.1S3.5");
    juniperVerMap.put("junosPacketForwardMTCommon", "9.1S3.5");
    juniperVerMap.put("junosPacketForwardMXCommon", "9.1S3.5");
    juniperVerMap.put("junosOnlineDoc", "9.1S3.5");
    juniperVerMap.put("junosRoutingSoftware", "9.1S3.5");
    EXPECTED_RESULTS_JUNIPER_VERSION.add(juniperVerMap);

    // Initialize EXPECTED_RESULTS_IFCFG
    Map<String, String> ifcfgMap1 = new TreeMap<>();
    ifcfgMap1.put("interface", "lo0");
    ifcfgMap1.put("mtu", "16384");
    ifcfgMap1.put("inet6", "::1");
    ifcfgMap1.put("prefixlen", "128");
    ifcfgMap1.put("inet4", "127.0.0.1");
    ifcfgMap1.put("netmask", "0xff000000");
    EXPECTED_RESULTS_IFCFG.add(ifcfgMap1);

    Map<String, String> ifcfgMap2 = new TreeMap<>();
    ifcfgMap2.put("interface", "en0");
    ifcfgMap2.put("ether", "34:15:9e:27:45:e3");
    ifcfgMap2.put("mtu", "1500");
    ifcfgMap2.put("inet6", "2001:db8::3615:9eff:fe27:45e3");
    ifcfgMap2.put("prefixlen", "64");
    ifcfgMap2.put("inet4", "192.0.2.215");
    ifcfgMap2.put("netmask", "0xfffffe00");
    EXPECTED_RESULTS_IFCFG.add(ifcfgMap2);

    Map<String, String> ifcfgMap3 = new TreeMap<>();
    ifcfgMap3.put("interface", "en1");
    ifcfgMap3.put("ether", "90:84:0d:f6:d1:55");
    ifcfgMap3.put("mtu", "1500");
    EXPECTED_RESULTS_IFCFG.add(ifcfgMap3);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_SAMPLE_FILE);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_CISCO_VERSION);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_CISCO_BGP_SURVEY);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_F10_IP_BGP_SURVEY);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_F10_VERSION);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_JUNIPER_BGP_VERSION);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_JUNIPER_VERSION);
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

    testFileProcessing(configResource, fileResource, EXPECTED_RESULTS_IFCFG);
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