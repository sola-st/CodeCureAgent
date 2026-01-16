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

class TypeOperations {
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

    public Type getUserDefinedType(String name, String schemaName,
                                    boolean raise) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(schemaName);
 
             if (schema != null) {
                 SchemaObject object = schema.typeLookup.getObject(name);
 
                 if (object != null) {
                     return (Type) object;
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
 
    public Type getDomainOrUDT(String name, String schemaName, boolean raise) {

        readLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(schemaName);

            if (schema != null) {
                SchemaObject object = schema.typeLookup.getObject(name);

                if (object != null) {
                    return (Type) object;
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

    public Type getDomain(String name, String schemaName, boolean raise) {

        readLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(schemaName);

            if (schema != null) {
                SchemaObject object = schema.typeLookup.getObject(name);

                if (object != null && ((Type) object).isDomainType()) {
                    return (Type) object;
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

    public Type getDistinctType(String name, String schemaName,
                                boolean raise) {

        readLock.lock();

        try {
            Schema schema = (Schema) schemaMap.get(schemaName);

            if (schema != null) {
                SchemaObject object = schema.typeLookup.getObject(name);

                if (object != null && ((Type) object).isDistinctType()) {
                    return (Type) object;
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