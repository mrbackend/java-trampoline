package org.jstuff.example;

import org.jstuff.trampoline.Trampoline;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Tree<A> {

    public final int size() {
        return foldLeft((accSize, value) -> accSize + 1, 0);
    }

    public final <B> B foldLeft(BiFunction<B, A, B> reduceF, B init) {
        return foldLeftAsTrampoline(reduceF, init).run();
    }

    public final <B> Trampoline<B> foldLeftAsTrampoline(BiFunction<B, A, B> reduceF, B init) {
        return visit(
                left -> value -> right ->
                        Trampoline.suspend(() ->
                                left.foldLeftAsTrampoline(reduceF, init)
                                        .map(folded1 -> reduceF.apply(folded1, value))
                                        .flatMap(folded2 -> right.foldLeftAsTrampoline(reduceF, folded2))),
                () -> Trampoline.ret(init));
    }

    public final <B> Trampoline<B> foldRightAsTrampoline(BiFunction<A, B, B> reduceF, B init) {
        return visit(
                left -> value -> right ->
                        Trampoline.suspend(() ->
                                right.foldRightAsTrampoline(reduceF, init)
                                        .map(folded1 -> reduceF.apply(value, folded1))
                                        .flatMap(folded2 -> left.foldRightAsTrampoline(reduceF, folded2))),
                () -> Trampoline.ret(init));
    }

    public abstract <B> B visit(Function<Tree<A>, Function<A, Function<Tree<A>, B>>> onBranch, Supplier<B> onLeaf);

    private static final class Branch<A> extends Tree<A> {
        private final Tree<A> left;
        private final A value;
        private final Tree<A> right;

        private Branch(Tree<A> left, A value, Tree<A> right) {
            this.left = left;
            this.value = value;
            this.right = right;
        }

        @Override
        public <B> B visit(Function<Tree<A>, Function<A, Function<Tree<A>, B>>> onBranch, Supplier<B> onLeaf) {
            return onBranch.apply(left).apply(value).apply(right);
        }
    }

    private static final class Leaf<A> extends Tree<A> {
        @Override
        public <B> B visit(Function<Tree<A>, Function<A, Function<Tree<A>, B>>> onBranch, Supplier<B> onLeaf) {
            return onLeaf.get();
        }
    }
}
