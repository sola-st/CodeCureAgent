```java
package com.jnape.palatable.lambda.adt.hlist;

import com.jnape.palatable.lambda.functions.builtin.fn1.Downcast;

import java.util.Objects;

/**
 * An immutable heterogeneous list supporting arbitrary depth type-safety via a linearly recursive type signature. Note
 * that due to its rapidly expanding type signature, specializations exist up to certain depths to minimize typing
 * overhead.
 *
 * @see SingletonHList
 * @see Tuple2
 * @see Tuple3
 * @see Tuple4
 * @see Tuple5
 * @see Tuple6
 */
public abstract class HList {

    private HList() {
    }

    /**
     * Cons an element onto the front of this HList.
     *
     * @param newHead   the new head element
     * @param <H> the new head type
     * @return the updated HList
     */
    public abstract <H> HCons<H, ? extends HList> cons(H newHead);

    @Override
    public final String toString() {
        StringBuilder body = new StringBuilder("HList{");

        HList next = this;
        while (next != HNil.INSTANCE) {
            HCons<?, ?> hCons = (HCons<?, ?>) next;
            body.append(" ").append(hCons.head).append(" ");
            next = hCons.tail;
            if (next != HNil.INSTANCE)
                body.append("::");
        }

        return body.append("}").toString();
    }

    /**
     * Static factory method for creating empty HLists.
     *
     * @return an empty HList
     */
    public static HNil nil() {
        return HNil.INSTANCE;
    }

    /**
     * Static factory method for creating an HList from the given head and tail.
     *
     * @param head   the head element
     * @param tail   the tail HList
     * @param <H> the head type
     * @param <T> the tail type
     * @return the newly created HList
     */
    public static <H, T extends HList> HCons<H, T> cons(H head, T tail) {
        return Downcast.<HCons<H, T>, HCons<H, ? extends HList>>downcast(tail.cons(head));
    }

    /**
     * Static factory method for creating a singleton HList.
     *
     * @param head   the head element
     * @param <H> the head element type
     * @return the singleton HList
     */
    public static <H> SingletonHList<H> singletonHList(H head) {
        return new SingletonHList<>(head);
    }

    /**
     * Static factory method for creating a 2-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param <A> the head element type
     * @param <B> the second element type
     * @return the 2-element HList
     * @see Tuple2
     */
    public static <A, B> Tuple2<A, B> tuple(A _1, B _2) {
        return singletonHList(_2).cons(_1);
    }

    /**
     * Static factory method for creating a 3-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param <A> the head element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @return the 3-element HList
     * @see Tuple3
     */
    public static <A, B, C> Tuple3<A, B, C> tuple(A _1, B _2, C _3) {
        return tuple(_2, _3).cons(_1);
    }

    /**
     * Static factory method for creating a 4-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param <A> the head element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @param <D> the fourth element type
     * @return the 4-element HList
     * @see Tuple4
     */
    public static <A, B, C, D> Tuple4<A, B, C, D> tuple(A _1, B _2, C _3, D _4) {
        return tuple(_2, _3, _4).cons(_1);
    }

    /**
     * Static factory method for creating a 5-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param _5   the fifth element
     * @param <A> the head element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @param <D> the fourth element type
     * @param <E> the fifth element type
     * @return the 5-element HList
     * @see Tuple5
     */
    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> tuple(A _1, B _2, C _3, D _4, E _5) {
        return tuple(_2, _3, _4, _5).cons(_1);
    }

    /**
     * Static factory method for creating a 6-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param _5   the fifth element
     * @param _6   the sixth element
     * @param <A> the head element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @param <D> the fourth element type
     * @param <E> the fifth element type
     * @param <F> the sixth element type
     * @return the 6-element HList
     * @see Tuple6
     */
    public static <A, B, C, D, E, F> Tuple6<A, B, C, D, E, F> tuple(A _1, B _2, C _3, D _4, E _5,
                                                                                F _6) {
        return tuple(_2, _3, _4, _5, _6).cons(_1);
    }

    /**
     * Static factory method for creating a 7-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param _5   the fifth element
     * @param _6   the sixth element
     * @param _7   the seventh element
     * @param <A> the head element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @param <D> the fourth element type
     * @param <E> the fifth element type
     * @param <F> the sixth element type
     * @param <G> the seventh element type
     * @return the 7-element HList
     * @see Tuple7
     */
    public static <A, B, C, D, E, F, G> Tuple7<A, B, C, D, E, F, G> tuple(A _1, B _2, C _3, D _4,
                                                                                        E _5, F _6, G _7) {
        return tuple(_2, _3, _4, _5, _6, _7).cons(_1);
    }

    /**
     * Static factory method for creating an 8-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param _5   the fifth element
     * @param _6   the sixth element
     * @param _7   the seventh element
     * @param _8   the eighth element
     * @param <A> the head element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @param <D> the fourth element type
     * @param <E> the fifth element type
     * @param <F> the sixth element type
     * @param <G> the seventh element type
     * @param <H> the eighth element type
     * @return the 8-element HList
     * @see Tuple8
     */
    public static <A, B, C, D, E, F, G, H> Tuple8<A, B, C, D, E, F, G, H> tuple(A _1, B _2, C _3,
                                                                                                D _4, E _5, F _6,
                                                                                                G _7, H _8) {
        return tuple(_2, _3, _4, _5, _6, _7, _8).cons(_1);
    }

    /**
     * The consing of a head element to a tail <code>HList</code>.
     *
     * @param <H> the head element type
     * @param <T> the HList tail type
     */
    public static class HCons<H, T extends HList> extends HList {
        private final H head;
        private final T tail;

        HCons(H head, T tail) {
            this.head = head;
            this.tail = tail;
        }

        /**
         * The head element of the <code>HList</code>.
         *
         * @return the head element
         */
        public H head() {
            return head;
        }

        /**
         * The remaining tail of the <code>HList</code>; returns an HNil if this is the last element.
         *
         * @return the tail
         */
        public T tail() {
            return tail;
        }

        @Override
        public <NH> HCons<NH, ? extends HCons<H, T>> cons(NH newHead) {
            return new HCons<>(newHead, this);
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof HCons) {
                HCons<?, ?> that = (HCons<?, ?>) other;
                return this.head.equals(that.head)
                        && this.tail.equals(that.tail);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return 31 * Objects.hashCode(head) + tail.hashCode();
        }
    }

    /**
     * The empty <code>HList</code>.
     */
    public static final class HNil extends HList {
        private static final HNil INSTANCE = new HNil();

        private HNil() {
        }

        @Override
        public <H> SingletonHList<H> cons(H head) {
            return new SingletonHList<>(head);
        }
    }
}

