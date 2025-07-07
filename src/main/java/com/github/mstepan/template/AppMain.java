package com.github.mstepan.template;

import java.util.concurrent.TimeUnit;

public class AppMain {

    public static void main(String[] args) throws Exception {

        Thread th =
                Thread.ofVirtual()
                        .start(
                                () -> {
                                    long startTime = System.currentTimeMillis();

                                    System.out.println("Virtual thread started");

                                    try {
                                        TimeUnit.MILLISECONDS.sleep(250L);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        System.out.println("Virtual thread interrupted");
                                    }

                                    System.out.println("Virtual thread ended");

                                    long endTime = System.currentTimeMillis();

                                    System.out.printf(
                                            "Elapsed time: %d ms%n", (endTime - startTime));
                                });

        th.join();

        double maxRamInGb = ((double) Runtime.getRuntime().maxMemory()) / 1024.0 / 1024.0 / 1024.0;

        System.out.printf("Max ram: %.1f GB%n", maxRamInGb);

        TimeUnit.SECONDS.sleep(300000000L);

        System.out.printf("Java version: %s. Main done...%n", System.getProperty("java.version"));
    }
}
