import java.util.*;

class VideoData {
    String videoId;
    String content; // simplified in-memory representation

    VideoData(String videoId, String content) {
        this.videoId = videoId;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Video{" + videoId + "}";
    }
}

public class MultiLevelCache {

    /** L1 Cache: Memory (LinkedHashMap with access-order for LRU) **/
    private LinkedHashMap<String, VideoData> l1Cache;
    private final int L1_CAPACITY = 10000;

    /** L2 Cache: SSD-backed simulation (HashMap with access counts) **/
    private Map<String, VideoData> l2Cache;
    private Map<String, Integer> l2AccessCount;
    private final int L2_CAPACITY = 100000;
    private final int PROMOTE_THRESHOLD = 5; // accesses needed to promote L2 -> L1

    /** L3 Database: simulation (HashMap for simplicity) **/
    private Map<String, VideoData> l3Database;

    /** Stats **/
    private int l1Hits = 0, l2Hits = 0, l3Hits = 0, totalRequests = 0;
    private double l1Time = 0.5, l2Time = 5, l3Time = 150; // simulated ms

    public MultiLevelCache() {
        l1Cache = new LinkedHashMap<>(L1_CAPACITY, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
                return size() > L1_CAPACITY;
            }
        };
        l2Cache = new HashMap<>();
        l2AccessCount = new HashMap<>();
        l3Database = new HashMap<>();
    }

    /** Add video to database (L3) **/
    public void addVideoToDB(VideoData video) {
        l3Database.put(video.videoId, video);
    }

    /** Get video with multi-level cache lookup **/
    public VideoData getVideo(String videoId) {
        totalRequests++;

        // --- L1 Lookup ---
        if (l1Cache.containsKey(videoId)) {
            l1Hits++;
            return l1Cache.get(videoId); // access-order updates LRU
        }

        // --- L2 Lookup ---
        if (l2Cache.containsKey(videoId)) {
            l2Hits++;
            int count = l2AccessCount.getOrDefault(videoId, 0) + 1;
            l2AccessCount.put(videoId, count);

            // Promote to L1 if threshold reached
            if (count >= PROMOTE_THRESHOLD) {
                VideoData data = l2Cache.get(videoId);
                l1Cache.put(videoId, data);
            }
            return l2Cache.get(videoId);
        }

        // --- L3 Lookup ---
        if (l3Database.containsKey(videoId)) {
            l3Hits++;
            VideoData data = l3Database.get(videoId);

            // Add to L2 cache
            if (l2Cache.size() >= L2_CAPACITY) {
                // remove random or LRU-like for simplicity
                String keyToRemove = l2Cache.keySet().iterator().next();
                l2Cache.remove(keyToRemove);
                l2AccessCount.remove(keyToRemove);
            }
            l2Cache.put(videoId, data);
            l2AccessCount.put(videoId, 1);

            return data;
        }

        return null; // video not found
    }

    /** Get cache statistics **/
    public void printStats() {
        double totalTime = (l1Hits*l1Time + l2Hits*l2Time + l3Hits*l3Time)/totalRequests;
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("L1 Hits: " + l1Hits + ", L2 Hits: " + l2Hits + ", L3 Hits: " + l3Hits);
        System.out.printf("L1 Hit Rate: %.2f%%, Avg Time: %.2fms\n", 100.0*l1Hits/totalRequests, l1Time);
        System.out.printf("L2 Hit Rate: %.2f%%, Avg Time: %.2fms\n", 100.0*l2Hits/totalRequests, l2Time);
        System.out.printf("L3 Hit Rate: %.2f%%, Avg Time: %.2fms\n", 100.0*l3Hits/totalRequests, l3Time);
        System.out.printf("Overall Avg Lookup Time: %.2fms\n", totalTime);
    }

    /** MAIN DEMO **/
    public static void main(String[] args) {
        MultiLevelCache cache = new MultiLevelCache();

        // populate L3 DB
        for (int i = 1; i <= 1000; i++) {
            cache.addVideoToDB(new VideoData("video_" + i, "Content_" + i));
        }

        // simulate access
        cache.getVideo("video_1");  // L3 -> L2
        cache.getVideo("video_1");  // L2 access 2
        cache.getVideo("video_1");  // L2 access 3
        cache.getVideo("video_1");  // L2 access 4
        cache.getVideo("video_1");  // L2 access 5 -> promote to L1
        cache.getVideo("video_1");  // L1 hit

        cache.getVideo("video_500"); // L3 -> L2

        cache.printStats();
    }
}