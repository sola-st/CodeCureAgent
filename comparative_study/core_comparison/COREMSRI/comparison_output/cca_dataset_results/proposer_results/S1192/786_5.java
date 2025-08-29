package com.jnape.palatable.lambda.adt.hmap;

import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.NoSuchElementException;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.adt.hmap.HMap.emptyHMap;
import static com.jnape.palatable.lambda.adt.hmap.HMap.hMap;
import static com.jnape.palatable.lambda.adt.hmap.HMap.singletonHMap;
import static com.jnape.palatable.lambda.adt.hmap.TypeSafeKey.typeSafeKey;
import static com.jnape.palatable.lambda.optics.Iso.simpleIso;
import static java.math.BigInteger.ONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static testsupport.matchers.IterableMatcher.iterates;

public class HMapTest {

    private static final String STRING_VALUE = "string value";
    private static final String NEW_VALUE = "new value";
    private static final String STRING = "string";
    private static final String ANOTHER_STRING_VALUE = "another string value";
    private static final String STRING_KEY = "string key";

    @Test
    public void getForPresentKey() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();
        assertEquals(just(STRING_VALUE),
                     singletonHMap(stringKey, STRING_VALUE).get(stringKey));
    }

    @Test
    public void getForAbsentKey() {
        assertEquals(nothing(),
                     singletonHMap(typeSafeKey(), STRING_VALUE)
                             .get(typeSafeKey()));
    }

    @Test
    public void isEmpty() {
        assertTrue(emptyHMap().isEmpty());
        assertFalse(singletonHMap(typeSafeKey(), "foo").isEmpty());
    }

    @Test
    public void storesTypeSafeKeyBaseValue() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();
        TypeSafeKey<String, Long> longKey = stringKey.andThen(simpleIso(Long::parseLong,
                                                                        String::valueOf));
        TypeSafeKey<String, BigInteger> bigIntegerKey = longKey.andThen(simpleIso(BigInteger::valueOf,
                                                                                  BigInteger::longValue));

        HMap hMap = singletonHMap(stringKey, "1");
        assertEquals(just("1"), hMap.get(stringKey));
        assertEquals(just(1L), hMap.get(longKey));
        assertEquals(just(ONE), hMap.get(bigIntegerKey));

        assertNotEquals(typeSafeKey(), typeSafeKey());

        assertEquals(emptyHMap().put(longKey, 1L).get(longKey), emptyHMap().put(stringKey, "1").get(longKey));
        assertEquals(emptyHMap().put(stringKey, "1").get(stringKey), emptyHMap().put(longKey, 1L).get(stringKey));
        assertEquals(emptyHMap().put(stringKey, "1").get(stringKey),
                     emptyHMap().put(bigIntegerKey, ONE).get(stringKey));

        assertEquals(singletonHMap(stringKey, "1"), singletonHMap(longKey, 1L));
        assertEquals(singletonHMap(stringKey, "1"), singletonHMap(bigIntegerKey, ONE));
        assertEquals(singletonHMap(longKey, 1L), singletonHMap(bigIntegerKey, ONE));
    }

    @Test
    public void getForPresentKeyWithNullValue() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();
        assertEquals(nothing(),
                     singletonHMap(stringKey, null).get(stringKey));
    }

    @Test
    public void put() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();
        assertEquals(singletonHMap(stringKey, STRING_VALUE),
                     emptyHMap().put(stringKey, STRING_VALUE));

        assertEquals(singletonHMap(stringKey, NEW_VALUE),
                     emptyHMap()
                             .put(stringKey, STRING_VALUE)
                             .put(stringKey, NEW_VALUE));
    }

    @Test
    public void putAll() {
        TypeSafeKey<String, String>   stringKey1 = typeSafeKey();
        TypeSafeKey<String, String>   stringKey2 = typeSafeKey();
        TypeSafeKey<Integer, Integer> intKey     = typeSafeKey();

        HMap left = hMap(stringKey1, STRING_VALUE,
                         intKey, 1);
        HMap right = hMap(stringKey2, ANOTHER_STRING_VALUE,
                          intKey, 2);

        assertEquals(hMap(stringKey1, STRING_VALUE,
                          stringKey2, ANOTHER_STRING_VALUE,
                          intKey, 2),
                     left.putAll(right));
        assertEquals(hMap(stringKey1, STRING_VALUE,
                          stringKey2, ANOTHER_STRING_VALUE,
                          intKey, 1),
                     right.putAll(left));
    }

    @Test
    public void remove() {
        TypeSafeKey<String, String> stringKey1 = typeSafeKey();
        TypeSafeKey<String, String> stringKey2 = typeSafeKey();
        assertEquals(emptyHMap(),
                     emptyHMap()
                             .put(stringKey1, STRING_VALUE)
                             .remove(stringKey1));

        assertEquals(singletonHMap(stringKey2, ANOTHER_STRING_VALUE),
                     emptyHMap()
                             .put(stringKey1, STRING_VALUE)
                             .put(stringKey2, ANOTHER_STRING_VALUE)
                             .remove(stringKey1));
    }

    @Test
    public void removeAll() {
        TypeSafeKey<String, String> stringKey1 = typeSafeKey();
        TypeSafeKey<String, String> stringKey2 = typeSafeKey();

        HMap hMap1 = hMap(stringKey1, "foo",
                          stringKey2, "bar");
        HMap hMap2 = singletonHMap(stringKey1, "foo");

        assertEquals(singletonHMap(stringKey2, "bar"),
                     hMap1.removeAll(hMap2));
    }

    @Test
    public void containsKey() {
        TypeSafeKey<String, String>   stringKey1 = typeSafeKey();
        TypeSafeKey<String, String>   stringKey2 = typeSafeKey();
        TypeSafeKey<Integer, Integer> intKey     = typeSafeKey();

        HMap hMap = singletonHMap(stringKey1, STRING);

        assertTrue(hMap.containsKey(stringKey1));
        assertFalse(hMap.containsKey(stringKey2));
        assertFalse(hMap.containsKey(intKey));
    }

    @Test
    public void demandForPresentKey() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();
        assertEquals(STRING_VALUE,
                     singletonHMap(stringKey, STRING_VALUE).demand(stringKey));
    }

    @Test(expected = NoSuchElementException.class)
    public void demandForAbsentKey() {
        emptyHMap().demand(typeSafeKey());
    }

    @Test
    @SuppressWarnings("serial")
    public void toMap() {
        TypeSafeKey<String, String>   stringKey = typeSafeKey();
        TypeSafeKey<Integer, Integer> intKey    = typeSafeKey();

        assertEquals(new HashMap<TypeSafeKey<?, ?>, Object>() {{
            put(stringKey, STRING);
            put(intKey, 1);
        }}, hMap(stringKey, STRING,
                 intKey, 1).toMap());
    }

    @Test
    public void iteratesKVPairsAsTuples() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();

        assertThat(singletonHMap(stringKey, STRING_VALUE),
                   iterates(tuple(stringKey, STRING_VALUE)));
    }

    @Test
    public void keys() {
        TypeSafeKey<String, String> stringKey = typeSafeKey();

        assertThat(singletonHMap(stringKey, STRING_VALUE).keys(),
                   iterates(stringKey));
    }

    @Test
    public void values() {
        assertThat(singletonHMap(typeSafeKey(), STRING_VALUE).values(),
                   iterates(STRING_VALUE));
    }

    @Test
    public void convenienceStaticFactoryMethods() {
        TypeSafeKey.Simple<String>    stringKey = typeSafeKey();
        TypeSafeKey.Simple<Integer>   intKey    = typeSafeKey();
        TypeSafeKey.Simple<Float>     floatKey  = typeSafeKey();
        TypeSafeKey.Simple<Byte>      byteKey   = typeSafeKey();
        TypeSafeKey.Simple<Short>     shortKey  = typeSafeKey();
        TypeSafeKey.Simple<Long>      longKey   = typeSafeKey();
        TypeSafeKey.Simple<Double>    doubleKey = typeSafeKey();
        TypeSafeKey.Simple<Character> charKey   = typeSafeKey();

        HMap m1 = emptyHMap().put(stringKey, STRING_VALUE);
        HMap m2 = m1.put(intKey, 1);
        HMap m3 = m2.put(floatKey, 1f);
        HMap m4 = m3.put(byteKey, (byte) 1);
        HMap m5 = m4.put(shortKey, (short) 1);
        HMap m6 = m5.put(longKey, 1L);
        HMap m7 = m6.put(doubleKey, 1D);
        HMap m8 = m7.put(charKey, '1');

        assertEquals(m1,
                     singletonHMap(stringKey, STRING_VALUE));

        assertEquals(m2,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1));

        assertEquals(m3,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1,
                          floatKey, 1f));

        assertEquals(m4,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1,
                          floatKey, 1f,
                          byteKey, (byte) 1));

        assertEquals(m5,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1,
                          floatKey, 1f,
                          byteKey, (byte) 1,
                          shortKey, (short) 1));

        assertEquals(m6,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1,
                          floatKey, 1f,
                          byteKey, (byte) 1,
                          shortKey, (short) 1,
                          longKey, 1L));

        assertEquals(m7,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1,
                          floatKey, 1f,
                          byteKey, (byte) 1,
                          shortKey, (short) 1,
                          longKey, 1L,
                          doubleKey, 1D));

        assertEquals(m8,
                     hMap(stringKey, STRING_VALUE,
                          intKey, 1,
                          floatKey, 1f,
                          byteKey, (byte) 1,
                          shortKey, (short) 1,
                          longKey, 1L,
                          doubleKey, 1D,
                          charKey, '1'));
    }

    @Test
    public void equality() {
        assertEquals(emptyHMap(), emptyHMap());

        TypeSafeKey<String, String> stringKey = typeSafeKey();
        assertEquals(emptyHMap().put(stringKey, "one"), emptyHMap().put(stringKey, "one"));

        assertNotEquals(emptyHMap(), emptyHMap().put(stringKey, STRING_KEY));
        assertNotEquals(emptyHMap().put(stringKey, STRING_KEY), emptyHMap());
        assertNotEquals(emptyHMap().put(typeSafeKey(), "one"), emptyHMap().put(typeSafeKey(), "one"));
        assertNotEquals(emptyHMap().put(typeSafeKey(), "one"), emptyHMap().put(typeSafeKey(), 1));
        assertNotEquals(emptyHMap().put(typeSafeKey(), 1), emptyHMap().put(typeSafeKey(), "one"));
    }

    @Test
    public void hashCodeUsesDecentDistribution() {
        assertEquals(emptyHMap().hashCode(), emptyHMap().hashCode());
        TypeSafeKey<String, String> stringKey = typeSafeKey();
        assertEquals(singletonHMap(stringKey, STRING_VALUE).hashCode(),
                     singletonHMap(stringKey, STRING_VALUE).hashCode());

        assertNotEquals(emptyHMap(), singletonHMap(stringKey, STRING_VALUE));
        assertNotEquals(singletonHMap(stringKey, STRING_VALUE),
                        singletonHMap(stringKey, ANOTHER_STRING_VALUE));
    }

    @Test
    public void emptyHMapReusesInstance() {
        assertSame(emptyHMap(), emptyHMap());
    }
}