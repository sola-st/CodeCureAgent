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

class TableOperations {
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

    public Table getTable(Session session, String name, String schema) {
 
        readLock.lock();

        try {
            Table t = null;

            if (Tokens.T_MODULE.equals(schema)
                    || Tokens.T_SESSION.equals(schema)) {
                t = findSessionTable(session, name);

                if (t == null) {
                    throw Error.error(ErrorCode.X_42501, name);
                }

                return t;
            }

            if (schema == null) {
                if (session.database.sqlSyntaxOra) {
                    if (Tokens.T_DUAL.equals(name)) {
                        return dualTable;
                    }
                }

                t = findSessionTable(session, name);
            }

            if (t == null) {
                schema = session.getSchemaName(schema);
                t      = findUserTable(session, name, schema);
            }

            if (t == null) {
                if (SqlInvariants.INFORMATION_SCHEMA.equals(schema)
                        && database.dbInfo != null) {
                    t = database.dbInfo.getSystemTable(session, name);
                }
            }

            if (t == null) {
                throw Error.error(ErrorCode.X_42501, name);
            }

            return t;
        } finally {
            readLock.unlock();
        }
    }
 
    public Table getUserTable(Session session, HsqlName name) {
        return getUserTable(session, name.name, name.schema.name);
    }

    public Table getUserTable(Session session, String name, String schema) {

        Table t = findUserTable(session, name, schema);

        if (t == null) {
            String longName = schema == null ? name
                                             : schema + '.' + name;

            throw Error.error(ErrorCode.X_42501, longName);
        }

        return t;
    }
 
     public Table findUserTable(Session session, String name,
                                String schemaName) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(schemaName);
 
             if (schema == null) {
                 return null;
             }
 
             int i = schema.tableList.getIndex(name);
 
             if (i == -1) {
                 return null;
             }
 
