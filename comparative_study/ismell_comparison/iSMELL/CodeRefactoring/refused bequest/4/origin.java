public class GoTransitionToTarget extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getRuleName()
     */
    public String getRuleName() {
        return Translator.localize ("misc.transition.target-state");
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Collection col = new ArrayList();
            col.add(Model.getFacade().getTarget(parent));
            return col;
        }
        return null;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }
}
