import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleInventory {

    // Tracks product stock: productId -> available units
    private final Map<String, AtomicInteger> stockMap;

    // Waiting list for out-of-stock products: productId -> queue of userIds
    private final Map<String, Queue<Integer>> waitingList;

    public FlashSaleInventory() {
        stockMap = new ConcurrentHashMap<>();
        waitingList = new ConcurrentHashMap<>();
    }

    /**
     * Initialize stock for a product.
     */
    public void addProduct(String productId, int initialStock) {
        stockMap.put(productId, new AtomicInteger(initialStock));
        waitingList.put(productId, new ConcurrentLinkedQueue<>()); // FIFO waiting list
    }

    /**
     * Check available stock for a product.
     */
    public int checkStock(String productId) {
        AtomicInteger stock = stockMap.get(productId);
        return stock != null ? stock.get() : 0;
    }

    /**
     * Attempt to purchase a product for a user.
     */
    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) {
            return "Product not found.";
        }

        while (true) {
            int currentStock = stock.get();
            if (currentStock > 0) {
                // Attempt to decrement stock atomically
                if (stock.compareAndSet(currentStock, currentStock - 1)) {
                    return "Success, " + (currentStock - 1) + " units remaining";
                }
                // else, retry due to concurrent modification
            } else {
                // Out of stock, add to waiting list
                Queue<Integer> queue = waitingList.get(productId);
                queue.add(userId);
                return "Out of stock. Added to waiting list, position #" + queue.size();
            }
        }
    }

    /**
     * Get the current waiting list for a product.
     */
    public List<Integer> getWaitingList(String productId) {
        Queue<Integer> queue = waitingList.get(productId);
        return queue != null ? new ArrayList<>(queue) : Collections.emptyList();
    }

    /**
     * For demonstration: main method
     */
    public static void main(String[] args) throws InterruptedException {
        FlashSaleInventory inventory = new FlashSaleInventory();
        String product = "IPHONE15_256GB";
        inventory.addProduct(product, 5); // small stock for demo

        // Simulate 10 users trying to buy concurrently
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int userId = 1; userId <= 10; userId++) {
            final int uid = userId;
            executor.submit(() -> {
                String result = inventory.purchaseItem(product, uid);
                System.out.println("User " + uid + ": " + result);
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Final stock: " + inventory.checkStock(product));
        System.out.println("Waiting list: " + inventory.getWaitingList(product));
    }
}