package com.example.uc6;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * An executor that only queues tasks; the test runs them when it wants the
 * background work to finish. No threads, no sleeps, no flaky timing.
 */
class ManualExecutor implements Executor {

    private final Queue<Runnable> tasks = new ArrayDeque<>();

    @Override
    public synchronized void execute(Runnable task) {
        tasks.add(task);
    }

    synchronized int pending() {
        return tasks.size();
    }

    void runAll() {
        Runnable task;
        while ((task = poll()) != null) {
            task.run();
        }
    }

    private synchronized Runnable poll() {
        return tasks.poll();
    }
}
