// Separate class to handle GoStateToInternalTrans specific functionalities
class StateInternalTransHandler {

    public String getRuleName() {
        return Translator.localize("misc.state.internal-transitions");
    }

    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAState(parent)) {
            return Model.getFacade().getInternalTransitions(parent);
        }
        return Collections.emptyList();
    }

    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAState(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return Collections.emptySet();
    }
}

// Refactored GoStateToInternalTrans class without inheritance
public class GoStateToInternalTrans {
    private StateInternalTransHandler handler;

    public GoStateToInternalTrans() {
        this.handler = new StateInternalTransHandler();
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
