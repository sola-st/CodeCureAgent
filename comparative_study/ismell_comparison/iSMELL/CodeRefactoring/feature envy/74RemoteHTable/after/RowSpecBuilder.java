package org.apache.hadoop.hbase.rest.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.CoprocessorProtocol;
import org.apache.hadoop.util.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.RowLock;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.TimeRange;
import org.apache.hadoop.hbase.rest.Constants;
import org.apache.hadoop.hbase.rest.model.CellModel;
import org.apache.hadoop.hbase.rest.model.CellSetModel;
import org.apache.hadoop.hbase.rest.model.RowModel;
import org.apache.hadoop.hbase.rest.model.ScannerModel;
import org.apache.hadoop.hbase.rest.model.TableSchemaModel;
import org.apache.hadoop.hbase.util.Bytes;

public class RowSpecBuilder {
    private final byte[] row;
    private final Map familyMap;
    private final long startTime;
    private final long endTime;
    private final int maxVersions;
    private final byte[] name;
    private final String accessToken;

    public RowSpecBuilder(byte[] row, Map familyMap, long startTime, long endTime, int maxVersions, byte[] name, String accessToken) {
        this.row = row;
        this.familyMap = familyMap;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxVersions = maxVersions;
        this.name = name;
        this.accessToken = accessToken;
    }

    public String buildRowSpec() {
        StringBuilder sb = new StringBuilder();
        appendAccessToken(sb);
        appendName(sb);
        appendRow(sb);
        appendFamilies(sb);
        appendTime(sb);
        appendMaxVersions(sb);
        return sb.toString();
    }

    private void appendAccessToken(StringBuilder sb) {
        if (accessToken != null) {
            sb.append(accessToken);
            sb.append('/');
        }
    }

    private void appendName(StringBuilder sb) {
        sb.append(Bytes.toStringBinary(name));
        sb.append('/');
    }

    private void appendRow(StringBuilder sb) {
        sb.append(Bytes.toStringBinary(row));
    }

    private void appendFamilies(StringBuilder sb) {
        Set<Map.Entry> families = familyMap.entrySet();
        if (families != null && !families.isEmpty()) {
            sb.append('/');
            Iterator<Map.Entry> i = families.iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry)i.next();
                Collection quals = (Collection)e.getValue();
                if (quals != null && !quals.isEmpty()) {
                    Iterator ii = quals.iterator();
                    while (ii.hasNext()) {
                        sb.append(Bytes.toStringBinary((byte[])e.getKey()));
                        sb.append(':');
                        Object o = ii.next();
                        // Puts use byte[] but Deletes use KeyValue
                        if (o instanceof byte[]) {
                            sb.append(Bytes.toStringBinary((byte[])o));
                        } else if (o instanceof KeyValue) {
                            sb.append(Bytes.toStringBinary(((KeyValue)o).getQualifier()));
                        } else {
                            throw new RuntimeException("object type not handled");
                        }
                        if (ii.hasNext()) {
                            sb.append(',');
                        }
                    }
                } else {
                    sb.append(Bytes.toStringBinary((byte[])e.getKey()));
                    sb.append(':');
                }
                if (i.hasNext()) {
                    sb.append(',');
                }
            }
        }
    }    

    private void appendTime(StringBuilder sb) {
        // Check if both start and end times are specified and different from their default values
        if (startTime != 0 && endTime != Long.MAX_VALUE) {
            sb.append('/');
            sb.append(startTime);
            // Append the end time only if it's different from the start time
            if (startTime != endTime) {
                sb.append(',');
                sb.append(endTime);
            }
        } else if (endTime != Long.MAX_VALUE) {
            // If only the end time is specified and it's different from its default value
            sb.append('/');
            sb.append(endTime);
        }
    }    

    private void appendMaxVersions(StringBuilder sb) {
        if (maxVersions > 1) {
            sb.append("?v=");
            sb.append(maxVersions);
        }
    }
}
