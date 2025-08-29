```java
package com.restfully.shop.test;

import com.restfully.shop.domain.Customers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CustomerResourceTest
{
   private static Client client;
   private static final Logger logger = Logger.getLogger(CustomerResourceTest.class.getName());

   @BeforeClass
   public static void initClient()
   {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void closeClient()
   {
      client.close();
   }

   @Test
   public void testQueryCustomers() throws Exception
   {
      URI uri = new URI("http://localhost:8080/services/customers");
      while (uri != null)
      {
         WebTarget target = client.target(uri);
         String output = target.request().get(String.class);
         logger.info("** XML from " + uri.toString());
         logger.info(output);

         Customers customers = target.request().get(Customers.class);
         uri = customers.getNext();
      }
   }
}
