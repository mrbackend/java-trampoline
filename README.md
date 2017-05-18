# java-trampoline

![Build Status](https://travis-ci.org/mrbackend/java-trampoline.svg?branch=master)

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

<img src="https://rawgit.com/mrbackend/java-trampoline/master/svg/resume-flatmap-flatmap.svg">