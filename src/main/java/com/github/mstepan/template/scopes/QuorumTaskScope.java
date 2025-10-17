package com.github.mstepan.template.scopes;

public final class QuorumTaskScope {}

// public final class QuorumTaskScope extends StructuredTaskScope<Void> {
//
//    private final int quorumSize;
//
//    private final AtomicInteger completed = new AtomicInteger();
//
//    public QuorumTaskScope(int quorumSize) {
//        super("QuorumTaskScope", Thread.ofVirtual().factory());
//        this.quorumSize = quorumSize;
//    }
//
//    @Override
//    protected void handleComplete(Subtask<? extends Void> subtask) {
//        if (subtask.state() == Subtask.State.SUCCESS) {
//            completed.incrementAndGet();
//        }
//
//        if (completed.get() >= quorumSize) {
//            super.shutdown();
//        }
//    }
//
//    public boolean isQuorumReached() {
//        return completed.get() >= quorumSize;
//    }
// }
