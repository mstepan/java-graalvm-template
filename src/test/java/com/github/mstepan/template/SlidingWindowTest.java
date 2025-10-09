package com.github.mstepan.template;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mstepan.template.ds.SlidingWindow;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class SlidingWindowTest {

    @Test
    void sampleCaseProducesExpectedMinima() {
        int[] arr = {0, 5, 5, 3, 10, 0, 4};
        int k = 3;
        int[] expected = {0, 3, 3, 0, 0};
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void increasingArray() {
        int[] arr = {1, 2, 3, 4, 5};
        int k = 3;
        int[] expected = {1, 2, 3};
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void decreasingArray() {
        int[] arr = {5, 4, 3, 2, 1};
        int k = 3;
        int[] expected = {3, 2, 1};
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void windowSizeOneReturnsOriginalArray() {
        int[] arr = {7, -1, 2};
        int k = 1;
        int[] expected = Arrays.copyOf(arr, arr.length);
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void windowEqualsArrayLength() {
        int[] arr = {2, 2, 2};
        int k = arr.length;
        int[] expected = {2};
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void duplicatesAreHandledCorrectly() {
        int[] arr = {2, 2, 2, 1, 1, 3};
        int k = 3;
        int[] expected = {2, 1, 1, 1};
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void negativesAndZeros() {
        int[] arr = {-2, 0, -1, -3, 5};
        int k = 2;
        int[] expected = {-2, -1, -3, -3};
        int[] actual = computeWithSlidingWindow(arr, k);
        assertArrayEquals(expected, actual);
    }

    @Test
    void removeLeftNoOpWhenLeavingIsNotMin() {
        // Build window from {5, 1, 4} => deque = [1, 4], min = 1
        SlidingWindow w = SlidingWindow.fromArray(new int[] {5, 1, 4}, 3);
        assertEquals(1, w.getMin());

        // Remove the left value (5) which is not the current min -> no change
        w.removeLeft(5);
        assertEquals(1, w.getMin());

        // Add 0 -> should drop 4 then 1 from the back, new min becomes 0
        w.addRight(0);
        assertEquals(0, w.getMin());

        // Now removing 1 is a no-op since it was popped during addRight(0)
        w.removeLeft(1);
        assertEquals(0, w.getMin());
    }

    @Test
    void getMinOnEmptyThrows() {
        SlidingWindow w = new SlidingWindow();
        assertThrows(IllegalStateException.class, w::getMin);
    }

    @Test
    void randomizedAgainstNaive() {
        Random rnd = new Random(0xC0FFEE);
        for (int t = 0; t < 50; ++t) {
            int n = 50 + rnd.nextInt(51); // [50..100]
            int[] arr = new int[n];
            for (int i = 0; i < n; ++i) {
                arr[i] = rnd.nextInt(101) - 50; // [-50..50]
            }
            int k = 1 + rnd.nextInt(n); // [1..n]
            int[] expected = computeNaiveMinima(arr, k);
            int[] actual = computeWithSlidingWindow(arr, k);
            assertArrayEquals(
                    expected, actual, "Mismatch for k=" + k + " and arr=" + Arrays.toString(arr));
        }
    }

    // Helper that exercises AppMain.SlidingWindow as used by slidingWindowMin
    private static int[] computeWithSlidingWindow(int[] arr, int k) {
        if (arr.length == 0 && k == 0) {
            return new int[] {};
        }
        if (k <= 0 || k > arr.length) {
            throw new IllegalArgumentException("Invalid window size k=" + k);
        }

        int[] res = new int[arr.length - k + 1];
        SlidingWindow w = SlidingWindow.fromArray(arr, k);
        res[0] = w.getMin();

        for (int i = 1; i < res.length; ++i) {
            w.removeLeft(arr[i - 1]);
            w.addRight(arr[i + k - 1]);
            res[i] = w.getMin();
        }
        return res;
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
