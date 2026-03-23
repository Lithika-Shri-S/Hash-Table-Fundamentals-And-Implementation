import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameChecker {

    // Stores username -> userId mapping
    private final Map<String, String> registeredUsers;

    // Tracks username attempt frequency
    private final Map<String, AtomicInteger> attemptFrequency;

    public UsernameChecker() {
        // ConcurrentHashMap supports thread-safe reads/writes for concurrent checks
        registeredUsers = new ConcurrentHashMap<>();
        attemptFrequency = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a username is available.
     * Updates the attempt frequency counter.
     */
    public boolean checkAvailability(String username) {
        attemptFrequency.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
        return !registeredUsers.containsKey(username);
    }

    /**
     * Registers a username for a userId.
     */
    public boolean registerUsername(String username, String userId) {
        if (checkAvailability(username)) {
            registeredUsers.put(username, userId);
            return true;
        }
        return false;
    }

    /**
     * Suggest alternative usernames if the requested one is taken.
     */
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        int suffix = 1;

        while (suggestions.size() < 5) { // limit to 5 suggestions
            String suggestion = username + suffix;
            if (!registeredUsers.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
            suffix++;
        }

        // Optionally, add a dot-replacement style suggestion
        if (suggestions.size() < 5) {
            String dotSuggestion = username.replace("_", ".") + "1";
            if (!registeredUsers.containsKey(dotSuggestion)) {
                suggestions.add(dotSuggestion);
            }
        }

        return suggestions;
    }

    /**
     * Returns the most attempted username.
     */
    public String getMostAttempted() {
        return attemptFrequency.entrySet()
                .stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * For demonstration: populate some existing usernames.
     */
    public void seedUsers() {
        registeredUsers.put("john_doe", "user123");
        registeredUsers.put("admin", "user1");
        registeredUsers.put("jane_smith", "user456");
        attemptFrequency.put("admin", new AtomicInteger(10543));
    }

    // Main method for quick demonstration
    public static void main(String[] args) {
        UsernameChecker checker = new UsernameChecker();
        checker.seedUsers();

        System.out.println("Is 'john_doe' available? " + checker.checkAvailability("john_doe")); // false
        System.out.println("Is 'jane_smith' available? " + checker.checkAvailability("jane_smith")); // false
        System.out.println("Is 'alice_wonder' available? " + checker.checkAvailability("alice_wonder")); // true

        System.out.println("Suggestions for 'john_doe': " + checker.suggestAlternatives("john_doe")); // ["john_doe1", "john_doe2", ...]
        System.out.println("Most attempted username: " + checker.getMostAttempted()); // "admin"
    }
}