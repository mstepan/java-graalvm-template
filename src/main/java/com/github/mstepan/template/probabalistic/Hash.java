package com.github.mstepan.template.probabalistic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Universal hash function.
 *
 * <p>h(k) = (a*k + b) % p
 *
 * <p>'p' - big prime number
 *
 * <p>'a' should be in range [1; p-1]
 *
 * <p>'b' should be in range [0; p-1]
 */
public class Hash<T> {

    // Integer.MAX_VALUE is biggest prime number that can be represented as int, should be good
    // enough.
    private static final long P = Integer.MAX_VALUE;

    private final long a;
    private final long b;

    @SuppressFBWarnings(
            value = "PREDICTABLE_RANDOM",
            justification =
                    "ThreadLocalRandom is acceptable here because this is not security-sensitive")
    public Hash() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        a = 1L + rand.nextLong(P - 1);
        b = rand.nextLong(P - 1);
    }

    public long hash(T key) {
        int keyHashCode = (key == null) ? 0 : key.hashCode();
        return (a * keyHashCode + b) % P;
    }
}
