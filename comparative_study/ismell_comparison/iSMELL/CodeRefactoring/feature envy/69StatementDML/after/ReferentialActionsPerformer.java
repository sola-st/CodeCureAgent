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

public class ReferentialActionsPerformer {
    private Session session;
    private Table table;
    private RowSetNavigatorDataChange navigator;
    private HashSet path;

    public ReferentialActionsPerformer(Session session, Table table, RowSetNavigatorDataChange navigator, HashSet path) {
        this.session = session;
        this.table = table;
        this.navigator = navigator;
        this.path = path;
    }

    public void performReferentialActions(Row row, Object[] data, int[] changedCols) {
        if (!session.database.isReferentialIntegrity()) {
            return;
        }

        boolean delete = data == null;

        for (int i = 0, size = table.fkMainConstraints.length; i < size; i++) {
            ConstraintHandler handler = new ConstraintHandler(table.fkMainConstraints[i], session, path, navigator);
            handler.performReferentialActions(row, data, changedCols);
        }
    }
}