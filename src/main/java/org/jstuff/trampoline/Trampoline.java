package org.jstuff.trampoline;

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

    private static final Trampoline<Object> RETURN_DUMMY = ret(new Object());

    /**
     * Creates a Trampoline that represents a thunk (an unevaluated calculation).
     *
     * @param thunk A Supplier that will return a value
     * @param <A>   The type of the value
     * @return A new Trampoline that represents the unevaluated calculation.
     */
    public static <A> Trampoline<A> suspend(Supplier<Trampoline<A>> thunk) {
        return RETURN_DUMMY.flatMap(ignored -> thunk.get());
    }

    private Trampoline() {
    }

    /**
     * Runs this Trampoline in constant stack space (disregarding the stack space that is consumed by
     * the functions used to construct this Trampoline).
     *
     * @return The calculated value
     */
    public final A run() {
        Trampoline<A> curr = this;
        while (curr instanceof FlatMap) {
            curr = ((FlatMap<?, A>) curr).resume();
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
        return flatMap(value -> ret(f.apply(value)));
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

    abstract <B> Trampoline<B> applyFlatMap(Function<A, Trampoline<B>> f);


    private static final class Return<A> extends Trampoline<A> {

        private final A value;

        private Return(A value) {
            this.value = value;
        }

        @Override
        <B> Trampoline<B> applyFlatMap(Function<A, Trampoline<B>> f) {
            return f.apply(value);
        }

    }


    private static final class FlatMap<A, B> extends Trampoline<B> {

        private final Trampoline<A> trampoline;
        private final Function<A, Trampoline<B>> nextTrampolineF;

        private FlatMap(Trampoline<A> trampoline, Function<A, Trampoline<B>> nextTrampolineF) {
            this.trampoline = trampoline;
            this.nextTrampolineF = nextTrampolineF;
        }

        /*
         * resume(FlatMap(Return(x), f)) = f.apply(x)
         * resume(FlatMap(FlatMap(a, f), g)) = FlatMap(a, x -> FlatMap(f.apply(x), g))
         */
        private Trampoline<B> resume() {
            return trampoline.applyFlatMap(nextTrampolineF);
        }

        @Override
        <C> Trampoline<C> applyFlatMap(Function<B, Trampoline<C>> f) {
            return trampoline.flatMap(value -> nextTrampolineF.apply(value).flatMap(f));
        }

    }

}
