package rbac.audit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditLog implements AutoCloseable {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final List<String> entries = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;

    public AuditLog() {
        worker = new Thread(this::consumeLoop, "rbac-audit-log");
        worker.setDaemon(true);
        worker.start();
    }

    public void log(String message) {
        if (!running.get()) {
            throw new IllegalStateException("Audit log already closed");
        }
        queue.offer("[%s] %s".formatted(LocalDateTime.now(), message));
    }

    public List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    private void consumeLoop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                String entry = queue.poll(200, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    entries.add(entry);
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        worker.interrupt();
        try {
            worker.join(1000);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
