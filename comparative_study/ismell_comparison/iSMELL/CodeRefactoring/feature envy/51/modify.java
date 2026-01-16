public class RowSpecBuilder {
    private byte[] name;
    private byte[] row;
    private Map<byte[], Collection<byte[]>> familyMap;
    private long startTime;
    private long endTime;
    private int maxVersions;
    private String accessToken;

    public RowSpecBuilder(byte[] name, byte[] row, Map<byte[], Collection<byte[]>> familyMap,
                          long startTime, long endTime, int maxVersions, String accessToken) {
        this.name = name;
        this.row = row;
        this.familyMap = familyMap;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxVersions = maxVersions;
        this.accessToken = accessToken;
    }

    public String build() {
        StringBuffer sb = new StringBuffer();
        sb.append('/');
        if (accessToken != null) {
            sb.append(accessToken);
            sb.append('/');
        }
        sb.append(Bytes.toStringBinary(name));
        sb.append('/');
        sb.append(Bytes.toStringBinary(row));
        appendFamilies(sb);
        appendTimeRange(sb);
        appendMaxVersions(sb);
        return sb.toString();
    }

    private void appendFamilies(StringBuffer sb) {
        Set<Map.Entry<byte[], Collection<byte[]>>> families = familyMap.entrySet();
        if (!families.isEmpty()) {
            sb.append('/');
            Iterator<Map.Entry<byte[], Collection<byte[]>>> i = families.iterator();
            while (i.hasNext()) {
                Map.Entry<byte[], Collection<byte[]>> e = i.next();
                appendFamily(sb, e);
                if (i.hasNext()) {
                    sb.append(',');
                }
            }
        }
    }

    private void appendFamily(StringBuffer sb, Map.Entry<byte[], Collection<byte[]>> family) {
        Collection<byte[]> quals = family.getValue();
        if (quals != null && !quals.isEmpty()) {
            Iterator<byte[]> ii = quals.iterator();
            while (ii.hasNext()) {
                sb.append(Bytes.toStringBinary(family.getKey()));
                sb.append(':');
                byte[] o = ii.next();
                if (o instanceof byte[]) {
                    sb.append(Bytes.toStringBinary(o));
                } else if (o instanceof KeyValue) {
                    sb.append(Bytes.toStringBinary(((KeyValue) o).getQualifier()));
                } else {
                    throw new RuntimeException("object type not handled");
                }
                if (ii.hasNext()) {
                    sb.append(',');
                }
            }
        } else {
            sb.append(Bytes.toStringBinary(family.getKey()));
            sb.append(':');
        }
    }

    private void appendTimeRange(StringBuffer sb) {
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

    // Usage example in another part of the code
    protected String buildRowSpec(final byte[] row, final Map<byte[], Collection<byte[]>> familyMap,
                                  final long startTime, final long endTime, final int maxVersions) {
        String accessToken = retrieveAccessToken(); // Assume method exists to retrieve the token
        byte[] name = retrieveName(); // Assume method exists to retrieve the name
        RowSpecBuilder builder = new RowSpecBuilder(name, row, familyMap, startTime, endTime, maxVersions, accessToken);
        return builder.build();
    }
