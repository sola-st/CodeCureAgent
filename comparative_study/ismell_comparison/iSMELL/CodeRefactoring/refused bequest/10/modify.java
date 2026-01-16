public class ObjectFlowStateTypeNotationUml extends ObjectFlowStateTypeNotation {

    /**
     * The constructor.
     *
     * @param objectflowstate the ObjectFlowState represented by this notation
     */
    public ObjectFlowStateTypeNotationUml(Object objectflowstate) {
        super(objectflowstate);
    }

    /**
     * Override the parse method to customize parsing behavior for UML-specific
     * notations while leveraging the basic parsing infrastructure provided by the superclass.
     *
     * @param text the text to parse
     * @return the resulting string after parsing
     */
    @Override
    public String parse(String text) {
        try {
            parseObjectFlowState1(myObjectFlowState, text);
        } catch (ParseException pe) {
            handleParseException(pe);
        }
        return toString();
    }

    /**
     * Do the actual parsing for UML.
     *
     * @param objectFlowState the given element to be altered
     * @param s the new string
     * @throws ParseException when the given text was rejected
     */
    protected Object parseObjectFlowState1(Object objectFlowState, String s)
            throws ParseException {
        Object c = findClassifier(objectFlowState, s);
        if (c != null) {
            setType(objectFlowState, c);
        } else {
            throwParseException(s);
        }
        return objectFlowState;
    }

    /**
     * Helper method to encapsulate exception handling logic.
     *
     * @param pe the ParseException to handle
     */
    private void handleParseException(ParseException pe) {
        String msg = "statusmsg.bar.error.parsing.objectflowstate";
        Object[] args = {pe.getLocalizedMessage(), pe.getErrorOffset()};
        ProjectBrowser.getInstance().getStatusBar().showStatus(
                Translator.messageFormat(msg, args));
    }

    /**
     * Utilizes existing model utilities to find classifiers by name.
     *
     * @param objectFlowState the context in which to find the classifier
     * @param name the name of the classifier
     * @return the found classifier
     */
    private Object findClassifier(Object objectFlowState, String name) {
        return Model.getActivityGraphsHelper().findClassifierByName(objectFlowState, name);
    }

    /**
     * Sets the type of an objectFlowState.
     *
     * @param objectFlowState the ObjectFlowState whose type is to be set
     * @param classifier the classifier to set as the type
     */
    private void setType(Object objectFlowState, Object classifier) {
        Model.getCoreHelper().setType(objectFlowState, classifier);
    }

    /**
     * Throws a ParseException with a specific message formatted for the given classifier name.
     *
     * @param classifierName the classifier name that could not be found
     * @throws ParseException the exception to be thrown
     */
    private void throwParseException(String classifierName) throws ParseException {
        String msg = "parsing.error.object-flow-type.classifier-not-found";
        Object[] args = {classifierName};
        throw new ParseException(Translator.localize(msg, args), 0);
    }

    /**
     * Provides parsing help specific to UML.
     *
     * @return the parsing help message
     */
    @Override
    public String getParsingHelp() {
        return "parsing.help.fig-objectflowstate1";
    }

    /**
     * Converts the ObjectFlowState to a string representation.
     *
     * @return the string representation of the ObjectFlowState
     */
    @Override
    public String toString() {
        Object classifier = Model.getFacade().getType(myObjectFlowState);
        if (Model.getFacade().isAClassifierInState(classifier)) {
            classifier = Model.getFacade().getType(classifier);
        }
        if (classifier == null) {
            return "";
        }
        String name = Model.getFacade().getName(classifier);
        if (name == null) {
            name = "";
        }
        return name;
    }
}
