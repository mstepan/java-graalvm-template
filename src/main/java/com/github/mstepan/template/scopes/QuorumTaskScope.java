package com.github.mstepan.template.scopes;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * A StructuredTaskScope.Joiner that completes as soon as a required quorum of subtasks has
 * completed successfully.
 *
 * <p>Thread-safety: updates to the success counter are performed via an AtomicInteger.
 *
 * <p>The joiner's result is a Stream containing a single Boolean indicating whether the quorum was
 * reached.
 */
@SuppressWarnings({"preview", "unused"})
public final class QuorumTaskScope implements StructuredTaskScope.Joiner<Void, Stream<Boolean>> {

    private final int quorumSize;

    private final AtomicInteger completed = new AtomicInteger();

    /**
     * Create a joiner that requires the specified number of successful subtasks to reach quorum.
     *
     * @param quorumSize number of successful subtasks required; must be greater than 0
     * @throws IllegalArgumentException if {@code quorumSize} is less than or equal to 0
     */
    public QuorumTaskScope(int quorumSize) {
        if (quorumSize <= 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "'quorumSize' max be positive value, quorumSize = %d", quorumSize));
        }
        this.quorumSize = quorumSize;
    }

    /**
     * Called by the StructuredTaskScope when a subtask completes. Increments the internal success
     * counter if the subtask completed successfully.
     *
     * @param subtask the completed subtask
     * @return {@code true} if the quorum has been reached and joining can stop, {@code false}
     *     otherwise
     */
    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends Void> subtask) {
        if (subtask.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
            completed.incrementAndGet();
        }

        return completed.get() >= quorumSize;
    }

    /**
     * Return a stream containing a single boolean value that indicates whether the quorum has been
     * reached.
     *
     * @return a Stream with exactly one element: {@code true} if the number of successful subtasks
     *     is greater than or equal to the quorum size; {@code false} otherwise
     */
    @Override
    public Stream<Boolean> result() {
        return Stream.of(completed.get() >= quorumSize);
    }
}
