import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Grocery Inventory System (console app, no external dependencies).
 *
 * Run directly with:  java GroceryInventory.java
 * Or compile:         javac GroceryInventory.java && java GroceryInventory
 *
 * Data is saved to grocery_inventory.csv in the working directory.
 */
public class GroceryInventory {

    // ------------------------------------------------------------------
    // Model
    // ------------------------------------------------------------------
    static class Product {
        private final String id;
        private String name;
        private String category;
        private int quantity;
        private double price;
        private int reorderLevel;
        private LocalDate expiry; // null = no expiry

        Product(String id, String name, String category, int quantity,
                double price, int reorderLevel, LocalDate expiry) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.price = price;
            this.reorderLevel = reorderLevel;
            this.expiry = expiry;
        }

        String getId() { return id; }
        String getName() { return name; }
        String getCategory() { return category; }
        int getQuantity() { return quantity; }
        double getPrice() { return price; }
        int getReorderLevel() { return reorderLevel; }
        LocalDate getExpiry() { return expiry; }

        void setPrice(double price) { this.price = price; }
        void setQuantity(int quantity) { this.quantity = quantity; }

        double totalValue() { return quantity * price; }
        boolean isLowStock() { return quantity <= reorderLevel; }

        boolean isExpired() {
            return expiry != null && expiry.isBefore(LocalDate.now());
        }

        boolean expiresWithin(int days) {
            if (expiry == null) return false;
            long left = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
            return left >= 0 && left <= days;
        }

        String toCsv() {
            return String.join(",", id, clean(name), clean(category),
                    String.valueOf(quantity), String.valueOf(price),
                    String.valueOf(reorderLevel),
                    expiry == null ? "" : expiry.toString());
        }

