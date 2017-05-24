# How it works

## Trampoline types

A `Trampoline` instance is of one of two subclasses:
* `Return(value)` represents an immediate value
* `FlatMap(trampoline,f)` represents a suspended, chained evaluation, where `f` will be applied to the result of
running `trampoline`, returning a new `Trampoline`.

None of the subclasses are visible to client code.
 
### `Return(value)`

<img width="127px" height="43px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/return.svg">

A `Return(value)` is created with `Trampoline.ret(value)`. 

Where `value` has type `A`, `Return(value)` has type `Return<A>`, which is a subtype of `Trampoline<A>`.
 
### `FlatMap(trampoline,f)`

<img width="295px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/flatmap.svg">

A `FlatMap(trampoline,f)` may be created in one of three ways:
* `Trampoline.suspend(thunk)` creates a `FlatMap(trampoline,f)`, where `trampoline` is `Return(some dummy value)` and
`f` is `ignored -> thunk.get()`. The dummy value is not used, the intention is simply to suspend the actual evaluation
of `thunk`.
* `trampoline.map(f)` creates a `FlatMap(trampoline,g)` where `g` is `value -> Trampoline.ret(f.apply(value))`.
* `trampoline.flatMap(f)` creates a `FlatMap(trampoline,f)`.

Where `trampoline` has type `Trampoline<A>` and `f` has type `Function<A,Trampoline<B>>`, `FlatMap(trampoline,f)` has
type `FlatMap<A,B>`, which is a subtype of `Trampoline<B>`. 

The name `flatMap` is a shorthand for `flatten(map(...))`. A hypothetical `flatten`method would have had the
signature `static <A> Trampoline<A> flatten(Trampoline<Trampoline<A>> trampoline)`, so that given 
`Function<A, Trampoline<B>> f`, `trampoline.flatMap(f)` would have been the same as
`Trampoline.flatten(trampoline.map(f))`.

## Running a `Trampoline`

When `trampoline.run()` is called, it is stepwise reduced in a loop:

```java
public abstract class Trampoline<A> {
    // ...
    public final A run() {
        Trampoline<A> curr = this;
        while (curr instanceof FlatMap) {
            curr = ((FlatMap<?, A>) curr).resume();
        }
        return ((Return<A>) curr).value;
    }
    // ...
}
```

As long as `curr` is a `FlatMap`, it is transformed using one of two transforms. This sequence of transforms must
eventually result in a single `Return(value)`, at which point `value` will be returned from `run`.

### Resuming `FlatMap(Return(value),f)`

<img width="505px" height="127px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-return.svg">

The new trampoline will be `f.apply(value)`. This reduces the evaluation tree by two nodes (three discarded nodes and 
one new node).

### Resuming `FlatMap(FlatMap(trampoline,f),g)`

<img width="841px" height="211px" src="https://rawgit.com/mrbackend/java-trampoline/master/docs/svg/resume-flatmap-flatmap.svg">

The new trampoline will be `FlatMap(trampoline,x -> FlatMap(f.apply(x),g))`. This shifts the weight of the evaluation
tree to the right (two discarded nodes and two new nodes).
