package org.jstuff.trampoline;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class that represents a calculation which, when run, will not overflow the stack.
 *
 * @param <A> The type of the calculated value
 */
public abstract class Trampoline<A> {

    /**
     * Creates a Trampoline that represents a constant value.
     *
     * @param value The value which will be returned by this Trampoline.
     * @param <A>   The type of the value
     * @return A new Trampoline that represents the value.
     */
    public static <A> Trampoline<A> ret(A value) {
        return new Return<>(value);
    }

    /**
     * Creates a Trampoline that represents a thunk (an unevaluated calculation).
     *
     * @param thunk A Supplier that will return a value
     * @param <A>   The type of the value
     * @return A new Trampoline that represents the unevaluated calculation.
     */
    public static <A> Trampoline<A> suspend(Supplier<Trampoline<A>> thunk) {
        return new Suspend<>(thunk);
    }

    private Trampoline() {
    }

    /**
     * Runs this Trampoline in constant stack space (disregarding the stack space which is consumed by
     * the functions that are used to construct this Trampoline).
     *
     * @return The calculated value
     */
    public final A run() {
        Trampoline<A> curr = this;
        while (!(curr instanceof Return)) {
            curr = curr.resume();
        }
        return ((Return<A>) curr).value;
    }

    /**
     * Creates a Trampoline that represents an evaluation of this Trampoline, followed by a calculation
     * that transforms the result of this Trampoline to another value.
     *
     * @param f   The function used to transform the result of this Trampoline
     * @param <B> The type of the Transformed value
     * @return A new Trampoline that represents the application of f to this Trampoline
     */
    public final <B> Trampoline<B> map(Function<A, B> f) {
        return flatMap(a -> ret(f.apply(a)));
    }

    /**
     * Creates a Trampoline that represents an evaluation of this Trampoline, followed by a calculation
     * that produces a new Trampoline. This can be viewed as the sequence of two Trampolines, where the second
     * Trampoline depends on the result of this Trampoline.
     *
     * @param f   The function used to calculate the next Trampoline
     * @param <B> The type of the value returned by the next Trampoline (and, consequently, by the new Trampoline)
     * @return A new Trampoline that represents the sequence of this Trampoline followed by the next Trampoline
     */
    public final <B> Trampoline<B> flatMap(Function<A, Trampoline<B>> f) {
        return new FlatMap<>(this, f);
    }

    /*
     * resume(Return(x)) = Return(x)
     * resume(Suspend(f)) = f.get()
     * resume(FlatMap(Return(x), f)) = f.apply(x)
     * resume(FlatMap(Suspend(f), g)) = FlatMap(f.get(), g)
     * resume(FlatMap(FlatMap(a, f), g)) = FlatMap(a, x -> FlatMap(f.apply(x), g))
     */
    abstract Trampoline<A> resume();

    abstract <B> Trampoline<B> reAssoc(Function<A, Trampoline<B>> f);


    private static final class Return<A> extends Trampoline<A> {

        private final A value;

        private Return(A value) {
            this.value = value;
        }

        @Override
        Trampoline<A> resume() {
            return this;
        }

        @Override
        <B> Trampoline<B> reAssoc(Function<A, Trampoline<B>> f) {
            return f.apply(value);
        }

    }


    private static final class Suspend<A> extends Trampoline<A> {

        private final Supplier<Trampoline<A>> thunk;

        private Suspend(Supplier<Trampoline<A>> thunk) {
            this.thunk = Objects.requireNonNull(thunk);
        }

        @Override
        Trampoline<A> resume() {
            return thunk.get();
        }

        @Override
        <B> Trampoline<B> reAssoc(Function<A, Trampoline<B>> f) {
            return thunk.get().flatMap(f);
        }

    }


    private static final class FlatMap<X, A> extends Trampoline<A> {

        private final Trampoline<X> trampoline;
        private final Function<X, Trampoline<A>> nextTrampolineF;

        private FlatMap(Trampoline<X> trampoline, Function<X, Trampoline<A>> nextTrampolineF) {
            this.trampoline = trampoline;
            this.nextTrampolineF = nextTrampolineF;
        }

        @Override
        Trampoline<A> resume() {
            return trampoline.reAssoc(nextTrampolineF);
        }

        @Override
        <B> Trampoline<B> reAssoc(Function<A, Trampoline<B>> f) {
            return trampoline.flatMap(x -> nextTrampolineF.apply(x).flatMap(f));
        }

    }

}
