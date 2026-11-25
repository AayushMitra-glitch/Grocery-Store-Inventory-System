https://drive.google.com/drive/folders/1G8EEj1mLK9ZQqEKXirvWYJ7wdxBDCZEp?usp=sharing

Grocery Inventory SystemA lightweight, console-based Grocery Inventory Management System built in pure Core Java with zero external dependencies. Designed as a Data Structures and Algorithms (DSA) project, it leverages a LinkedHashMap for constant-time ($O(1)$) product lookups, Java Streams for declarative reporting, java.time for expiry tracking, and flat-file CSV persistence.FeaturesProduct Management (CRUD): Add new products with auto-generated sequential IDs (P001, P002, ...), view all inventory, update unit prices, and delete items with confirmation.Stock Operations: Restock incoming inventory and process sales/usage with built-in oversell protection (prevents negative stock) and instant reorder-level warnings.Keyword Search: Case-insensitive substring search across product names and categories.Automated Alerts & Status Flags: Dynamically computes product statuses (OK, LOW, OUT, EXPIRING, EXPIRED) in aligned console tables.Expiry Tracking: Separates already-expired items from perishable items expiring within the next 7 days, sorted chronologically.Inventory Valuation Reports: Calculates total stock value (quantity × price) and breaks down value alphabetically by category.Defensive Input Validation: Re-prompts cleanly on empty strings, out-of-range numbers, invalid date formats, or EOF (Ctrl+D).CSV Persistence: Automatically loads existing records from grocery_inventory.csv on startup and saves changes upon exit.Project ArchitectureAll components are cleanly encapsulated within a single source file (GroceryInventory.java) across three static nested classes:ComponentRoleKey ResponsibilitiesProductData ModelStores item attributes (id, name, category, quantity, price, reorderLevel, nullable expiry), computes totalValue(), isLowStock(), isExpired(), expiresWithin(days), and handles CSV serialization/deserialization.InventoryLogic & StorageManages a LinkedHashMap<String, Product>, executes Java Stream filters/sorts/aggregations, maintains auto-incrementing IDs, and handles file I/O via java.nio.file.Files.GroceryInventoryConsole UIRuns the 11-option interactive CLI menu (0–10), enforces input validation helpers, and formats aligned ASCII output tables.Algorithmic ComplexityLet $N$ = total number of products, $K$ = number of matching items in a filtered report, and $C$ = number of distinct categories:OperationData Structure / APITime ComplexityAdd / Get / Restock / Sell / Delete by IDLinkedHashMap (put, get, remove)$O(1)$ averageSearch by Name or CategoryStream.filter()$O(N)$Low-Stock & Expiry ReportsStream.filter() + .sorted() (TimSort)$O(N + K \log K)$Category Valuation SummaryCollectors.groupingBy(TreeMap)$O(N \log C)$Total Inventory ValueStream.mapToDouble().sum()$O(N)$Load / Save CSV FileFiles.readAllLines() / Files.write()$O(N)$Getting StartedPrerequisitesJava Development Kit (JDK) 11 or higher (JDK 17 or 21 recommended)No external libraries or database setup requiredRunning the ApplicationClone the repository:git clone https://github.com/<your-username>/grocery-inventory-system.git
cd grocery-inventory-system
Run directly (Single-File Source-Code Execution, Java 11+):java GroceryInventory.java
Or compile and run traditionally:javac GroceryInventory.java
java GroceryInventory
Usage & Menu Options=== Grocery Inventory System ===
1) Add product        6) Update price
2) View all           7) Delete product
3) Search             8) Low stock report
4) Restock            9) Expiry report
5) Sell / use stock  10) Inventory value
0) Save & exit
Sample Console Output--- Low stock items (2) ---
ID     Name                 Category        Qty     Price Expiry      Status
--------------------------------------------------------------------------------
P005   Sugar                Grocery           3      1.10 -           LOW
P001   Milk                 Dairy             5      2.50 2026-11-20  EXPIRING,LOW
CSV Data Format (grocery_inventory.csv)Each product is persisted as a 7-column comma-separated row (commas inside product names or categories are sanitized to spaces upon export):P001,Milk,Dairy,5,2.5,10,2026-11-20
P002,Rice,Grains,40,1.2,10,
Future Roadmap[ ] Relational database persistence (SQLite / MySQL via JDBC)[ ] Graphical user interface (JavaFX / Swing) or REST API backend[ ] Barcode scanning and customer billing receipts[ ] Supplier tracking and automated purchase order suggestions[ ] Role-based authentication (Admin / Storekeeper)
AuthorAayush MitraB.Tech CSE (AI & ML) — Rungta International Skills University
