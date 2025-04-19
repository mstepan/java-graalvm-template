package com.github.mstepan.template;

import com.github.mstepan.template.scopes.RateLimiterTaskScope;
import java.util.concurrent.TimeUnit;

public class AppMain {

    public static void main(String[] args) throws Exception {

        try (RateLimiterTaskScope scope = new RateLimiterTaskScope(2)) {
            for (int i = 0; i < 10; ++i) {
                final int idx = i;
                scope.fork(
                        () -> {
                            search(idx);
                            return null;
                        });
            }
            scope.join();
        }

        System.out.println("Main done...");
    }

    record SearchResult(String value) {}

    public static SearchResult search(int idx) {

        System.out.println("Search result started: " + idx);

        try {
            //            ThreadLocalRandom rand = ThreadLocalRandom.current();
            TimeUnit.SECONDS.sleep(1L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.printf("Search with idx %d interrupted%n", idx);
        }

        System.out.println("Search result completed: " + idx);

        return new SearchResult("search-result-" + idx);
    }
}
