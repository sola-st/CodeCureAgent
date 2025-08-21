package com.jnape.palatable.lambda.functions;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

import static org.junit.Assert.assertEquals;

public class Fn0Test {

    @Test
    public void fromSupplier() {
        IntSupplier supplier = () -> 1;
        Fn0<Integer>      fn0      = Fn0.fromSupplier(supplier::getAsInt);
        assertEquals((Integer) 1, fn0.apply());
    }

    @Test
    public void fromCallable() {
        Callable<Integer> callable = () -> 1;
        Fn0<Integer>      fn0      = Fn0.fromCallable(callable);
        assertEquals((Integer) 1, fn0.apply());
    }

    @Test
    public void toSupplier() {
        Fn0<Integer>      fn0      = () -> 1;
        IntSupplier supplier = fn0::apply;
        assertEquals(1, supplier.getAsInt());
    }

    @Test
    public void toCallable() throws Exception {
        Fn0<Integer>      fn0      = () -> 1;
        Callable<Integer> callable = fn0.toCallable();
        assertEquals((Integer) 1, callable.call());
    }
}