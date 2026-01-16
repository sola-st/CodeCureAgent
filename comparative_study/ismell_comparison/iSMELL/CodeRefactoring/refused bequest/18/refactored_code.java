// Separate class to handle GoStimulusToAction specific functionalities
class StimulusActionHandler {

    public Collection getChildren(Object parent) {
        if (!Model.getFacade().isAStimulus(parent)) {
            return Collections.emptyList();
        }
        Object ms = parent;
        Object action = Model.getFacade().getDispatchAction(ms);
        Vector vector = new Vector();
        vector.addElement(action);
        return vector;
    }

    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAStimulus(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return Collections.emptySet();
    }

    public String getRuleName() {
        return Translator.localize("misc.stimulus.action");
    }
}

// Refactored GoStimulusToAction class without inheritance
public class GoStimulusToAction {
    private StimulusActionHandler handler;

    public GoStimulusToAction() {
        this.handler = new StimulusActionHandler();
    }

    public Collection getChildren(Object parent) {
        return handler.getChildren(parent);
    }

    public Set getDependencies(Object parent) {
        return handler.getDependencies(parent);
    }

    public String getRuleName() {
        return handler.getRuleName();
    }
}
