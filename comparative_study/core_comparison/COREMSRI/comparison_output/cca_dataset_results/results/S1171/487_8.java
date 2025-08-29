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
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()));

        // static initializer to put values into the example map for asCopy()
    }

    @Test
    public void asCopyWithCopyFn() {
        assertLensLawfulness(MapLens.asCopy(LinkedHashMap::new),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()));

        LinkedHashMap<String, Integer> exampleMap = new LinkedHashMap<>();
        exampleMap.put("foo", 1);
        exampleMap.put("bar", 2);
        exampleMap.put("baz", 3);
        assertThat(view(MapLens.asCopy(LinkedHashMap::new), exampleMap).keySet(), iterates("foo", "bar", "baz"));
    }

    @Test
    public void valueAt() {
        assertLensLawfulness(MapLens.valueAt("foo"),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()),
                             asList(nothing(), just(1)));
    }

    @Test
    public void valueAtWithCopyFn() {
        assertLensLawfulness(MapLens.valueAt("foo"),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()),
                             asList(nothing(), just(1)));
    }


    @Test
    public void valueAtWithDefaultValue() {
        Lens.Simple<Map<String, Integer>, Integer> atFoo = MapLens.valueAt("foo", -1);

        HashMap<String, Integer> map1 = new HashMap<>();
        map1.put("foo", 1);
        map1.put("bar", 2);
        map1.put("baz", 3);

        assertEquals((Integer) 1, view(atFoo, map1));
        assertEquals((Integer) (-1), view(atFoo, emptyMap()));

        HashMap<String, Integer> map2 = new HashMap<>();
        map2.put("foo", 1);
        map2.put("bar", 2);
        map2.put("baz", 3);

        Map<String, Integer> updated = set(atFoo, 11, map2);
        HashMap<String, Integer> expected = new HashMap<>();
        expected.put("foo", 11);
        expected.put("bar", 2);
        expected.put("baz", 3);

        assertEquals(expected, updated);

        HashMap<String, Integer> original = new HashMap<>();
        original.put("foo", 1);
        original.put("bar", 2);
        original.put("baz", 3);

        assertNotSame(original, updated);
    }

    @Test
    public void keysFocusesOnKeys() {
        assertLensLawfulness(keys(),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()),
                             asList(emptySet(), singleton("foo"),
                                    new HashSet<>(asList("foo", "bar", "baz", "quux")),
                                    new HashSet<>(asList("foo", "baz", "quux"))));
    }

    @Test
    public void valuesFocusesOnValues() {
        Lens.Simple<Map<String, Integer>, Collection<Integer>> values = MapLens.values();

        HashMap<String, Integer> exampleMap = new HashMap<>();
        exampleMap.put("foo", 1);
        exampleMap.put("bar", 2);
        exampleMap.put("baz", 3);

        assertThat(view(values, exampleMap), hasItems(2, 1, 3));

        HashMap<String, Integer> map2 = new HashMap<>();
        map2.put("foo", 1);
        map2.put("bar", 2);
        map2.put("baz", 3);

        Map<String, Integer> updated = set(values, asList(1, 2), map2);

        HashMap<String, Integer> expected = new HashMap<>();
        expected.put("foo", 1);
        expected.put("bar", 2);

        assertEquals(expected, updated);

        HashMap<String, Integer> original = new HashMap<>();
        original.put("foo", 1);
        original.put("bar", 2);
        original.put("baz", 3);

        assertNotSame(original, updated);
    }

    @Test
    public void invertedFocusesOnMapWithKeysAndValuesSwitched() {
        assertLensLawfulness(MapLens.inverted(),
                             asList(emptyMap(), singletonMap("foo", 1),
                                    new HashMap<String, Integer>()),
                             asList(emptyMap(), singletonMap(1, "foo"),
                                    new HashMap<Integer, String>()));
    }

    @Test
    public void mappingValuesWithIsoRetainsMapStructureWithMappedValues() {
        assertLensLawfulness(mappingValues(iso(Integer::parseInt, Object::toString)),
                             asList(emptyMap(),
                                    singletonMap("foo", "1"),
                                    unmodifiableMap(new HashMap<String, String>())),
                             asList(emptyMap(),
                                    singletonMap("foo", 1),
                                    unmodifiableMap(new HashMap<String, Integer>())));
    }
}
