# Anatomy of `Trampoline`

## Trampoline types

A `Trampoline` instance is of one of two subclasses:
* `Return(value)` represents an immediate value
* `FlatMap(trampoline, calcNextTrampoline)` represents a suspended, chained evaluation, where `calcNextTrampoline` will
be applied to the result of `trampoline`, returning a new `Trampoline`.

None of the subclasses are visible to client code.
 
### `Return(value)`

<img width="127px" height="43px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/return.svg">

A `Return(value)` is created with `Trampoline.ret(value)`. 

Where `value` has type `A`, `Return(value)` has type `Return<A>`, which is a subtype of `Trampoline<A>`.
 
### `FlatMap(trampoline,f)`

<img width="295px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/flatmap.svg">

A `FlatMap(trampoline,f)` may be created in one of three ways:
* `Trampoline.suspend(thunk)` creates a `FlatMap(Return(some dummy value), ignored -> thunk.get())`. The dummy value is
not used, the intention is simply to suspend the actual evaluation of `thunk`.
* `trampoline.map(transformValue)` creates a
`FlatMap(trampoline, value -> Trampoline.ret(transformValue.apply(value)))`. 
* `trampoline.flatMap(calcNextTrampoline)` creates a `FlatMap(trampoline, calcNextTrampoline)`.

Where `trampoline` has type `Trampoline<A>` and `calcNextTrampoline` has type `Function<A, Trampoline<B>>`,
`FlatMap(trampoline, calcNextTrampoline)` has type `FlatMap<A, B>`, which is a subtype of `Trampoline<B>`. 

## Running a `Trampoline`

When `run` is called, it is stepwise reduced in a loop:

```java
public abstract class Trampoline<A> {
    // ...
    public final A run() {
        Trampoline<A> curr = this;
        while (curr.isSuspended()) {
            curr = curr.resume();
        }
        return curr.getValue();
    }
    // ...
}
```

As long as `curr` is a `FlatMap`, it is transformed using one of two transforms. This sequence of transforms must
eventually result in a single `Return(value)`, at which point `run` will return `value`.

### Resuming `FlatMap(Return(value), calcNextTrampoline)`

<img width="505px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-return.svg">

The new trampoline will be `calcNextTrampoline.apply(value)`. This reduces the evaluation tree by two nodes
(three discarded nodes replaced by one new node).

### Resuming `FlatMap(FlatMap(trampoline, calcNextTrampoline1), calcNextTrampoline2)`

<img width="841px" height="211px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-flatmap.svg">

The new trampoline will be
`FlatMap(trampoline, value -> FlatMap(calcNextTrampoline1.apply(value), calcNextTrampoline2))`. This shifts the weight
of the evaluation tree to the right (two discarded nodes replaced by two new nodes).
