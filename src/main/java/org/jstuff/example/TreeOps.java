package org.jstuff.example;

import java.util.function.BiFunction;

public final class TreeOps {
    public static <A, B> B foldLeft(Tree<A> tree, BiFunction<B, A, B> reduce, B init) {
        return tree.visit(
                leafValue -> reduce.apply(init, leafValue),
                child -> foldLeft(child, reduce, init),
                (leftChild, rightChild) -> {
                    B leftAcc = foldLeft(leftChild, reduce, init);
                    B resultAcc = foldLeft(rightChild, reduce, leftAcc);
                    return resultAcc;
                });
    }

    private TreeOps() {
    }
}
