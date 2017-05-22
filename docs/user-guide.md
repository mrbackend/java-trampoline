# User guide

Consider the following recursive `List` type:
```java
public abstract class List<A> {
    private List() {
    }
    
    public final int size() {
        return foldLeft((accSize, elem) -> accSize + 1, 0);
        // or return foldRight((elem, accSize) -> accSize + 1, 0);
    }

    public final <B> B foldLeft(BiFunction<B, A, B> reduceF, B init) {
        return visit(
                () -> init,
                (head, tail) -> {
                    B folded = reduceF.apply(init, head);
                    return tail.foldLeft(reduceF, folded);
                });
    }

    public final <B> B foldRight(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                () -> init,
                (head, tail) -> {
                    B folded = tail.foldRight(reduceF, init);
                    return reduceF.apply(head, folded);
                });
    }

    abstract <B> B visit(Supplier<B> onNil, BiFunction<A, List<A>, B> onCons);

    private static final class Nil<A> extends List<A> {
        @Override
        <B> B visit(Supplier<B> onNil, BiFunction<A, List<A>, B> onCons) {
            return onNil.get();
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
        <B> B visit(Supplier<B> onNil, BiFunction<A, List<A>, B> onCons) {
            return onCons.apply(head, tail);
        }
    }
}
```
_Example 1: Stack-unsafe `List` class_

(Note 1: Please disregard that this class offers no means for constructing a `List`. The subject of this text is
recursion.)

(Note 2: `visit` implements a sort of "functional visitor pattern", but without a separate `Visitor` class. For the
purpose of presenting our `Trampoline` type, its only role is to help us implement recursion in single methods
instead of once per subclass, which should make it easier to spot the differences between examples. However, it is a
useful general purpose pattern to know.) 

`foldLeft` and `foldRight` are general-purpose methods to traverse the `List` from left (first element) to right,
respectively from right to left. You can see an example of their use by looking at the implementation of `size`. 
`foldLeft` and `foldRight` are implemented using recursion. However, for large enough `List`s, running any of them will
overflow the stack.

## The solution

Any tail-recursive algorithm can be rewritten as a loop (See
[Wikipedia: Tail call](https://en.wikipedia.org/wiki/Tail_call)). That does not apply to all recursive algorithms,
though. Some data stuctures, such as trees, don't lend themselves well to tail-recursive traversal. Besides, sometimes,
a loop version of an originally tail-recursive solution may be harder to comprehend than the recursive version.

One solution is using a trampoline. Compare these implementations of `foldLeft` and `foldRight`:
```java
public abstract class List<A> {
    //...
    public final <B> B foldLeft(BiFunction<B, A, B> reduceF, B init) {
        return visit(
                () -> init,
                (head, tail) -> {
                    B folded = reduceF.apply(init, head);
                    return tail.foldLeft(reduceF, folded);
                });
    }

    public final <B> B foldRight(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                () -> init,
                (head, tail) -> {
                    B folded = tail.foldRight(reduceF, init);
                    return reduceF.apply(head, folded);
                });
    }
    //...
}
```
_Example 1 excerpt: Stack-unsafe folds_

```java
public abstract class List<A> {
    //...
    public final <B> B foldLeft(BiFunction<B, A, B> reduceF, B init) {
        return foldLeftAsTrampoline(reduceF, init).run();
    }

    private <B> Trampoline<B> foldLeftAsTrampoline(BiFunction<B, A, B> reduceF, B init) {
        return visit(
                () -> Trampoline.ret(init),
                (head, tail) -> {
                    B folded = reduceF.apply(init, head);
                    return Trampoline.suspend(() -> 
                        tail.foldLeftAsTrampoline(reduceF, folded));
                });
    }

    public final <B> B foldRight(BiFunction<A, B, B> reduceF, B init) {
        return foldRightAsTrampoline(reduceF, init).run();
    }

    private <B> Trampoline<B> foldRightAsTrampoline(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                () -> Trampoline.ret(init),
                (head, tail) -> Trampoline.suspend(() -> {
                        Trampoline<B> foldedAsTrampoline = tail.foldRightAsTrampoline(reduceF, init);
                        return foldedAsTrampoline.map(folded -> reduceF.apply(head, folded));
                    }));
    }
    //...
}
```
_Example 2: Stack-safe folds_

Instead of returning a value, the recursive methods return a `Trampoline`. A `Trampoline` is a data structure that
represents either a value, a single unevaluated calculation or a chain of unevaluated calculations. When run, the 
`Trampoline` evaluates the calculations one by one without growing the stack.
