package org.hsqldb;

import org.hsqldb.HsqlNameManager.HsqlName;
import org.hsqldb.ParserDQL.CompileContext;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.ArrayUtil;
import org.hsqldb.lib.HashSet;
import org.hsqldb.lib.OrderedHashSet;
import org.hsqldb.navigator.RangeIterator;
import org.hsqldb.navigator.RowIterator;
import org.hsqldb.navigator.RowSetNavigator;
import org.hsqldb.navigator.RowSetNavigatorClient;
import org.hsqldb.navigator.RowSetNavigatorDataChange;
import org.hsqldb.persist.PersistentStore;
import org.hsqldb.result.Result;
import org.hsqldb.result.ResultConstants;
import org.hsqldb.types.Type;

public class ConstraintHandler {
    private Constraint constraint;
    private Session session;
    private HashSet path;
    private RowSetNavigatorDataChange navigator;

    public ConstraintHandler(Constraint constraint, Session session, HashSet path, RowSetNavigatorDataChange navigator) {
        this.constraint = constraint;
        this.session = session;
        this.path = path;
        this.navigator = navigator;
    }

    public boolean isDeleteAction() {
        return constraint.core.deleteAction == SchemaObject.ReferentialAction.CASCADE;
    }

    public boolean isSetNullAction() {
        return constraint.core.updateAction == SchemaObject.ReferentialAction.SET_NULL;
    }

    public boolean isSetDefaultAction() {
        return constraint.core.updateAction == SchemaObject.ReferentialAction.SET_DEFAULT;
    }

    public boolean isNoActionOrRestrict() {
        return constraint.core.updateAction == SchemaObject.ReferentialAction.NO_ACTION ||
                constraint.core.updateAction == SchemaObject.ReferentialAction.RESTRICT;
    }

    public void performReferentialActions(Row row, Object[] data, int[] changedCols) {
        Constraint c      = table.fkMainConstraints[i];
        int        action = delete ? c.core.deleteAction
                                   : c.core.updateAction;

        if (!delete) {
            if (!ArrayUtil.haveCommonElement(changedCols,
                                             c.core.mainCols)) {
                continue;
            }

            if (c.core.mainIndex.compareRowNonUnique(
                    session, row.getData(), data, c.core.mainCols) == 0) {
                continue;
            }
        }

        RowIterator refiterator = c.findFkRef(session, row.getData());

        if (!refiterator.hasNext()) {
            refiterator.release();

            continue;
        }

        while (refiterator.hasNext()) {
            Row      refRow  = refiterator.getNextRow();
            Object[] refData = null;

            /** @todo use MATCH */
            if (c.core.refIndex.compareRowNonUnique(
                    session, refRow.getData(), row.getData(),
                    c.core.mainCols) != 0) {
                break;
            }

            if (delete && refRow.getId() == row.getId()) {
                continue;
            }

            switch (action) {

                case SchemaObject.ReferentialAction.CASCADE : {
                    if (delete) {
                        if (navigator.addRow(refRow)) {
                            performReferentialActions(session,
                                                      c.core.refTable,
                                                      navigator, refRow,
                                                      null, null, path);
                        }

                        continue;
                    }

                    refData = c.core.refTable.getEmptyRowData();

                    System.arraycopy(refRow.getData(), 0, refData, 0,
                                     refData.length);

                    for (int j = 0; j < c.core.refCols.length; j++) {
                        refData[c.core.refCols[j]] =
                            data[c.core.mainCols[j]];
                    }

                    break;
                }
                case SchemaObject.ReferentialAction.SET_NULL : {
                    refData = c.core.refTable.getEmptyRowData();

                    System.arraycopy(refRow.getData(), 0, refData, 0,
                                     refData.length);

                    for (int j = 0; j < c.core.refCols.length; j++) {
                        refData[c.core.refCols[j]] = null;
                    }

                    break;
                }
                case SchemaObject.ReferentialAction.SET_DEFAULT : {
                    refData = c.core.refTable.getEmptyRowData();

                    System.arraycopy(refRow.getData(), 0, refData, 0,
                                     refData.length);

                    for (int j = 0; j < c.core.refCols.length; j++) {
                        ColumnSchema col =
                            c.core.refTable.getColumn(c.core.refCols[j]);

                        refData[c.core.refCols[j]] =
                            col.getDefaultValue(session);
                    }

                    break;
                }
                case SchemaObject.ReferentialAction.NO_ACTION :
                case SchemaObject.ReferentialAction.RESTRICT : {
                    if (navigator.containsDeletedRow(refRow)) {
                        continue;
                    }

                    int errorCode = c.core.deleteAction
                                    == SchemaObject.ReferentialAction
                                        .NO_ACTION ? ErrorCode.X_23504
                                                   : ErrorCode.X_23001;
                    String[] info = new String[] {
                        c.core.refName.name, c.core.refTable.getName().name
                    };

                    refiterator.release();

                    throw Error.error(null, errorCode,
                                      ErrorCode.CONSTRAINT, info);
                }
                default :
                    continue;
            }

            refData = navigator.addRow(session, refRow, refData,
                                       table.getColumnTypes(),
                                       c.core.refCols);

            if (!path.add(c)) {
                continue;
            }

            performReferentialActions(session, c.core.refTable, navigator,
                                      refRow, refData, c.core.refCols,
                                      path);
            path.remove(c);
        }

        refiterator.release();
    }
}