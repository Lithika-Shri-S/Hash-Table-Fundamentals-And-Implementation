import java.util.*;

class ParkingSpot {
    String licensePlate;
    long entryTime;
    boolean occupied;

    ParkingSpot() {
        this.licensePlate = null;
        this.entryTime = 0;
        this.occupied = false;
    }
}

public class BookMyStayApp {
    private ParkingSpot[] spots;
    private int capacity;
    private int totalProbes = 0;
    private int parkedVehicles = 0;
    private Map<Integer, Integer> hourlyOccupancy = new HashMap<>(); // hour -> count

    public BookMyStayApp(int capacity) {
        this.capacity = capacity;
        spots = new ParkingSpot[capacity];
        for (int i = 0; i < capacity; i++) {
            spots[i] = new ParkingSpot();
        }
    }

    // Simple hash function: licensePlate → spot index
    private int hash(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % capacity;
    }

    // Park a vehicle using linear probing
    public int parkVehicle(String licensePlate) {
        int start = hash(licensePlate);
        int probes = 0;

        for (int i = 0; i < capacity; i++) {
            int spotIndex = (start + i) % capacity;
            if (!spots[spotIndex].occupied) {
                spots[spotIndex].licensePlate = licensePlate;
                spots[spotIndex].entryTime = System.currentTimeMillis();
                spots[spotIndex].occupied = true;

                totalProbes += probes;
                parkedVehicles++;

                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                hourlyOccupancy.put(hour, hourlyOccupancy.getOrDefault(hour, 0) + 1);

                System.out.println("Vehicle " + licensePlate + " parked at spot #" + spotIndex + " (" + probes + " probes)");
                return spotIndex;
            }
            probes++;
        }
        System.out.println("Parking Full! Could not park " + licensePlate);
        return -1; // Parking full
    }

    // Exit a vehicle and calculate fee
    public double exitVehicle(String licensePlate) {
        for (int i = 0; i < capacity; i++) {
            if (spots[i].occupied && spots[i].licensePlate.equals(licensePlate)) {
                long durationMillis = System.currentTimeMillis() - spots[i].entryTime;
                double hours = durationMillis / (1000.0 * 60 * 60);
                double fee = Math.ceil(hours) * 5.0; // $5 per hour, rounded up

                spots[i].licensePlate = null;
                spots[i].occupied = false;
                spots[i].entryTime = 0;
                parkedVehicles--;

                System.out.println("Vehicle " + licensePlate + " exited from spot #" + i +
                        ", Duration: " + String.format("%.2f", hours) + "h, Fee: $" + String.format("%.2f", fee));
                return fee;
            }
        }
        System.out.println("Vehicle " + licensePlate + " not found in parking lot!");
        return 0;
    }

    // Generate statistics
    public void getStatistics() {
        double avgProbes = parkedVehicles > 0 ? (double) totalProbes / parkedVehicles : 0;
        int occupancy = (int) ((double) parkedVehicles / capacity * 100);

        int peakHour = hourlyOccupancy.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        System.out.println("Occupancy: " + occupancy + "%");
        System.out.println("Avg Probes: " + String.format("%.2f", avgProbes));
        System.out.println("Peak Hour: " + (peakHour != -1 ? peakHour + ":00" : "N/A"));
    }

    // Main method for demo
    public static void main(String[] args) {
        BookMyStayApp parkingLot = new BookMyStayApp(500);

        parkingLot.parkVehicle("ABC-1234");
        parkingLot.parkVehicle("ABC-1235");
        parkingLot.parkVehicle("XYZ-9999");

        parkingLot.exitVehicle("ABC-1234");

        parkingLot.getStatistics();
    }
}