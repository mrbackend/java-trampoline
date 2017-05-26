package org.jstuff.trampoline;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public final class TrampolineTest {

    @Test
    public void testRet() {
        // Given
        int x = -1265660452;

        // When
        Trampoline<Integer> actual = Trampoline.ret(x);

        // Then
        int actualResult = actual.run();
        assertEquals(x, actualResult);
    }

    @Test
    public void testSuspendNull() {
        // When - Then
        expectException(
                () -> Trampoline.suspend(null),
                NullPointerException.class);
    }

    @Test
    public void testSuspend() {
        // Given
        int x = 812476349;

        // When
        Trampoline<Integer> actual = Trampoline.suspend(() -> Trampoline.ret(x));

        // Then
        int actualResult = actual.run();
        assertEquals(x, actualResult);
    }

    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void testSuspendIsStackSafe() {
        // Given
        Trampoline<Integer> instance = suspended(1293394316, 100000);

        // When
        instance.run();
    }

    private static Trampoline<Integer> suspended(int x, int depth) {
        return (depth == 0) ?
                Trampoline.ret(x) :
                Trampoline.suspend(() -> suspended(x, depth - 1));
    }

    @Test
    public void testMapNull() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(0);

        // When - Then
        expectException(
                () -> instance.map(null),
                NullPointerException.class);
    }

    @Test
    public void testMapOnRet() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-2027639537);
        Function<Integer, Integer> f = x -> x - 1716704574;

        // When
        Trampoline<Integer> actual = instance.map(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run());
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testMapOnFlatMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1715890257).flatMap(x -> Trampoline.ret(x - 1046750814));
        Function<Integer, Integer> f = x -> x + 176502226;

        // When
        Trampoline<Integer> actual = instance.map(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run());
        assertEquals(expectedResult, actualResult);
    }

    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void testMapIsStackSafe() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-1396586044);
        Function<Integer, Integer> f = x -> x + 1568394758;
        for (int i = 0; i < 100000; ++i) {
            instance = instance.map(f);
        }

        // When
        instance.run();
    }

    @Test
    public void testFlatMapNull() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(0);

        // When - Then
        expectException(
                () -> instance.flatMap(null),
                NullPointerException.class);
    }

    @Test
    public void testFlatMapFromRetToRet() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-1546978697);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(x + 760609985);

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromRetToFlatMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-1701222901);
        Function<Integer, Trampoline<Integer>> f =
                x -> Trampoline.ret(858780315).flatMap(y -> Trampoline.ret(x + y + 637589551));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromFlatMappedToRet() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-1870413411).flatMap(x -> Trampoline.ret(x - 1338307766));
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(x - 1400084479);

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromFlatMappedToFlatMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-179065790).flatMap(x -> Trampoline.ret(x - 26564703));
        Function<Integer, Trampoline<Integer>> f =
                x -> Trampoline.ret(-1470929881).flatMap(y -> Trampoline.ret((x + y) - 1103607279));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void testLeftHeavyFlatMapIsStackSafe() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1037279461);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(x - 1883573890);
        for (int i = 0; i < 100000; ++i) {
            instance = instance.flatMap(f);
        }

        // When
        instance.run();
    }

    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void testRightHeavyFlatMapIsStackSafe() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(774471901).flatMap(x -> rightHeavy(x, 100000));

        // When
        instance.run();
    }

    private static Trampoline<Integer> rightHeavy(int x, int depth) {
        return (depth == 0) ?
                Trampoline.ret(x - 391018460) :
                Trampoline.ret(1480669361).flatMap(y -> rightHeavy((x + y) - 1332171485, depth - 1));
    }

    /*
     * The following tests test identities that should hold for all monads (types with flatMap)
     */

    /*
     * The left identity monad law says that ret(x).flatMap(f) = f(x)
     */
    @Test
    public void testLeftIdentityMonadLaw() {
        // Given
        int x = -444582204;
        Function<Integer, Trampoline<Integer>> calcNextTrampoline = y -> Trampoline.ret(y - 136899410);

        // When
        int actual = Trampoline.ret(x).flatMap(calcNextTrampoline).run();

        // Then
        int expected = calcNextTrampoline.apply(x).run();
        assertEquals(expected, actual);
    }

    /*
     * The right identity monad law says that t.flatMap(x -> ret(x)) = t
     */
    @Test
    public void testRightIdentityMonadLaw() {
        // Given
        Trampoline<Integer> trampoline = Trampoline.ret(61690741);

        // When
        int actual = trampoline.flatMap(x -> Trampoline.ret(x)).run();

        // Then
        int expected = trampoline.run();
        assertEquals(expected, actual);
    }

    /*
     * We use a rock-paper-scissors algebra to test that flatMap is associative even for non-associative algebras.
     *
     * RPS_BEST_OF[RPS_BEST_OF[0][1]][2] = RPS_BEST_OF[1][2] = 2
     * RPS_BEST_OF[0][RPS_BEST_OF[1][2]] = RPS_BEST_OF[0][2] = 0
     */
    private static final int[][] RPS_BEST_OF = {
            {0, 1, 0},
            {1, 1, 2},
            {0, 2, 2}};

    /*
     * The associativity monad law says that t.flatMap(f).flatMap(g) = t.flatMap(x -> f(x).flatMap(g))
     */
    @Test
    public void testAssociativityMonadLaw() {
        // Given
        Trampoline<Integer> trampoline = Trampoline.ret(0);
        Function<Integer, Trampoline<Integer>> calcNextTrampoline1 = x -> Trampoline.ret(RPS_BEST_OF[x][1]);
        Function<Integer, Trampoline<Integer>> calcNextTrampoline2 = x -> Trampoline.ret(RPS_BEST_OF[x][2]);

        // When
        int actual = trampoline.flatMap(calcNextTrampoline1).flatMap(calcNextTrampoline2).run();

        // Then
        int expected = trampoline.flatMap(x -> calcNextTrampoline1.apply(x).flatMap(calcNextTrampoline2)).run();
        assertEquals(expected, actual);
    }

    /*
     * Test t.map(f) = t.flatMap(x -> ret(f(x)))
     */
    @Test
    public void testMapToFlatMapRetIdentity() {
        // Given
        Trampoline<Integer> trampoline = Trampoline.ret(2123764208);
        Function<Integer, Integer> transformValue = x -> x + 1015637170;

        // When
        int actual = trampoline.map(transformValue).run();

        // Then
        int expected = trampoline.flatMap(x -> Trampoline.ret(transformValue.apply(x))).run();
        assertEquals(expected, actual);
    }

    /*
     * Test flatten(t) == t.flatMap(identity())
     */
    @Test
    public void testFlattenToFlatMapIdentityIdentity() {
        // Given
        Trampoline<Trampoline<Integer>> trampolinedTrampoline = Trampoline.ret(Trampoline.ret(232486295));

        // When
        int actual = flatten(trampolinedTrampoline).run();

        // Then
        int expected = trampolinedTrampoline.flatMap(trampoline -> trampoline).run();
        assertEquals(expected, actual);
    }

    /*
     * Test t.flatMap(f) = flatten(t.map(f))
     */
    @Test
    public void testFlatMapToFlattenMapIdentity() {
        // Given
        Trampoline<Integer> trampoline = Trampoline.ret(1411084928);
        Function<Integer, Trampoline<Integer>> calcNextTrampoline = x -> Trampoline.ret(x + 1625544605);

        // When
        int actual = trampoline.flatMap(calcNextTrampoline).run();

        // Then
        int expected = flatten(trampoline.map(calcNextTrampoline)).run();
        assertEquals(expected, actual);
    }

    private static <A> Trampoline<A> flatten(Trampoline<Trampoline<A>> trampolinedTrampoline) {
        return Trampoline.suspend(() -> trampolinedTrampoline.run());
    }

    private static <A> void expectException(
            Supplier<A> thunkThatIsExpectedToThrowException,
            Class<? extends Exception> expectedExceptionClass) {

        try {
            thunkThatIsExpectedToThrowException.get();
            Assert.fail(String.format("Expected exception %s, but none was thrown", expectedExceptionClass));
        } catch (Exception actualException) {
            if (!expectedExceptionClass.isInstance(actualException)) {
                Assert.fail(String.format("Expected exception %s, but got %s", expectedExceptionClass, actualException));
            }
        }
    }

}