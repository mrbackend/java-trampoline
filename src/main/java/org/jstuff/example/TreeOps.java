package org.jstuff.example;

import org.jstuff.trampoline.Trampoline;

import java.util.function.BiFunction;

public final class TreeOps {
    public static <A, B> B foldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return trampolinedFoldLeft2(tree, reduce, init).run();
    }

    public static <A, B> Trampoline<B> trampolinedFoldLeft1(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return tree.visit(
                leftValue -> Trampoline.ret(reduce.apply(init, leftValue)),
                child -> Trampoline.suspend(() -> trampolinedFoldLeft1(child, reduce, init)),
                (leftChild, rightChild) -> Trampoline
                        .suspend(() -> trampolinedFoldLeft1(leftChild, reduce, init))
                        .flatMap(leftAcc -> trampolinedFoldLeft1(rightChild, reduce, leftAcc)));
    }

    public static <A, B> Trampoline<B> trampolinedFoldLeft2(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        Trampoline<B> accTrampoline = Trampoline.ret(init);
        Tree<A> currTree = tree;
        while (isBranch(currTree)) {
            accTrampoline = trampolinedFoldLeftOfLeftChild(currTree, reduce, accTrampoline);
            currTree = getRightmostChild(currTree);
        }
        return accTrampoline;
    }

    private static boolean isBranch(Tree<?> tree) {
        return tree.visit(
                leafValue -> false,
                child -> true,
                (leftChild, rightChild) -> true);
    }

    private static <A, B> Trampoline<B> trampolinedFoldLeftOfLeftChild(
            Tree<A> tree,
            BiFunction<B, A, B> reduce,
            Trampoline<B> initTrampoline) {
        return tree.visit(
                leafValue -> {
                    throw new AssertionError();
                },
                child -> initTrampoline,
                (leftChild, rightChild) -> initTrampoline.flatMap(init -> trampolinedFoldLeft2(leftChild, reduce, init)));
    }

    private static <A> Tree<A> getRightmostChild(Tree<A> tree) {
        return tree.visit(
                leafValue -> {
                    throw new AssertionError();
                },
                child -> child,
                (leftChild, rightChild) -> rightChild);
    }

    private TreeOps() {
    }
}
