package com.github.mstepan.template;

import java.util.concurrent.*;

public final class SimpleThreadPool implements AutoCloseable {

    private final BlockingQueue<MyFutureTask<?>> workQueue;

    private volatile boolean active = true;
    private final ThreadGroup threadGroup;

    public SimpleThreadPool(int poolSize, int queueSize) {
        assert poolSize > 0 && poolSize < 256;
        assert queueSize > 0 && queueSize < 2048;

        this.workQueue = new LinkedBlockingDeque<>(queueSize);
        this.threadGroup = new ThreadGroup("SimpleThreadPool");

        for (int i = 0; i < poolSize; i++) {
            new Worker(threadGroup, "SimpleThreadPoolWorker-" + i).start();
        }
    }

    public void execute(Runnable task) {
        try {
            MyFutureTask<Void> futureTask = new MyFutureTask<>(task);

            workQueue.put(futureTask);

        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
        }
    }

    public <T> Future<T> submit(Callable<T> task) {
        MyFutureTask<T> futureTask = new MyFutureTask<>(task);
        try {
            workQueue.put(futureTask);
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
        }

        return futureTask;
    }

    public void shutdown() {
        active = false;
        threadGroup.interrupt();
    }

    @Override
    public void close() {
        while (!workQueue.isEmpty()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100L);
            } catch (InterruptedException interEx) {
                Thread.currentThread().interrupt();
            }
        }

        shutdown();
    }

    private static final class MyFutureTask<T> extends FutureTask<T> {
        public MyFutureTask(Callable<T> callable) {
            super(callable);
        }

        public MyFutureTask(Runnable callable) {
            super(
                    () -> {
                        callable.run();
                        return null;
                    });
        }

        void markAsFailed(Exception ex) {
            setException(ex);
        }
    }

    private final class Worker extends Thread {

        public Worker(ThreadGroup group, String name) {
            super(group, name);
        }

        @Override
        public void run() {

            System.out.printf("[%s] started%n", Thread.currentThread().getName());

            while (active) {
                try {
                    MyFutureTask<?> task = workQueue.take();
                    try {
                        task.run();
                    } catch (Exception ex) {
                        task.markAsFailed(ex);
                    }

                } catch (InterruptedException interEx) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.printf("[%s] completed%n", Thread.currentThread().getName());
        }
    }
}
