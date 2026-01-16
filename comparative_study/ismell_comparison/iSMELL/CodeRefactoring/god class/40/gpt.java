// New class to handle schema information
class SchemaManager {
    private Database db;

    SchemaManager(Database db) {
        this.db = db;
    }

    // Schema-related methods extracted from DatabaseInformationMain...
}

// New class to handle table information
class TableManager {
    private Database db;

    TableManager(Database db) {
        this.db = db;
    }

    // Table-related methods extracted from DatabaseInformationMain...
}

// New class to handle system table cache
class SystemTableCache {
    private Database db;
    private Table[] sysTables;

    SystemTableCache(Database db) {
        this.db = db;
        this.sysTables = new Table[sysTableNames.length];
    }

    // System table cache management methods extracted from DatabaseInformationMain...
}

// Refactored DatabaseInformationMain class
class DatabaseInformationMain extends DatabaseInformation {
    private SchemaManager schemaManager;
    private TableManager tableManager;
    private SystemTableCache systemTableCache;

    DatabaseInformationMain(Database db) {
        super(db);
        this.schemaManager = new SchemaManager(db);
        this.tableManager = new TableManager(db);
        this.systemTableCache = new SystemTableCache(db);
        // Initialize system tables...
        initSystemTables();
    }

    private void initSystemTables() {
        // Initialization logic for system tables...
    }

    // Delegate methods to the new classes
    public Table getSystemTable(String name) {
        return systemTableCache.getSystemTable(name);
    }

    public void clearSystemTableCache() {
        systemTableCache.clearCache();
    }

    // Other methods that delegate to the new classes...
}

    // Example of refactored method inside SystemTableCache
    public Table getSystemTable(String name) {
        // Logic to retrieve a system table by name...
    }