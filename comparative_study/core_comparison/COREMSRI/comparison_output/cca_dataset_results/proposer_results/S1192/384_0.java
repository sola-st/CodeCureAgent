```java
package org.sqlite;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlite.SQLiteConfig.TransactionMode;

/**
 * These tests assume that Statements and PreparedStatements are working as per
 * normal and test the interactions of commit(), rollback() and
 * setAutoCommit(boolean) with multiple connections to the same db.
 */
public class TransactionTest
{
    private static final String CREATE_TABLE_TEST = "create table test (c1);";
    private static final String INSERT_INTO_TEST_VALUES_1 = "insert into test values (1);";
    private static final String INSERT_INTO_TEST_VALUES_2 = "insert into test values (2);";
    private static final String INSERT_INTO_TEST_VALUES_3 = "insert into test values (3);";
    private static final String SELECT_C1_FROM_TEST = "select c1 from test";
    private static final String SELECT_SUM_C1_FROM_TEST = "select sum(c1) from test;";
    private static final String SELECT_ALL_FROM_T = "select * from t;";
    private static final String CREATE_TABLE_TRANS = "create table trans (c1);";
    private static final String INSERT_INTO_TRANS_VALUES_3 = "insert into trans values (3);";
    private static final String INSERT_INTO_TRANS_VALUES_4 = "insert into trans values (4);";
    private static final String SELECT_COUNT_FROM_TRANS = "select count(*) from trans;";
    private static final String SELECT_ALL_FROM_TRANS = "select * from trans;";
    private static final String CREATE_TABLE_T = "create table t (c1);";
    private static final String INSERT_INTO_T_VALUES_1 = "insert into t values (1);";
    private static final String INSERT_INTO_T_VALUES_2 = "insert into t values (2);";
    private static final String INSERT_INTO_T_VALUES_3 = "insert into t values (3);";
    private static final String INSERT_INTO_T_VALUES_4 = "insert into t values (4);";
    private static final String INSERT_INTO_T_VALUES_5 = "insert into t values (5);";
    private static final String INSERT_INTO_T_VALUES_7 = "insert into t values (7);";
    private static final String SELECT_SUM_C1_FROM_T = "select sum(c1) from t;";
    private static final String SELECT_ALL_FROM_T_NO_SEMICOLON = "select * from t";

    private Connection conn1, conn2, conn3;
    private Statement  stat1, stat2, stat3;

    boolean            done = false;

    @BeforeClass
    public static void forName() throws Exception {
        System.out.println("running in " + (SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java") + " mode");
    }

    @Before
    public void connect() throws Exception {
        File tmpFile = File.createTempFile("test-trans", ".db");
	// tmpFile.deleteOnExit();

        Properties prop = new Properties();
        prop.setProperty("shared_cache", "false");

        conn1 = DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath(), prop);
        conn2 = DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath(), prop);
        conn3 = DriverManager.getConnection("jdbc:sqlite:" + tmpFile.getAbsolutePath(), prop);

        stat1 = conn1.createStatement();
        stat2 = conn2.createStatement();
        stat3 = conn3.createStatement();

        //        if (SQLiteJDBCLoader.isPureJavaMode()) {
        //            stat1.setQueryTimeout(3);
        //            stat2.setQueryTimeout(3);
        //            stat3.setQueryTimeout(3);
        //        }
    }

    @After
    public void close() throws Exception {
        stat1.close();
        stat2.close();
        stat3.close();
        conn1.close();
        conn2.close();
        conn3.close();
    }

    private void failedUpdatedPreventedFutureRollback(boolean prepared) throws SQLException {
        stat1.execute(CREATE_TABLE_TEST);
        stat1.execute(INSERT_INTO_TEST_VALUES_1);

        // First transaction starts
        conn1.setAutoCommit(false);
        stat1.execute(INSERT_INTO_TEST_VALUES_2);

        final PreparedStatement pstat2 = prepared ? conn2.prepareStatement(INSERT_INTO_TEST_VALUES_3) : null;

        // Second transaction starts and tries to complete but fails because first is still running
        boolean gotException = false;
        try {
            ((SQLiteConnection) conn2).setBusyTimeout(10);
            conn2.setAutoCommit(false);
            if (pstat2 != null) {
                // The prepared case would fail regardless of whether this was "execute" or "executeUpdate"
                pstat2.execute();
            } else {
                // If you changed this to "executeUpdate" instead of "execute", the test would pass
                stat2.execute(INSERT_INTO_TEST_VALUES_3);
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("is locked")) {
                gotException = true;
            } else {
                throw e;
            }
        }
        assertTrue(gotException);
        conn2.rollback();
        // The test would fail here: the trivial "transaction" created in between the rollback we just
        // did and this point would fail to commit because "SQL statements in progress"
        conn2.setAutoCommit(true);

        // First transaction completes
        conn1.setAutoCommit(true);

        // Second transaction retries
        conn2.setAutoCommit(false);
        if (pstat2 != null) {
            pstat2.execute();
        } else {
            stat2.execute(INSERT_INTO_TEST_VALUES_3);
        };
        conn2.setAutoCommit(true);

        final ResultSet rs = stat1.executeQuery(SELECT_C1_FROM_TEST);
        final Set<Integer> seen = new HashSet<Integer>();
        while (rs.next()) {
            assertTrue(seen.add(rs.getInt(1)));
        }

        assertEquals(new HashSet<Integer>(Arrays.asList(1, 2, 3)), seen);
    }

    @Test
    public void failedUpdatePreventedFutureRollbackUnprepared() throws SQLException {
        failedUpdatedPreventedFutureRollback(false);
    }

    @Test
    public void failedUpdatePreventedFutureRollbackPrepared() throws SQLException {
        failedUpdatedPreventedFutureRollback(true);
    }

    @Test
    public void multiConn() throws SQLException {
        stat1.executeUpdate(CREATE_TABLE_TEST);
        stat1.executeUpdate(INSERT_INTO_TEST_VALUES_1);
        stat2.executeUpdate(INSERT_INTO_TEST_VALUES_2);
        stat3.executeUpdate(INSERT_INTO_TEST_VALUES_3);

        ResultSet rs = stat1.executeQuery(SELECT_SUM_C1_FROM_TEST);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 6);
        rs.close();

        rs = stat3.executeQuery(SELECT_SUM_C1_FROM_TEST);
        assertTrue(rs.next());
        assertEquals(rs.getInt(1), 6);
        rs.close();
    }

    @Test
    public void locking() throws SQLException {
        stat1.executeUpdate(CREATE_TABLE_TEST);
        stat1.executeUpdate("begin immediate;");
        stat2.executeUpdate(SELECT_ALL_FROM_T);
    }

    @Test
    public void insert() throws SQLException {
        ResultSet rs;
        String countSql = SELECT_COUNT_FROM_TRANS;

        stat1.executeUpdate(CREATE_TABLE_TRANS);
        conn1.setAutoCommit(false);

        assertEquals(1, stat1.executeUpdate(INSERT_INTO_TRANS_VALUES_4));

        // transaction not yet commited, conn1 can see, conn2 can not
        rs = stat1.executeQuery(countSql);
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        rs.close();
        rs = stat2.executeQuery(countSql);
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
        rs.close();

        conn1.commit();

        // all connects can see data
        rs = stat2.executeQuery(countSql);
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        rs.close();
    }

    @Test
    public void rollback() throws SQLException {
        String select = SELECT_ALL_FROM_TRANS;
        ResultSet rs;

        stat1.executeUpdate(CREATE_TABLE_TRANS);
        conn1.setAutoCommit(false);
        stat1.executeUpdate(INSERT_INTO_TRANS_VALUES_3);

        rs = stat1.executeQuery(select);
        assertTrue(rs.next());
        rs.close();

        conn1.rollback();

        rs = stat1.executeQuery(select);
        assertFalse(rs.next());
        rs.close();
    }

    @Test
    public void multiRollback() throws SQLException {
        ResultSet rs;

        stat1.executeUpdate(CREATE_TABLE_T);
        conn1.setAutoCommit(false);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_1);
        conn1.commit();
        stat1.executeUpdate(INSERT_INTO_T_VALUES_1);
        conn1.rollback();
        stat1.addBatch("insert into t values (2);");
        stat1.addBatch("insert into t values (3);");
        stat1.executeBatch();
        conn1.commit();
        stat1.addBatch(INSERT_INTO_T_VALUES_7);
        stat1.executeBatch();
        conn1.rollback();
        stat1.executeUpdate(INSERT_INTO_T_VALUES_4);
        conn1.setAutoCommit(true);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_5);
        conn1.setAutoCommit(false);
        PreparedStatement p = conn1.prepareStatement("insert into t values (?);");
        p.setInt(1, 6);
        p.executeUpdate();
        p.setInt(1, 7);
        p.executeUpdate();

        // conn1 can see (1+...+7), conn2 can see (1+...+5)
        rs = stat1.executeQuery(SELECT_SUM_C1_FROM_T);
        assertTrue(rs.next());
        assertEquals(1 + 2 + 3 + 4 + 5 + 6 + 7, rs.getInt(1));
        rs.close();
        rs = stat2.executeQuery(SELECT_SUM_C1_FROM_T);
        assertTrue(rs.next());
        assertEquals(1 + 2 + 3 + 4 + 5, rs.getInt(1));
        rs.close();
    }

    @Test
    public void transactionsDontMindReads() throws SQLException {
        stat1.executeUpdate(CREATE_TABLE_T);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_1);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_2);
        ResultSet rs = stat1.executeQuery(SELECT_ALL_FROM_T_NO_SEMICOLON);
        assertTrue(rs.next()); // select is open

        conn2.setAutoCommit(false);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_2);

        rs.close();
        conn2.commit();
    }

    @Test
    public void secondConnWillWait() throws Exception {
        stat1.executeUpdate(CREATE_TABLE_T);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_1);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_2);
        ResultSet rs = stat1.executeQuery(SELECT_ALL_FROM_T_NO_SEMICOLON);
        assertTrue(rs.next());

        final TransactionTest lock = this;
        lock.done = false;
        new Thread() {
            @Override
            public void run() {
                try {
                    stat2.executeUpdate("insert into t values (3);");
                }
                catch (SQLException e) {
                    e.printStackTrace();
                    return;
                }

                synchronized (lock) {
                    lock.done = true;
                    lock.notify();
                }
            }
        }.start();

        Thread.sleep(100);
        rs.close();

        synchronized (lock) {
            if(!lock.done) {
                lock.wait(5000);
                if (!lock.done)
                    throw new Exception("should be done");
            }
        }
    }

    @Test(expected = SQLException.class)
    public void secondConnMustTimeout() throws SQLException {
        stat1.setQueryTimeout(1);
        stat1.executeUpdate(CREATE_TABLE_T);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_1);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_2);
        ResultSet rs = stat1.executeQuery(SELECT_ALL_FROM_T_NO_SEMICOLON);
        assertTrue(rs.next());

        ((SQLiteConnection) conn2).setBusyTimeout(10);
        stat2.executeUpdate("insert into t values (3);"); // can't be done
    }

    //    @Test(expected= SQLException.class)
    @Test
    public void cantUpdateWhileReading() throws SQLException {
        stat1.executeUpdate(CREATE_TABLE_T);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_1);
        stat1.executeUpdate(INSERT_INTO_T_VALUES_2);
        ResultSet rs = conn1.createStatement().executeQuery(SELECT_ALL_FROM_T_NO_SEMICOLON);
        assertTrue(rs.next());

        // commit now succeeds since sqlite 3.6.5
        stat1.executeUpdate(INSERT_INTO_T_VALUES_3); // can't be done
    }

    @Test(expected = SQLException.class)
    public void cantCommit() throws SQLException {
        conn1.commit();
    }

    @Test(expected = SQLException.class)
    public void cantRollback() throws SQLException {
        conn1.rollback();
    }

    @Test
    public void transactionModes() throws Exception {
        File tmpFile = File.createTempFile("test-trans", ".db");

        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tmpFile.getAbsolutePath());

        // deferred
        SQLiteConnection con = (SQLiteConnection)ds.getConnection();
        assertEquals(TransactionMode.DEFERRED, con.getConnectionConfig().getTransactionMode());
        assertEquals("begin;", con.getConnectionConfig().transactionPrefix());
        runUpdates(con, "tbl1");
        
        ds.setTransactionMode(TransactionMode.DEFERRED.name());
        con = (SQLiteConnection)ds.getConnection();
        assertEquals(TransactionMode.DEFERRED, con.getConnectionConfig().getTransactionMode());
        assertEquals("begin;", con.getConnectionConfig().transactionPrefix());

        // Misspelled deferred should be accepted for backwards compatibility
        ds.setTransactionMode("DEFFERED");
        con = (SQLiteConnection)ds.getConnection();
        assertEquals(TransactionMode.DEFERRED, con.getConnectionConfig().getTransactionMode());
        assertEquals("begin;", con.getConnectionConfig().transactionPrefix());

        con = (SQLiteConnection)ds.getConnection();
        con.getConnectionConfig().setTransactionMode(TransactionMode.valueOf("DEFFERED"));
        assertEquals(TransactionMode.DEFERRED, con.getConnectionConfig().getTransactionMode());
        assertEquals("begin;", con.getConnectionConfig().transactionPrefix());

        // immediate
        ds.setTransactionMode(TransactionMode.IMMEDIATE.name());
        con = (SQLiteConnection)ds.getConnection();
        assertEquals(TransactionMode.IMMEDIATE, con.getConnectionConfig().getTransactionMode());
        assertEquals("begin immediate;", con.getConnectionConfig().transactionPrefix());
        runUpdates(con, "tbl2");

        // exclusive
        ds.setTransactionMode(TransactionMode.EXCLUSIVE.name());
        con = (SQLiteConnection)ds.getConnection();
        assertEquals(TransactionMode.EXCLUSIVE, con.getConnectionConfig().getTransactionMode());
        assertEquals("begin exclusive;", con.getConnectionConfig().transactionPrefix());
        runUpdates(con, "tbl3");

        tmpFile.delete();
    }

    public void runUpdates(Connection con, String table) throws SQLException {
        Statement stat = con.createStatement(); 

        con.setAutoCommit(false);
        stat.execute("create table " + table + "(id)");
        stat.executeUpdate("insert into " + table + " values(1)");
        stat.executeUpdate("insert into " + table + " values(2)");
        con.commit();

        ResultSet rs = stat.executeQuery("select * from " + table);
        rs.next();
        assertEquals(1, rs.getInt(1));
        rs.next();
        assertEquals(2, rs.getInt(1));
        rs.close();
        con.close();
    }
}
