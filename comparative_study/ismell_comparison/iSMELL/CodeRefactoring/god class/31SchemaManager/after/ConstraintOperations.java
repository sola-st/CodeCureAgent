package org.hsqldb;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

class ConstraintOperations {
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

    void dropConstraint(Session session, HsqlName name, boolean cascade) {
 
        writeLock.lock();

        try {
            Table t = getTable(session, name.parent.name,
                               name.parent.schema.name);
            TableWorks tw = new TableWorks(session, t);

            tw.dropConstraint(name.name, cascade);
        } finally {
            writeLock.unlock();
        }
    }
}