package com.jnape.palatable.lambda.optics.lenses;

import com.jnape.palatable.lambda.optics.Lens;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.optics.Iso.iso;
import static com.jnape.palatable.lambda.optics.functions.Set.set;
import static com.jnape.palatable.lambda.optics.functions.View.view;
import static com.jnape.palatable.lambda.optics.lenses.MapLens.keys;
import static com.jnape.palatable.lambda.optics.lenses.MapLens.mappingValues;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static testsupport.assertion.LensAssert.assertLensLawfulness;
import static testsupport.matchers.IterableMatcher.iterates;

@SuppressWarnings("serial")
public class MapLensTest {

    @Test
    public void asCopy() {
        assertLensLawfulness(MapLens.asCopy(),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()));
    }

    @Test
    public void asCopyWithCopyFn() {
        assertLensLawfulness(MapLens.asCopy(LinkedHashMap::new),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()));

        assertThat(view(MapLens.asCopy(LinkedHashMap::new), createLinkedHashMapFooBarBaz()).keySet(), iterates("foo", "bar", "baz"));
    }

    @Test
    public void valueAt() {
        assertLensLawfulness(MapLens.valueAt("foo"),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()),
                             asList(nothing(), just(1)));
    }

    @Test
    public void valueAtWithCopyFn() {
        assertLensLawfulness(MapLens.valueAt("foo"),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()),
                             asList(nothing(), just(1)));
    }


    @Test
    public void valueAtWithDefaultValue() {
        Lens.Simple<Map<String, Integer>, Integer> atFoo = MapLens.valueAt("foo", -1);

        assertEquals((Integer) 1, view(atFoo, createHashMapFooBarBaz()));
        assertEquals((Integer) (-1), view(atFoo, emptyMap()));

        Map<String, Integer> updated = set(atFoo, 11, createHashMapFooBarBaz());
        assertEquals(createHashMapFooBarBazUpdated(), updated);
        assertNotSame(createHashMapFooBarBaz(), updated);
    }

    @Test
    public void keysFocusesOnKeys() {
        assertLensLawfulness(keys(),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBazWithQuux(), createHashMapFooBazQuux()),
                             asList(emptySet(), singleton("foo"), new HashSet<>(asList("foo", "bar", "baz", "quux")), new HashSet<>(asList("foo", "baz", "quux"))));
    }

    @Test
    public void valuesFocusesOnValues() {
        Lens.Simple<Map<String, Integer>, Collection<Integer>> values = MapLens.values();

        assertThat(view(values, createHashMapFooBarBaz()), hasItems(2, 1, 3));

        Map<String, Integer> updated = set(values, asList(1, 2), createHashMapFooBarBaz());
        assertEquals(createHashMapFooBar(), updated);
        assertNotSame(createHashMapFooBarBaz(), updated);
    }

    @Test
    public void invertedFocusesOnMapWithKeysAndValuesSwitched() {
        assertLensLawfulness(MapLens.inverted(),
                             asList(emptyMap(), singletonMap("foo", 1), createHashMapFooBarBaz()),
                             asList(emptyMap(), singletonMap(1, "foo"), createHashMapOneTwoThree()));
    }

    @Test
    public void mappingValuesWithIsoRetainsMapStructureWithMappedValues() {
        assertLensLawfulness(mappingValues(iso(Integer::parseInt, Object::toString)),
                             asList(emptyMap(),
                                    singletonMap("foo", "1"),
                                    unmodifiableMap(createHashMapFooBarBazString())),
                             asList(emptyMap(),
                                    singletonMap("foo", 1),
                                    unmodifiableMap(createHashMapFooBarBaz())));
    }

    // Helper methods to replace double brace initialization
    private static HashMap<String, Integer> createHashMapFooBarBaz() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("foo", 1);
        map.put("bar", 2);
        map.put("baz", 3);
        return map;
    }

    private static HashMap<String, Integer> createHashMapFooBarBazUpdated() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("foo", 11);
        map.put("bar", 2);
        map.put("baz", 3);
        return map;
    }

    private static LinkedHashMap<String, Integer> createLinkedHashMapFooBarBaz() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("foo", 1);
        map.put("bar", 2);
        map.put("baz", 3);
        return map;
    }

    private static HashMap<String, Integer> createHashMapFooBar() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("foo", 1);
        map.put("bar", 2);
        return map;
    }

    private static HashMap<String, Integer> createHashMapFooBarBazWithQuux() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("foo", 1);
        map.put("bar", 2);
        map.put("baz", 3);
        map.put("quux", 4);
        return map;
    }

    private static HashMap<String, Integer> createHashMapFooBazQuux() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("foo", 1);
        map.put("baz", 3);
        map.put("quux", 4);
        return map;
    }

    private static HashMap<Integer, String> createHashMapOneTwoThree() {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "foo");
        map.put(2, "bar");
        map.put(3, "baz");
        return map;
    }

    private static HashMap<String, String> createHashMapFooBarBazString() {
        HashMap<String, String> map = new HashMap<>();
        map.put("foo", "1");
        map.put("bar", "2");
        map.put("baz", "3");
        return map;
    }
}