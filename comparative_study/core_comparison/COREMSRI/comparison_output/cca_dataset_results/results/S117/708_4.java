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
     * @param one   the head element
     * @param two   the second element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @return the 2-element HList
     * @see Tuple2
     */
    public static <One, Two> Tuple2<One, Two> tuple(One one, Two two) {
        return singletonHList(two).cons(one);
    }

    /**
     * Static factory method for creating a 3-element HList.
     *
     * @param one   the head element
     * @param two   the second element
     * @param three the third element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @param <Three> the third element type
     * @return the 3-element HList
     * @see Tuple3
     */
    public static <One, Two, Three> Tuple3<One, Two, Three> tuple(One one, Two two, Three three) {
        return tuple(two, three).cons(one);
    }

    /**
     * Static factory method for creating a 4-element HList.
     *
     * @param one   the head element
     * @param two   the second element
     * @param three the third element
     * @param four  the fourth element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @param <Three> the third element type
     * @param <Four> the fourth element type
     * @return the 4-element HList
     * @see Tuple4
     */
    public static <One, Two, Three, Four> Tuple4<One, Two, Three, Four> tuple(One one, Two two, Three three, Four four) {
        return tuple(two, three, four).cons(one);
    }

    /**
     * Static factory method for creating a 5-element HList.
     *
     * @param one   the head element
     * @param two   the second element
     * @param three the third element
     * @param four  the fourth element
     * @param five  the fifth element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @param <Three> the third element type
     * @param <Four> the fourth element type
     * @param <Five> the fifth element type
     * @return the 5-element HList
     * @see Tuple5
     */
    public static <One, Two, Three, Four, Five> Tuple5<One, Two, Three, Four, Five> tuple(One one, Two two, Three three, Four four, Five five) {
        return tuple(two, three, four, five).cons(one);
    }

    /**
     * Static factory method for creating a 6-element HList.
     *
     * @param one   the head element
     * @param two   the second element
     * @param three the third element
     * @param four  the fourth element
     * @param five  the fifth element
     * @param six   the sixth element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @param <Three> the third element type
     * @param <Four> the fourth element type
     * @param <Five> the fifth element type
     * @param <Six> the sixth element type
     * @return the 6-element HList
     * @see Tuple6
     */
    public static <One, Two, Three, Four, Five, Six> Tuple6<One, Two, Three, Four, Five, Six> tuple(One one, Two two, Three three, Four four, Five five,
                                                                                                  Six six) {
        return tuple(two, three, four, five, six).cons(one);
    }

    /**
     * Static factory method for creating a 7-element HList.
     *
     * @param one   the head element
     * @param two   the second element
     * @param three the third element
     * @param four  the fourth element
     * @param five  the fifth element
     * @param six   the sixth element
     * @param seven the seventh element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @param <Three> the third element type
     * @param <Four> the fourth element type
     * @param <Five> the fifth element type
     * @param <Six> the sixth element type
     * @param <Seven> the seventh element type
     * @return the 7-element HList
     * @see Tuple7
     */
    public static <One, Two, Three, Four, Five, Six, Seven> Tuple7<One, Two, Three, Four, Five, Six, Seven> tuple(One one, Two two, Three three, Four four,
                                                                                                                Five five, Six six, Seven seven) {
        return tuple(two, three, four, five, six, seven).cons(one);
    }

    /**
     * Static factory method for creating an 8-element HList.
     *
     * @param one   the head element
     * @param two   the second element
     * @param three the third element
     * @param four  the fourth element
     * @param five  the fifth element
     * @param six   the sixth element
     * @param seven the seventh element
     * @param eight the eighth element
     * @param <One> the head element type
     * @param <Two> the second element type
     * @param <Three> the third element type
     * @param <Four> the fourth element type
     * @param <Five> the fifth element type
     * @param <Six> the sixth element type
     * @param <Seven> the seventh element type
     * @param <Eight> the eighth element type
     * @return the 8-element HList
     * @see Tuple8
     */
    public static <One, Two, Three, Four, Five, Six, Seven, Eight> Tuple8<One, Two, Three, Four, Five, Six, Seven, Eight> tuple(One one, Two two, Three three,
                                                                                                                        Four four, Five five, Six six,
                                                                                                                        Seven seven, Eight eight) {
        return tuple(two, three, four, five, six, seven, eight).cons(one);
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