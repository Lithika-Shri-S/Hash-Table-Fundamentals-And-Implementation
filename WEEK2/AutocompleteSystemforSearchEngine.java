import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isWord = false;
    String word = null;
    int frequency = 0;
}

public class AutocompleteSystem {

    private final TrieNode root;
    private final int suggestionLimit;

    public AutocompleteSystem(int suggestionLimit) {
        this.root = new TrieNode();
        this.suggestionLimit = suggestionLimit;
    }

    /** Insert a new query or update its frequency */
    public void insertQuery(String query) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isWord = true;
        node.word = query;
        node.frequency += 1;
    }

    /** Return top K suggestions for a given prefix */
    public List<String> getSuggestions(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return Collections.emptyList();
        }
        // Use min-heap to keep top K frequent queries
        PriorityQueue<TrieNode> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.frequency));
        dfs(node, heap);
        List<String> result = new ArrayList<>();
        while (!heap.isEmpty()) {
            result.add(heap.poll().word + " (" + heap.peekFrequency() + " searches)");
        }
        Collections.reverse(result); // highest frequency first
        return result;
    }

    /** DFS traversal to collect words in subtree */
    private void dfs(TrieNode node, PriorityQueue<TrieNode> heap) {
        if (node.isWord) {
            if (heap.size() < suggestionLimit) {
                heap.offer(node);
            } else if (node.frequency > heap.peek().frequency) {
                heap.poll();
                heap.offer(node);
            }
        }
        for (TrieNode child : node.children.values()) {
            dfs(child, heap);
        }
    }

    /** Update frequency for an existing query (or insert if new) */
    public void updateFrequency(String query) {
        insertQuery(query);
    }

    /** Demo usage */
    public static void main(String[] args) {
        AutocompleteSystem autocomplete = new AutocompleteSystem(10);

        // Simulate bulk insert (like 10M previous queries)
        autocomplete.insertQuery("java tutorial");
        autocomplete.insertQuery("javascript");
        autocomplete.insertQuery("java download");
        autocomplete.insertQuery("java tutorial"); // increment frequency
        autocomplete.insertQuery("jav bus");
        autocomplete.insertQuery("jav debug");
        autocomplete.insertQuery("java download");

        // Get suggestions for prefix "jav"
        List<String> suggestions = autocomplete.getSuggestions("jav");
        System.out.println("Suggestions for 'jav':");
        for (String s : suggestions) {
            System.out.println(s);
        }

        // Update frequency for trending query
        autocomplete.updateFrequency("java 21 features");
        autocomplete.updateFrequency("java 21 features");
        autocomplete.updateFrequency("java 21 features");
        System.out.println("\nUpdated frequency suggestions for 'java 21':");
        for (String s : autocomplete.getSuggestions("java 21")) {
            System.out.println(s);
        }
    }
}