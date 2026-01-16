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

class IndexOperations {
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

    void dropIndex(Session session, HsqlName name) {
 
        writeLock.lock();

        try {
            Table t = getTable(session, name.parent.name,
                               name.parent.schema.name);
            TableWorks tw = new TableWorks(session, t);

            tw.dropIndex(name.name);
        } finally {
            writeLock.unlock();
        }
    }

    public void setIndexRoots(int[][] roots) {
 
        readLock.lock();

        try {
            HsqlArrayList allTables =
                database.schemaManager.getAllTables(true);

            for (int i = 0, size = allTables.size(); i < size; i++) {
                Table t = (Table) allTables.get(i);

                if (t.getTableType() == TableBase.CACHED_TABLE) {
                    int[] rootsArray = roots[i];

                    if (roots != null) {
                        t.setIndexRoots(rootsArray);
                    }
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public int[][] getIndexRoots(Session session) {
 
        readLock.lock();

        try {
            if (tempIndexRoots != null) {
                int[][] roots = tempIndexRoots;

                tempIndexRoots = null;

                return roots;
            }

            HsqlArrayList allTables = getAllTables(true);
            HsqlArrayList list      = new HsqlArrayList();

            for (int i = 0, size = allTables.size(); i < size; i++) {
                Table t = (Table) allTables.get(i);

                if (t.getTableType() == TableBase.CACHED_TABLE) {
                    int[] roots = t.getIndexRootsArray();

                    list.add(roots);
                } else {
                    list.add(null);
                }
            }

            int[][] array = new int[list.size()][];

            list.toArray(array);

            return array;
        } finally {
            readLock.unlock();
        }
    }
}