public class GoUseCaseToExtensionPoint extends AbstractPerspectiveRule {
    /**
     * Give a name to this rule.<p>
     *
     * @return  The name of the rule ("<code>Use Case->Extension
     *          Point</code>").
     */
    public String getRuleName() {
        return Translator.localize ("misc.use-case.extension-point");
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAUseCase(parent)) {
            return Model.getFacade().getExtensionPoints(parent);
        }
        return null;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAUseCase(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }
}  /* End of class GoUseCaseToExtensionPoint */
