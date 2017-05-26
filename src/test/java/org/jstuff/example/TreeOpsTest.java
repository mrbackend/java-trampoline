package org.jstuff.example;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.BiFunction;

import static org.jstuff.example.Tree.binaryBranch;
import static org.jstuff.example.Tree.leaf;
import static org.jstuff.example.Tree.unaryBranch;

public final class TreeOpsTest {

    @Test
    public void test() {
        BiFunction<String, Integer, String> reduce = (acc, value) -> acc + value;

        Assert.assertEquals("0", TreeOps.foldLeft(
                leaf(0),
                reduce, ""));
        Assert.assertEquals("0", TreeOps.foldLeft(
                unaryBranch(leaf(0)),
                reduce, ""));
        Assert.assertEquals("0", TreeOps.foldLeft(
                unaryBranch(unaryBranch(leaf(0))),
                reduce, ""));
        Assert.assertEquals("01", TreeOps.foldLeft(
                unaryBranch(binaryBranch(leaf(0), leaf(1))),
                reduce, ""));
        Assert.assertEquals("01", TreeOps.foldLeft(
                binaryBranch(leaf(0), leaf(1)),
                reduce, ""));
        Assert.assertEquals("01", TreeOps.foldLeft(
                binaryBranch(leaf(0), unaryBranch(leaf(1))),
                reduce, ""));
        Assert.assertEquals("012", TreeOps.foldLeft(
                binaryBranch(leaf(0), binaryBranch(leaf(1), leaf(2))),
                reduce, ""));
        Assert.assertEquals("01", TreeOps.foldLeft(
                binaryBranch(unaryBranch(leaf(0)), leaf(1)),
                reduce, ""));
        Assert.assertEquals("01", TreeOps.foldLeft(
                binaryBranch(unaryBranch(leaf(0)), unaryBranch(leaf(1))),
                reduce, ""));
        Assert.assertEquals("012", TreeOps.foldLeft(
                binaryBranch(unaryBranch(leaf(0)), binaryBranch(leaf(1), leaf(2))),
                reduce, ""));
        Assert.assertEquals("012", TreeOps.foldLeft(
                binaryBranch(binaryBranch(leaf(0), leaf(1)), leaf(2)),
                reduce, ""));
        Assert.assertEquals("012", TreeOps.foldLeft(
                binaryBranch(binaryBranch(leaf(0), leaf(1)), unaryBranch(leaf(2))),
                reduce, ""));
        Assert.assertEquals("0123", TreeOps.foldLeft(
                binaryBranch(binaryBranch(leaf(0), leaf(1)), binaryBranch(leaf(2), leaf(3))),
                reduce, ""));
    }

}