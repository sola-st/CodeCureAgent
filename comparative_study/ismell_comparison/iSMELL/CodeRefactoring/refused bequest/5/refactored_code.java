// Refactored class without inheritance from AbstractPerspectiveRule
public class UseCaseExtensionPointProvider {
    /**
     * Give a name to this provider.<p>
     *
     * @return  The name of the provider ("<code>Use Case->Extension Point</code>").
     */
    public String getName() {
        return Translator.localize("misc.use-case.extension-point");
    }

    /**
     * Retrieves children based on the provided parent object.
     *
     * @param parent The parent object to evaluate.
     * @return A collection of extension points if the parent is a use case, otherwise null.
     */
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAUseCase(parent)) {
            return Model.getFacade().getExtensionPoints(parent);
        }
        return null;
    }

    /**
     * Identifies dependencies related to the provided parent object.
     *
     * @param parent The parent object to evaluate.
     * @return A set containing the parent if it is a use case, otherwise null.
     */
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAUseCase(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }
}
