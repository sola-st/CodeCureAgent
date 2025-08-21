/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modelmapper.internal.converter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.modelmapper.MappingException;
import org.modelmapper.spi.ConditionalConverter.MatchResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Adapted from the BeanUtils test suite.
 */
@Test
public class NumberConverterTest extends AbstractConverterTest {
  public NumberConverterTest() {
    super(new NumberConverter());
  }

  @DataProvider(name = "numbersProvider")
  public Object[][] provideNumbers() {
    return new Object[][] { { Integer.valueOf(36) }, { Short.valueOf("44") }, { Double.valueOf(55) },
        { Byte.valueOf("3") }, { Long.valueOf(2345) }, { new BigDecimal(3454) },
        { BigInteger.valueOf(7773) }, { Float.valueOf(664) } };
  }

  @DataProvider(name = "typesProvider")
  public Object[][] provideTypes() {
    return new Object[][] { { Integer.class }, { Short.class }, { Double.class }, { Byte.class },
        { Long.class }, { BigDecimal.class }, { BigInteger.class }, { Float.class } };
  }

  /**
   * Test specifying an invalid type.
   */
  @Test(expectedExceptions = MappingException.class, dataProvider = "numbersProvider")
  public void shouldFailOnInvalidDestinationType(Number number) {
    convert(number, Object.class);
  }

