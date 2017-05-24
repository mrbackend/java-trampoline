# User guide

## The stack safety problem

That a program is stack safe means that it won't overflow the stack, even for infinitely large input. Stack safety is
closely related to recursive algorithms, since deep recursion normally requires a large stack.  
 
Consider the following (binary) `Tree` type:
```java
public interface Tree<A> {
    public <B> B visit(
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

We can make a very general purpose method for recursively traversing the leaves of `Tree`:
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
_Example 2: A recursive traversal algorithm_

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

