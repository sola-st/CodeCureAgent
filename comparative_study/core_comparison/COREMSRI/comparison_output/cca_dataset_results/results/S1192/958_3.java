package org.modelmapper.internal.valueaccess;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.modelmapper.AbstractTest;
import org.modelmapper.convention.MatchingStrategies;
import org.testng.annotations.Test;

/**
 * Tests the mapping of a Map to a POJO and visa versa.
 * 
 * @author Jonathan Halterman
 */
@Test(groups = "functional")
public class MapValueReaderTest extends AbstractTest {
  private static final String ID = "id";
  private static final String CUSTOMER = "customer";
  private static final String STREET_ADDRESS = "streetAddress";
  private static final String ADDRESS_CITY = "addressCity";
  private static final String CUSTOMER_ID = "customerId";
  private static final String CUSTOMER_STREET_ADDRESS = "customerStreetAddress";
  private static final String CUSTOMER_ADDRESS_CITY = "customerAddressCity";
  private static final String CUSTOMER_CITY = "customerCity";
  private static final String STREET = "street";
  private static final String CITY = "city";

  public static class Order {
    int id;
    Customer customer;
  }

  public static class Customer {
    int id;
    Address address;
  }

  public static class Address {
    String street;
    String city;
  }

  public void shouldMapMapToBean() {
    Map<String, Object> orderMap = new HashMap<String, Object>();
    Map<String, Object> customerMap = new HashMap<String, Object>();
    orderMap.put(ID, 456);
    orderMap.put(CUSTOMER, customerMap);
    customerMap.put(ID, 789);
    customerMap.put(STREET_ADDRESS, "1234 Main Street");
    customerMap.put(ADDRESS_CITY, "Seattle");

    Order order = modelMapper.map(orderMap, Order.class);

    assertEquals(order.id, 456);
    assertEquals(order.customer.id, 789);
    assertEquals(order.customer.address.street, "1234 Main Street");
    assertEquals(order.customer.address.city, "Seattle");

    orderMap.clear();
    orderMap.put(ID, 444);
    orderMap.put(CUSTOMER_ID, 555);
    orderMap.put(CUSTOMER_STREET_ADDRESS, "1234 Main Street");
    orderMap.put(CUSTOMER_ADDRESS_CITY, "LA");

    order = modelMapper.map(orderMap, Order.class, "flat");

    assertEquals(order.id, 444);
    assertEquals(order.customer.id, 555);
    assertEquals(order.customer.address.street, "1234 Main Street");
    assertEquals(order.customer.address.city, "LA");
  }

  /**
   * Demonstrates that structural information (accessors/mutators) for generic types such as maps is
   * not cached.
   */
  public void shouldMapAnotherMapToBean() {
    Map<String, Object> orderMap = new HashMap<String, Object>();
    orderMap.put(CUSTOMER_STREET_ADDRESS, "1234 Main Street");
    orderMap.put(CUSTOMER_CITY, "Seattle");

    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
    Order order = modelMapper.map(orderMap, Order.class);

    assertEquals(order.customer.address.street, "1234 Main Street");
    assertEquals(order.customer.address.city, "Seattle");
  }

  // Disabled until support is added for mapping TO generic types
  @Test(enabled = false)
  public void shouldMapBeanToMap() {
    Order order = new Order();
    order.customer = new Customer();
    order.customer.address = new Address();
    order.customer.address.city = "Seattle";
    order.customer.address.street = "1234 Main Street";

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Map<String, String>>> map = modelMapper.map(order, LinkedHashMap.class);

    modelMapper.validate();
    assertEquals(map.get(CUSTOMER).get("address").get(CITY), order.customer.address.city);
    assertEquals(map.get(CUSTOMER).get("address").get(STREET), order.customer.address.street);
  }

  public void shouldMapNullValue() {
    Map<String, Object> addressMap1 = new HashMap<String, Object>();
    addressMap1.put(STREET, "Street A");
    addressMap1.put(CITY, null);

    Address address1 = modelMapper.map(addressMap1, Address.class);
    assertEquals(address1.street, "Street A");
    assertNull(address1.city);

    Map<String, Object> addressMap2 = new HashMap<String, Object>();
    addressMap2.put(STREET, "Street B");
    addressMap2.put(CITY, "City");

    Address address2 = modelMapper.map(addressMap2, Address.class);

    assertEquals(address2.street, "Street B");
    assertEquals(address2.city, "City");

    Map<String, Object> addressMap3 = new HashMap<String, Object>();
    addressMap3.put(STREET, "Street C");
    addressMap3.put(CITY, "City II");

    Address address3 = modelMapper.map(addressMap3, Address.class);

    assertEquals(address3.street, "Street C");
    assertEquals(address3.city, "City II");

    Map<String, Object> addressMap4 = new HashMap<String, Object>();
    addressMap4.put(STREET, "Street C");
    addressMap4.put(CITY, null);

    Address address4 = modelMapper.map(addressMap4, Address.class);

    assertEquals(address4.street, "Street C");
    assertNull(address4.city);
  }
}
