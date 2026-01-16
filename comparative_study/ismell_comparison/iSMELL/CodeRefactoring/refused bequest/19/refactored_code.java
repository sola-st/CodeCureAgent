// Separate class to handle GoTransitionToSource specific functionalities
class TransitionSourceHandler {

    public String getRuleName() {
        return Translator.localize("misc.transition.source-state");
    }

    public Collection getChildren(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Collection col = new ArrayList();
            col.add(Model.getFacade().getSource(parent));
            return col;
        }
        return Collections.emptyList();
    }

    public Set getDependencies(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return Collections.emptySet();
    }
}

// Refactored GoTransitionToSource class without inheritance
public class GoTransitionToSource {
    private TransitionSourceHandler handler;

    public GoTransitionToSource() {
        this.handler = new TransitionSourceHandler();
    }

    public String getRuleName() {
        return handler.getRuleName();
    }

    public Collection getChildren(Object parent) {
        return handler.getChildren(parent);
    }

    public Set getDependencies(Object parent) {
        return handler.getDependencies(parent);
    }
}
