package server;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


public class SpeedTracker {

    private static class ClientStats {
        final String clientId;
        final AtomicLong bytesInInterval = new AtomicLong(0);
        final AtomicLong totalBytes = new AtomicLong(0);
        final long startTimeMs;
        volatile long lastReportTimeMs;
        volatile boolean everReported = false;

        ClientStats(String clientId) {
            this.clientId = clientId;
            this.startTimeMs = System.currentTimeMillis();
            this.lastReportTimeMs = this.startTimeMs;
        }
    }

    private final ConcurrentHashMap<String, ClientStats> clients = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public SpeedTracker() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SpeedTracker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::reportAllSpeeds, 3, 3, TimeUnit.SECONDS);
    }

    public void registerClient(String clientId) {
        clients.put(clientId, new ClientStats(clientId));
    }

    // Учитывает принятые байты для указанного клиента.
    public void addBytes(String clientId, long bytes) {
        ClientStats stats = clients.get(clientId);
        if (stats != null) {
            stats.bytesInInterval.addAndGet(bytes);
            stats.totalBytes.addAndGet(bytes);
        }
    }

    public void unregisterClient(String clientId) {
        ClientStats stats = clients.remove(clientId);
        if (stats != null && !stats.everReported) {
            printStats(stats);
        }
    }

    // Периодический отчёт, выводит скорость для всех активных клиентов
    private void reportAllSpeeds() {
        for (ClientStats stats : clients.values()) {
            printStats(stats);
            stats.everReported = true;
        }
    }

    // Выводит мгновенную и среднюю скорость для одного клиента.
    private void printStats(ClientStats stats) {
        long now = System.currentTimeMillis();
        long intervalBytes = stats.bytesInInterval.getAndSet(0);
        long totalBytes = stats.totalBytes.get();
        long totalElapsedMs = now - stats.startTimeMs;
        long intervalElapsedMs = now - stats.lastReportTimeMs;

        // байты за интервал / длительность интервала
        double intervalSec = Math.max(intervalElapsedMs / 1000.0, 0.001);
        double instantSpeed = intervalBytes / intervalSec;

        // всего байт / общее время
        double avgSpeed = totalElapsedMs > 0 ? (totalBytes * 1000.0 / totalElapsedMs) : 0;

        System.out.printf("[%s] Мгновенная: %,.0f байт/с | Средняя: %,.0f байт/с%n",
                stats.clientId, instantSpeed, avgSpeed);

        stats.lastReportTimeMs = now;
    }

    // Останавливает планировщик отчётов.
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
