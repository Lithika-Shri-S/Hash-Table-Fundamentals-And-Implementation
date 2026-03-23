import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

class Transaction {
    int id;
    double amount;
    String merchant;
    String account;
    LocalDateTime timestamp;

    Transaction(int id, double amount, String merchant, String account, String time) {
        this.id = id;
        this.amount = amount;
        this.merchant = merchant;
        this.account = account;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        this.timestamp = LocalTime.parse(time, formatter).atDate(LocalDate.now());
        this.merchant = merchant;
        this.account = account;
    }

    @Override
    public String toString() {
        return "{id:" + id + ", amount:" + amount + ", merchant:" + merchant + ", account:" + account + ", time:" + timestamp.toLocalTime() + "}";
    }
}

public class BookMyStayApp {

    private List<Transaction> transactions;

    public BookMyStayApp() {
        this.transactions = new ArrayList<>();
    }

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }

    /** CLASSIC TWO-SUM **/
    public List<List<Transaction>> findTwoSum(double target) {
        Map<Double, Transaction> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                result.add(Arrays.asList(map.get(complement), t));
            }
            map.put(t.amount, t);
        }
        return result;
    }

    /** TWO-SUM WITH TIME WINDOW (in minutes) **/
    public List<List<Transaction>> findTwoSumWithinTime(double target, long minutesWindow) {
        Map<Double, Transaction> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                Transaction prev = map.get(complement);
                long diff = Math.abs(Duration.between(prev.timestamp, t.timestamp).toMinutes());
                if (diff <= minutesWindow) {
                    result.add(Arrays.asList(prev, t));
                }
            }
            map.put(t.amount, t);
        }
        return result;
    }

    /** DUPLICATE DETECTION: same amount, same merchant, different accounts **/
    public List<Map<String, Object>> detectDuplicates() {
        Map<String, List<Transaction>> map = new HashMap<>();
        List<Map<String, Object>> duplicates = new ArrayList<>();

        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : map.entrySet()) {
            List<Transaction> list = entry.getValue();
            Set<String> accounts = new HashSet<>();
            for (Transaction t : list) accounts.add(t.account);
            if (accounts.size() > 1) {
                Map<String, Object> dup = new HashMap<>();
                dup.put("amount", list.get(0).amount);
                dup.put("merchant", list.get(0).merchant);
                dup.put("accounts", accounts);
                duplicates.add(dup);
            }
        }

        return duplicates;
    }

    /** K-SUM (recursive with hash table for memoization) **/
    public List<List<Transaction>> findKSum(int k, double target) {
        List<List<Transaction>> results = new ArrayList<>();
        findKSumHelper(transactions, k, target, 0, new ArrayList<>(), results);
        return results;
    }

    private void findKSumHelper(List<Transaction> list, int k, double target, int start, List<Transaction> path, List<List<Transaction>> results) {
        if (k == 0 && Math.abs(target) < 1e-6) { // allow floating point precision tolerance
            results.add(new ArrayList<>(path));
            return;
        }
        if (k == 0) return;

        for (int i = start; i < list.size(); i++) {
            path.add(list.get(i));
            findKSumHelper(list, k - 1, target - list.get(i).amount, i + 1, path, results);
            path.remove(path.size() - 1);
        }
    }

    /** MAIN DEMO **/
    public static void main(String[] args) {
        BookMyStayApp app = new BookMyStayApp();

        app.addTransaction(new Transaction(1, 500, "Store A", "acc1", "10:00"));
        app.addTransaction(new Transaction(2, 300, "Store B", "acc2", "10:15"));
        app.addTransaction(new Transaction(3, 200, "Store C", "acc3", "10:30"));
        app.addTransaction(new Transaction(4, 500, "Store A", "acc2", "11:00"));

        System.out.println("=== Classic Two-Sum (Target=500) ===");
        app.findTwoSum(500).forEach(System.out::println);

        System.out.println("\n=== Two-Sum within 60 mins (Target=500) ===");
        app.findTwoSumWithinTime(500, 60).forEach(System.out::println);

        System.out.println("\n=== Detect Duplicates ===");
        app.detectDuplicates().forEach(System.out::println);

        System.out.println("\n=== 3-Sum (Target=1000) ===");
        app.findKSum(3, 1000).forEach(System.out::println);
    }
}