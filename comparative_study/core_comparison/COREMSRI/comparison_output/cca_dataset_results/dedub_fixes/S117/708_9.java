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
     * @param p1   the head element
     * @param p2   the second element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @return the 2-element HList
     * @see Tuple2
     */
    public static <P1, P2> Tuple2<P1, P2> tuple(P1 p1, P2 p2) {
        return singletonHList(p2).cons(p1);
    }

    /**
     * Static factory method for creating a 3-element HList.
     *
     * @param p1   the head element
     * @param p2   the second element
     * @param p3   the third element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @param <P3> the third element type
     * @return the 3-element HList
     * @see Tuple3
     */
    public static <P1, P2, P3> Tuple3<P1, P2, P3> tuple(P1 p1, P2 p2, P3 p3) {
        return tuple(p2, p3).cons(p1);
    }

    /**
     * Static factory method for creating a 4-element HList.
     *
     * @param p1   the head element
     * @param p2   the second element
     * @param p3   the third element
     * @param p4   the fourth element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @param <P3> the third element type
     * @param <P4> the fourth element type
     * @return the 4-element HList
     * @see Tuple4
     */
    public static <P1, P2, P3, P4> Tuple4<P1, P2, P3, P4> tuple(P1 p1, P2 p2, P3 p3, P4 p4) {
        return tuple(p2, p3, p4).cons(p1);
    }

    /**
     * Static factory method for creating a 5-element HList.
     *
     * @param p1   the head element
     * @param p2   the second element
     * @param p3   the third element
     * @param p4   the fourth element
     * @param p5   the fifth element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @param <P3> the third element type
     * @param <P4> the fourth element type
     * @param <P5> the fifth element type
     * @return the 5-element HList
     * @see Tuple5
     */
    public static <P1, P2, P3, P4, P5> Tuple5<P1, P2, P3, P4, P5> tuple(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
        return tuple(p2, p3, p4, p5).cons(p1);
    }

    /**
     * Static factory method for creating a 6-element HList.
     *
     * @param p1   the head element
     * @param p2   the second element
     * @param p3   the third element
     * @param p4   the fourth element
     * @param p5   the fifth element
     * @param p6   the sixth element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @param <P3> the third element type
     * @param <P4> the fourth element type
     * @param <P5> the fifth element type
     * @param <P6> the sixth element type
     * @return the 6-element HList
     * @see Tuple6
     */
    public static <P1, P2, P3, P4, P5, P6> Tuple6<P1, P2, P3, P4, P5, P6> tuple(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5,
                                                                              P6 p6) {
        return tuple(p2, p3, p4, p5, p6).cons(p1);
    }

    /**
     * Static factory method for creating a 7-element HList.
     *
     * @param p1   the head element
     * @param p2   the second element
     * @param p3   the third element
     * @param p4   the fourth element
     * @param p5   the fifth element
     * @param p6   the sixth element
     * @param p7   the seventh element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @param <P3> the third element type
     * @param <P4> the fourth element type
     * @param <P5> the fifth element type
     * @param <P6> the sixth element type
     * @param <P7> the seventh element type
     * @return the 7-element HList
     * @see Tuple7
     */
    public static <P1, P2, P3, P4, P5, P6, P7> Tuple7<P1, P2, P3, P4, P5, P6, P7> tuple(P1 p1, P2 p2, P3 p3, P4 p4,
                                                                                    P5 p5, P6 p6, P7 p7) {
        return tuple(p2, p3, p4, p5, p6, p7).cons(p1);
    }

    /**
     * Static factory method for creating an 8-element HList.
     *
     * @param p1   the head element
     * @param p2   the second element
     * @param p3   the third element
     * @param p4   the fourth element
     * @param p5   the fifth element
     * @param p6   the sixth element
     * @param p7   the seventh element
     * @param p8   the eighth element
     * @param <P1> the head element type
     * @param <P2> the second element type
     * @param <P3> the third element type
     * @param <P4> the fourth element type
     * @param <P5> the fifth element type
     * @param <P6> the sixth element type
     * @param <P7> the seventh element type
     * @param <P8> the eighth element type
     * @return the 8-element HList
     * @see Tuple8
     */
    public static <P1, P2, P3, P4, P5, P6, P7, P8> Tuple8<P1, P2, P3, P4, P5, P6, P7, P8> tuple(P1 p1, P2 p2, P3 p3,
                                                                                                P4 p4, P5 p5, P6 p6,
                                                                                                P7 p7, P8 p8) {
        return tuple(p2, p3, p4, p5, p6, p7, p8).cons(p1);
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