package com.github.mstepan.template.algorithms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class SlidingWindowMinimumTest {

    @Test
    void sampleCaseProducesExpectedMinima() {
        int[] arr = {0, 5, 5, 3, 10, 0, 4};
        int k = 3;
        int[] expected = {0, 3, 3, 0, 0};
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void increasingArray() {
        int[] arr = {1, 2, 3, 4, 5};
        int k = 3;
        int[] expected = {1, 2, 3};
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void decreasingArray() {
        int[] arr = {5, 4, 3, 2, 1};
        int k = 3;
        int[] expected = {3, 2, 1};
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void windowSizeOneReturnsOriginalArray() {
        int[] arr = {7, -1, 2};
        int k = 1;
        int[] expected = Arrays.copyOf(arr, arr.length);
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void windowEqualsArrayLength() {
        int[] arr = {2, 2, 2};
        int k = arr.length;
        int[] expected = {2};
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void duplicatesAreHandledCorrectly() {
        int[] arr = {2, 2, 2, 1, 1, 3};
        int k = 3;
        int[] expected = {2, 1, 1, 1};
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void negativesAndZeros() {
        int[] arr = {-2, 0, -1, -3, 5};
        int k = 2;
        int[] expected = {-2, -1, -3, -3};
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void emptyArrayAndZeroWindowReturnsEmpty() {
        int[] arr = {};
        int k = 0;
        int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
        assertArrayEquals(new int[] {}, actual);
    }

    @Test
    void invalidWindowSizesThrow() {
        int[] arr = {1, 2, 3};

        assertThrows(
                IllegalArgumentException.class,
                () -> SlidingWindowMinimum.slidingWindowMin(arr, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> SlidingWindowMinimum.slidingWindowMin(arr, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> SlidingWindowMinimum.slidingWindowMin(arr, 4));

        // empty array but positive window should throw
        assertThrows(
                IllegalArgumentException.class,
                () -> SlidingWindowMinimum.slidingWindowMin(new int[] {}, 1));
    }

    @Test
    void nullArrayThrowsNpe() {
        assertThrows(
                NullPointerException.class, () -> SlidingWindowMinimum.slidingWindowMin(null, 1));
    }

    @Test
    void randomizedAgainstNaive() {
        Random rnd = new Random(0xBADC0DE);
        for (int t = 0; t < 50; ++t) {
            int n = 50 + rnd.nextInt(51); // [50..100]
            int[] arr = new int[n];
            for (int i = 0; i < n; ++i) {
                arr[i] = rnd.nextInt(101) - 50; // [-50..50]
            }
            int k = 1 + rnd.nextInt(n); // [1..n]
            int[] expected = computeNaiveMinima(arr, k);
            int[] actual = SlidingWindowMinimum.slidingWindowMin(arr, k);
            assertArrayEquals(
                    expected, actual, "Mismatch for k=" + k + " and arr=" + Arrays.toString(arr));
        }

        // Also include an explicit check for the special (empty,0) case
        assertArrayEquals(new int[] {}, SlidingWindowMinimum.slidingWindowMin(new int[] {}, 0));
    }

    // O(N*K) baseline
    private static int[] computeNaiveMinima(int[] arr, int k) {
        if (arr.length == 0 && k == 0) {
            return new int[] {};
        }
        if (k <= 0 || k > arr.length) {
            throw new IllegalArgumentException("Invalid window size k=" + k);
        }

        int n = arr.length;
        int[] res = new int[n - k + 1];
        for (int i = 0; i <= n - k; ++i) {
            int min = Integer.MAX_VALUE;
            for (int j = i; j < i + k; ++j) {
                if (arr[j] < min) {
                    min = arr[j];
                }
            }
            res[i] = min;
        }
        return res;
    }
}
