package com.github.mstepan.template;

import java.math.BigInteger;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class AppMain {

    public static void main(String[] args) throws Exception {

        final int fibIndex = 1_000_000;

        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            BigInteger fibResult = pool.submit(new FibonacciTask(fibIndex)).join();
            System.out.printf("fib(%d): %d%n", fibIndex, fibResult);
        }

        System.out.println("Main done...");
    }

    // 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377
    private static class FibonacciTask extends RecursiveTask<BigInteger> {

        private final int idx;

        FibonacciTask(int idx) {
            this.idx = idx;
        }

        @Override
        protected BigInteger compute() {
            if (idx < 2) {
                return BigInteger.ONE;
            }

            var first = new FibonacciTask(idx - 1).fork();
            var second = new FibonacciTask(idx - 2).fork();

            return first.join().add(second.join());
        }
    }
}
