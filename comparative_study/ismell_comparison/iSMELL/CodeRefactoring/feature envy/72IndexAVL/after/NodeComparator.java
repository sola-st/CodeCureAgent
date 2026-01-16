package org.hsqldb.index;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hsqldb.Constraint;
import org.hsqldb.HsqlNameManager;
import org.hsqldb.HsqlNameManager.HsqlName;
import org.hsqldb.OpTypes;
import org.hsqldb.Row;
import org.hsqldb.RowAVL;
import org.hsqldb.SchemaObject;
import org.hsqldb.Session;
import org.hsqldb.Table;
import org.hsqldb.TableBase;
import org.hsqldb.Tokens;
import org.hsqldb.TransactionManager;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.ArrayUtil;
import org.hsqldb.lib.OrderedHashSet;
import org.hsqldb.navigator.RowIterator;
import org.hsqldb.persist.PersistentStore;
import org.hsqldb.rights.Grantee;
import org.hsqldb.types.Type;

public class NodeComparator {
    private Session session;
    private int fieldCount;
    private int compareType;
    private int[] rowColMap;
    private Object[] rowdata;

    public NodeComparator(Session session, int fieldCount, int compareType, int[] rowColMap, Object[] rowdata) {
        this.session = session;
        this.fieldCount = fieldCount;
        this.compareType = compareType;
        this.rowColMap = rowColMap;
        this.rowdata = rowdata;
    }

    public NodeAVL compare(NodeAVL x, PersistentStore store) {
        NodeAVL n = null;
        NodeAVL result = null;
        Row currentRow = x.getRow(store);

        int i = 0;
        if (fieldCount > 0) {
            i = compareRowNonUnique(session, currentRow.getData(), rowdata, rowColMap, fieldCount);
        }

        if (i == 0) {
            switch (compareType) {
 
                case OpTypes.IS_NULL :
                case OpTypes.EQUAL : {
                    result = x;
                    n      = x.getLeft(store);

                    break;
                }
                case OpTypes.NOT :
                case OpTypes.GREATER : {
                    i = compareObject(session, currentRow.getData(),
                                      rowdata, rowColMap, fieldCount);

                    if (i <= 0) {
                        n = x.getRight(store);
                    } else {
                        result = x;
                        n      = x.getLeft(store);
                    }

                    break;
                }
                case OpTypes.GREATER_EQUAL : {
                    i = compareObject(session, currentRow.getData(),
                                      rowdata, rowColMap, fieldCount);

                    if (i < 0) {
                        n = x.getRight(store);
                    } else {
                        result = x;
                        n      = x.getLeft(store);
                    }

                    break;
                }
                case OpTypes.SMALLER : {
                    i = compareObject(session, currentRow.getData(),
                                      rowdata, rowColMap, fieldCount);

                    if (i < 0) {
                        result = x;
                        n      = x.getRight(store);
                    } else {
                        n = x.getLeft(store);
                    }

                    break;
                }
                case OpTypes.SMALLER_EQUAL : {
                    i = compareObject(session, currentRow.getData(),
                                      rowdata, rowColMap, fieldCount);

                    if (i <= 0) {
                        result = x;
                        n      = x.getRight(store);
                    } else {
                        n = x.getLeft(store);
                    }

                    break;
                }
                default :
                    Error.runtimeError(ErrorCode.U_S0500, "Index");
            }
        } else if (i < 0) {
            n = x.getRight(store);
        } else if (i > 0) {
            n = x.getLeft(store);
        }

        return n;
    }
}