import java.util.*;
import java.nio.file.*;
import java.io.*;

/**
 * Plagiarism Detection using n-grams and hash maps
 */
public class PlagiarismDetector {

    // n-gram size
    private final int N;
    // Maps n-gram -> set of document IDs containing it
    private final Map<String, Set<String>> ngramIndex = new HashMap<>();
    // Stores document ID -> n-grams for faster similarity calculation
    private final Map<String, List<String>> docNgrams = new HashMap<>();

    public PlagiarismDetector(int n) {
        this.N = n;
    }

    /** Read document, tokenize, and extract n-grams */
    private List<String> extractNGrams(String content) {
        String[] words = content.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        List<String> ngrams = new ArrayList<>();
        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N; j++) {
                if (j > 0) sb.append(" ");
                sb.append(words[i + j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }

    /** Index a document by its n-grams */
    public void indexDocument(String docId, String content) {
        List<String> ngrams = extractNGrams(content);
        docNgrams.put(docId, ngrams);

        for (String ngram : ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(docId);
        }

        System.out.println("Document " + docId + " indexed with " + ngrams.size() + " n-grams.");
    }

    /** Analyze a new document against indexed documents */
    public void analyzeDocument(String docId, String content) {
        List<String> ngrams = extractNGrams(content);
        Map<String, Integer> matchCount = new HashMap<>();

        for (String ngram : ngrams) {
            Set<String> docs = ngramIndex.getOrDefault(ngram, Collections.emptySet());
            for (String otherDoc : docs) {
                if (!otherDoc.equals(docId)) {
                    matchCount.put(otherDoc, matchCount.getOrDefault(otherDoc, 0) + 1);
                }
            }
        }

        System.out.println("Analysis for " + docId + ":");
        for (Map.Entry<String, Integer> entry : matchCount.entrySet()) {
            String otherDoc = entry.getKey();
            int matches = entry.getValue();
            int totalNgrams = docNgrams.get(otherDoc).size();
            double similarity = (matches * 100.0) / totalNgrams;
            String status = similarity > 50.0 ? "PLAGIARISM DETECTED" : "suspicious";
            System.out.printf("→ Matches with %s: %d n-grams, Similarity: %.2f%% (%s)%n",
                    otherDoc, matches, similarity, status);
        }
    }

    /** Demo */
    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector(5); // 5-grams

        // Sample documents
        String doc1 = "The quick brown fox jumps over the lazy dog in the forest";
        String doc2 = "A quick brown fox jumps over a lazy dog near the river";
        String doc3 = "Completely unrelated text without any matching words";

        detector.indexDocument("essay_001", doc1);
        detector.indexDocument("essay_002", doc2);
        detector.indexDocument("essay_003", doc3);

        String newSubmission = "The quick brown fox jumps over a lazy dog in the forest";
        detector.analyzeDocument("essay_004", newSubmission);
    }
}