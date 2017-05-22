package org.jstuff.example;

import org.jstuff.trampoline.Trampoline;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class Optimizer {
    public static <A> A simpleOptimize(Predicate<A> isSolved, UnaryOperator<A> calcNextSolution, A solution) {
        return isSolved.test(solution) ?
                solution :
                simpleOptimize(isSolved, calcNextSolution, calcNextSolution.apply(solution));
    }

    public static <A> Trampoline<A> simpleOptimizeTrampoline(
            Predicate<A> isSolved,
            UnaryOperator<A> calcNextSolution,
            A solution) {

        return isSolved.test(solution) ?
                Trampoline.ret(solution) :
                Trampoline.suspend(() ->
                        simpleOptimizeTrampoline(isSolved, calcNextSolution, calcNextSolution.apply(solution)));
    }

    public static <A> A preparedOptimize(
            Predicate<A> isPrepared,
            UnaryOperator<A> calcPrepared,
            Predicate<A> isSolved,
            UnaryOperator<A> calcNextSolution,
            A solution) {

        A result;
        if (isSolved.test(solution)) {
            result = solution;
        } else {
            A prepared = prepare(isPrepared, calcPrepared, solution);
            result = preparedOptimize(
                    isPrepared,
                    calcPrepared,
                    isSolved,
                    calcNextSolution,
                    calcNextSolution.apply(prepared));
        }
        return result;
    }

    public static <A> A prepare(Predicate<A> isPrepared, UnaryOperator<A> calcPrepared, A solution) {
        return isPrepared.test(solution) ?
                solution :
                prepare(isPrepared, calcPrepared, calcPrepared.apply(solution));
    }

    public static <A> Trampoline<A> preparedOptimizeTrampoline(
            Predicate<A> isPrepared,
            UnaryOperator<A> calcPrepared,
            Predicate<A> isSolved,
            UnaryOperator<A> calcNextSolution,
            A solution) {

        Trampoline<A> result;
        if (isSolved.test(solution)) {
            result = Trampoline.ret(solution);
        } else {
            Trampoline<A> preparedTrampoline = prepareTrampoline(isPrepared, calcPrepared, solution);
            result = preparedTrampoline.flatMap(prepared ->
                    preparedOptimizeTrampoline(
                            isPrepared,
                            calcPrepared,
                            isSolved,
                            calcNextSolution,
                            calcNextSolution.apply(prepared)));
        }
        return result;
    }

    public static <A> Trampoline<A> prepareTrampoline(
            Predicate<A> isPrepared,
            UnaryOperator<A> calcPrepared,
            A solution) {

        return isPrepared.test(solution) ?
                Trampoline.ret(solution) :
                Trampoline.suspend(() -> prepareTrampoline(isPrepared, calcPrepared, calcPrepared.apply(solution)));
    }

    private Optimizer() {
    }
}
