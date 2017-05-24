#User guide

## The stack safety problem

That a program is stack safe means that it won't overflow the stack, even for infinitely large input. Stack safety is
closely related to recursive algorithms, since deep recursion normally requires a large stack.  
 
Consider the following (binary) `Tree` type:
```java
public class Tree<A> {
    public abstract <B> B visit(
            Function<A, B> onLeaf,
            Function<Tree<A>, B> onUnaryBranch,
            BiFunction<Tree<A>, Tree<A>, B> onBinaryBranch);
}
```
_Example 1: A very basic Tree type_

Branches of this tree type can have either one or two children. None of the branches have any associated value. Each
leaf, however, has a value of type `A`. You might notice that there is no way that this type can represent an empty
tree. That doesn't matter much, this example is for instructional purposes only.
 
As it happens, this simple interface is all we need to inspect and traverse the tree. The `visit` method realizes kind
of a functional visitor pattern, but without the need for a separate `TreeVisitor` class. Instead, we must provide three
callbacks, one for each node type.

We can make a very general purpose method for recursively traversing the leaves of a `Tree`:
```java
public class TreeOps {
    public static <A, B> B foldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return tree.visit(
                leafValue -> reduce.apply(init, leafValue),
                child -> foldLeft(child, reduce, init),
                (leftChild, rightChild) -> {
                    B leftAcc = foldLeft(leftChild, reduce, init);
                    B resultAcc = foldLeft(rightChild, reduce, leftAcc);
                    return resultAcc;
                });
    }
}
```
_Example 2: A stack unsafe recursive traversal algorithm_

`foldLeft` does a prefix traversal (from left to right), applying `reduce` to each leaf value to accumulate a result.
The accumulator is initialized with the value of `init`. Spend a little time to understand this algorithm before
proceeding.

This algorithm won't work if the depth of the tree is in the order of thousands. That case is not as contrived as you
might think; this tree type could be very fit for algorithms that require a fast append operation, in which case it
could become very unbalanced, leaning either to the left or the right.

