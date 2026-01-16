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

class SequenceOperations {
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

     public NumberSequence getSequence(String name, String schemaName,
                                       boolean raise) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(schemaName);
 
             if (schema != null) {
                 NumberSequence object =
                     (NumberSequence) schema.sequenceList.get(name);
 
                 if (object != null) {
                     return object;
                 }
             }
 
             if (raise) {
                 throw Error.error(ErrorCode.X_42501, name);
             }
 
             return null;
         } finally {
             readLock.unlock();
         }
     }
}