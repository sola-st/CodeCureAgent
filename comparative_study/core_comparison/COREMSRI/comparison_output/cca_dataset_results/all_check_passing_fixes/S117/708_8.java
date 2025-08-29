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
     * @param <NewHead> the new head type
     * @return the updated HList
     */
    public abstract <NewHead> HCons<NewHead, ? extends HList> cons(NewHead newHead);

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
     * @param <Head> the head type
     * @param <Tail> the tail type
     * @return the newly created HList
     */
    public static <Head, Tail extends HList> HCons<Head, Tail> cons(Head head, Tail tail) {
        return Downcast.<HCons<Head, Tail>, HCons<Head, ? extends HList>>downcast(tail.cons(head));
    }

    /**
     * Static factory method for creating a singleton HList.
     *
     * @param head   the head element
     * @param <Head> the head element type
     * @return the singleton HList
     */
    public static <Head> SingletonHList<Head> singletonHList(Head head) {
        return new SingletonHList<>(head);
    }

    /**
     * Static factory method for creating a 2-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @return the 2-element HList
     * @see Tuple2
     */
    public static <E1, E2> Tuple2<E1, E2> tuple(E1 e1, E2 e2) {
        return singletonHList(e2).cons(e1);
    }

    /**
     * Static factory method for creating a 3-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param e3   the third element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @param <E3> the third element type
     * @return the 3-element HList
     * @see Tuple3
     */
    public static <E1, E2, E3> Tuple3<E1, E2, E3> tuple(E1 e1, E2 e2, E3 e3) {
        return tuple(e2, e3).cons(e1);
    }

    /**
     * Static factory method for creating a 4-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param e3   the third element
     * @param e4   the fourth element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @param <E3> the third element type
     * @param <E4> the fourth element type
     * @return the 4-element HList
     * @see Tuple4
     */
    public static <E1, E2, E3, E4> Tuple4<E1, E2, E3, E4> tuple(E1 e1, E2 e2, E3 e3, E4 e4) {
        return tuple(e2, e3, e4).cons(e1);
    }

    /**
     * Static factory method for creating a 5-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param e3   the third element
     * @param e4   the fourth element
     * @param e5   the fifth element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @param <E3> the third element type
     * @param <E4> the fourth element type
     * @param <E5> the fifth element type
     * @return the 5-element HList
     * @see Tuple5
     */
    public static <E1, E2, E3, E4, E5> Tuple5<E1, E2, E3, E4, E5> tuple(E1 e1, E2 e2, E3 e3, E4 e4, E5 e5) {
        return tuple(e2, e3, e4, e5).cons(e1);
    }

    /**
     * Static factory method for creating a 6-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param e3   the third element
     * @param e4   the fourth element
     * @param e5   the fifth element
     * @param e6   the sixth element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @param <E3> the third element type
     * @param <E4> the fourth element type
     * @param <E5> the fifth element type
     * @param <E6> the sixth element type
     * @return the 6-element HList
     * @see Tuple6
     */
    public static <E1, E2, E3, E4, E5, E6> Tuple6<E1, E2, E3, E4, E5, E6> tuple(E1 e1, E2 e2, E3 e3, E4 e4, E5 e5,
                                                                              E6 e6) {
        return tuple(e2, e3, e4, e5, e6).cons(e1);
    }

    /**
     * Static factory method for creating a 7-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param e3   the third element
     * @param e4   the fourth element
     * @param e5   the fifth element
     * @param e6   the sixth element
     * @param e7   the seventh element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @param <E3> the third element type
     * @param <E4> the fourth element type
     * @param <E5> the fifth element type
     * @param <E6> the sixth element type
     * @param <E7> the seventh element type
     * @return the 7-element HList
     * @see Tuple7
     */
    public static <E1, E2, E3, E4, E5, E6, E7> Tuple7<E1, E2, E3, E4, E5, E6, E7> tuple(E1 e1, E2 e2, E3 e3, E4 e4,
                                                                                    E5 e5, E6 e6, E7 e7) {
        return tuple(e2, e3, e4, e5, e6, e7).cons(e1);
    }

    /**
     * Static factory method for creating an 8-element HList.
     *
     * @param e1   the head element
     * @param e2   the second element
     * @param e3   the third element
     * @param e4   the fourth element
     * @param e5   the fifth element
     * @param e6   the sixth element
     * @param e7   the seventh element
     * @param e8   the eighth element
     * @param <E1> the head element type
     * @param <E2> the second element type
     * @param <E3> the third element type
     * @param <E4> the fourth element type
     * @param <E5> the fifth element type
     * @param <E6> the sixth element type
     * @param <E7> the seventh element type
     * @param <E8> the eighth element type
     * @return the 8-element HList
     * @see Tuple8
     */
    public static <E1, E2, E3, E4, E5, E6, E7, E8> Tuple8<E1, E2, E3, E4, E5, E6, E7, E8> tuple(E1 e1, E2 e2, E3 e3,
                                                                                            E4 e4, E5 e5, E6 e6,
                                                                                            E7 e7, E8 e8) {
        return tuple(e2, e3, e4, e5, e6, e7, e8).cons(e1);
    }

    /**
     * The consing of a head element to a tail <code>HList</code>.
     *
     * @param <Head> the head element type
     * @param <Tail> the HList tail type
     */
    public static class HCons<Head, Tail extends HList> extends HList {
        private final Head head;
        private final Tail tail;

        HCons(Head head, Tail tail) {
            this.head = head;
            this.tail = tail;
        }

        /**
         * The head element of the <code>HList</code>.
         *
         * @return the head element
         */
        public Head head() {
            return head;
        }

        /**
         * The remaining tail of the <code>HList</code>; returns an HNil if this is the last element.
         *
         * @return the tail
         */
        public Tail tail() {
            return tail;
        }

        @Override
        public <NewHead> HCons<NewHead, ? extends HCons<Head, Tail>> cons(NewHead newHead) {
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
        public <Head> SingletonHList<Head> cons(Head head) {
            return new SingletonHList<>(head);
        }
    }
}