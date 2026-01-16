public class GoLinkToStimuli extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(
     *         java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (!Model.getFacade().isALink(parent)) {
            return null;
        }
        return Model.getFacade().getStimuli(parent);
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getRuleName()
     */
    public String getRuleName() {
        return Translator.localize ("misc.link.stimuli");
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(
     *         java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isALink(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return null;
    }
}
