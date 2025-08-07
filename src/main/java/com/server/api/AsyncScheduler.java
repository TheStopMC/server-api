package com.server.api;

import java.util.concurrent.*;

public class AsyncScheduler {
    private static final DelayQueue<ScheduledTask> queue = new DelayQueue<>();
    private static final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
    private static final ExecutorService taskExecutor = AbstractServer.EXECUTOR;
    private static volatile boolean running = true;

    static {
        dispatcher.submit(() -> {
            while (running) {
                try {
                    ScheduledTask task = queue.take(); // blocks until ready
                    taskExecutor.submit(task.runnable);
                    if (task.repeating) {
                        task.reschedule();
                        queue.put(task);
                    }
                } catch (InterruptedException ignored) {}
            }
        });
    }

    public static ScheduledTask schedule(Runnable task, long delayMs, boolean repeating) {
        ScheduledTask scheduledTask = new ScheduledTask(task, delayMs, repeating);
        queue.put(scheduledTask);
        return scheduledTask;
    }

    public static void shutdown() {
        running = false;
        dispatcher.shutdown();
        try {
            if (!dispatcher.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("AsyncScheduler did not terminate in time, forcing shutdown...");
                dispatcher.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Shutdown interrupted, forcing...");
            dispatcher.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class ScheduledTask implements Delayed {
        private final Runnable runnable;
        private final long intervalMs;
        private final boolean repeating;
        private long scheduledTime;

        public ScheduledTask(Runnable runnable, long delayMs, boolean repeating) {
            this.runnable = runnable;
            this.intervalMs = delayMs;
            this.repeating = repeating;
            this.scheduledTime = System.currentTimeMillis() + delayMs;
        }

        public void reschedule() {
            this.scheduledTime = System.currentTimeMillis() + intervalMs;
        }

        public void cancel() {
            queue.remove(this); // stop future execution
        }

        @Override public long getDelay(TimeUnit unit) {
            return unit.convert(scheduledTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override public int compareTo(Delayed o) {
            return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}