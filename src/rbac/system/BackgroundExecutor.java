package rbac.system;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundExecutor implements AutoCloseable {

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    public BackgroundExecutor(int workerThreads) {
        this.executorService = Executors.newFixedThreadPool(workerThreads);
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
    }
}
