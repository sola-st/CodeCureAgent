protected String buildRowSpec(final byte[] row, final Map familyMap,
final long startTime, final long endTime, final int maxVersions) {
        StringBuffer sb = new StringBuffer();
        sb.append('/');
        if (accessToken != null) {
        sb.append(accessToken);
        sb.append('/');
        }
        sb.append(Bytes.toStringBinary(name));
        sb.append('/');
        sb.append(Bytes.toStringBinary(row));
        Set families = familyMap.entrySet();
        if (families != null) {
        Iterator i = familyMap.entrySet().iterator();
        if (i.hasNext()) {
        sb.append('/');
        }
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
        if (maxVersions > 1) {
        sb.append("?v=");
        sb.append(maxVersions);
        }
        return sb.toString();
        }
