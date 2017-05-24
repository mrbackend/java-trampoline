package org.jstuff.trampoline;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class that represents a calculation which, when run, will not overflow the stack.
 *
 * @param <A> The type of the calculated value
 * @see <a href="https://mrbackend.github.io/java-trampoline/index.html">java-trampoline web site</a>
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
        while (curr.isSuspended()) {
            curr = curr.resume();
        }
        return curr.getValue();
    }

    /**
     * Creates a {@code Trampoline} that will return the result of applying the passed function to the result of this
     * {@code Trampoline}.
     *
     * @param transformValue The function used to transform the result of this Trampoline
     * @param <B>            The transformed value's type
     * @return A new Trampoline that represents the application of transformValue to the result of this Trampoline
     * @throws NullPointerException if transformValue is null
     */
    public final <B> Trampoline<B> map(Function<A, B> transformValue) {
        Objects.requireNonNull(transformValue, "transformValue");
        return flatMap(value -> ret(transformValue.apply(value)));
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

    abstract boolean isSuspended();

    /*
     * Transforms this Trampoline to one that's one step closer to a Return(value).
     *
     * resume(FlatMap(Return(value), calcNextTrampoline)) ==> calcNextTrampoline.apply(value)
     * resume(FlatMap(FlatMap(trampoline, calcNextTrampoline), parentCalcNextTrampoline)) ==>
     *    FlatMap(trampoline, value -> FlatMap(calcNextTrampoline.apply(value), parentCalcNextTrampoline))
     *
     * Throws IllegalStateException if called for a Return(value)
     */
    abstract Trampoline<A> resume();

    /*
     * Apply the parent's calcNextTrampoline function to the result of this Trampoline
     */
    abstract <B> Trampoline<B> applyFlatMap(Function<A, Trampoline<B>> parentCalcNextTrampoline);

    abstract A getValue();

    private static final class Return<A> extends Trampoline<A> {

        private final A value;

        private Return(A value) {
            this.value = value;
        }

        @Override
        boolean isSuspended() {
            return false;
        }

        @Override
        Trampoline<A> resume() {
            throw new IllegalStateException();
        }

        @Override
        <B> Trampoline<B> applyFlatMap(Function<A, Trampoline<B>> parentCalcNextTrampoline) {
            return parentCalcNextTrampoline.apply(value);
        }

        @Override
        A getValue() {
            return value;
        }

    }


    private static final class FlatMap<A, B> extends Trampoline<B> {

        private final Trampoline<A> trampoline;
        private final Function<A, Trampoline<B>> calcNextTrampoline;

        private FlatMap(Trampoline<A> trampoline, Function<A, Trampoline<B>> calcNextTrampoline) {
            this.trampoline = trampoline;
            this.calcNextTrampoline = calcNextTrampoline;
        }

        @Override
        boolean isSuspended() {
            return true;
        }

        @Override
        Trampoline<B> resume() {
            return trampoline.applyFlatMap(calcNextTrampoline);
        }

        @Override
        <C> Trampoline<C> applyFlatMap(Function<B, Trampoline<C>> parentCalcNextTrampoline) {
            return trampoline.flatMap(value -> calcNextTrampoline.apply(value).flatMap(parentCalcNextTrampoline));
        }

        @Override
        B getValue() {
            throw new IllegalStateException();
        }

    }

}
