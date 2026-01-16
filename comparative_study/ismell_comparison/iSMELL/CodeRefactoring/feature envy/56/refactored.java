public class RowSpecBuilder {
    private byte[] row;
    private Map familyMap;
    private long startTime;
    private long endTime;
    private int maxVersions;

    public RowSpecBuilder(byte[] row, Map familyMap, long startTime, long endTime, int maxVersions) {
        this.row = row;
        this.familyMap = familyMap;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxVersions = maxVersions;
    }

    public String buildRowSpec() {
        StringBuffer sb = new StringBuffer();
        appendRow(sb);
        appendFamilies(sb);
        appendTime(sb);
        appendMaxVersions(sb);
        return sb.toString();
    }

    private void appendRow(StringBuffer sb) {
        sb.append('/');
        sb.append(Bytes.toStringBinary(row));
    }

    private void appendFamilies(StringBuffer sb) {
        Set families = familyMap.entrySet();
        if (families != null) {
            Iterator i = familyMap.entrySet().iterator();
            if (i.hasNext()) {
                sb.append('/');
            }
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry)i.next();
                appendFamily(sb, e);
            }
        }
    }

    private void appendFamily(StringBuffer sb, Map.Entry e) {
        Collection quals = (Collection)e.getValue();
        if (quals != null && !quals.isEmpty()) {
            appendQuals(sb, e, quals);
        } else {
            sb.append(Bytes.toStringBinary((byte[])e.getKey()));
            sb.append(':');
        }
    }

    private void appendQuals(StringBuffer sb, Map.Entry e, Collection quals) {
        Iterator ii = quals.iterator();
        while (ii.hasNext()) {
            sb.append(Bytes.toStringBinary((byte[])e.getKey()));
            sb.append(':');
            Object o = ii.next();
            appendQual(sb, o);
            if (ii.hasNext()) {
                sb.append(',');
            }
        }
    }

    private void appendQual(StringBuffer sb, Object o) {
        if (o instanceof byte[]) {
            sb.append(Bytes.toStringBinary((byte[])o));
        } else if (o instanceof KeyValue) {
            sb.append(Bytes.toStringBinary(((KeyValue)o).getQualifier()));
        } else {
            throw new RuntimeException("object type not handled");
        }
    }

    private void appendTime(StringBuffer sb) {
        if (startTime != 0 && endTime != Long.MAX_VALUE) {
            sb.append('/');
            sb.append(startTime);
            if (startTime != endTime) {
                sb.append(',');
                sb.append(endTime);
            }
        } else if (endTime != Long.MAX_VALUE) {
            sb.append('/');
            sb.append(endTime);
        }
    }

    private void appendMaxVersions(StringBuffer sb) {
        if (maxVersions > 1) {
            sb.append("?v=");
            sb.append(maxVersions);
        }
    }
}

protected String buildRowSpec(final byte[] row, final Map familyMap,
final long startTime, final long endTime, final int maxVersions) {
    RowSpecBuilder builder = new RowSpecBuilder(row, familyMap, startTime, endTime, maxVersions);
    return builder.buildRowSpec();
}
