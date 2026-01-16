/* Copyright (c) 2001-2011, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


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

import feature_envy.Routine.重构后.Routine;
 
 /**
  * Manages all SCHEMA related database objects
  *
  * @author Fred Toussi (fredt@users dot sourceforge.net)
  * @version  2.2.6
  * @since 1.8.0
  */
 public class SchemaManager {
 
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

     private SchemaOperations schemaOperations;
    private TableOperations tableOperations;
    private SequenceOperations sequenceOperations;
    private TypeOperations typeOperations;
    private IndexOperations indexOperations;
    private ConstraintOperations constraintOperations;
    private TriggerOperations triggerOperations;
    private RoutineOperations routineOperations;

    public SchemaManager(Database database) {
        this.database         = database;
        defaultSchemaHsqlName = SqlInvariants.INFORMATION_SCHEMA_HSQLNAME;
        catalogNameArray      = new HsqlName[]{ database.getCatalogName() };

        Schema schema =
            new Schema(SqlInvariants.INFORMATION_SCHEMA_HSQLNAME,
                       SqlInvariants.INFORMATION_SCHEMA_HSQLNAME.owner);

        schemaMap.put(schema.getName().name, schema);

        try {
            schema.typeLookup.add(TypeInvariants.CARDINAL_NUMBER);
            schema.typeLookup.add(TypeInvariants.YES_OR_NO);
            schema.typeLookup.add(TypeInvariants.CHARACTER_DATA);
            schema.typeLookup.add(TypeInvariants.SQL_IDENTIFIER);
            schema.typeLookup.add(TypeInvariants.TIME_STAMP);
            schema.charsetLookup.add(TypeInvariants.SQL_TEXT);
            schema.charsetLookup.add(TypeInvariants.SQL_IDENTIFIER_CHARSET);
            schema.charsetLookup.add(TypeInvariants.SQL_CHARACTER);
        } catch (HsqlException e) {}

        this.schemaOperations = new SchemaOperations(/* parameters */);
        this.tableOperations = new TableOperations(/* parameters */);
        this.sequenceOperations = new SequenceOperations(/* parameters */);
        this.typeOperations = new TypeOperations(/* parameters */);
        this.indexOperations = new IndexOperations(/* parameters */);
        this.constraintOperations = new ConstraintOperations(/* parameters */);
        this.triggerOperations = new TriggerOperations(/* parameters */);
        this.routineOperations = new RoutineOperations(/* parameters */);
    }
 
     public SchemaObject getSchemaObject(String name, String schemaName,
                                         int type) {
 
         readLock.lock();
 
         try {
             SchemaObject object = findSchemaObject(name, schemaName, type);
 
             if (object == null) {
                 throw Error.error(SchemaObjectSet.getGetErrorCode(type), name);
             }
 
             return object;
         } finally {
             readLock.unlock();
         }
     }
 
     public SchemaObject findSchemaObject(String name, String schemaName,
                                          int type) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(schemaName);
 
             if (schema == null) {
                 return null;
             }
 
             SchemaObjectSet set = null;
             HsqlName        objectName;
             Table           table;
 
             switch (type) {
 
                 case SchemaObject.SEQUENCE :
                     return schema.sequenceLookup.getObject(name);
 
                 case SchemaObject.TABLE :
                 case SchemaObject.VIEW :
                     return schema.tableLookup.getObject(name);
 
                 case SchemaObject.CHARSET :
                     if (name.equals("SQL_IDENTIFIER")) {
                         return TypeInvariants.SQL_IDENTIFIER_CHARSET;
                     }
 
                     if (name.equals("SQL_TEXT")) {
                         return TypeInvariants.SQL_TEXT;
                     }
 
                     if (name.equals("LATIN1")) {
                         return TypeInvariants.LATIN1;
                     }
 
                     if (name.equals("ASCII_GRAPHIC")) {
                         return TypeInvariants.ASCII_GRAPHIC;
                     }
 
                     return schema.charsetLookup.getObject(name);
 
                 case SchemaObject.COLLATION :
                     return schema.collationLookup.getObject(name);
 
                 case SchemaObject.PROCEDURE :
                     return schema.procedureLookup.getObject(name);
 
                 case SchemaObject.FUNCTION :
                     return schema.functionLookup.getObject(name);
 
                 case SchemaObject.ROUTINE : {
                     SchemaObject object =
                         schema.procedureLookup.getObject(name);
 
                     if (object == null) {
                         object = schema.functionLookup.getObject(name);
                     }
 
                     return object;
                 }
                 case SchemaObject.SPECIFIC_ROUTINE :
                     return schema.specificRoutineLookup.getObject(name);
 
                 case SchemaObject.DOMAIN :
                 case SchemaObject.TYPE :
                     return schema.typeLookup.getObject(name);
 
                 case SchemaObject.INDEX :
                     set        = schema.indexLookup;
                     objectName = set.getName(name);
 
                     if (objectName == null) {
                         return null;
                     }
 
                     table =
                         (Table) schema.tableList.get(objectName.parent.name);
 
                     return table.getIndex(name);
 
                 case SchemaObject.CONSTRAINT :
                     set        = schema.constraintLookup;
                     objectName = set.getName(name);
 
                     if (objectName == null) {
                         return null;
                     }
 
                     table =
                         (Table) schema.tableList.get(objectName.parent.name);
 
                     if (table == null) {
                         return null;
                     }
 
                     return table.getConstraint(name);
 
                 case SchemaObject.TRIGGER :
                     set        = schema.indexLookup;
                     objectName = set.getName(name);
 
                     if (objectName == null) {
                         return null;
                     }
 
                     table =
                         (Table) schema.tableList.get(objectName.parent.name);
 
                     return table.getTrigger(name);
 
                 default :
                     throw Error.runtimeError(ErrorCode.U_S0500,
                                              "SchemaManager");
             }
         } finally {
             readLock.unlock();
         }
     }
 
     void removeDependentObjects(HsqlName name) {
 
         writeLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(name.schema.name);
 
             schema.indexLookup.removeParent(name);
             schema.constraintLookup.removeParent(name);
             schema.triggerLookup.removeParent(name);
         } finally {
             writeLock.unlock();
         }
     }
 
     /**
      *  Removes any foreign key Constraint objects (exported keys) held by any
      *  tables referenced by the specified table. <p>
      *
      *  This method is called as the last step of a successful call to
      *  dropTable() in order to ensure that the dropped Table ceases to be
      *  referenced when enforcing referential integrity.
      *
      * @param  toDrop The table to which other tables may be holding keys.
      *      This is a table that is in the process of being dropped.
      */
     void removeExportedKeys(Table toDrop) {
 
         writeLock.lock();
 
         try {
 
             // toDrop.schema may be null because it is not registerd
             Schema schema =
                 (Schema) schemaMap.get(toDrop.getSchemaName().name);
 
             for (int i = 0; i < schema.tableList.size(); i++) {
                 Table        table       = (Table) schema.tableList.get(i);
                 Constraint[] constraints = table.getConstraints();
 
                 for (int j = constraints.length - 1; j >= 0; j--) {
                     Table refTable = constraints[j].getRef();
 
                     if (toDrop == refTable) {
                         table.removeConstraint(j);
                     }
                 }
             }
         } finally {
             writeLock.unlock();
         }
     }
 
     public Iterator databaseObjectIterator(String schemaName, int type) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(schemaName);
 
             return schema.schemaObjectIterator(type);
         } finally {
             readLock.unlock();
         }
     }
 
     public Iterator databaseObjectIterator(int type) {
 
         readLock.lock();
 
         try {
             Iterator it      = schemaMap.values().iterator();
             Iterator objects = new WrapperIterator();
 
             while (it.hasNext()) {
                 int targetType = type;
 
                 if (type == SchemaObject.ROUTINE) {
                     targetType = SchemaObject.FUNCTION;
                 }
 
                 Schema          temp = (Schema) it.next();
                 SchemaObjectSet set  = temp.getObjectSet(targetType);
                 Object[]        values;
 
                 if (set.map.size() != 0) {
                     values = new Object[set.map.size()];
 
                     set.map.valuesToArray(values);
 
                     objects = new WrapperIterator(objects,
                                                   new WrapperIterator(values));
                 }
 
                 if (type == SchemaObject.ROUTINE) {
                     set = temp.getObjectSet(SchemaObject.PROCEDURE);
 
                     if (set.map.size() != 0) {
                         values = new Object[set.map.size()];
 
                         set.map.valuesToArray(values);
 
                         objects =
                             new WrapperIterator(objects,
                                                 new WrapperIterator(values));
                     }
                 }
             }
 
             return objects;
         } finally {
             readLock.unlock();
         }
     }
 
     // references
     private void addReferencesFrom(SchemaObject object) {
 
         OrderedHashSet set  = object.getReferences();
         HsqlName       name = object.getName();
 
         if (set == null) {
             return;
         }
 
         for (int i = 0; i < set.size(); i++) {
             HsqlName referenced = (HsqlName) set.get(i);
 
             if (object instanceof Routine) {
                 name = ((Routine) object).getSpecificName();
             }
 
             referenceMap.put(referenced, name);
         }
     }
 
     private void removeReferencesTo(OrderedHashSet set) {
 
         for (int i = 0; i < set.size(); i++) {
             HsqlName referenced = (HsqlName) set.get(i);
 
             referenceMap.remove(referenced);
         }
     }
 
     private void removeReferencesTo(HsqlName referenced) {
         referenceMap.remove(referenced);
     }
 
     private void removeReferencesFrom(SchemaObject object) {
 
         HsqlName       name = object.getName();
         OrderedHashSet set  = object.getReferences();
 
         if (set == null) {
             return;
         }
 
         for (int i = 0; i < set.size(); i++) {
             HsqlName referenced = (HsqlName) set.get(i);
 
             if (object instanceof Routine) {
                 name = ((Routine) object).getSpecificName();
             }
 
             referenceMap.remove(referenced, name);
         }
     }
 
     private void removeTableDependentReferences(Table table) {
 
         OrderedHashSet mainSet = table.getReferencesForDependents();
 
         if (mainSet == null) {
             return;
         }
 
         for (int i = 0; i < mainSet.size(); i++) {
             HsqlName     name   = (HsqlName) mainSet.get(i);
             SchemaObject object = null;
 
             switch (name.type) {
 
                 case SchemaObject.CONSTRAINT :
                     object = table.getConstraint(name.name);
                     break;
 
                 case SchemaObject.TRIGGER :
                     object = table.getTrigger(name.name);
                     break;
 
                 case SchemaObject.COLUMN :
                     object = table.getColumn(table.getColumnIndex(name.name));
                     break;
             }
 
             removeReferencesFrom(object);
         }
     }
 
     public OrderedHashSet getReferencesTo(HsqlName object) {
 
         readLock.lock();
 
         try {
             OrderedHashSet set = new OrderedHashSet();
             Iterator       it  = referenceMap.get(object);
 
             while (it.hasNext()) {
                 HsqlName name = (HsqlName) it.next();
 
                 set.add(name);
             }
 
             return set;
         } finally {
             readLock.unlock();
         }
     }
 
     public OrderedHashSet getReferencesTo(HsqlName table, HsqlName column) {
 
         readLock.lock();
 
         try {
             OrderedHashSet set = new OrderedHashSet();
             Iterator       it  = referenceMap.get(table);
 
             while (it.hasNext()) {
                 HsqlName       name       = (HsqlName) it.next();
                 SchemaObject   object     = getSchemaObject(name);
                 OrderedHashSet references = object.getReferences();
 
                 if (references.contains(column)) {
                     set.add(name);
                 }
             }
 
             return set;
         } finally {
             readLock.unlock();
         }
     }
 
     private boolean isReferenced(HsqlName object) {
 
         writeLock.lock();
 
         try {
             return referenceMap.containsKey(object);
         } finally {
             writeLock.unlock();
         }
     }
 
     //
     public void getCascadingReferencesTo(HsqlName object, OrderedHashSet set) {
 
         readLock.lock();
 
         try {
             OrderedHashSet newSet = new OrderedHashSet();
             Iterator       it     = referenceMap.get(object);
 
             while (it.hasNext()) {
                 HsqlName name  = (HsqlName) it.next();
                 boolean  added = set.add(name);
 
                 if (added) {
                     newSet.add(name);
                 }
             }
 
             for (int i = 0; i < newSet.size(); i++) {
                 HsqlName name = (HsqlName) newSet.get(i);
 
                 getCascadingReferencesTo(name, set);
             }
         } finally {
             readLock.unlock();
         }
     }
 
     public void getCascadingReferencesToSchema(HsqlName schema,
             OrderedHashSet set) {
 
         Iterator mainIterator = referenceMap.keySet().iterator();
 
         while (mainIterator.hasNext()) {
             HsqlName name = (HsqlName) mainIterator.next();
 
             if (name.schema != schema) {
                 continue;
             }
 
             getCascadingReferencesTo(name, set);
         }
 
         for (int i = 0; i < set.size(); i++) {
             HsqlName name = (HsqlName) set.get(i);
 
             if (name.schema == schema) {
                 set.remove(i);
 
                 i--;
             }
         }
     }
 
     public SchemaObject getSchemaObject(HsqlName name) {
 
         readLock.lock();
 
         try {
             Schema schema = (Schema) schemaMap.get(name.schema.name);
 
             if (schema == null) {
                 return null;
             }
 
             switch (name.type) {
 
                 case SchemaObject.SEQUENCE :
                     return (SchemaObject) schema.sequenceList.get(name.name);
 
                 case SchemaObject.TABLE :
                 case SchemaObject.VIEW :
                     return (SchemaObject) schema.tableList.get(name.name);
 
                 case SchemaObject.CHARSET :
                     return schema.charsetLookup.getObject(name.name);
 
                 case SchemaObject.COLLATION :
                     return schema.collationLookup.getObject(name.name);
 
                 case SchemaObject.PROCEDURE :
                     return schema.procedureLookup.getObject(name.name);
 
                 case SchemaObject.FUNCTION :
                     return schema.functionLookup.getObject(name.name);
 
                 case RoutineSchema.SPECIFIC_ROUTINE :
                     return schema.specificRoutineLookup.getObject(name.name);
 
                 case RoutineSchema.ROUTINE :
                     SchemaObject object =
                         schema.functionLookup.getObject(name.name);
 
                     if (object == null) {
                         object = schema.procedureLookup.getObject(name.name);
                     }
 
                     return object;
 
                 case SchemaObject.DOMAIN :
                 case SchemaObject.TYPE :
                     return schema.typeLookup.getObject(name.name);
 
                 case SchemaObject.TRIGGER : {
                     name = schema.triggerLookup.getName(name.name);
 
                     if (name == null) {
                         return null;
                     }
 
                     HsqlName tableName = name.parent;
                     Table table = (Table) schema.tableList.get(tableName.name);
 
                     return table.getTrigger(name.name);
                 }
                 case SchemaObject.CONSTRAINT : {
                     name = schema.constraintLookup.getName(name.name);
 
                     if (name == null) {
                         return null;
                     }
 
                     HsqlName tableName = name.parent;
                     Table table = (Table) schema.tableList.get(tableName.name);
 
                     return table.getConstraint(name.name);
                 }
                 case SchemaObject.ASSERTION :
                     return null;
 
                 case SchemaObject.INDEX :
                     name = schema.indexLookup.getName(name.name);
 
                     if (name == null) {
                         return null;
                     }
 
                     HsqlName tableName = name.parent;
                     Table table = (Table) schema.tableList.get(tableName.name);
 
                     return table.getIndex(name.name);
             }
 
             return null;
         } finally {
             readLock.unlock();
         }
     }
 
     public void checkColumnIsReferenced(HsqlName tableName, HsqlName name) {
 
         OrderedHashSet set = getReferencesTo(tableName, name);
 
         if (!set.isEmpty()) {
             HsqlName objectName = (HsqlName) set.get(0);
 
             throw Error.error(ErrorCode.X_42502,
                               objectName.getSchemaQualifiedStatementName());
         }
     }
 
     public void checkObjectIsReferenced(HsqlName name) {
 
         OrderedHashSet set     = getReferencesTo(name);
         HsqlName       refName = null;
 
         for (int i = 0; i < set.size(); i++) {
             refName = (HsqlName) set.get(i);
 
             // except columns of same table
             if (refName.parent != name) {
                 break;
             }
 
             refName = null;
         }
 
         if (refName == null) {
             return;
         }
 
         int errorCode = ErrorCode.X_42502;
 
         if (refName.type == SchemaObject.ConstraintTypes.FOREIGN_KEY) {
             errorCode = ErrorCode.X_42533;
         }
 
         throw Error.error(errorCode,
                           refName.getSchemaQualifiedStatementName());
     }
 
     public void addSchemaObject(SchemaObject object) {
 
         writeLock.lock();
 
         try {
             HsqlName        name   = object.getName();
             Schema          schema = (Schema) schemaMap.get(name.schema.name);
             SchemaObjectSet set    = getSchemaObjectSet(schema, name.type);
 
             switch (name.type) {
 
                 case SchemaObject.PROCEDURE :
                 case SchemaObject.FUNCTION : {
                     RoutineSchema routine =
                         (RoutineSchema) set.getObject(name.name);
 
                     if (routine == null) {
                         routine = new RoutineSchema(name.type, name);
 
                         routine.addSpecificRoutine(database, (Routine) object);
                         set.checkAdd(name);
 
                         SchemaObjectSet specificSet =
                             getSchemaObjectSet(schema,
                                                SchemaObject.SPECIFIC_ROUTINE);
 
                         specificSet.checkAdd(
                             ((Routine) object).getSpecificName());
                         set.add(routine);
                         specificSet.add(object);
                     } else {
                         SchemaObjectSet specificSet =
                             getSchemaObjectSet(schema,
                                                SchemaObject.SPECIFIC_ROUTINE);
                         HsqlName specificName =
                             ((Routine) object).getSpecificName();
 
                         if (specificName != null) {
                             specificSet.checkAdd(specificName);
                         }
 
                         routine.addSpecificRoutine(database, (Routine) object);
                         specificSet.add(object);
                     }
 
                     addReferencesFrom(object);
 
                     return;
                 }
                 case SchemaObject.TABLE : {
                     OrderedHashSet refs =
                         ((Table) object).getReferencesForDependents();
 
                     for (int i = 0; i < refs.size(); i++) {
                         HsqlName ref = (HsqlName) refs.get(i);
 
                         switch (ref.type) {
 
                             case SchemaObject.COLUMN : {
                                 int index =
                                     ((Table) object).findColumn(ref.name);
                                 ColumnSchema column =
                                     ((Table) object).getColumn(index);
 
                                 addSchemaObject(column);
 
                                 break;
                             }
                         }
                     }
 
                     break;
                 }
                 case SchemaObject.COLUMN : {
                     if (object.getReferences().isEmpty()) {
                         return;
                     }
 
                     break;
                 }
             }
 
             if (set != null) {
                 set.add(object);
             }
 
             addReferencesFrom(object);
         } finally {
             writeLock.unlock();
         }
     }
 
     public void removeSchemaObject(HsqlName name, boolean cascade) {
 
         writeLock.lock();
 
         try {
             OrderedHashSet objectSet = new OrderedHashSet();
 
             switch (name.type) {
 
                 case SchemaObject.ROUTINE :
                 case SchemaObject.PROCEDURE :
                 case SchemaObject.FUNCTION : {
                     RoutineSchema routine =
                         (RoutineSchema) getSchemaObject(name);
 
                     if (routine != null) {
                         Routine[] specifics = routine.getSpecificRoutines();
 
                         for (int i = 0; i < specifics.length; i++) {
                             getCascadingReferencesTo(
                                 specifics[i].getSpecificName(), objectSet);
                         }
                     }
                 }
                 break;
 
                 case SchemaObject.SEQUENCE :
                 case SchemaObject.TABLE :
                 case SchemaObject.VIEW :
                 case SchemaObject.TYPE :
                 case SchemaObject.CHARSET :
                 case SchemaObject.COLLATION :
                 case SchemaObject.SPECIFIC_ROUTINE :
                     getCascadingReferencesTo(name, objectSet);
                     break;
 
                 case SchemaObject.DOMAIN :
                     OrderedHashSet set = getReferencesTo(name);
                     Iterator       it  = set.iterator();
 
                     while (it.hasNext()) {
                         HsqlName ref = (HsqlName) it.next();
 
                         if (ref.type == SchemaObject.COLUMN) {
                             it.remove();
                         }
                     }
 
                     if (!set.isEmpty()) {
                         HsqlName objectName = (HsqlName) set.get(0);
 
                         throw Error.error(
                             ErrorCode.X_42502,
                             objectName.getSchemaQualifiedStatementName());
                     }
                     break;
             }
 
             if (objectSet.isEmpty()) {
                 removeSchemaObject(name);
 
                 return;
             }
 
             if (!cascade) {
                 HsqlName objectName = (HsqlName) objectSet.get(0);
 
                 throw Error.error(
                     ErrorCode.X_42502,
                     objectName.getSchemaQualifiedStatementName());
             }
 
             objectSet.add(name);
             removeSchemaObjects(objectSet);
         } finally {
             writeLock.unlock();
         }
     }
 
     public void removeSchemaObjects(OrderedHashSet set) {
 
         writeLock.lock();
 
         try {
             for (int i = 0; i < set.size(); i++) {
                 HsqlName name = (HsqlName) set.get(i);
 
                 removeSchemaObject(name);
             }
         } finally {
             writeLock.unlock();
         }
     }
 
     public void removeSchemaObject(HsqlName name) {
 
         writeLock.lock();
 
         try {
             Schema          schema = (Schema) schemaMap.get(name.schema.name);
             SchemaObject    object = null;
             SchemaObjectSet set    = null;
 
             switch (name.type) {
 
                 case SchemaObject.SEQUENCE :
                     set    = schema.sequenceLookup;
                     object = set.getObject(name.name);
                     break;
 
                 case SchemaObject.TABLE :
                 case SchemaObject.VIEW : {
                     set    = schema.tableLookup;
                     object = set.getObject(name.name);
 
                     break;
                 }
                 case SchemaObject.COLUMN : {
                     Table table = (Table) getSchemaObject(name.parent);
 
                     if (table != null) {
                         object =
                             table.getColumn(table.getColumnIndex(name.name));
                     }
 
                     break;
                 }
                 case SchemaObject.CHARSET :
                     set    = schema.charsetLookup;
                     object = set.getObject(name.name);
                     break;
 
                 case SchemaObject.COLLATION :
                     set    = schema.collationLookup;
                     object = set.getObject(name.name);
                     break;
 
                 case SchemaObject.PROCEDURE : {
                     set = schema.procedureLookup;
 
                     RoutineSchema routine =
                         (RoutineSchema) set.getObject(name.name);
 
                     object = routine;
 
                     Routine[] specifics = routine.getSpecificRoutines();
 
                     for (int i = 0; i < specifics.length; i++) {
                         removeSchemaObject(specifics[i].getSpecificName());
                     }
 
                     break;
                 }
                 case SchemaObject.FUNCTION : {
                     set = schema.functionLookup;
 
                     RoutineSchema routine =
                         (RoutineSchema) set.getObject(name.name);
 
                     object = routine;
 
                     Routine[] specifics = routine.getSpecificRoutines();
 
                     for (int i = 0; i < specifics.length; i++) {
                         removeSchemaObject(specifics[i].getSpecificName());
                     }
 
                     break;
                 }
                 case SchemaObject.SPECIFIC_ROUTINE : {
                     set = schema.specificRoutineLookup;
 
                     Routine routine = (Routine) set.getObject(name.name);
 
                     object = routine;
 
                     routine.routineSchema.removeSpecificRoutine(routine);
 
                     if (routine.routineSchema.getSpecificRoutines().length
                             == 0) {
                         removeSchemaObject(routine.getName());
                     }
 
                     break;
                 }
                 case SchemaObject.DOMAIN :
                 case SchemaObject.TYPE :
                     set    = schema.typeLookup;
                     object = set.getObject(name.name);
                     break;
 
                 case SchemaObject.INDEX :
                     set = schema.indexLookup;
                     break;
 
                 case SchemaObject.CONSTRAINT : {
                     set = schema.constraintLookup;
 
                     if (name.parent.type == SchemaObject.TABLE) {
                         Table table =
                             (Table) schema.tableList.get(name.parent.name);
 
                         object = table.getConstraint(name.name);
 
                         table.removeConstraint(name.name);
                     } else if (name.parent.type == SchemaObject.DOMAIN) {
                         Type type = (Type) schema.typeLookup.getObject(
                             name.parent.name);
 
                         object =
                             type.userTypeModifier.getConstraint(name.name);
 
                         type.userTypeModifier.removeConstraint(name.name);
                     }
 
                     break;
                 }
                 case SchemaObject.TRIGGER : {
                     set = schema.triggerLookup;
 
                     Table table =
                         (Table) schema.tableList.get(name.parent.name);
 
                     object = table.getTrigger(name.name);
 
                     if (object != null) {
                         table.removeTrigger((TriggerDef) object);
                     }
 
                     break;
                 }
                 default :
                     throw Error.runtimeError(ErrorCode.U_S0500,
                                              "SchemaManager");
             }
 
             if (object != null) {
                 database.getGranteeManager().removeDbObject(name);
                 removeReferencesFrom(object);
             }
 
             if (set != null) {
                 set.remove(name.name);
             }
 
             removeReferencesTo(name);
         } finally {
             writeLock.unlock();
         }
     }
 
     public void renameSchemaObject(HsqlName name, HsqlName newName) {
 
         writeLock.lock();
 
         try {
             if (name.schema != newName.schema) {
                 throw Error.error(ErrorCode.X_42505, newName.schema.name);
             }
 
             checkObjectIsReferenced(name);
 
             Schema          schema = (Schema) schemaMap.get(name.schema.name);
             SchemaObjectSet set    = getSchemaObjectSet(schema, name.type);
 
             set.rename(name, newName);
         } finally {
             writeLock.unlock();
         }
     }
 
     public void replaceReferences(SchemaObject oldObject,
                                   SchemaObject newObject) {
 
         writeLock.lock();
 
         try {
             removeReferencesFrom(oldObject);
             addReferencesFrom(newObject);
         } finally {
             writeLock.unlock();
         }
     }
 
     public String[] getSQLArray() {
 
         readLock.lock();
 
         try {
             OrderedHashSet resolved   = new OrderedHashSet();
             OrderedHashSet unresolved = new OrderedHashSet();
             HsqlArrayList  list       = new HsqlArrayList();
             Iterator       schemas    = schemaMap.values().iterator();
 
             schemas = schemaMap.values().iterator();
 
             while (schemas.hasNext()) {
                 Schema schema = (Schema) schemas.next();
 
                 if (SqlInvariants.isSystemSchemaName(schema.getName().name)) {
                     continue;
                 }
 
                 if (SqlInvariants.isLobsSchemaName(schema.getName().name)) {
                     continue;
                 }
 
                 list.add(schema.getSQL());
                 schema.addSimpleObjects(unresolved);
             }
 
             while (true) {
                 Iterator it = unresolved.iterator();
 
                 if (!it.hasNext()) {
                     break;
                 }
 
                 OrderedHashSet newResolved = new OrderedHashSet();
 
                 SchemaObjectSet.addAllSQL(resolved, unresolved, list, it,
                                           newResolved);
                 unresolved.removeAll(newResolved);
 
                 if (newResolved.size() == 0) {
                     break;
                 }
             }
 
             schemas = schemaMap.values().iterator();
 
             while (schemas.hasNext()) {
                 Schema schema = (Schema) schemas.next();
 
                 if (SqlInvariants.isLobsSchemaName(schema.getName().name)) {
                     continue;
                 }
 
                 if (SqlInvariants.isSystemSchemaName(schema.getName().name)) {
                     continue;
                 }
 
                 list.addAll(schema.getSQLArray(resolved, unresolved));
             }
 
             while (true) {
                 Iterator it = unresolved.iterator();
 
                 if (!it.hasNext()) {
                     break;
                 }
 
                 OrderedHashSet newResolved = new OrderedHashSet();
 
                 SchemaObjectSet.addAllSQL(resolved, unresolved, list, it,
                                           newResolved);
                 unresolved.removeAll(newResolved);
 
                 if (newResolved.size() == 0) {
                     break;
                 }
             }
 
             Iterator it = unresolved.iterator();
 
             while (it.hasNext()) {
                 SchemaObject object = (SchemaObject) it.next();
 
                 if (object instanceof Routine) {
                     list.add(((Routine) object).getSQLDeclaration());
                 }
             }
 
             it = unresolved.iterator();
 
             while (it.hasNext()) {
                 SchemaObject object = (SchemaObject) it.next();
 
                 if (object instanceof Routine) {
                     list.add(((Routine) object).getSQLAlter());
                 } else {
                     list.add(object.getSQL());
                 }
             }
 
             schemas = schemaMap.values().iterator();
 
             while (schemas.hasNext()) {
                 Schema schema = (Schema) schemas.next();
 
                 if (SqlInvariants.isLobsSchemaName(schema.getName().name)) {
                     continue;
                 }
 
                 if (SqlInvariants.isSystemSchemaName(schema.getName().name)) {
                     continue;
                 }
 
                 String[] t = schema.getTriggerSQL();
 
                 if (t.length > 0) {
                     list.add(Schema.getSetSchemaSQL(schema.getName()));
                     list.addAll(t);
                 }
             }
 
             schemas = schemaMap.values().iterator();
 
             while (schemas.hasNext()) {
                 Schema schema = (Schema) schemas.next();
 
                 list.addAll(schema.getSequenceRestartSQL());
             }
 
             if (defaultSchemaHsqlName != null) {
                 StringBuffer sb = new StringBuffer();
 
                 sb.append(Tokens.T_SET).append(' ').append(Tokens.T_DATABASE);
                 sb.append(' ').append(Tokens.T_DEFAULT).append(' ');
                 sb.append(Tokens.T_INITIAL).append(' ').append(
                     Tokens.T_SCHEMA);
                 sb.append(' ').append(defaultSchemaHsqlName.statementName);
                 list.add(sb.toString());
             }
 
             String[] array = new String[list.size()];
 
             list.toArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     public String[] getTablePropsSQL(boolean withHeader) {
 
         readLock.lock();
 
         try {
             HsqlArrayList tableList = getAllTables(false);
             HsqlArrayList list      = new HsqlArrayList();
 
             for (int i = 0; i < tableList.size(); i++) {
                 Table t = (Table) tableList.get(i);
 
                 if (t.isText()) {
                     String[] ddl = t.getSQLForTextSource(withHeader);
 
                     list.addAll(ddl);
                 }
 
                 String ddl = t.getSQLForReadOnly();
 
                 if (ddl != null) {
                     list.add(ddl);
                 }
 
                 if (t.isCached()) {
                     ddl = t.getSQLForClustered();
 
                     if (ddl != null) {
                         list.add(ddl);
                     }
                 }
             }
 
             String[] array = new String[list.size()];
 
             list.toArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     public String[] getIndexRootsSQL() {
 
         readLock.lock();
 
         try {
             Session       sysSession = database.sessionManager.getSysSession();
             int[][]       rootsArray = getIndexRoots(sysSession);
             HsqlArrayList tableList  = getAllTables(true);
             HsqlArrayList list       = new HsqlArrayList();
 
             for (int i = 0; i < rootsArray.length; i++) {
                 Table t = (Table) tableList.get(i);
 
                 if (rootsArray[i] != null && rootsArray[i].length > 0
                         && rootsArray[i][0] != -1) {
                     String ddl = ((Table) tableList.get(i)).getIndexRootsSQL(
                         rootsArray[i]);
 
                     list.add(ddl);
                 }
             }
 
             String[] array = new String[list.size()];
 
             list.toArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     public String[] getCommentsArray() {
 
         readLock.lock();
 
         try {
             HsqlArrayList tableList = getAllTables(false);
             HsqlArrayList list      = new HsqlArrayList();
             StringBuffer  sb        = new StringBuffer();
 
             for (int i = 0; i < tableList.size(); i++) {
                 Table table = (Table) tableList.get(i);
 
                 if (table.getTableType() == Table.INFO_SCHEMA_TABLE) {
                     continue;
                 }
 
                 int colCount = table.getColumnCount();
 
                 for (int j = 0; j < colCount; j++) {
                     ColumnSchema column = table.getColumn(j);
 
                     if (column.getName().comment == null) {
                         continue;
                     }
 
                     sb.setLength(0);
                     sb.append(Tokens.T_COMMENT).append(' ').append(
                         Tokens.T_ON);
                     sb.append(' ').append(Tokens.T_COLUMN).append(' ');
                     sb.append(
                         table.getName().getSchemaQualifiedStatementName());
                     sb.append('.').append(column.getName().statementName);
                     sb.append(' ').append(Tokens.T_IS).append(' ');
                     sb.append(
                         StringConverter.toQuotedString(
                             column.getName().comment, '\'', true));
                     list.add(sb.toString());
                 }
 
                 if (table.getName().comment == null) {
                     continue;
                 }
 
                 sb.setLength(0);
                 sb.append(Tokens.T_COMMENT).append(' ').append(Tokens.T_ON);
                 sb.append(' ').append(Tokens.T_TABLE).append(' ');
                 sb.append(table.getName().getSchemaQualifiedStatementName());
                 sb.append(' ').append(Tokens.T_IS).append(' ');
                 sb.append(
                     StringConverter.toQuotedString(
                         table.getName().comment, '\'', true));
                 list.add(sb.toString());
             }
 
             Iterator it = databaseObjectIterator(SchemaObject.ROUTINE);
 
             while (it.hasNext()) {
                 SchemaObject object = (SchemaObject) it.next();
 
                 if (object.getName().comment == null) {
                     continue;
                 }
 
                 sb.setLength(0);
                 sb.append(Tokens.T_COMMENT).append(' ').append(Tokens.T_ON);
                 sb.append(' ').append(Tokens.T_ROUTINE).append(' ');
                 sb.append(object.getName().getSchemaQualifiedStatementName());
                 sb.append(' ').append(Tokens.T_IS).append(' ');
                 sb.append(
                     StringConverter.toQuotedString(
                         object.getName().comment, '\'', true));
                 list.add(sb.toString());
             }
 
             String[] array = new String[list.size()];
 
             list.toArray(array);
 
             return array;
         } finally {
             readLock.unlock();
         }
     }
 
     int[][] tempIndexRoots;
 
     public void setTempIndexRoots(int[][] roots) {
         tempIndexRoots = roots;
     }
 
     
 
     public void setDefaultTableType(int type) {
         defaultTableType = type;
     }
 
     public int getDefaultTableType() {
         return defaultTableType;
     }
 
     public void createSystemTables() {
 
         dualTable =
             TableUtil.newLookupTable(database,
                                      SqlInvariants.DUAL_TABLE_HSQLNAME,
                                      TableBase.SYSTEM_TABLE,
                                      SqlInvariants.DUAL_COLUMN_HSQLNAME,
                                      Type.SQL_VARCHAR);
 
         dualTable.insertSys(database.sessionManager.getSysSession(),
                             dualTable.getRowStore(null), new Object[]{ "X" });
         dualTable.setDataReadOnly(true);
     }
 }