Now, a tail recursive algorithm can always be rewritten as a loop (see 
[Wikiedia: Tail call](https://en.wikipedia.org/wiki/Tail_call)), but this tree traversal is not, and can't be rewritten
to be, tail recursive. To make it stack safe, we can either rewrite the algorithm to use an explicit stack that resides
on the heap, or &mdash;
 
## Use a trampoline

A trampoline is a data structure that represents either an unevaluated calculation or a single value. Alternatively, it
can be viewed as a value that will be resolved later. Instead of running methods/functions immediately, we put them into
a type that can be run.

When a trampoline is run, it resolves the calculations inside one by one inside a loop. When there are no more
unevaluated calculations left, the final calculated value is returned. This process is described in 
[How it works](https://mrbackend.github.io/java-trampoline/how-it-works.html).

To make `foldLeft` stack safe using a trampoline, we rewrite it like this:
```java
public final class TreeOps {
    public static <A, B> B foldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return trampolinedFoldLeft(tree, reduce, init).run();
    }

    private static <A, B> Trampoline<B> trampolinedFoldLeft(
            Tree<A> tree, 
            BiFunction<B, A, B> reduce, B init) {
        return tree.visit(
                leafValue -> Trampoline.ret(reduce.apply(init, leafValue)),
                child -> Trampoline.suspend(() -> trampolinedFoldLeft(child, reduce, init)),
                (leftChild, rightChild) -> {
                    Trampoline<B> leftAccTrampoline = Trampoline.suspend(
                            () -> trampolinedFoldLeft(leftChild, reduce, init));
                    Trampoline<B> resultAccTrampoline = leftAccTrampoline.flatMap(
                            leftAcc -> trampolinedFoldLeft(rightChild, reduce, leftAcc));
                    return resultAccTrampoline;
                });
    }
}
```
_Example 3: A stack safe recursive traversal algorithm_

`trampolinedFoldLeft` is a recursive method that immediately returns a `Trampoline` instance; only one `visit` at the
root is done at this point. There is a one-to-one correspondence between each part of `trampolinedFoldLeft` and 
Example 2's `foldLeft`. Let's look at the available `Trampoline` methods.

### ret

Signature: `public static <A> Trampoline<A> ret(A value)`

`ret` takes a single value and returns a trampoline that wraps this value. When the resulting trampoline is run, the
value is returned.

The purpose of `ret` method is to represent the bottom values of recursions.

### suspend

Signature: `public static <A> Trampoline<A> suspend(Supplier<Trampoline<A>> thunk)`

A thunk is an unevaluated function that takes no parameters. When the resulting trampoline is run, it will evaluate
`thunk` to get a new trampoline and then run that one to get and return its result.

The purpose of `suspend` is to avoid an immediate recursion, instead returning a trampoline that represents the
recursion to be run later.

### map

Signature: `public static <B> Trampoline<B> map(Function<A,B> transformValue)`

`map` takes a function parameter that, when evaluated will return a new value that depends on the result of "this"
trampoline. When the resulting trampoline is run, it will first run "this" trampoline, then call `transformValue`
and return its result.

The purpose of `map` is to transform the resulting value of a trampoline. The transform must be stack safe; if a deep
recursion is performed to transform the value, it should be wrapped in a trampoline instead, and passed to `flatMap`
instead of `map`.

Calling `map(transformValue)` is synonymous to calling `flatMap(value -> Trampoline.ret(transformValue.apply(value))`.

### flatMap

Signature: `public <B> Trampoline<A> flatMap(Function<A,Trampoline<B>> getNextTrampoline)`

`flatMap` takes a function parameter that, when evaluated will return a new trampoline that depends on the result of
"this" trampoline. When the resulting trampoline is run, it will first run "this" trampoline, then call 
`getNextTrampoline` to get a new trampoline, and finally run the new trampoline and return its result.

The purpose of `flatMap` is to chain two recursions where the second recursion depends on the first one.

### run

Signature: `public A run()`

`run` evaluates a trampoline by evaluating each of the functions and sub-trampolines used to create the trampoline.
The trick is that each of these steps is done in a loop, such that the stack does not grow during the evaluation.

## The stack safe `foldLeft` explained

```java
leafValue -> Trampoline.ret(reduce.apply(init, leafValue))
```
This callback is used for leaves of the tree. When recursion hits the bottom, a value is returned. Since the
`foldLeft` method returns a `Trampoline`, the value must be wrapped in a `Trampoline`.

```java
child -> Trampoline.suspend(() -> trampolinedFoldLeft(child, reduce, init))
```
This callback is used for branches with one child. If we left out the `suspend` here, `trampolinedFoldLeft`
would have been called immediately, leaving us with the exact problem we had originally. Hence, we wrap the recursive
call in a `suspend`, creating a trampoline that will do the recursive call later.

```java
(leftChild, rightChild) -> {
    Trampoline<B> leftAccTrampoline =
        Trampoline.suspend(() -> trampolinedFoldLeft(leftChild, reduce, init));
    Trampoline<B> resultAccTrampoline =
        leftAccTrampoline.flatMap(leftAcc -> trampolinedFoldLeft(rightChild, reduce, leftAcc));
    return resultAccTrampoline;
}
```
This callback is used for branches with two children. When this branch is traversed, the left child is traversed first,
then the right child. Just as for the single-child branch, we need to `suspend` the traversal of the left child, to
avoid the immediate recursion. In `foldLeft`, the accumulated result from traversing the left child is used in the
traversal of the right child. Hence, the second recursion is dependent of the first one, which is exactly what `flatMap`
is for.

The last callback could also have been written as:
```java
(leftChild, rightChild) -> Trampoline.suspend(() -> {
    Trampoline<B> leftAccTrampoline = trampolinedFoldLeft(leftChild, reduce, init);
    Trampoline<B> resultAccTrampoline =
        leftAccTrampoline.flatMap(leftAcc -> trampolinedFoldLeft(rightChild, reduce, leftAcc));
    return resultAccTrampoline;
})
```
The difference is that instead of suspending the traversal of the left child, we suspend the traversal of the whole
branch. The two are equivalent with regards to functionality and stack safety. The only observable difference is that if
there were some time consuming computation while processing the branch node itself, the latter would create the
resulting trampoline faster, postponing the time consuming calculation until the resulting trampoline is run.

## Don't do this

A developer who's unfamiliar with trampolines might try to write the two-children branch like this:
```java
(leftChild, rightChild) -> Trampoline.suspend(() -> {
    B leftAcc = trampolinedFoldLeft(leftChild, reduce, init).run();
    Trampoline<B> resultAccTrampoline = trampolinedFoldLeft(rightChild,reduce,leftAcc);
    return resultAccTrampoline;
})
```
This is not stack safe. The reason is that when `run` is called, the corresponding code for child nodes is
"unsuspended", forcing the "unsuspension" of their child nodes and so on. The golden rule is:

> Don't call `run` on a sub-trampoline of the trampoline you're creating!

A sign that you're doing it wrong is if you call `run` inside a method that returns a `Trampoline`. This is safe if
the trampoline you're running is unrelated (that is, not using any of the same recursive methods) to the one you're
creating, but even in that case it is safer (with regards to maintainability) to use `map`/`flatMap`.

## Why is it called &mdash;

### "Trampoline"?

While running a trampoline, the stack returns to the same state between each step. If you look at a stack trace while
stepping through a trampolined calculation, you will observe this. It's like the stack pointer jumps up and down.

### "map"?

"Mapping" is a mathematical term which basically means associating a value in one domain with a value in another domain.
The `transformValue` function passed to `map` represents such a mapping.

### "flatMap"?

Imagine that there existed a function 
`static <A> Trampoline<A> flatten(Trampoline<Trampoline<A>> trampolinedTrampoline)`. Simply by studying the signature, 
you should be able to figure out what it does. Originally, the `map` and `flatten` terms were used for Lists, where
mapping from a list element to a list would produce a List of Lists. In that context, a function that converts a List of
Lists to a List is naturally named "flatten".

The expected relations between `ret`, `map`, `flatten` and `flatMap` are:
* `trampoline.map(transformValue) = trampoline.flatMap(value -> Trampoline.ret(transformValue.apply(value)))`
* `Trampoline.flatten(trampolinedTrampoline) = trampolinedTrampoline.flatMap(trampoline -> trampoline)`
* `trampoline.flatMap(getNextTrampoline) = Trampoline.flatten(trampoline.map(getNextTrampoline))`

The last relation should explain why `flatMap`: It is a composition of `flatten` and `map`.