        static Product fromCsv(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 7) throw new IllegalArgumentException("Bad record: " + line);
            LocalDate exp = p[6].isBlank() ? null : LocalDate.parse(p[6]);
            return new Product(p[0], p[1], p[2], Integer.parseInt(p[3]),
                    Double.parseDouble(p[4]), Integer.parseInt(p[5]), exp);
        }

        private static String clean(String s) {
            return s.replace(",", " ").trim();
        }
    }

    // ------------------------------------------------------------------
    // Inventory (business logic + persistence)
    // ------------------------------------------------------------------
    static class Inventory {
        private final Map<String, Product> products = new LinkedHashMap<>();
        private final Path file;
        private int nextId = 1;

        Inventory(String filename) {
            this.file = Paths.get(filename);
        }

        Product add(String name, String category, int qty, double price,
                    int reorderLevel, LocalDate expiry) {
            String id = String.format("P%03d", nextId++);
            Product p = new Product(id, name, category, qty, price, reorderLevel, expiry);
            products.put(id, p);
            return p;
        }

        Product get(String id) {
            return products.get(id.trim().toUpperCase());
        }

        boolean remove(String id) {
            return products.remove(id.trim().toUpperCase()) != null;
        }

        List<Product> all() {
            return new ArrayList<>(products.values());
        }

        List<Product> search(String term) {
            String t = term.toLowerCase();
            return products.values().stream()
                    .filter(p -> p.getName().toLowerCase().contains(t)
                            || p.getCategory().toLowerCase().contains(t))
                    .collect(Collectors.toList());
        }

        List<Product> lowStock() {
            return products.values().stream()
                    .filter(Product::isLowStock)
                    .sorted(Comparator.comparingInt(Product::getQuantity))
                    .collect(Collectors.toList());
        }

        List<Product> expired() {
            return products.values().stream()
                    .filter(Product::isExpired)
                    .collect(Collectors.toList());
        }

        List<Product> expiringSoon(int days) {
            return products.values().stream()
                    .filter(p -> p.expiresWithin(days))
                    .sorted(Comparator.comparing(Product::getExpiry))
                    .collect(Collectors.toList());
        }

        double totalValue() {
            return products.values().stream().mapToDouble(Product::totalValue).sum();
        }

        Map<String, Double> valueByCategory() {
            return products.values().stream().collect(Collectors.groupingBy(
                    Product::getCategory, java.util.TreeMap::new,
                    Collectors.summingDouble(Product::totalValue)));
        }

        void save() throws IOException {
            List<String> lines = products.values().stream()
                    .map(Product::toCsv).collect(Collectors.toList());
            Files.write(file, lines);
        }

        void load() {
            if (!Files.exists(file)) return;
            try {
                for (String line : Files.readAllLines(file)) {
                    if (line.isBlank()) continue;
                    Product p = Product.fromCsv(line);
                    products.put(p.getId(), p);
                    try {
                        int n = Integer.parseInt(p.getId().substring(1));
                        nextId = Math.max(nextId, n + 1);
                    } catch (NumberFormatException ignored) { }
                }
                System.out.println("Loaded " + products.size() + " product(s) from " + file);
            } catch (IOException | RuntimeException e) {
                System.out.println("Could not fully load data: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------
    // Console UI
    // ------------------------------------------------------------------
    private static final Scanner in = new Scanner(System.in);
    private static final Inventory inventory = new Inventory("grocery_inventory.csv");

    public static void main(String[] args) {
        System.out.println("=== Grocery Inventory System ===");
        inventory.load();

        boolean running = true;
        while (running) {
            printMenu();
            switch (readLine("Choose an option: ")) {
                case "1": addProduct(); break;
                case "2": printTable(inventory.all(), "All products"); break;
                case "3": searchProducts(); break;
                case "4": restock(); break;
                case "5": sell(); break;
                case "6": updatePrice(); break;
                case "7": deleteProduct(); break;
                case "8": printTable(inventory.lowStock(), "Low stock items"); break;
                case "9": expiryReport(); break;
                case "10": valueReport(); break;
                case "0":
                    running = false;
                    break;
                default: System.out.println("Invalid option, try again.");
            }
        }

        try {
            inventory.save();
            System.out.println("Inventory saved. Goodbye!");
        } catch (IOException e) {
            System.out.println("Error saving inventory: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1) Add product        6) Update price");
        System.out.println("2) View all           7) Delete product");
        System.out.println("3) Search             8) Low stock report");
        System.out.println("4) Restock            9) Expiry report");
        System.out.println("5) Sell / use stock  10) Inventory value");
        System.out.println("0) Save & exit");
    }

    private static void addProduct() {
        String name = readNonEmpty("Name: ");
        String category = readNonEmpty("Category (e.g. Dairy, Produce): ");
        int qty = readInt("Quantity: ", 0);
        double price = readDouble("Unit price: ", 0);
        int reorder = readInt("Reorder level (alert at or below): ", 0);
        LocalDate expiry = readOptionalDate("Expiry date (yyyy-mm-dd, blank for none): ");

        Product p = inventory.add(name, category, qty, price, reorder, expiry);
        System.out.println("Added " + p.getName() + " with ID " + p.getId());
    }

    private static void searchProducts() {
        String term = readNonEmpty("Search name or category: ");
        printTable(inventory.search(term), "Search results");
    }

    private static void restock() {
        Product p = pickProduct();
        if (p == null) return;
        int amount = readInt("Quantity to add: ", 1);
        p.setQuantity(p.getQuantity() + amount);
        System.out.println(p.getName() + " now has " + p.getQuantity() + " in stock.");
    }

    private static void sell() {
        Product p = pickProduct();
        if (p == null) return;
        int amount = readInt("Quantity to remove (in stock: " + p.getQuantity() + "): ", 1);
        if (amount > p.getQuantity()) {
            System.out.println("Not enough stock.");
            return;
        }
        p.setQuantity(p.getQuantity() - amount);
        System.out.printf("Removed %d. %s now has %d left.%n", amount, p.getName(), p.getQuantity());
        if (p.isLowStock()) {
            System.out.println("WARNING: " + p.getName() + " is at or below its reorder level!");
        }
    }

    private static void updatePrice() {
        Product p = pickProduct();
        if (p == null) return;
        p.setPrice(readDouble("New unit price: ", 0));
        System.out.printf("Price for %s set to %.2f%n", p.getName(), p.getPrice());
    }

    private static void deleteProduct() {
        Product p = pickProduct();
        if (p == null) return;
        String confirm = readLine("Delete " + p.getName() + "? (y/n): ");
        if (confirm.equalsIgnoreCase("y")) {
            inventory.remove(p.getId());
            System.out.println("Deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void expiryReport() {
        printTable(inventory.expired(), "EXPIRED items");
        printTable(inventory.expiringSoon(7), "Expiring within 7 days");
    }

    private static void valueReport() {
        System.out.println("\n--- Inventory value by category ---");
        inventory.valueByCategory().forEach((cat, val) ->
                System.out.printf("%-20s %10.2f%n", cat, val));
        System.out.printf("%-20s %10.2f%n", "TOTAL", inventory.totalValue());
    }

    // ------------------------------------------------------------------
    // Display helpers
    // ------------------------------------------------------------------
    private static void printTable(List<Product> list, String title) {
        System.out.println("\n--- " + title + " (" + list.size() + ") ---");
        if (list.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        String fmt = "%-6s %-20s %-12s %6s %9s %-11s %s%n";
        System.out.printf(fmt, "ID", "Name", "Category", "Qty", "Price", "Expiry", "Status");
        System.out.println("-".repeat(80));
        for (Product p : list) {
            System.out.printf(fmt, p.getId(), truncate(p.getName(), 20),
                    truncate(p.getCategory(), 12), p.getQuantity(),
                    String.format("%.2f", p.getPrice()),
                    p.getExpiry() == null ? "-" : p.getExpiry().toString(),
                    status(p));
        }
    }

    private static String status(Product p) {
        List<String> flags = new ArrayList<>();
        if (p.isExpired()) flags.add("EXPIRED");
        else if (p.expiresWithin(7)) flags.add("EXPIRING");
        if (p.getQuantity() == 0) flags.add("OUT");
        else if (p.isLowStock()) flags.add("LOW");
        return flags.isEmpty() ? "OK" : String.join(",", flags);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------
    private static Product pickProduct() {
        String id = readNonEmpty("Product ID (e.g. P001): ");
        Product p = inventory.get(id);
        if (p == null) System.out.println("No product with ID " + id.toUpperCase());
        return p;
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) { // input closed (Ctrl+D) -> exit cleanly
            System.out.println();
            return "0";
        }
        return in.nextLine().trim();
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("This field is required.");
        }
    }

    private static int readInt(String prompt, int min) {
        while (true) {
            try {
                int v = Integer.parseInt(readLine(prompt));
                if (v >= min) return v;
                System.out.println("Value must be at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt, double min) {
        while (true) {
            try {
                double v = Double.parseDouble(readLine(prompt));
                if (v >= min) return v;
                System.out.println("Value must be at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static LocalDate readOptionalDate(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) return null;
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("Use the format yyyy-mm-dd (e.g. 2026-12-31).");
            }
        }
    }
}