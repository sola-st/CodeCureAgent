public class GoStimulusToAction extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (!Model.getFacade().isAStimulus(parent))
            return null;
        Object ms = /*(MStimulus)*/ parent;
        Object action = Model.getFacade().getDispatchAction(ms);
        Vector vector = new Vector();
        vector.addElement(action);
        return vector;

    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAStimulus(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getRuleName()
     */
    public String getRuleName() {
        return Translator.localize("misc.stimulus.action");
    }
}
