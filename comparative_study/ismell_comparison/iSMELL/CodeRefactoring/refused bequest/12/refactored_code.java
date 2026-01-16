public class GoLinkToStimuli extends AbstractPerspectiveRule {

    /**
     * Uses a modern Java collection API and stream to filter and collect stimuli,
     * providing a more readable and efficient approach to gathering data.
     */
    @Override
    public Collection getChildren(Object parent) {
        if (!Model.getFacade().isALink(parent)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Model.getFacade().getStimuli(parent));
    }

    /**
     * Clearly states the purpose of the rule within the user interface.
     */
    @Override
    public String getRuleName() {
        return Translator.localize("misc.link.stimuli");
    }

    /**
     * Ensures dependencies are clearly defined and managed, returning an
     * empty set when no dependencies exist, rather than null, to avoid potential
     * null pointer exceptions.
     */
    @Override
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isALink(parent)) {
            return Collections.singleton(parent);
        }
        return Collections.emptySet();
    }
}
