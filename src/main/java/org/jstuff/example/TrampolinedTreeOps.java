package org.jstuff.example;

import org.jstuff.trampoline.Trampoline;

import java.util.function.BiFunction;

public final class TrampolinedTreeOps {
    public static <A, B> B foldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return trampolinedFoldLeft(tree, reduce, init).run();
    }

    public static <A, B> Trampoline<B> trampolinedFoldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return tree.visit(
                leafValue -> Trampoline.ret(reduce.apply(init, leafValue)),
                child -> Trampoline.suspend(() -> trampolinedFoldLeft(child, reduce, init)),
                (leftChild, rightChild) -> {
                    Trampoline<B> leftAccTrampoline =
                            Trampoline.suspend(() -> trampolinedFoldLeft(leftChild, reduce, init));
                    Trampoline<B> resultAccTrampoline =
                            leftAccTrampoline.flatMap(leftAcc -> trampolinedFoldLeft(rightChild, reduce, leftAcc));
                    return resultAccTrampoline;
                });
    }

    private TrampolinedTreeOps() {
    }
}
