package com.github.mstepan.template;

import java.util.concurrent.StructuredTaskScope;

public class AppMain {

    private static final ScopedValue<String> USERNAME_SCOPED = ScopedValue.newInstance();
    private static final ScopedValue<String> ROLE_SCOPED = ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {

        ScopedValue.where(USERNAME_SCOPED, "Maksym")
                .where(ROLE_SCOPED, "ADMIN")
                .run(
                        () -> {
                            try (StructuredTaskScope.ShutdownOnFailure scope =
                                    new StructuredTaskScope.ShutdownOnFailure()) {

                                for (int i = 0; i < 4; ++i) {
                                    final int id = i;
                                    scope.fork(
                                            () -> {
                                                System.out.printf(
                                                        "Task-%d, User: %s, Role: %s%n",
                                                        id,
                                                        USERNAME_SCOPED.get(),
                                                        ROLE_SCOPED.get());
                                                return null;
                                            });
                                }

                                scope.join().throwIfFailed();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });

        System.out.printf("Java version: %s. Main done...%n", System.getProperty("java.version"));
    }
}
