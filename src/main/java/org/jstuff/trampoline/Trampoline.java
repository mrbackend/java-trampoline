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
     * Creates a {@code Trampoline} that represents a constant value.
     *
     * @param value The value which will be returned by this {@code Trampoline}.
     * @param <A>   The type of the value
     * @return A new Trampoline that represents the value.
     */
    public static <A> Trampoline<A> ret(A value) {
        return new Return<>(value);
    }

    /*
     * Used to encode suspend as a flatMap
     */
    private static final Trampoline<Object> RETURN_DUMMY = ret(null);

    /**
     * Creates a {@code Trampoline} that represents a thunk (an unevaluated calculation).
     *
     * @param thunk A Supplier that will return a value
     * @param <A>   The type of the value
     * @return A new Trampoline that represents the unevaluated calculation.
     * @throws NullPointerException if thunk is null
     */
    public static <A> Trampoline<A> suspend(Supplier<Trampoline<A>> thunk) {
        Objects.requireNonNull(thunk, "thunk");
        return RETURN_DUMMY.flatMap(ignored -> thunk.get());
    }

    private Trampoline() {
    }

    /**
     * Evaluates this {@code Trampoline} in constant stack space (disregarding the stack space that is consumed by
     * the functions used to construct this {@code Trampoline}).
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
     * Creates a {@code Trampoline} that will return the result of applying the passed function to the result of this
     * {@code Trampoline}.
     *
     * @param getTransformedValue The function used to transform the result of this Trampoline
     * @param <B>                 The transformed value's type
     * @return A new Trampoline that represents the application of getTransformedValue to the result of this Trampoline
     * @throws NullPointerException if getTransformedValue is null
     */
    public final <B> Trampoline<B> map(Function<A, B> getTransformedValue) {
        Objects.requireNonNull(getTransformedValue, "getTransformedValue");
        return flatMap(value -> ret(getTransformedValue.apply(value)));
    }

    /**
     * Creates a {@code Trampoline} that will return the result of the {@code Trampoline} that's returned from applying
     * the passed function to the result of this {@code Trampoline}.
     *
     * @param calcNextTrampoline The function used to calculate the next Trampoline
     * @param <B>                The type of the value returned by the next Trampoline (and, consequently, the returned
     *                           Trampoline)
     * @return A new Trampoline that represents the sequence of this Trampoline followed by the next Trampoline
     * @throws NullPointerException if calcNextTrampoline is null
     */
    public final <B> Trampoline<B> flatMap(Function<A, Trampoline<B>> calcNextTrampoline) {
        Objects.requireNonNull(calcNextTrampoline, "calcNextTrampoline");
        return new FlatMap<>(this, calcNextTrampoline);
    }

    /*
     * Apply the parent's calcNextTrampoline function to the result of this Trampoline
     */
    abstract <B> Trampoline<B> applyFlatMap(Function<A, Trampoline<B>> parentCalcNextTrampoline);


    private static final class Return<A> extends Trampoline<A> {

        private final A value;

        private Return(A value) {
            this.value = value;
        }

        @Override
        <B> Trampoline<B> applyFlatMap(Function<A, Trampoline<B>> parentCalcNextTrampoline) {
            return parentCalcNextTrampoline.apply(value);
        }

    }


    private static final class FlatMap<A, B> extends Trampoline<B> {

        private final Trampoline<A> trampoline;
        private final Function<A, Trampoline<B>> calcNextTrampoline;

        private FlatMap(Trampoline<A> trampoline, Function<A, Trampoline<B>> calcNextTrampoline) {
            this.trampoline = trampoline;
            this.calcNextTrampoline = calcNextTrampoline;
        }

        /*
         * Transforms this Trampoline to one that's one step closer to a Return(value)
         *
         * resume(FlatMap(Return(value), calcNextTrampoline)) ==> calcNextTrampoline.apply(value)
         * resume(FlatMap(FlatMap(trampoline, calcNextTrampoline), parentCalcNextTrampoline)) ==>
         *    FlatMap(trampoline, value -> FlatMap(calcNextTrampoline.apply(value), parentCalcNextTrampoline))
         */
        private Trampoline<B> resume() {
            return trampoline.applyFlatMap(calcNextTrampoline);
        }

        @Override
        <C> Trampoline<C> applyFlatMap(Function<B, Trampoline<C>> parentCalcNextTrampoline) {
            return trampoline.flatMap(value -> calcNextTrampoline.apply(value).flatMap(parentCalcNextTrampoline));
        }

    }

}
