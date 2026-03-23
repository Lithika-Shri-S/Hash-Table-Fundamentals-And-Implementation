import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class TokenBucket {
    private final int maxTokens;
    private final int refillRatePerSecond;
    private double tokens;
    private long lastRefillTimestamp;

    public TokenBucket(int maxTokens, int refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = maxTokens;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    /** Attempt to consume 1 token */
    public synchronized boolean allowRequest() {
        refillTokens();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    /** Calculate time to next token in milliseconds */
    public synchronized long getRetryAfterMillis() {
        refillTokens();
        if (tokens >= 1) return 0;
        return (long) ((1 - tokens) * 1000.0 / refillRatePerSecond);
    }

    /** Refill tokens based on elapsed time */
    private void refillTokens() {
        long now = System.currentTimeMillis();
        double secondsElapsed = (now - lastRefillTimestamp) / 1000.0;
        tokens = Math.min(maxTokens, tokens + secondsElapsed * refillRatePerSecond);
        lastRefillTimestamp = now;
    }

    /** Get current token count (approximate) */
    public synchronized int getTokens() {
        refillTokens();
        return (int) tokens;
    }
}

public class RateLimiterService {

    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
    private final int maxRequestsPerHour;

    public RateLimiterService(int maxRequestsPerHour) {
        this.maxRequestsPerHour = maxRequestsPerHour;
    }

    /** Check if client request is allowed */
    public boolean checkRateLimit(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(
                clientId,
                id -> new TokenBucket(maxRequestsPerHour, maxRequestsPerHour / 3600) // refill per second
        );
        return bucket.allowRequest();
    }

    /** Get retry-after seconds if request denied */
    public long getRetryAfterSeconds(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) return 0;
        return getRetryAfter(bucket);
    }

    private long getRetryAfter(TokenBucket bucket) {
        long retryMs = bucket.getRetryAfterMillis();
        return (retryMs + 999) / 1000; // round up to nearest second
    }

    /** Get current usage status */
    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) return String.format("{used: 0, limit: %d, reset: 3600s}", maxRequestsPerHour);
        int remaining = bucket.getTokens();
        int used = maxRequestsPerHour - remaining;
        long retry = getRetryAfter(bucket);
        return String.format("{used: %d, limit: %d, retry_after_s: %d}", used, maxRequestsPerHour, retry);
    }

    /** Demo usage */
    public static void main(String[] args) throws InterruptedException {
        RateLimiterService limiter = new RateLimiterService(10); // 10 requests per hour for testing
        String client = "abc123";

        for (int i = 0; i < 12; i++) {
            boolean allowed = limiter.checkRateLimit(client);
            if (allowed) {
                System.out.println("Request #" + (i + 1) + ": Allowed → " + limiter.getRateLimitStatus(client));
            } else {
                System.out.println("Request #" + (i + 1) + ": Denied → retry after " + limiter.getRetryAfterSeconds(client) + "s");
            }
        }
    }
}