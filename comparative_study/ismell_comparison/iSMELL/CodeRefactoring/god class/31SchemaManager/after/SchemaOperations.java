package org.hsqldb;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.validation.Schema;

import org.hsqldb.HsqlNameManager.HsqlName;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.HashMappedList;
import org.hsqldb.lib.HsqlArrayList;
import org.hsqldb.lib.Iterator;
import org.hsqldb.lib.MultiValueHashMap;
import org.hsqldb.lib.OrderedHashSet;
import org.hsqldb.lib.StringConverter;
import org.hsqldb.lib.WrapperIterator;
import org.hsqldb.navigator.RowIterator;
import org.hsqldb.rights.Grantee;
import org.hsqldb.types.Type;

class SchemaOperations {
    Database          database;
     HsqlName          defaultSchemaHsqlName;
     HashMappedList    schemaMap        = new HashMappedList();
     MultiValueHashMap referenceMap     = new MultiValueHashMap();
     int               defaultTableType = TableBase.MEMORY_TABLE;
     long              schemaChangeTimestamp;
     HsqlName[]        catalogNameArray;
 
     //
     ReadWriteLock lock      = new ReentrantReadWriteLock();
     Lock          readLock  = lock.readLock();
     Lock          writeLock = lock.writeLock();
 
     //
     Table dualTable;

    public void createSchema(HsqlName name, Grantee owner) {
 
        writeLock.lock();

        try {
            SqlInvariants.checkSchemaNameNotSystem(name.name);

            Schema schema = new Schema(name, owner);

            schemaMap.add(name.name, schema);
        } finally {
            writeLock.unlock();
        }
    }

