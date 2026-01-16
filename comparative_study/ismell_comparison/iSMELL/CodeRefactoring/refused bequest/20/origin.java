public class GoStateToInternalTrans extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getRuleName()
     */
    public String getRuleName() {
        return Translator.localize ("misc.state.internal-transitions");
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAState(parent)) {
            return Model.getFacade().getInternalTransitions(parent);
        }
        return null;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAState(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }
} /* end class GoStateToInternalTrans */
