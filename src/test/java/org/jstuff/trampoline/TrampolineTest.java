package org.jstuff.trampoline;

import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public final class TrampolineTest {

    @Test
    public void testRet() {
        // Given
        int value = -1265660452;

        // When
        Trampoline<Integer> actual = Trampoline.ret(value);

        // Then
        int actualResult = actual.run();
        assertEquals(value, actualResult);
    }

    @Test
    public void testSuspend() {
        // Given
        int value = 812476349;

        // When
        Trampoline<Integer> actual = Trampoline.suspend(() -> Trampoline.ret(value));

        // Then
        int actualResult = actual.run();
        assertEquals(value, actualResult);
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
    public void testMapOnSuspend() {
        // Given
        Trampoline<Integer> instance = Trampoline.suspend(() -> Trampoline.ret(-1292849567));
        Function<Integer, Integer> f = x -> x - 34896427;

        // When
        Trampoline<Integer> actual = instance.map(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run());
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testMapOnMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1775006026).map(x -> x + 1611991788);
        Function<Integer, Integer> f = x -> x - 1591063217;

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
    public void testFlatMapFromRetToSuspend() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-1788738074);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.suspend(() -> Trampoline.ret(x - 1610923702));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromRetToMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(498225094);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(1958893485).map(y -> (x + y) - 765599703);

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
    public void testFlatMapFromSuspendToRet() {
        // Given
        Trampoline<Integer> instance = Trampoline.suspend(() -> Trampoline.ret(850505357));
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(x - 742609518);

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromSuspendToSuspend() {
        // Given
        Trampoline<Integer> instance = Trampoline.suspend(() -> Trampoline.ret(-120053608));
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.suspend(() -> Trampoline.ret(x + 912930563));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromSuspendToMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.suspend(() -> Trampoline.ret(119636552));
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(-2111365093).map(y -> x + y + 1499652798);

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromSuspendToFlatMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.suspend(() -> Trampoline.ret(-1104440380));
        Function<Integer, Trampoline<Integer>> f =
                x -> Trampoline.ret(843528009).flatMap(y -> Trampoline.ret(x + y + 1659767107));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromMappedToRet() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1721165158).map(x -> x - 420424603);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(x - 322094596);

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromMappedToSuspend() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(23031202).map(x -> x - 1021714441);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.suspend(() -> Trampoline.ret(x + 300155659));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromMappedToMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1300273615).map(x -> x - 480010421);
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(528706493).map(y -> (x + y) - 1761999522);

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromMappedToFlatMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(-1317792119).map(x -> x - 1736441694);
        Function<Integer, Trampoline<Integer>> f =
                x -> Trampoline.ret(1120352122).flatMap(y -> Trampoline.ret(x + y + 2122991623));

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
    public void testFlatMapFromFlatMappedToSuspend() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1945294623).flatMap(x -> Trampoline.ret(x + 498465731));
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.suspend(() -> Trampoline.ret(x - 1055223754));

        // When
        Trampoline<Integer> actual = instance.flatMap(f);

        // Then
        int actualResult = actual.run();
        int expectedResult = f.apply(instance.run()).run();
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFlatMapFromFlatMappedToMapped() {
        // Given
        Trampoline<Integer> instance = Trampoline.ret(1904152875).flatMap(x -> Trampoline.ret(x + 1105176313));
        Function<Integer, Trampoline<Integer>> f = x -> Trampoline.ret(1469949990).map(y -> (x + y) - 587649679);

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

}