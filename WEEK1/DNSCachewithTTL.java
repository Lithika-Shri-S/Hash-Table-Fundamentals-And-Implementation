import java.util.*;
import java.util.concurrent.*;

/**
 * DNS Cache with TTL and LRU eviction
 */
public class DNSCache {

    /** Represents a single DNS cache entry */
    static class DNSEntry {
        String domain;
        String ipAddress;
        long expiryTime; // epoch millis when entry expires

        DNSEntry(String domain, String ipAddress, long ttlMillis) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final int capacity;
    private final long defaultTTL; // in milliseconds
    private final Map<String, DNSEntry> cacheMap;
    private long hits = 0;
    private long misses = 0;

    /** Executor for background cleanup */
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public DNSCache(int capacity, long ttlSeconds) {
        this.capacity = capacity;
        this.defaultTTL = ttlSeconds * 1000;

        // LRU Cache using LinkedHashMap
        this.cacheMap = new LinkedHashMap<String, DNSEntry>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > DNSCache.this.capacity;
            }
        };

        // Start background cleaner every 1 second
        cleaner.scheduleAtFixedRate(this::cleanupExpired, 1, 1, TimeUnit.SECONDS);
    }

    /** Resolve domain, simulate upstream DNS if cache miss */
    public synchronized String resolve(String domain) {
        DNSEntry entry = cacheMap.get(domain);
        long startTime = System.nanoTime();

        if (entry != null && !entry.isExpired()) {
            hits++;
            long duration = System.nanoTime() - startTime;
            System.out.println("Cache HIT: " + domain + " → " + entry.ipAddress + " (retrieved in " + duration / 1_000_000.0 + "ms)");
            return entry.ipAddress;
        } else {
            misses++;
            // Simulate upstream DNS lookup
            String ip = queryUpstreamDNS(domain);
            cacheMap.put(domain, new DNSEntry(domain, ip, defaultTTL));

            if (entry != null && entry.isExpired()) {
                System.out.println("Cache EXPIRED: " + domain + " → Query upstream → " + ip);
            } else {
                System.out.println("Cache MISS: " + domain + " → Query upstream → " + ip);
            }
            return ip;
        }
    }

    /** Simulate upstream DNS resolution */
    private String queryUpstreamDNS(String domain) {
        // Fake IP for simulation purposes
        return "192.168." + new Random().nextInt(256) + "." + new Random().nextInt(256);
    }

    /** Clean expired entries */
    private synchronized void cleanupExpired() {
        Iterator<Map.Entry<String, DNSEntry>> iterator = cacheMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DNSEntry> e = iterator.next();
            if (e.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    /** Get cache statistics */
    public synchronized void getCacheStats() {
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
        System.out.println("Cache Stats → Hits: " + hits + ", Misses: " + misses + ", Hit Rate: " + String.format("%.2f", hitRate) + "%");
        System.out.println("Current cache size: " + cacheMap.size());
    }

    /** Shutdown background cleaner */
    public void shutdown() {
        cleaner.shutdownNow();
    }

    /** Demo */
    public static void main(String[] args) throws InterruptedException {
        DNSCache dnsCache = new DNSCache(3, 5); // capacity=3, TTL=5 sec

        dnsCache.resolve("google.com");
        dnsCache.resolve("facebook.com");
        Thread.sleep(1000);
        dnsCache.resolve("google.com");
        dnsCache.resolve("amazon.com");
        dnsCache.resolve("twitter.com"); // triggers LRU eviction
        dnsCache.resolve("facebook.com"); // may be evicted depending on LRU
        Thread.sleep(6000); // wait for TTL expiration
        dnsCache.resolve("google.com"); // expired
        dnsCache.getCacheStats();

        dnsCache.shutdown();
    }
}