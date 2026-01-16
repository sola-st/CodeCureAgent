public class SchemaManager {

    Database          database;
    HsqlName          defaultSchemaHsqlName;
    HashMappedList    schemaMap = new HashMappedList();
    // ... (Other existing code and member variables remain unchanged)

    // New class to handle schema creation
    class SchemaCreator {
        private Database database;
        private HsqlName defaultSchemaHsqlName;
        private HashMappedList schemaMap;

        public SchemaCreator(Database database, HsqlName defaultSchemaHsqlName, HashMappedList schemaMap) {
            this.database = database;
            this.defaultSchemaHsqlName = defaultSchemaHsqlName;
            this.schemaMap = schemaMap;
        }

        public void createPublicSchema() {
            // Implementation of creating public schema
        }

        public void createSchema(HsqlName name, Grantee owner) {
            // Implementation of creating a schema
        }
    }

    // New class to handle schema dropping
    class SchemaDropper {
        private HashMappedList schemaMap;
        private Lock writeLock;

        public SchemaDropper(HashMappedList schemaMap, Lock writeLock) {
            this.schemaMap = schemaMap;
            this.writeLock = writeLock;
        }

        public void dropSchema(Session session, String name, boolean cascade) {
            // Implementation of dropping a schema
        }
    }

    // Use the new classes within the SchemaManager methods
    private SchemaCreator schemaCreator;
    private SchemaDropper schemaDropper;

    public SchemaManager(Database database) {
        // ... (Other existing constructor code remains unchanged)
        this.schemaCreator = new SchemaCreator(database, defaultSchemaHsqlName, schemaMap);
        this.schemaDropper = new SchemaDropper(schemaMap, writeLock);
    }

    // Example method usage after refactoring
    void createPublicSchema() {
        schemaCreator.createPublicSchema();
    }

    void createSchema(HsqlName name, Grantee owner) {
        schemaCreator.createSchema(name, owner);
    }

    void dropSchema(Session session, String name, boolean cascade) {
        schemaDropper.dropSchema(session, name, cascade);
    }

    // ... (Other methods remain unchanged, but may also delegate to the new classes)
}