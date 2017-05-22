package org.jstuff.example;

import org.jstuff.trampoline.Trampoline;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class List<A> {
    private List() {
    }

    public final int size() {
        return foldLeft((accSize, elem) -> accSize + 1, 0);
        // or return foldRight((elem, accSize) -> accSize + 1, 0);
    }

    public final <B> B foldLeft(BiFunction<B, A, B> reduceF, B init) {
        return foldLeftAsTrampoline(reduceF, init).run();
    }

    public final <B> Trampoline<B> foldLeftAsTrampoline(BiFunction<B, A, B> reduceF, B init) {
        return visit(
                (head, tail) -> Trampoline.suspend(() -> {
                    B folded = reduceF.apply(init, head);
                    return tail.foldLeftAsTrampoline(reduceF, folded);
                }),
                () -> Trampoline.ret(init)
        );
    }

    public final <B> B foldRight(BiFunction<A, B, B> reduceF, B init) {
        return foldRightAsTrampoline(reduceF, init).run();
    }

    public final <B> Trampoline<B> foldRightAsTrampoline(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                (head, tail) -> Trampoline.suspend(() -> {
                    Trampoline<B> foldedTrampoline = tail.foldRightAsTrampoline(reduceF, init);
                    return foldedTrampoline.map(folded -> reduceF.apply(head, folded));
                }),
                () -> Trampoline.ret(init)
        );
    }

    abstract <B> B visit(BiFunction<A, List<A>, B> onCons, Supplier<B> onNil);

    private static final class Cons<A> extends List<A> {
        private final A head;
        private final List<A> tail;

        private Cons(A head, List<A> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        <B> B visit(BiFunction<A, List<A>, B> onCons, Supplier<B> onNil) {
            return onCons.apply(head, tail);
        }
    }

    private static final class Nil<A> extends List<A> {
        @Override
        <B> B visit(BiFunction<A, List<A>, B> onCons, Supplier<B> onNil) {
            return onNil.get();
        }
    }
}