    public void dropSchema(Session session, String name, boolean cascade) {

        writeLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(name);

            if (schema == null) {
                throw Error.error(ErrorCode.X_42501, name);
            }

            if (SqlInvariants.isLobsSchemaName(name)) {
                throw Error.error(ErrorCode.X_42503, name);
            }

            if (!cascade && !schema.isEmpty()) {
                throw Error.error(ErrorCode.X_2B000);
            }

            OrderedHashSet externalReferences = new OrderedHashSet();

            getCascadingReferencesToSchema(schema.getName(),
                                           externalReferences);
            removeSchemaObjects(externalReferences);

            Iterator tableIterator =
                schema.schemaObjectIterator(SchemaObject.TABLE);

            while (tableIterator.hasNext()) {
                Table        table = ((Table) tableIterator.next());
                Constraint[] list  = table.getFKConstraints();

                for (int i = 0; i < list.length; i++) {
                    Constraint constraint = list[i];

                    if (constraint.getMain().getSchemaName()
                            != schema.getName()) {
                        constraint.getMain().removeConstraint(
                            constraint.getMainName().name);
                        removeReferencesFrom(constraint);
                    }
                }

                removeTable(session, table);
            }

            Iterator sequenceIterator =
                schema.schemaObjectIterator(SchemaObject.SEQUENCE);

            while (sequenceIterator.hasNext()) {
                NumberSequence sequence =
                    ((NumberSequence) sequenceIterator.next());

                database.getGranteeManager().removeDbObject(
                    sequence.getName());
            }

            schema.clearStructures();
            schemaMap.remove(name);

            if (defaultSchemaHsqlName.name.equals(name)) {
                HsqlName hsqlName = database.nameManager.newHsqlName(name,
                    false, SchemaObject.SCHEMA);

                schema = new Schema(hsqlName,
                                    database.getGranteeManager().getDBARole());
                defaultSchemaHsqlName = schema.getName();

                schemaMap.put(schema.getName().name, schema);
            }

            // these are called last and in this particular order
            database.getUserManager().removeSchemaReference(name);
            database.getSessionManager().removeSchemaReference(schema);
        } finally {
            writeLock.unlock();
        }
    }

    public void renameSchema(HsqlName name, HsqlName newName) {

        writeLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(name.name);
            Schema exists = (Schema) schemaMap.get(newName.name);

            if (schema == null) {
                throw Error.error(ErrorCode.X_42501, name.name);
            }

            if (exists != null) {
                throw Error.error(ErrorCode.X_42504, newName.name);
            }

            SqlInvariants.checkSchemaNameNotSystem(name.name);
            SqlInvariants.checkSchemaNameNotSystem(newName.name);

            int index = schemaMap.getIndex(name.name);

            schema.getName().rename(newName);
            schemaMap.set(index, newName.name, schema);
        } finally {
            writeLock.unlock();
        }
    }

    public void setSchemaChangeTimestamp() {
         schemaChangeTimestamp = database.txManager.getGlobalChangeTimestamp();
     }
 
     public long getSchemaChangeTimestamp() {
         return schemaChangeTimestamp;
     }
 
     // pre-defined
     public HsqlName getSQLJSchemaHsqlName() {
         return SqlInvariants.SQLJ_SCHEMA_HSQLNAME;
     }
 
     // SCHEMA management
     public void createPublicSchema() {
 
         writeLock.lock();
 
         try {
             HsqlName name = database.nameManager.newHsqlName(null,
                 SqlInvariants.PUBLIC_SCHEMA, SchemaObject.SCHEMA);
             Schema schema =
                 new Schema(name, database.getGranteeManager().getDBARole());
 
             defaultSchemaHsqlName = schema.getName();
 
             schemaMap.put(schema.getName().name, schema);
         } finally {
             writeLock.unlock();
         }
     }
  
     public void clearStructures() {
 
         writeLock.lock();
 
         try {
             Iterator it = schemaMap.values().iterator();
 
             while (it.hasNext()) {
                 Schema schema = (Schema) it.next();
 
                 schema.clearStructures();
             }
         } finally {
             writeLock.unlock();
         }
     }
 
     public String[] getSchemaNamesArray() {
 
         readLock.lock();
 
         try {
             String[] array = new String[schemaMap.size()];
 
             schemaMap.toKeysArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     public Schema[] getAllSchemas() {
 
         readLock.lock();
 
         try {
             Schema[] objects = new Schema[schemaMap.size()];
 
             schemaMap.toValuesArray(objects);
 
             return objects;
         } finally {
             readLock.unlock();
         }
     }
 
     public HsqlName getUserSchemaHsqlName(String name) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(name);
 
             if (schema == null) {
                 throw Error.error(ErrorCode.X_3F000, name);
             }
 
             if (schema.getName()
                     == SqlInvariants.INFORMATION_SCHEMA_HSQLNAME) {
                 throw Error.error(ErrorCode.X_3F000, name);
             }
 
             return schema.getName();
         } finally {
             readLock.unlock();
         }
     }
 
     public Grantee toSchemaOwner(String name) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(name);
 
             return schema == null ? null
                                   : schema.getOwner();
         } finally {
             readLock.unlock();
         }
     }
 
     public HsqlName getDefaultSchemaHsqlName() {
         return defaultSchemaHsqlName;
     }
 
     public void setDefaultSchemaHsqlName(HsqlName name) {
         defaultSchemaHsqlName = name;
     }
 
     public boolean schemaExists(String name) {
 
         readLock.lock();
 
         try {
             return schemaMap.containsKey(name);
         } finally {
             readLock.unlock();
         }
     }
 
     public HsqlName findSchemaHsqlName(String name) {
 
         readLock.lock();
 
         try {
             Schema schema = ((Schema) schemaMap.get(name));
 
             if (schema == null) {
                 return null;
             }
 
             return schema.getName();
         } finally {
             readLock.unlock();
         }
     }


     public HsqlName getSchemaHsqlName(String name) {
 
         if (name == null) {
             return defaultSchemaHsqlName;
         }
 
         readLock.lock();
 
         try {
             Schema schema = ((Schema) schemaMap.get(name));
 
             if (schema == null) {
                 throw Error.error(ErrorCode.X_3F000, name);
             }
 
             return schema.getName();
         } finally {
             readLock.unlock();
         }
     }

     public String getSchemaName(String name) {
         return getSchemaHsqlName(name).name;
     }
 
     public Schema findSchema(String name) {
 
         readLock.lock();
 
         try {
             return ((Schema) schemaMap.get(name));
         } finally {
             readLock.unlock();
         }
     }
 
     public void dropSchemas(Session session, Grantee grantee,
                             boolean cascade) {
 
         writeLock.lock();
 
         try {
             HsqlArrayList list = getSchemas(grantee);
             Iterator      it   = list.iterator();
 
             while (it.hasNext()) {
                 Schema schema = (Schema) it.next();
 
                 dropSchema(session, schema.getName().name, cascade);
             }
         } finally {
             writeLock.unlock();
         }
     }
 
     public HsqlArrayList getSchemas(Grantee grantee) {
 
         readLock.lock();
 
         try {
             HsqlArrayList list = new HsqlArrayList();
             Iterator      it   = schemaMap.values().iterator();
 
             while (it.hasNext()) {
                 Schema schema = (Schema) it.next();
 
                 if (grantee.equals(schema.getOwner())) {
                     list.add(schema);
                 }
             }
 
             return list;
         } finally {
             readLock.unlock();
         }
     }
 
     public boolean hasSchemas(Grantee grantee) {
 
         readLock.lock();
 
         try {
             Iterator it = schemaMap.values().iterator();
 
             while (it.hasNext()) {
                 Schema schema = (Schema) it.next();
 
                 if (grantee.equals(schema.getOwner())) {
                     return true;
                 }
             }
 
             return false;
         } finally {
             readLock.unlock();
         }
     }
 
     public HsqlName[] getCatalogNameArray() {
         return catalogNameArray;
     }
 
     public HsqlName[] getCatalogAndBaseTableNames() {
 
         readLock.lock();
 
         try {
             OrderedHashSet names  = new OrderedHashSet();
             HsqlArrayList  tables = getAllTables(false);
 
             for (int i = 0; i < tables.size(); i++) {
                 Table table = (Table) tables.get(i);
 
                 if (!table.isTemp()) {
                     names.add(table.getName());
                 }
             }
 
             names.add(database.getCatalogName());
 
             HsqlName[] array = new HsqlName[names.size()];
 
             names.toArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     public HsqlName[] getCatalogAndBaseTableNames(HsqlName name) {
 
         readLock.lock();
 
         if (name == null) {
             return catalogNameArray;
         }
 
         try {
             switch (name.type) {
 
                 case SchemaObject.SCHEMA : {
                     if (findSchemaHsqlName(name.name) == null) {
                         return catalogNameArray;
                     }
 
                     OrderedHashSet names = new OrderedHashSet();
 
                     names.add(database.getCatalogName());
 
                     HashMappedList list = getTables(name.name);
 
                     for (int i = 0; i < list.size(); i++) {
                         names.add(((SchemaObject) list.get(i)).getName());
                     }
 
                     HsqlName[] array = new HsqlName[names.size()];
 
                     names.toArray(array);
 
                     return array;
                 }
                 case SchemaObject.GRANTEE : {
                     return catalogNameArray;
                 }
                 case SchemaObject.INDEX :
                 case SchemaObject.CONSTRAINT :
                     findSchemaObject(name.name, name.schema.name, name.type);
             }
 
             SchemaObject object = findSchemaObject(name.name,
                                                    name.schema.name,
                                                    name.type);
 
             if (object == null) {
                 return catalogNameArray;
             }
 
             HsqlName       parent     = object.getName().parent;
             OrderedHashSet references = getReferencesTo(object.getName());
             OrderedHashSet names      = new OrderedHashSet();
 
             names.add(database.getCatalogName());
 
             if (parent != null) {
                 SchemaObject parentObject = findSchemaObject(parent.name,
                     parent.schema.name, parent.type);
 
                 if (parentObject != null
                         && parentObject.getName().type == SchemaObject.TABLE) {
                     names.add(parentObject.getName());
                 }
             }
 
             if (object.getName().type == SchemaObject.TABLE) {
                 names.add(object.getName());
             }
 
             for (int i = 0; i < references.size(); i++) {
                 HsqlName reference = (HsqlName) references.get(i);
 
                 if (reference.type == SchemaObject.TABLE) {
                     Table table = findUserTable(null, reference.name,
                                                 reference.schema.name);
 
                     if (table != null && !table.isTemp()) {
                         names.add(reference);
                     }
                 }
             }
 
             HsqlName[] array = new HsqlName[names.size()];
 
             names.toArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     private SchemaObjectSet getSchemaObjectSet(Schema schema, int type) {
 
         readLock.lock();
 
         try {
             SchemaObjectSet set = null;
 
             switch (type) {
 
                 case SchemaObject.SEQUENCE :
                     set = schema.sequenceLookup;
                     break;
 
                 case SchemaObject.TABLE :
                 case SchemaObject.VIEW :
                     set = schema.tableLookup;
                     break;
 
                 case SchemaObject.CHARSET :
                     set = schema.charsetLookup;
                     break;
 
                 case SchemaObject.COLLATION :
                     set = schema.collationLookup;
                     break;
 
                 case SchemaObject.PROCEDURE :
                     set = schema.procedureLookup;
                     break;
 
                 case SchemaObject.FUNCTION :
                     set = schema.functionLookup;
                     break;
 
                 case SchemaObject.DOMAIN :
                 case SchemaObject.TYPE :
                     set = schema.typeLookup;
                     break;
 
                 case SchemaObject.INDEX :
                     set = schema.indexLookup;
                     break;
 
                 case SchemaObject.CONSTRAINT :
                     set = schema.constraintLookup;
                     break;
 
                 case SchemaObject.TRIGGER :
                     set = schema.triggerLookup;
                     break;
 
                 case SchemaObject.SPECIFIC_ROUTINE :
                     set = schema.specificRoutineLookup;
             }
 
             return set;
         } finally {
             readLock.unlock();
         }
     }
 
     public void checkSchemaObjectNotExists(HsqlName name) {
 
         readLock.lock();
 
         try {
             Schema          schema = (Schema) schemaMap.get(name.schema.name);
             SchemaObjectSet set    = getSchemaObjectSet(schema, name.type);
 
             set.checkAdd(name);
         } finally {
             readLock.unlock();
         }
     }
 
     public MultiValueHashMap getReferencesToSchema(String schemaName) {
 
         MultiValueHashMap map          = new MultiValueHashMap();
         Iterator          mainIterator = referenceMap.keySet().iterator();
 
         while (mainIterator.hasNext()) {
             HsqlName name = (HsqlName) mainIterator.next();
 
             if (!name.schema.name.equals(schemaName)) {
                 continue;
             }
 
             Iterator it = referenceMap.get(name);
 
             while (it.hasNext()) {
                 map.put(name, it.next());
             }
         }
 
         return map;
     }
 
     //
     public HsqlName getSchemaObjectName(HsqlName schemaName, String name,
                                         int type, boolean raise) {
 
         readLock.lock();
 
         try {
             Schema          schema = (Schema) schemaMap.get(schemaName.name);
             SchemaObjectSet set    = null;
 
             if (schema == null) {
                 if (raise) {
                     throw Error.error(SchemaObjectSet.getGetErrorCode(type));
                 } else {
                     return null;
                 }
             }
 
             if (type == SchemaObject.ROUTINE) {
                 set = schema.functionLookup;
 
                 SchemaObject object = schema.functionLookup.getObject(name);
 
                 if (object == null) {
                     set    = schema.procedureLookup;
                     object = schema.procedureLookup.getObject(name);
                 }
             } else {
                 set = getSchemaObjectSet(schema, type);
             }
 
             if (raise) {
                 set.checkExists(name);
             }
 
             return set.getName(name);
         } finally {
             readLock.unlock();
         }
     }
 
     public void checkSchemaNameCanChange(HsqlName name) {
 
         readLock.lock();
 
         try {
             Iterator it      = referenceMap.values().iterator();
             HsqlName refName = null;
 
             mainLoop:
             while (it.hasNext()) {
                 refName = (HsqlName) it.next();
 
                 switch (refName.type) {
 
                     case SchemaObject.VIEW :
                     case SchemaObject.ROUTINE :
                     case SchemaObject.FUNCTION :
                     case SchemaObject.PROCEDURE :
                     case SchemaObject.TRIGGER :
                     case SchemaObject.SPECIFIC_ROUTINE :
                         if (refName.schema == name) {
                             break mainLoop;
                         }
                         break;
 
                     default :
                         break;
                 }
 
                 refName = null;
             }
 
             if (refName == null) {
                 return;
             }
 
             throw Error.error(ErrorCode.X_42502,
                               refName.getSchemaQualifiedStatementName());
         } finally {
             readLock.unlock();
         }
     }
}