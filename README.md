# java-trampoline

[![Build Status](https://travis-ci.org/mrbackend/java-trampoline.svg?branch=master)](https://travis-ci.org/mrbackend/java-trampoline)

## The problem

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

(Note 1: Please disregard that this class offers no means for constructing an `List`. The subject of this text is
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
```
_Example 1 excerpt: Stack-unsafe folds_

```java
    //...
    public final <B> B foldLeft(BiFunction<B, A, B> reduceF, B init) {
        return foldLeftAsTrampoline(reduceF, init).run();
    }

    private <B> Trampoline<B> foldLeftAsTrampoline(BiFunction<B, A, B> reduceF, B init) {
        return visit(
                () -> Trampoline.ret(init),
                (head, tail) -> {
                    B folded = reduceF.apply(init, head);
                    return Trampoline.suspend(() -> tail.foldLeftAsTrampoline(reduceF, folded));
                });
    }

    public final <B> B foldRight(BiFunction<A, B, B> reduceF, B init) {
        return foldRightAsTrampoline(reduceF, init).run();
    }

    private <B> Trampoline<B> foldRightAsTrampoline(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                () -> Trampoline.ret(init),
                (head, tail) -> {
                    Trampoline<B> foldedAsTrampoline = Trampoline.suspend(() -> tail.foldRightAsTrampoline(reduceF, init));
                    return foldedAsTrampoline.map(folded -> reduceF.apply(head, folded));
                });
    }
    //...
```
_Example 2: Stack-safe folds_

Instead of returning a value, the recursive methods return a `Trampoline`. A `Trampoline` is a data structure that
represents either a value, a single unevaluated calculation or a chain of unevaluated calculations. When run, the 
`Trampoline` evaluates the calculations one by one without growing the stack.

This is a good time to look at the internals of a `Trampoline`.

## The internals

A `Trampoline` instance is one of three kinds:
* `Return(value)` represents an immediate value
* `Suspend(thunk)` represents an unevaluated calculation that will return a `Trampoline`
* `FlatMap(trampoline,f)` represents a `Trampoline` followed by a calculation that will take the result of the first
`Trampoline` and calculate a new `Trampoline`

Each of these are implemented by their corresponding subclass. None of the subclasses are visible to client code.
 
### `Return(value)`

<img width="127px" height="43px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/return.svg">

Where `value` has the type `A`, `Return(value)` has the type `Return<A>`, which is a subtype of `Trampoline<A>`.
 
### `Suspend(f)`

<img width="190px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/suspend.svg">

A `Suspend` is created using the `public static <A> Trampoline<A> suspend(Supplier<Trampoline<A>> thunk)` method.

`Suspend` is used for postponing tail calls. To prevent a tail call from happening immediately, put the call in a
`Suspend`, as in `foldLeftAsTrampoline`above.

`Suspend` cannot be used to make non-tail calls stack safe. While you might be tempted to write something like
```java
    private <B> Trampoline<B> foldRightAsTrampoline(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                () -> Trampoline.ret(init),
                (head, tail) -> {
                    B folded = tail.foldRightAsTrampoline(reduceF, init).run();
                    return Trampoline.suspend(() -> Trampoline.ret(reduceF.apply(head, folded)));
                });
    }
```
, you will still overflow the stack for large `List`s. The problem here is that since `foldRightAsTrampoline` is
recursive, the `run()` method will itself call `run()` recursively. The golden rule is: _Never call `run()`, directly
or indirectly, from a function that returns a `Trampoline`_.  

Where `thunk` has the type `Supplier<Trampoline<A>>`, `Suspend(thunk)` has the type `Suspend<A>`, which is a subtype of `Trampoline<A>`.

### `FlatMap(trampoline,f)`

<img width="295px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/flatmap.svg">

The difference between `map` and `flatMap` is that the function passed to `map` will return a value, whereas the
function passed to `flatMap` will return a `Trampoline`. `trampoline.map(x -> f.apply(x))` is the same as
`trampoline.flatMap(x -> Trampoline.ret(f.apply(x)))`.

The name `flatMap` is a shorthand for `flatten(map(...))`. A hypothetical `flatten`method would have had the
signature `static <A> Trampoline<A> flatten(Trampoline<Trampoline<A>> trampoline)`, so that given 
`Function<A, Trampoline<B>> f`, `trampoline.flatMap(f)` would have been the same as
`Trampoline.flatten(trampoline.map(f))`.

Where `trampoline` has the type `Trampoline<A>`, and `f` has the type `Function<A, Trampoline<B>>`,
`FlatMap(trampoline, f)` has the type `FlatMap<A, B>`, which is a subtype of `Trampoline<B>`.

## How resume() works

When a `Trampoline` is evaluated, using `run()`, it is stepwise transformed in a loop. Each step transforms the result closer to a
`Return(a)`, at which point `a` is returned. The transform performed in each step depends on the structure of the
current result:

---
<img width="400px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-suspend.svg">

If the current result is a `Suspend(thunk)`, the next result is `thunk.get()`.

---
<img width="505px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-return.svg">

If the current result is a `FlatMap(Return(value),f)`, the next result is `f.apply(value)`.

---
<img width="673px" height="211px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-suspend.svg">

If the current result is a `FlatMap(Suspend(thunk),f)`, the next result is `FlatMap(thunk.get(),f)`.

---
<img width="841px" height="211px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-flatmap.svg">

If the current result is a `FlatMap(FlatMap(trampoline,f),g)`, the next result is
`FlatMap(trampoline,x -> FlatMap(f.apply(x),g))`.

## Test
[Test](https://mrbackend.github.io/java-trampoline/TEST.html)

## Acknowledgements

Thanks to Rúnar Óli Bjarnason for discussing the monadic Trampoline in 
[Stackless Scala With Free Monads](http://blog.higher-order.com/assets/trampolines.pdf)  