  public void shouldConvertBigDecimals() {
    Object[] input = { "-17.2", "-1.1", "0.0", "1.1", "17.2", Byte.valueOf((byte) 7),
        Short.valueOf((short) 8), Integer.valueOf(9), Long.valueOf(10), Float.valueOf("11.1"), Double.valueOf("12.2") };

    BigDecimal[] expected = { new BigDecimal("-17.2"), new BigDecimal("-1.1"),
        new BigDecimal("0.0"), new BigDecimal("1.1"), new BigDecimal("17.2"), new BigDecimal("7"),
        new BigDecimal("8"), new BigDecimal("9"), new BigDecimal("10"), new BigDecimal("11.1"),
        new BigDecimal("12.2") };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(convert(input[i], BigDecimal.class), expected[i]);
    }
  }

  public void shouldConvertBigIntegers() {
    Object[] input = { String.valueOf(Long.MIN_VALUE), "-17", "-1", "0", "1", "17",
        String.valueOf(Long.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9),
        Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2) };

    BigInteger[] expected = { BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(-17),
        BigInteger.valueOf(-1), BigInteger.valueOf(0), BigInteger.valueOf(1),
        BigInteger.valueOf(17), BigInteger.valueOf(Long.MAX_VALUE), BigInteger.valueOf(7),
        BigInteger.valueOf(8), BigInteger.valueOf(9), BigInteger.valueOf(10),
        BigInteger.valueOf(11), BigInteger.valueOf(12) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], convert(input[i], BigInteger.class));
    }
  }

  public void shouldConvertBytes() {
    Object[] input = { String.valueOf(Byte.MIN_VALUE), "-17", "-1", "0", "1", "17",
        String.valueOf(Byte.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9),
        Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2) };

    Byte[] expected = { Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf((byte) -17), Byte.valueOf((byte) -1),
        Byte.valueOf((byte) 0), Byte.valueOf((byte) 1), Byte.valueOf((byte) 17), Byte.valueOf(Byte.MAX_VALUE),
        Byte.valueOf((byte) 7), Byte.valueOf((byte) 8), Byte.valueOf((byte) 9), Byte.valueOf((byte) 10),
        Byte.valueOf((byte) 11), Byte.valueOf((byte) 12) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], convert(input[i], Byte.class));
      assertEquals(expected[i], convert(input[i], Byte.TYPE));
    }
  }

  public void shouldConvertCalendarToLong() {
    Calendar calendarValue = Calendar.getInstance();
    assertEquals(Long.valueOf(calendarValue.getTime().getTime()), convert(calendarValue, Long.class));
  }

  /**
   * Date -> Long
   */
  @Test
  public void shouldConvertDateToLong() {
    Date dateValue = new Date();
    assertEquals(Long.valueOf(dateValue.getTime()), convert(dateValue, Long.class));
  }

  @Test
  public void shouldConvertXmlGregorianCalendarToLong() throws DatatypeConfigurationException {
    XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
    assertEquals(Long.valueOf(xmlGregorianCalendar.toGregorianCalendar().getTimeInMillis()), convert(xmlGregorianCalendar, Long.class));
  }

  public void shouldConvertDoubles() {
    Object[] input = { String.valueOf(Double.MIN_VALUE), "-17.2", "-1.1", "0.0", "1.1", "17.2",
        String.valueOf(Double.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9),
        Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2) };

    Double[] expected = { Double.valueOf(Double.MIN_VALUE), Double.valueOf(-17.2), Double.valueOf(-1.1),
        Double.valueOf(0.0), Double.valueOf(1.1), Double.valueOf(17.2), Double.valueOf(Double.MAX_VALUE),
        Double.valueOf(7), Double.valueOf(8), Double.valueOf(9), Double.valueOf(10), Double.valueOf(11.1),
        Double.valueOf(12.2) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].doubleValue(),
          ((Double) (convert(input[i], Double.class))).doubleValue(), 0.00001D);
      assertEquals(expected[i].doubleValue(),
          ((Double) (convert(input[i], Double.TYPE))).doubleValue(), 0.00001D);
    }
  }

  public void shouldConvertFloats() {
    Object[] input = { String.valueOf(Float.MIN_VALUE), "-17.2", "-1.1", "0.0", "1.1", "17.2",
        String.valueOf(Float.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9),
        Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2), };

    Float[] expected = { Float.valueOf(Float.MIN_VALUE), Float.valueOf(-17.2f), Float.valueOf(-1.1f),
        Float.valueOf(0.0f), Float.valueOf(1.1f), Float.valueOf(17.2f), Float.valueOf(Float.MAX_VALUE), Float.valueOf(7),
        Float.valueOf(8), Float.valueOf(9), Float.valueOf(10), Float.valueOf(11.1f), Float.valueOf(12.2f) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i].floatValue(),
          ((Float) (convert(input[i], Float.class))).floatValue(), 0.00001);
      assertEquals(expected[i].floatValue(),
          ((Float) (convert(input[i], Float.TYPE))).floatValue(), 0.00001);

    }
  }

  public void shouldConvertIntegers() {
    Object[] input = { String.valueOf(Integer.MIN_VALUE), "-17", "-1", "0", "1", "17",
        String.valueOf(Integer.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8),
        Integer.valueOf(9), Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2) };

    Integer[] expected = { Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(-17), Integer.valueOf(-1),
        Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(17), Integer.valueOf(Integer.MAX_VALUE),
        Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9), Integer.valueOf(10), Integer.valueOf(11),
        Integer.valueOf(12) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], convert(input[i], Integer.class));
      assertEquals(expected[i], convert(input[i], Integer.TYPE));

    }
  }

  public void shouldConvertLongs() {
    Object[] input = { String.valueOf(Long.MIN_VALUE), "-17", "-1", "0", "1", "17",
        String.valueOf(Long.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9),
        Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2) };

    Long[] expected = { Long.valueOf(Long.MIN_VALUE), Long.valueOf(-17), Long.valueOf(-1), Long.valueOf(0),
        Long.valueOf(1), Long.valueOf(17), Long.valueOf(Long.MAX_VALUE), Long.valueOf(7), Long.valueOf(8), Long.valueOf(9),
        Long.valueOf(10), Long.valueOf(11), Long.valueOf(12) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], convert(input[i], Long.class));
      assertEquals(expected[i], convert(input[i], Long.TYPE));
    }
  }

  public void shouldConvertShorts() {
    Object[] input = { String.valueOf(Short.MIN_VALUE), "-17", "-1", "0", "1", "17",
        String.valueOf(Short.MAX_VALUE), Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9),
        Long.valueOf(10), Float.valueOf(11.1f), Double.valueOf(12.2) };

    Short[] expected = { Short.valueOf(Short.MIN_VALUE), Short.valueOf((short) -17), Short.valueOf((short) -1),
        Short.valueOf((short) 0), Short.valueOf((short) 1), Short.valueOf((short) 17),
        Short.valueOf(Short.MAX_VALUE), Short.valueOf((short) 7), Short.valueOf((short) 8),
        Short.valueOf((short) 9), Short.valueOf((short) 10), Short.valueOf((short) 11), Short.valueOf((short) 12) };

    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], convert(input[i], Short.class));
      assertEquals(expected[i], convert(input[i], Short.TYPE));
    }
  }

  /**
   * Calendar -> Integer
   */
  @Test(expectedExceptions = MappingException.class)
  public void shouldThrowOnMapCalendarToInteger() {
    convert(Calendar.getInstance(), Integer.class);
  }

  /**
   * Date -> Integer
   */
  @Test(expectedExceptions = MappingException.class)
  public void shouldThrowOnMapDateToInteger() {
    convert(new Date(), Integer.class);
  }

  @Test(expectedExceptions = MappingException.class)
  public void shouldThrowOnNotANumber() {
    convert("XXXX", Integer.class);
  }

  @Test(dataProvider = "typesProvider")
  public void testBooleanToNumber(Class<?> type) {
    assertEquals(0, ((Number) convert(Boolean.FALSE, type)).intValue());
    assertEquals(1, ((Number) convert(Boolean.TRUE, type)).intValue());
  }

  public void testInvalidByteAmount() {
    Long min = Long.valueOf(Byte.MIN_VALUE);
    Long max = Long.valueOf(Byte.MAX_VALUE);
    Long minMinusOne = Long.valueOf(min.longValue() - 1);
    Long maxPlusOne = Long.valueOf(max.longValue() + 1);

    assertEquals(Byte.valueOf(Byte.MIN_VALUE), convert(min, Byte.class));
    assertEquals(Byte.valueOf(Byte.MAX_VALUE), convert(max, Byte.class));

    try {
      assertEquals(null, convert(minMinusOne, Byte.class));
      fail();
    } catch (Exception e) {
    }

    try {
      assertEquals(null, convert(maxPlusOne, Byte.class));
      fail();
    } catch (Exception e) {
    }
  }

  public void testInvalidFloatAmount() {
    Double max = Double.valueOf(Float.MAX_VALUE);
    Double tooBig = Double.valueOf(Double.MAX_VALUE);

    assertEquals(Float.valueOf(Float.MAX_VALUE), convert(max, Float.class));

    try {
      assertEquals(null, convert(tooBig, Float.class));
      fail("More than maximum, expected ConversionException");
    } catch (Exception expected) {
    }
  }

  public void testInvalidIntegerAmount() {
    Long min = Long.valueOf(Integer.MIN_VALUE);
    Long max = Long.valueOf(Integer.MAX_VALUE);
    Long minMinusOne = Long.valueOf(min.longValue() - 1);
    Long maxPlusOne = Long.valueOf(max.longValue() + 1);

    assertEquals(Integer.valueOf(Integer.MIN_VALUE), convert(min, Integer.class));
    assertEquals(Integer.valueOf(Integer.MAX_VALUE), convert(max, Integer.class));

    try {
      assertEquals(null, convert(minMinusOne, Integer.class));
      fail("Less than minimum, expected ConversionException");
    } catch (Exception expected) {
    }

    try {
      assertEquals(null, convert(maxPlusOne, Integer.class));
      fail("More than maximum, expected ConversionException");
    } catch (Exception expected) {
    }
  }

  public void testInvalidShortAmount() {
    Long min = Long.valueOf(Short.MIN_VALUE);
    Long max = Long.valueOf(Short.MAX_VALUE);
    Long minMinusOne = Long.valueOf(min.longValue() - 1);
    Long maxPlusOne = Long.valueOf(max.longValue() + 1);

    assertEquals(Short.valueOf(Short.MIN_VALUE), convert(min, Short.class));
    assertEquals(Short.valueOf(Short.MAX_VALUE), convert(max, Short.class));

    try {
      assertEquals(null, convert(minMinusOne, Short.class));
      fail("Less than minimum, expected ConversionException");
    } catch (Exception expected) {
    }

    try {
      assertEquals(null, convert(maxPlusOne, Short.class));
      fail("More than maximum, expected ConversionException");
    } catch (Exception expected) {
    }
  }

  @Test(dataProvider = "typesProvider")
  public void testConvertNumber(Class<?> type) {
    Object[] number = { Byte.valueOf((byte) 7), Short.valueOf((short) 8), Integer.valueOf(9), Long.valueOf(10),
        Float.valueOf(11.1f), Double.valueOf(12.2), new BigDecimal("17.2"), new BigInteger("33") };

    for (int i = 0; i < number.length; i++) {
      Object val = convert(number[i], type);
      assertNotNull(val);
      assertTrue(type.isInstance(val));
    }
  }

  public void testMatches() {
    Class<?>[] sourceTypes = { Byte.class, Byte.TYPE, Short.class, Short.TYPE, Integer.class,
        Integer.TYPE, Long.class, Long.TYPE, Float.class, Float.TYPE, Double.class, Double.TYPE,
        BigDecimal.class, BigInteger.class, Boolean.class, Boolean.TYPE, Date.class,
        Calendar.class, String.class, XMLGregorianCalendar.class };
    Class<?>[] destinationTypes = { Byte.class, Byte.TYPE, Short.class, Short.TYPE, Integer.class,
        Integer.TYPE, Long.class, Long.TYPE, Float.class, Float.TYPE, Double.class, Double.TYPE,
        BigDecimal.class, BigInteger.class };

    for (Class<?> sourceType : sourceTypes)
      for (Class<?> destinationType : destinationTypes)
        assertEquals(converter.match(sourceType, destinationType), MatchResult.FULL);

    // Negative
    assertEquals(converter.match(Object[].class, ArrayList.class), MatchResult.NONE);
    assertEquals(converter.match(Number.class, Boolean.class), MatchResult.NONE);
  }

  @Test(expectedExceptions = MappingException.class, dataProvider = "numbersProvider")
  public void testStringToNumber(Number number) {
    Object[][] types = provideTypes();

    for (int i = 0; i < types.length; i++) {
      Number result = (Number) convert(number.toString(), (Class<?>) types[i][0]);
      assertEquals(result.longValue(), number.longValue());
    }

    for (int i = 0; i < types.length; i++)
      convert("12x", (Class<?>) types[i][0]);
  }
}
