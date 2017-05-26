package org.jstuff.example;

import org.jstuff.trampoline.Trampoline;

import java.util.function.BiFunction;

public final class TreeOps {
    public static <A, B> B foldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return trampolinedFoldLeft(tree, reduce, init).run();
    }

    public static <A, B> Trampoline<B> trampolinedFoldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        Trampoline<B> accTrampoline = Trampoline.ret(init);
        Tree<A> currTree = tree;
        while (isBranch(currTree)) {
            accTrampoline = trampolinedFoldLeftOfLeftChild(currTree, reduce, accTrampoline);
            currTree = getRightmostChild(currTree);
        }
        Tree<A> rightmostLeaf = currTree;
        return accTrampoline.map(acc -> reduce.apply(acc, getLeafValue(rightmostLeaf)));
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
                (leftChild, rightChild) -> initTrampoline.flatMap(init ->
                        trampolinedFoldLeft(leftChild, reduce, init)));
    }

    private static <A> Tree<A> getRightmostChild(Tree<A> tree) {
        return tree.visit(
                leafValue -> {
                    throw new AssertionError();
                },
                child -> child,
                (leftChild, rightChild) -> rightChild);
    }

    private static <A> A getLeafValue(Tree<A> tree) {
        return tree.visit(
                leftValue -> leftValue,
                child -> {
                    throw new AssertionError();
                },
                (leftChild, rightChild) -> {
                    throw new AssertionError();
                });
    }

    private TreeOps() {
    }
}
