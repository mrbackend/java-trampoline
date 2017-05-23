package org.jstuff.example;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Tree<A> {
    public static <A> Tree<A> leaf(A value) {
        return new Leaf<>(value);
    }

    public static <A> Tree<A> unaryBranch(Tree<A> child) {
        return new UnaryBranch<>(child);
    }

    public static <A> Tree<A> binaryBranch(Tree<A> leftChild, Tree<A> rightChild) {
        return new BinaryBranch<>(leftChild, rightChild);
    }

    private Tree() {
    }

    public abstract <B> B visit(
            Function<A, B> onLeaf,
            Function<Tree<A>, B> onUnaryBranch,
            BiFunction<Tree<A>, Tree<A>, B> onBinaryBranch);

    private static final class Leaf<A> extends Tree<A> {
        private final A leafValue;

        private Leaf(A value) {
            this.leafValue = value;
        }

        @Override
        public <B> B visit(
                Function<A, B> onLeaf,
                Function<Tree<A>, B> onUnaryBranch,
                BiFunction<Tree<A>, Tree<A>, B> onBinaryBranch) {
            return onLeaf.apply(leafValue);
        }
    }

    private static final class UnaryBranch<A> extends Tree<A> {
        private final Tree<A> child;

        private UnaryBranch(Tree<A> child) {
            this.child = child;
        }

        @Override
        public <B> B visit(
                Function<A, B> onLeaf,
                Function<Tree<A>, B> onUnaryBranch,
                BiFunction<Tree<A>, Tree<A>, B> onBinaryBranch) {
            return onUnaryBranch.apply(child);
        }
    }

    private static final class BinaryBranch<A> extends Tree<A> {
        private final Tree<A> leftChild;
        private final Tree<A> rightChild;

        private BinaryBranch(Tree<A> leftChild, Tree<A> rightChild) {
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        @Override
        public <B> B visit(
                Function<A, B> onLeaf,
                Function<Tree<A>, B> onUnaryBranch,
                BiFunction<Tree<A>, Tree<A>, B> onBinaryBranch) {
            return onBinaryBranch.apply(leftChild, rightChild);
        }
    }
}
