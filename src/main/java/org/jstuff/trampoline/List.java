package org.jstuff.trampoline;

public abstract class List<A> {

    private List() {
    }

    public abstract int size();

    private static final class Nil<A> extends List<A> {
        @Override
        public int size() {
            return 0;
        }
    }

    private static final class Cons<A> extends List<A> {
        private final A head;
        private final List<A> tail;

        private Cons(A head, List<A> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public int size() {
            return tail.size() + 1;
        }
    }

}
