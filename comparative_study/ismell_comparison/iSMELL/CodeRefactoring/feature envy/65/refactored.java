public class SchemaObjectManager {
    private OrderedHashSet resolved;
    private OrderedHashSet unresolved;

    public SchemaObjectManager(OrderedHashSet resolved, OrderedHashSet unresolved) {
        this.resolved = resolved;
        this.unresolved = unresolved;
    }

    public boolean isObjectResolved(SchemaObject object) {
        OrderedHashSet references = object.getReferences();
        boolean isResolved = true;

        for (int j = 0; j < references.size(); j++) {
            HsqlName name = (HsqlName) references.get(j);

            if (SqlInvariants.isSchemaNameSystem(name)) {
                continue;
            }

            if (!resolved.contains(name)) {
                isResolved = false;
                break;
            }
        }

        return isResolved;
    }

    public void addResolvedObject(SchemaObject object, HsqlArrayList list, OrderedHashSet newResolved) {
        HsqlName name;

        if (object.getType() == SchemaObject.FUNCTION) {
            name = ((Routine) object).getSpecificName();
        } else {
            name = object.getName();
        }

        resolved.add(name);

        if (newResolved != null) {
            newResolved.add(object);
        }

        if (object.getType() == SchemaObject.TABLE) {
            list.addAll(((Table) object).getSQL(resolved, unresolved));
        } else {
            list.add(object.getSQL());
        }
    }
}

    static void addAllSQL(OrderedHashSet resolved, OrderedHashSet unresolved,
                          HsqlArrayList list, Iterator it,
                          OrderedHashSet newResolved) {
        SchemaObjectManager manager = new SchemaObjectManager(resolved, unresolved);

        while (it.hasNext()) {
            SchemaObject object = (SchemaObject) it.next();

            if (!manager.isObjectResolved(object)) {
                unresolved.add(object);
                continue;
            }

            manager.addResolvedObject(object, list, newResolved);
        }
    }