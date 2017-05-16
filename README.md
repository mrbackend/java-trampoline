# java-trampoline

![Build Status](https://travis-ci.com/mrbackend/java-trampoline.svg?token=ZXZHjq4KzqdbQ2QPWysp&branch=master)

## The problem with recursion

Consider the following recursive `List` type:
```java
public abstract class List<A> {

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
```

`int size()` is implemented using recursion. However, for large `List`s, running `size()` will overflow the stack.

## How it works

<svg width="100" height="100">
    <circle cx="50" cy="50" r="40" stroke="green" stroke-width="4" fill="yellow">
</svg>