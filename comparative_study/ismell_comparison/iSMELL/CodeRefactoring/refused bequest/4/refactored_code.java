public class GoTransitionToTarget {
    private PerspectiveRule perspectiveRule;

    public GoTransitionToTarget() {
        this.perspectiveRule = new AbstractPerspectiveRule();
    }

    public String getRuleName() {
        return Translator.localize("misc.transition.target-state");
    }

    public Collection getChildren(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Collection col = new ArrayList();
            col.add(Model.getFacade().getTarget(parent));
            return col;
        }
        return null;
    }

    public Set getDependencies(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }
}
