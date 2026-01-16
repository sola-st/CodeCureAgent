public class GoModelElementToContents extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.AbstractPerspectiveRule#getRuleName()
     */
    public String getRuleName() {
        return Translator.localize("misc.model-element.contents");
    }

    /**
     * @see org.argouml.ui.explorer.rules.AbstractPerspectiveRule#getChildren(java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAModelElement(parent)) {
            return Model.getFacade().getModelElementContents(parent);
        }
        return null;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        Set set = new HashSet();
        if (Model.getFacade().isAModelElement(parent)) {
            set.add(parent);
            set.addAll(Model.getFacade().getModelElementContents(parent));
        }
        return set;
    }

}