             return (Table) schema.tableList.get(i);
         } finally {
             readLock.unlock();
         }
     }

     public Table findSessionTable(Session session, String name) {
        return session.sessionContext.findSessionTable(name);
    }

    public void dropTableOrView(Session session, Table table,
                                boolean cascade) {

        writeLock.lock();

        try {
            if (table.isView()) {
                dropView(table, cascade);
            } else {
                dropTable(session, table, cascade);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void dropView(Table table, boolean cascade) {

        Schema schema = (Schema) schemaMap.get(table.getSchemaName().name);

        removeSchemaObject(table.getName(), cascade);
        schema.triggerLookup.removeParent(table.getName());
    }

    private void dropTable(Session session, Table table, boolean cascade) {

        Schema schema    = (Schema) schemaMap.get(table.getSchemaName().name);
        int    dropIndex = schema.tableList.getIndex(table.getName().name);
        OrderedHashSet externalConstraints =
            table.getDependentExternalConstraints();
        OrderedHashSet externalReferences = new OrderedHashSet();

        getCascadingReferencesTo(table.getName(), externalReferences);

        if (!cascade) {
            for (int i = 0; i < externalConstraints.size(); i++) {
                Constraint c       = (Constraint) externalConstraints.get(i);
                HsqlName   refname = c.getRefName();

                if (c.getConstraintType()
                        == SchemaObject.ConstraintTypes.MAIN) {
                    throw Error.error(
                        ErrorCode.X_42533,
                        refname.getSchemaQualifiedStatementName());
                }
            }

            if (!externalReferences.isEmpty()) {
                int i = 0;

                for (; i < externalReferences.size(); i++) {
                    HsqlName name = (HsqlName) externalReferences.get(i);

                    if (name.parent == table.getName()) {
                        continue;
                    }

                    throw Error.error(ErrorCode.X_42502,
                                      name.getSchemaQualifiedStatementName());
                }
            }
        }

        OrderedHashSet tableSet          = new OrderedHashSet();
        OrderedHashSet constraintNameSet = new OrderedHashSet();
        OrderedHashSet indexNameSet      = new OrderedHashSet();

        for (int i = 0; i < externalConstraints.size(); i++) {
            Constraint c = (Constraint) externalConstraints.get(i);
            Table      t = c.getMain();

            if (t != table) {
                tableSet.add(t);
            }

            t = c.getRef();

            if (t != table) {
                tableSet.add(t);
            }

            constraintNameSet.add(c.getMainName());
            constraintNameSet.add(c.getRefName());
            indexNameSet.add(c.getRefIndex().getName());
        }

        OrderedHashSet uniqueConstraintNames =
            table.getUniquePKConstraintNames();
        TableWorks tw = new TableWorks(session, table);

        tableSet = tw.makeNewTables(tableSet, constraintNameSet, indexNameSet);

        tw.setNewTablesInSchema(tableSet);
        tw.updateConstraints(tableSet, constraintNameSet);
        removeSchemaObjects(externalReferences);
        removeTableDependentReferences(table);    //
        removeReferencesTo(uniqueConstraintNames);
        removeReferencesTo(table.getName());
        removeReferencesFrom(table);
        schema.tableList.remove(dropIndex);
        schema.indexLookup.removeParent(table.getName());
        schema.constraintLookup.removeParent(table.getName());
        schema.triggerLookup.removeParent(table.getName());
        removeTable(session, table);
        recompileDependentObjects(tableSet);
    }

    private void removeTable(Session session, Table table) {

        database.getGranteeManager().removeDbObject(table.getName());
        table.releaseTriggers();

        if (table.hasLobColumn()) {
            RowIterator it = table.rowIterator(session);

            while (it.hasNext()) {
                Row      row  = it.getNextRow();
                Object[] data = row.getData();

                session.sessionData.adjustLobUsageCount(table, data, -1);
            }
        }

        database.persistentStoreCollection.releaseStore(table);
    }

    public void setTable(int index, Table table) {

        writeLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(table.getSchemaName().name);

            schema.tableList.set(index, table.getName().name, table);
        } finally {
            writeLock.unlock();
        }
    }

    public int getTableIndex(Table table) {

        readLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(table.getSchemaName().name);

            if (schema == null) {
                return -1;
            }

            HsqlName name = table.getName();

            return schema.tableList.getIndex(name.name);
        } finally {
            readLock.unlock();
        }
    }
 
    public HsqlArrayList getAllTables(boolean withLobTables) {

        readLock.lock();

        try {
            HsqlArrayList alltables = new HsqlArrayList();
            String[]      schemas   = getSchemaNamesArray();

            for (int i = 0; i < schemas.length; i++) {
                String name = schemas[i];

                if (!withLobTables && SqlInvariants.isLobsSchemaName(name)) {
                    continue;
                }

                if (SqlInvariants.isSystemSchemaName(name)) {
                    continue;
                }

                HashMappedList current = getTables(name);

                alltables.addAll(current.values());
            }

            return alltables;
        } finally {
            readLock.unlock();
        }
    }

    public HashMappedList getTables(String schema) {

        readLock.lock();

        try {
            Schema temp = (Schema) schemaMap.get(schema);

            return temp.tableList;
        } finally {
            readLock.unlock();
        }
    }
 
    public void recompileDependentObjects(OrderedHashSet tableSet) {

        writeLock.lock();

        try {
            OrderedHashSet set = new OrderedHashSet();

            for (int i = 0; i < tableSet.size(); i++) {
                Table table = (Table) tableSet.get(i);

                set.addAll(getReferencesTo(table.getName()));
            }

            Session session = database.sessionManager.getSysSession();

            for (int i = 0; i < set.size(); i++) {
                HsqlName name = (HsqlName) set.get(i);

                switch (name.type) {

                    case SchemaObject.VIEW :
                    case SchemaObject.CONSTRAINT :
                    case SchemaObject.ASSERTION :
                    case SchemaObject.ROUTINE :
                    case SchemaObject.PROCEDURE :
                    case SchemaObject.FUNCTION :
                    case SchemaObject.SPECIFIC_ROUTINE :
                        SchemaObject object = getSchemaObject(name);

                        object.compile(session, null);
                        break;
                }
            }

            if (Error.TRACE) {
                HsqlArrayList list = getAllTables(false);

                for (int i = 0; i < list.size(); i++) {
                    Table t = (Table) list.get(i);

                    t.verifyConstraintsIntegrity();
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void recompileDependentObjects(Table table) {

        writeLock.lock();

        try {
            OrderedHashSet set = new OrderedHashSet();

            getCascadingReferencesTo(table.getName(), set);

            Session session = database.sessionManager.getSysSession();

            for (int i = 0; i < set.size(); i++) {
                HsqlName name = (HsqlName) set.get(i);

                switch (name.type) {

                    case SchemaObject.VIEW :
                    case SchemaObject.CONSTRAINT :
                    case SchemaObject.ASSERTION :
                    case SchemaObject.ROUTINE :
                    case SchemaObject.PROCEDURE :
                    case SchemaObject.FUNCTION :
                    case SchemaObject.SPECIFIC_ROUTINE :
                        SchemaObject object = getSchemaObject(name);

                        object.compile(session, null);
                        break;
                }
            }

            if (Error.TRACE) {
                HsqlArrayList list = getAllTables(false);

                for (int i = 0; i < list.size(); i++) {
                    Table t = (Table) list.get(i);

                    t.verifyConstraintsIntegrity();
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    Table findUserTableForIndex(Session session, String name,
                                 String schemaName) {
 
         readLock.lock();
 
         try {
             Schema   schema    = (Schema) schemaMap.get(schemaName);
             HsqlName indexName = schema.indexLookup.getName(name);
 
             if (indexName == null) {
                 return null;
             }
 
             return findUserTable(session, indexName.parent.name, schemaName);
         } finally {
             readLock.unlock();
         }
    }
}