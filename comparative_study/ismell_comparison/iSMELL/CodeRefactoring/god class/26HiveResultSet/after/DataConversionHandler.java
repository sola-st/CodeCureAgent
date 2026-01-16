package org.apache.hadoop.hive.jdbc;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;

class DataConversionHandler {
    boolean wasNull = false;

    public Object getObject(ArrayList<?> row, int columnIndex) throws SQLException {
        if (row == null) {
            throw new SQLException("No row found.");
        }

        if (columnIndex > row.size()) {
            throw new SQLException("Invalid columnIndex: " + columnIndex);
        }

        try {
            wasNull = false;
            if (row.get(columnIndex - 1) == null) {
                wasNull = true;
            }

            return row.get(columnIndex - 1);
        } catch (Exception e) {
            throw new SQLException(e.toString());
        }
    }

    public boolean wasNull() {
        return wasNull;
    }

    public Array getArray(int i) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getArray(java.lang.String)
    */
 
   public Array getArray(String colName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getAsciiStream(int)
    */
 
   public InputStream getAsciiStream(int columnIndex) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getAsciiStream(java.lang.String)
    */
 
   public InputStream getAsciiStream(String columnName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBigDecimal(int)
    */
 
   public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBigDecimal(java.lang.String)
    */
 
   public BigDecimal getBigDecimal(String columnName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBigDecimal(int, int)
    */
 
   public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBigDecimal(java.lang.String, int)
    */
 
   public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBinaryStream(int)
    */
 
   public InputStream getBinaryStream(int columnIndex) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBinaryStream(java.lang.String)
    */
 
   public InputStream getBinaryStream(String columnName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBlob(int)
    */
 
   public Blob getBlob(int i) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBlob(java.lang.String)
    */
 
   public Blob getBlob(String colName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBoolean(int)
    */
 
   public boolean getBoolean(int columnIndex) throws SQLException {
     Object obj = getObject(columnIndex);
     if (Number.class.isInstance(obj)) {
       return ((Number) obj).intValue() != 0;
     }
     throw new SQLException("Cannot convert column " + columnIndex
         + " to boolean");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBoolean(java.lang.String)
    */
 
   public boolean getBoolean(String columnName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getByte(int)
    */
 
   public byte getByte(int columnIndex) throws SQLException {
     Object obj = getObject(columnIndex);
     if (Number.class.isInstance(obj)) {
       return ((Number) obj).byteValue();
     }
     throw new SQLException("Cannot convert column " + columnIndex + " to byte");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getByte(java.lang.String)
    */
 
   public byte getByte(String columnName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBytes(int)
    */
 
   public byte[] getBytes(int columnIndex) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
 
   /*
    * (non-Javadoc)
    * 
    * @see java.sql.ResultSet#getBytes(java.lang.String)
    */
 
   public byte[] getBytes(String columnName) throws SQLException {
     // TODO Auto-generated method stub
     throw new SQLException("Method not supported");
   }
}