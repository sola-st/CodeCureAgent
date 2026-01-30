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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestSampleFileExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);

  }

  private List<Map<String, String>> createTestSampleFileExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();
    {
      Map<String, String> map = new TreeMap<>();
      map.put("numberField", "123");
      map.put("someOtherNumberField", "456");
      map.put("stringFieldA", "what'll I do?");
      map.put("stringFieldB", "without you?");
      map.put("startOfInterval", "10/22/2015 12:00:00 AM");
      map.put("endOfInterval", "12/31/2015 11:59:00 PM");
      list.add(map);
    }
    {
      Map<String, String> map = new TreeMap<>();
      map.put("numberField", "987");
      map.put("someOtherNumberField", "654");
      map.put("stringFieldA", "some fields are missing!");
      map.put("startOfInterval", "10/22/2015 12:00:00 AM");
      list.add(map);
    }
    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestExampleCiscoVersionExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);

  }

  private List<Map<String, String>> createTestExampleCiscoVersionExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();
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
      list.add(map);
    }
    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestExampleCiscoBgpSurveyExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);
  }

  private List<Map<String, String>> createTestExampleCiscoBgpSurveyExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();

    //{localAS=65550, remoteAS=65551, remoteIp=192.0.2.77, routerId=192.0.2.70, status=1, uptime=5w4d},
    {
      Map<String, String> map = new TreeMap<>();
      map.put("localAS", "65550");
      map.put("remoteAS", "65551");
      map.put("remoteIp", "192.0.2.77");
      map.put("routerId", "192.0.2.70");
      map.put("status", "1");
      map.put("uptime", "5w4d");
      list.add(map);
    }
    //{localAS=65550, remoteAS=65552, remoteIp=192.0.2.78, routerId=192.0.2.70, status=10, uptime=5w4d}

    {
      Map<String, String> map = new TreeMap<>();
      map.put("localAS", "65550");
      map.put("remoteAS", "65552");
      map.put("remoteIp", "192.0.2.78");
      map.put("routerId", "192.0.2.70");
      map.put("status", "10");
      map.put("uptime", "5w4d");
      list.add(map);
    }

    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestExampleF10IPBgpSurveyExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);
  }

  private List<Map<String, String>> createTestExampleF10IPBgpSurveyExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();

    {
      Map<String, String> map = new TreeMap<>();
      map.put("localAS", "65551");
      map.put("remoteAS", "65551");
      map.put("remoteIp", "10.10.10.10");
      map.put("routerId", "192.0.2.1");
      map.put("status", "5");
      map.put("uptime", "10:37:12");
      list.add(map);
    }
    {
      Map<String, String> map = new TreeMap<>();
      map.put("localAS", "65551");
      map.put("remoteAS", "65552");
      map.put("remoteIp", "10.10.100.1");
      map.put("routerId", "192.0.2.1");
      map.put("status", "0");
      map.put("uptime", "10:38:27");
      list.add(map);
    }
    {
      Map<String, String> map = new TreeMap<>();
      map.put("localAS", "65551");
      map.put("remoteAS", "65553");
      map.put("remoteIp", "10.100.10.9");
      map.put("routerId", "192.0.2.1");
      map.put("status", "1");
      map.put("uptime", "07:55:38");
      list.add(map);
    }

    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestExampleF10VersionExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);

  }

  private List<Map<String, String>> createTestExampleF10VersionExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();
    {
      Map<String, String> map = new TreeMap<>();
      map.put("software", "7.7.1.1");
      map.put("chassis", "E1200");
      map.put("model", "E1200");
      map.put("imageFile", "flash://FTOS-EF-7.7.1.1.bin");
      list.add(map);
    }
    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestJuniperBgpVersionExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);

  }

  private List<Map<String, String>> createTestJuniperBgpVersionExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();
    /*
    10.247.68.182         65550     131725   28179233       0      11     6w3d17h Establ
      inet.0: 4/5/1
      inet6.0: 0/0/0
     */
    {
      Map<String, String> map = new TreeMap<>();
      map.put("remoteIp", "10.247.68.182");
      map.put("uptime", "6w3d17h");
      map.put("activeV4", "4");
      map.put("receivedV4", "5");
      map.put("accepted_V4", "1");
      map.put("activeV6", "0");
      map.put("receivedV6", "0");
      map.put("accepted_V6", "0");
      list.add(map);
    }
    /*
    10.254.166.246        65550     136159   29104942       0       0      6w5d6h Establ
      inet.0: 0/0/0
      inet6.0: 7/8/1
     */
    {
      Map<String, String> map = new TreeMap<>();
      map.put("remoteIp", "10.254.166.246");
      map.put("uptime", "6w5d6h");
      map.put("activeV4", "0");
      map.put("receivedV4", "0");
      map.put("accepted_V4", "0");
      map.put("activeV6", "7");
      map.put("receivedV6", "8");
      map.put("accepted_V6", "1");
      list.add(map);
    }

    /*
      192.0.2.100           65551    1269381    1363320       0       1      9w5d6h 1/2/3 4/5/6
     */
    {
      Map<String, String> map = new TreeMap<>();
      map.put("remoteIp", "192.0.2.100");
      map.put("uptime", "9w5d6h");
      map.put("activeV4", "1");
      map.put("receivedV4", "2");
      map.put("accepted_V4", "3");
      map.put("activeV6", "4");
      map.put("receivedV6", "5");
      map.put("accepted_V6", "6");
      list.add(map);
    }
    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestExampleJuniperVersionExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);
  }

  private List<Map<String, String>> createTestExampleJuniperVersionExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();
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
      list.add(map);
    }
    return list;
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = createTestExampleIfcfgExpectedResults();
    testFileProcessing(configResource, fileResource, expectedResults);
  }

  private List<Map<String, String>> createTestExampleIfcfgExpectedResults() {
    List<Map<String, String>> list = new ArrayList<>();
    /*
    lo0: flags=8049<UP,LOOPBACK,RUNNING,MULTICAST> mtu 16384
      inet6 ::1 prefixlen 128
      inet6 fe80::1%lo0 prefixlen 64 scopeid 0x1
      inet 127.0.0.1 netmask 0xff000000
     */
    {
      Map<String, String> map = new TreeMap<>();
      map.put("interface", "lo0");
      map.put("mtu", "16384");
      map.put("inet6", "::1");
      map.put("prefixlen", "128");
      map.put("inet4", "127.0.0.1");
      map.put("netmask", "0xff000000");
      list.add(map);
    }
    /*
      en0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
        ether 34:15:9e:27:45:e3
        inet6 fe80::3615:9eff:fe27:45e3%en0 prefixlen 64 scopeid 0x4
        inet6 2001:db8::3615:9eff:fe27:45e3 prefixlen 64 autoconf
        inet 192.0.2.215 netmask 0xfffffe00 broadcast 192.0.2.255
        media: autoselect (1000baseT <full-duplex,flow-control>)
        status: active
     */
    {
      Map<String, String> map = new TreeMap<>();
      map.put("interface", "en0");
      map.put("ether", "34:15:9e:27:45:e3");
      map.put("mtu", "1500");
      map.put("inet6", "2001:db8::3615:9eff:fe27:45e3");
      map.put("prefixlen", "64");
      map.put("inet4", "192.0.2.215");
      map.put("netmask", "0xfffffe00");
      list.add(map);
    }

    /*
      en1: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
        ether 90:84:0d:f6:d1:55
        media: <unknown subtype> (<unknown type>)
        status: inactive
     */
    {
      Map<String, String> map = new TreeMap<>();
      map.put("interface", "en1");
      map.put("ether", "90:84:0d:f6:d1:55");
      map.put("mtu", "1500");
      list.add(map);
    }
    return list;
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
    List<Map<String, String>> observedValues = new ArrayList<Map<String, String>>();
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