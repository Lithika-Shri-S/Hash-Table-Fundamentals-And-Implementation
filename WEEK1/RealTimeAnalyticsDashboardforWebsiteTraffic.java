import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class PageViewEvent {
    String url;
    String userId;
    String source;

    public PageViewEvent(String url, String userId, String source) {
        this.url = url;
        this.userId = userId;
        this.source = source;
    }
}

public class RealTimeAnalyticsDashboard {

    // Page views count
    private final ConcurrentHashMap<String, AtomicInteger> pageViews = new ConcurrentHashMap<>();
    // Unique visitors per page
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();
    // Traffic sources
    private final ConcurrentHashMap<String, AtomicInteger> trafficSources = new ConcurrentHashMap<>();
    // Lock for uniqueVisitors set updates
    private final Object visitorLock = new Object();

    /** Process incoming page view event */
    public void processEvent(PageViewEvent event) {
        // Increment total page views
        pageViews.computeIfAbsent(event.url, k -> new AtomicInteger(0)).incrementAndGet();

        // Track unique visitors
        uniqueVisitors.computeIfAbsent(event.url, k -> ConcurrentHashMap.newKeySet()).add(event.userId);

        // Increment source count
        trafficSources.computeIfAbsent(event.source.toLowerCase(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    /** Get top N pages based on views */
    private List<String> getTopPages(int n) {
        PriorityQueue<Map.Entry<String, AtomicInteger>> pq = new PriorityQueue<>(
                Comparator.comparingInt(e -> e.getValue().get())
        );

        for (Map.Entry<String, AtomicInteger> entry : pageViews.entrySet()) {
            pq.offer(entry);
            if (pq.size() > n) pq.poll();
        }

        List<String> topPages = new ArrayList<>();
        while (!pq.isEmpty()) {
            Map.Entry<String, AtomicInteger> e = pq.poll();
            int unique = uniqueVisitors.getOrDefault(e.getKey(), Collections.emptySet()).size();
            topPages.add(String.format("%s - %d views (%d unique)", e.getKey(), e.getValue().get(), unique));
        }
        Collections.reverse(topPages);
        return topPages;
    }

    /** Get traffic source distribution percentages */
    private Map<String, Double> getTrafficSources() {
        int total = trafficSources.values().stream().mapToInt(AtomicInteger::get).sum();
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : trafficSources.entrySet()) {
            double percent = (entry.getValue().get() * 100.0) / total;
            result.put(entry.getKey(), percent);
        }
        return result;
    }

    /** Print dashboard */
    public void displayDashboard(int topN) {
        System.out.println("----- Top Pages -----");
        List<String> topPages = getTopPages(topN);
        for (int i = 0; i < topPages.size(); i++) {
            System.out.println((i + 1) + ". " + topPages.get(i));
        }

        System.out.println("\n----- Traffic Sources -----");
        Map<String, Double> sources = getTrafficSources();
        sources.forEach((k, v) -> System.out.printf("%s: %.2f%%\n", k, v));
    }

    /** Demo simulation */
    public static void main(String[] args) throws InterruptedException {
        RealTimeAnalyticsDashboard dashboard = new RealTimeAnalyticsDashboard();

        // Simulate events
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_1", "Google"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_2", "Facebook"));
        dashboard.processEvent(new PageViewEvent("/sports/championship", "user_3", "Direct"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_1", "Google")); // duplicate user, not unique

        // Simulate batch update every 5 seconds
        System.out.println("=== Dashboard Snapshot ===");
        dashboard.displayDashboard(5);
    }
}