package com.jnape.palatable.lambda.adt.hlist;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.hlist.HList.HCons;
import com.jnape.palatable.lambda.adt.product.Product5;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.builtin.fn2.Into;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;
import com.jnape.palatable.lambda.functions.specialized.Pure;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.Bifunctor;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.MonadRec;
import com.jnape.palatable.lambda.traversable.Traversable;

import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Uncons.uncons;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;

/**
 * A 5-element tuple product type, implemented as a specialized HList. Supports random access.
 *
 * @param <T1> The first slot element type
 * @param <T2> The second slot element type
 * @param <T3> The third slot element type
 * @param <T4> The fourth slot element type
 * @param <T5> The fifth slot element type
 * @see Product5
 * @see HList
 * @see SingletonHList
 * @see Tuple2
 * @see Tuple3
 * @see Tuple4
 */
public class Tuple5<T1, T2, T3, T4, T5> extends HCons<T1, Tuple4<T2, T3, T4, T5>> implements
        Product5<T1, T2, T3, T4, T5>,
        MonadRec<T5, Tuple5<T1, T2, T3, T4, ?>>,
        Bifunctor<T4, T5, Tuple5<T1, T2, T3, ?, ?>>,
        Traversable<T5, Tuple5<T1, T2, T3, T4, ?>> {

    private final T1 _1;
    private final T2 _2;
    private final T3 _3;
    private final T4 _4;
    private final T5 _5;

    Tuple5(T1 _1, Tuple4<T2, T3, T4, T5> tail) {
        super(_1, tail);
        this._1 = _1;
        _2      = tail._1();
        _3      = tail._2();
        _4      = tail._3();
        _5      = tail._4();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T0> Tuple6<T0, T1, T2, T3, T4, T5> cons(T0 _0) {
        return new Tuple6<>(_0, this);
    }

    /**
     * Snoc an element onto the back of this {@link Tuple5}.
     *
     * @param _6   the new last element
     * @param <T6> the new last element type
     * @return the new {@link Tuple6}
     */
    public <T6> Tuple6<T1, T2, T3, T4, T5, T6> snoc(T6 _6) {
        return tuple(_1, _2, _3, _4, _5, _6);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T1 _1() {
        return _1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T2 _2() {
        return _2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T3 _3() {
        return _3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T4 _4() {
        return _4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T5 _5() {
        return _5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T2, T3, T4, T5, T1> rotateL5() {
        return tuple(_2, _3, _4, _5, _1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T5, T1, T2, T3, T4> rotateR5() {
        return tuple(_5, _1, _2, _3, _4);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T2, T3, T4, T1, T5> rotateL4() {
        return tuple(_2, _3, _4, _1, _5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T4, T1, T2, T3, T5> rotateR4() {
        return tuple(_4, _1, _2, _3, _5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T2, T3, T1, T4, T5> rotateL3() {
        return tuple(_2, _3, _1, _4, _5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T3, T1, T2, T4, T5> rotateR3() {
        return tuple(_3, _1, _2, _4, _5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tuple5<T2, T1, T3, T4, T5> invert() {
        return tuple(_2, _1, _3, _4, _5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> fmap(Fn1<? super T5, ? extends T5Prime> fn) {
        return MonadRec.super.<T5Prime>fmap(fn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T4Prime> Tuple5<T1, T2, T3, T4Prime, T5> biMapL(Fn1<? super T4, ? extends T4Prime> fn) {
        return (Tuple5<T1, T2, T3, T4Prime, T5>) Bifunctor.super.<T4Prime>biMapL(fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> biMapR(Fn1<? super T5, ? extends T5Prime> fn) {
        return (Tuple5<T1, T2, T3, T4, T5Prime>) Bifunctor.super.<T5Prime>biMapR(fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T4Prime, T5Prime> Tuple5<T1, T2, T3, T4Prime, T5Prime> biMap(Fn1<? super T4, ? extends T4Prime> lFn,
                                                                         Fn1<? super T5, ? extends T5Prime> rFn) {
        return new Tuple5<>(_1(), tail().biMap(lFn, rFn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> pure(T5Prime _5Prime) {
        return tuple(_1, _2, _3, _4, _5Prime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> zip(
            Applicative<Fn1<? super T5, ? extends T5Prime>, Tuple5<T1, T2, T3, T4, ?>> appFn) {
        return MonadRec.super.zip(appFn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Lazy<Tuple5<T1, T2, T3, T4, T5Prime>> lazyZip(
            Lazy<? extends Applicative<Fn1<? super T5, ? extends T5Prime>, Tuple5<T1, T2, T3, T4, ?>>> lazyAppFn) {
        return MonadRec.super.lazyZip(lazyAppFn).fmap(Monad<T5Prime, Tuple5<T1, T2, T3, T4, ?>>::coerce);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> discardL(Applicative<T5Prime, Tuple5<T1, T2, T3, T4, ?>> appB) {
        return MonadRec.super.discardL(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5> discardR(Applicative<T5Prime, Tuple5<T1, T2, T3, T4, ?>> appB) {
        return MonadRec.super.discardR(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> flatMap(
            Fn1<? super T5, ? extends Monad<T5Prime, Tuple5<T1, T2, T3, T4, ?>>> f) {
        return pure(f.apply(_5).<Tuple5<T1, T2, T3, T4, T5Prime>>coerce()._5());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime> Tuple5<T1, T2, T3, T4, T5Prime> trampolineM(
            Fn1<? super T5, ? extends MonadRec<RecursiveResult<T5, T5Prime>, Tuple5<T1, T2, T3, T4, ?>>> fn) {
        return fmap(trampoline(x -> fn.apply(x).<Tuple5<T1, T2, T3, T4, RecursiveResult<T5, T5Prime>>>coerce()._5()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T5Prime, App extends Applicative<?, App>, TravB extends Traversable<T5Prime, Tuple5<T1, T2, T3, T4, ?>>,
            AppTrav extends Applicative<TravB, App>> AppTrav traverse(
            Fn1<? super T5, ? extends Applicative<T5Prime, App>> fn,
            Fn1<? super TravB, ? extends AppTrav> pure) {
        return fn.apply(_5).fmap(_3Prime -> fmap(constantly(_3Prime))).<TravB>fmap(Applicative::coerce).coerce();
    }

    /**
     * Given a value of type <code>A</code>, produced an instance of this tuple with each slot set to that value.
     *
     * @param a   the value to fill the tuple with
     * @param <A> the value type
     * @return the filled tuple
     * @see Tuple2#fill
     */
    public static <A> Tuple5<A, A, A, A, A> fill(A a) {
        return tuple(a, a, a, a, a);
    }

    /**
     * Return {@link Maybe#just(Object) just} the first five elements from the given {@link Iterable}, or
     * {@link Maybe#nothing() nothing} if there are less than five elements.
     *
     * @param as  the {@link Iterable}
     * @param <A> the {@link Iterable} element type
     * @return {@link Maybe} the first five elements of the given {@link Iterable}
     */
    public static <A> Maybe<Tuple5<A, A, A, A, A>> fromIterable(Iterable<A> as) {
        return uncons(as).flatMap(Into.into((head, tail) -> Tuple4.fromIterable(tail).fmap(t -> t.cons(head))));
    }

    /**
     * The canonical {@link Pure} instance for {@link Tuple5}.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @param <T4> the fourth element type
     * @return the {@link Pure} instance
     */
    public static <T1, T2, T3, T4> Pure<Tuple5<T1, T2, T3, T4, ?>> pureTuple(T1 _1, T2 _2, T3 _3, T4 _4) {
        return new Pure<Tuple5<T1, T2, T3, T4, ?>>() {
            @Override
            public <T5> Tuple5<T1, T2, T3, T4, T5> checkedApply(T5 _5) throws Throwable {
                return tuple(_1, _2, _3, _4, _5);
            }
        };
    }
}