package com.jnape.palatable.lambda.internal.iteration;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.traitor.annotations.TestTraits;
import com.jnape.palatable.traitor.runners.Traits;
import org.junit.runner.RunWith;
import testsupport.traits.Deforesting;

@RunWith(Traits.class)
public class DistinctIterableTest {

    @TestTraits({Deforesting.class})
    public <T> Fn1<Iterable<T>, Iterable<T>> testSubject() {
        return DistinctIterable::new;
    }
}