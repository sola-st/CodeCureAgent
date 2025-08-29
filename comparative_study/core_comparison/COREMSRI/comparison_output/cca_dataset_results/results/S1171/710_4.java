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
  public void testSampleFile() throws IOException {
    String configResource = "sample.config.xml";
    String fileResource = "sample.import.txt";

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("numberField", "123");
      map1.put("someOtherNumberField", "456");
      map1.put("stringFieldA", "what'll I do?");
      map1.put("stringFieldB", "without you?");
      map1.put("startOfInterval", "10/22/2015 12:00:00 AM");
      map1.put("endOfInterval", "12/31/2015 11:59:00 PM");
      expectedResults.add(map1);

      Map<String, String> map2 = new TreeMap<>();
      map2.put("numberField", "987");
      map2.put("someOtherNumberField", "654");
      map2.put("stringFieldA", "some fields are missing!");
      map2.put("startOfInterval", "10/22/2015 12:00:00 AM");
      expectedResults.add(map2);
    }
    testFileProcessing(configResource, fileResource, expectedResults);

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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("version", "12.2(31)SGA1");
      map1.put("uptime", "3 days, 13 hours, 53 minutes");
      map1.put("reloadReason", "reload");
      map1.put("reloadTime", "05:09:09 PDT Wed Apr 2 2008");
      map1.put("imageFile", "bootflash:cat4500-entservicesk9-mz.122-31.SGA1.bin");
      map1.put("model", "WS-C4948-10GE");
      map1.put("memory", "262144K");
      map1.put("configRegister", "x2102");
      expectedResults.add(map1);
    }
    testFileProcessing(configResource, fileResource, expectedResults);

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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("localAS", "65550");
      map1.put("remoteAS", "65551");
      map1.put("remoteIp", "192.0.2.77");
      map1.put("routerId", "192.0.2.70");
      map1.put("status", "1");
      map1.put("uptime", "5w4d");
      expectedResults.add(map1);

      Map<String, String> map2 = new TreeMap<>();
      map2.put("localAS", "65550");
      map2.put("remoteAS", "65552");
      map2.put("remoteIp", "192.0.2.78");
      map2.put("routerId", "192.0.2.70");
      map2.put("status", "10");
      map2.put("uptime", "5w4d");
      expectedResults.add(map2);
    }
    testFileProcessing(configResource, fileResource, expectedResults);
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("localAS", "65551");
      map1.put("remoteAS", "65551");
      map1.put("remoteIp", "10.10.10.10");
      map1.put("routerId", "192.0.2.1");
      map1.put("status", "5");
      map1.put("uptime", "10:37:12");
      expectedResults.add(map1);

      Map<String, String> map2 = new TreeMap<>();
      map2.put("localAS", "65551");
      map2.put("remoteAS", "65552");
      map2.put("remoteIp", "10.10.100.1");
      map2.put("routerId", "192.0.2.1");
      map2.put("status", "0");
      map2.put("uptime", "10:38:27");
      expectedResults.add(map2);

      Map<String, String> map3 = new TreeMap<>();
      map3.put("localAS", "65551");
      map3.put("remoteAS", "65553");
      map3.put("remoteIp", "10.100.10.9");
      map3.put("routerId", "192.0.2.1");
      map3.put("status", "1");
      map3.put("uptime", "07:55:38");
      expectedResults.add(map3);
    }
    testFileProcessing(configResource, fileResource, expectedResults);
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("software", "7.7.1.1");
      map1.put("chassis", "E1200");
      map1.put("model", "E1200");
      map1.put("imageFile", "flash://FTOS-EF-7.7.1.1.bin");
      expectedResults.add(map1);
    }
    testFileProcessing(configResource, fileResource, expectedResults);

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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      /*
      10.247.68.182         65550     131725   28179233       0      11     6w3d17h Establ
        inet.0: 4/5/1
        inet6.0: 0/0/0
       */
      Map<String, String> map1 = new TreeMap<>();
      map1.put("remoteIp", "10.247.68.182");
      map1.put("uptime", "6w3d17h");
      map1.put("activeV4", "4");
      map1.put("receivedV4", "5");
      map1.put("accepted_V4", "1");
      map1.put("activeV6", "0");
      map1.put("receivedV6", "0");
      map1.put("accepted_V6", "0");
      expectedResults.add(map1);

      /*
      10.254.166.246        65550     136159   29104942       0       0      6w5d6h Establ
        inet.0: 0/0/0
        inet6.0: 7/8/1
       */
      Map<String, String> map2 = new TreeMap<>();
      map2.put("remoteIp", "10.254.166.246");
      map2.put("uptime", "6w5d6h");
      map2.put("activeV4", "0");
      map2.put("receivedV4", "0");
      map2.put("accepted_V4", "0");
      map2.put("activeV6", "7");
      map2.put("receivedV6", "8");
      map2.put("accepted_V6", "1");
      expectedResults.add(map2);

      /*
        192.0.2.100           65551    1269381    1363320       0       1      9w5d6h 1/2/3 4/5/6
       */
      Map<String, String> map3 = new TreeMap<>();
      map3.put("remoteIp", "192.0.2.100");
      map3.put("uptime", "9w5d6h");
      map3.put("activeV4", "1");
      map3.put("receivedV4", "2");
      map3.put("accepted_V4", "3");
      map3.put("activeV6", "4");
      map3.put("receivedV6", "5");
      map3.put("accepted_V6", "6");
      expectedResults.add(map3);
    }
    testFileProcessing(configResource, fileResource, expectedResults);

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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      Map<String, String> map1 = new TreeMap<>();
      map1.put("model", "mx960");
      map1.put("junosOsBoot", "9.1S3.5");
      map1.put("junosOsSoftware", "9.1S3.5");
      map1.put("junosKernelSoftware", "9.1S3.5");
      map1.put("junosCryptoSoftware", "9.1S3.5");
      map1.put("junosPacketForwardMTCommon", "9.1S3.5");
      map1.put("junosPacketForwardMXCommon", "9.1S3.5");
      map1.put("junosOnlineDoc", "9.1S3.5");
      map1.put("junosRoutingSoftware", "9.1S3.5");
      expectedResults.add(map1);
    }
    testFileProcessing(configResource, fileResource, expectedResults);
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

    // these are the files we expect from the config
    List<Map<String, String>> expectedResults = new ArrayList<>();
    {
      /*
      lo0: flags=8049<UP,LOOPBACK,RUNNING,MULTICAST> mtu 16384
        inet6 ::1 prefixlen 128
        inet6 fe80::1%lo0 prefixlen 64 scopeid 0x1
        inet 127.0.0.1 netmask 0xff000000
       */
      Map<String, String> map1 = new TreeMap<>();
      map1.put("interface", "lo0");
      map1.put("mtu", "16384");
      map1.put("inet6", "::1");
      map1.put("prefixlen", "128");
      map1.put("inet4", "127.0.0.1");
      map1.put("netmask", "0xff000000");
      expectedResults.add(map1);

      /*
        en0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
          ether 34:15:9e:27:45:e3
          inet6 fe80::3615:9eff:fe27:45e3%en0 prefixlen 64 scopeid 0x4
          inet6 2001:db8::3615:9eff:fe27:45e3 prefixlen 64 autoconf
          inet 192.0.2.215 netmask 0xfffffe00 broadcast 192.0.2.255
          media: autoselect (1000baseT <full-duplex,flow-control>)
          status: active
       */
      Map<String, String> map2 = new TreeMap<>();
      map2.put("interface", "en0");
      map2.put("ether", "34:15:9e:27:45:e3");
      map2.put("mtu", "1500");
      map2.put("inet6", "2001:db8::3615:9eff:fe27:45e3");
      map2.put("prefixlen", "64");
      map2.put("inet4", "192.0.2.215");
      map2.put("netmask", "0xfffffe00");
      expectedResults.add(map2);

      /*
        en1: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
          ether 90:84:0d:f6:d1:55
          media: <unknown subtype> (<unknown type>)
          status: inactive
       */
      Map<String, String> map3 = new TreeMap<>();
      map3.put("interface", "en1");
      map3.put("ether", "90:84:0d:f6:d1:55");
      map3.put("mtu", "1500");
      expectedResults.add(map3);
    }
    testFileProcessing(configResource, fileResource, expectedResults);
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