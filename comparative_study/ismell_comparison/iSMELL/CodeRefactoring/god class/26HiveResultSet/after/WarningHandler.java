package org.apache.hadoop.hive.jdbc;

import java.sql.SQLWarning;

class WarningHandler {
    SQLWarning warningChain = null;

    public void clearWarnings() {
        warningChain = null;
    }

    public SQLWarning getWarnings() {
        return warningChain;
    }
}