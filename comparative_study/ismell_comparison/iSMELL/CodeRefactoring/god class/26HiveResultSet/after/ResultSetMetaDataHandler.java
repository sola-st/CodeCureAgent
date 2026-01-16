package org.apache.hadoop.hive.jdbc;

import java.sql.ResultSetMetaData;
import java.util.List;

class ResultSetMetaDataHandler {
    List<String> columnNames;
    List<String> columnTypes;

    public ResultSetMetaDataHandler(List<String> columnNames, List<String> columnTypes) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
    }

    public ResultSetMetaData getMetaData() {
        return new HiveResultSetMetaData(columnNames, columnTypes);
    }
}