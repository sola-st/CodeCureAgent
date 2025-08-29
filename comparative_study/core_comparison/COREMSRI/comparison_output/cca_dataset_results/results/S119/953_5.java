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
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @return the 2-element HList
     * @see Tuple2
     */
    public static <T1, T2> Tuple2<T1, T2> tuple(T1 _1, T2 _2) {
        return singletonHList(_2).cons(_1);
    }

    /**
     * Static factory method for creating a 3-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @return the 3-element HList
     * @see Tuple3
     */
    public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 _1, T2 _2, T3 _3) {
        return tuple(_2, _3).cons(_1);
    }

    /**
     * Static factory method for creating a 4-element HList.
     *
     * @param _1   the head element
     * @param _2   the second element
     * @param _3   the third element
     * @param _4   the fourth element
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @param <T4> the fourth element type
     * @return the 4-element HList
     * @see Tuple4
     */
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(T1 _1, T2 _2, T3 _3, T4 _4) {
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
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @param <T4> the fourth element type
     * @param <T5> the fifth element type
     * @return the 5-element HList
     * @see Tuple5
     */
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> tuple(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
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
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @param <T4> the fourth element type
     * @param <T5> the fifth element type
     * @param <T6> the sixth element type
     * @return the 6-element HList
     * @see Tuple6
     */
    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> tuple(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5,
                                                                            T6 _6) {
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
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @param <T4> the fourth element type
     * @param <T5> the fifth element type
     * @param <T6> the sixth element type
     * @param <T7> the seventh element type
     * @return the 7-element HList
     * @see Tuple7
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple(T1 _1, T2 _2, T3 _3, T4 _4,
                                                                                    T5 _5, T6 _6, T7 _7) {
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
     * @param <T1> the head element type
     * @param <T2> the second element type
     * @param <T3> the third element type
     * @param <T4> the fourth element type
     * @param <T5> the fifth element type
     * @param <T6> the sixth element type
     * @param <T7> the seventh element type
     * @param <T8> the eighth element type
     * @return the 8-element HList
     * @see Tuple8
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> tuple(T1 _1, T2 _2, T3 _3,
                                                                                            T4 _4, T5 _5, T6 _6,
                                                                                            T7 _7, T8 _8) {
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
        public <NewH> HCons<NewH, ? extends HCons<H, T>> cons(NewH newHead) {
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