public class ObjectFlowStateTypeNotationUml
        extends ObjectFlowStateTypeNotation {

    /**
     * The constructor.
     *
     * @param objectflowstate the ObjectFlowState represented by this notation
     */
    public ObjectFlowStateTypeNotationUml(Object objectflowstate) {
        super(objectflowstate);
    }

    /**
     * @see org.argouml.notation.NotationProvider4#parse(java.lang.String)
     */
    public String parse(String text) {
        try {
            parseObjectFlowState1(myObjectFlowState, text);
        } catch (ParseException pe) {
            String msg = "statusmsg.bar.error.parsing.objectflowstate";
            Object[] args = {
                    pe.getLocalizedMessage(),
                    new Integer(pe.getErrorOffset()),
            };
            ProjectBrowser.getInstance().getStatusBar().showStatus(
                    Translator.messageFormat(msg, args));
        }
        return toString();
    }

    /**
     * Do the actual parsing.
     *
     * @param objectFlowState the given element to be altered
     * @param s the new string
     * @return the altered ObjectFlowState
     * @throws ParseException when the given text was rejected
     */
    protected Object parseObjectFlowState1(Object objectFlowState, String s)
            throws ParseException {
        Object c =
                Model.getActivityGraphsHelper()
                        .findClassifierByName(objectFlowState, s);
        if (c != null) {
            Model.getCoreHelper().setType(objectFlowState, c);
        } else {
            String msg = "parsing.error.object-flow-type.classifier-not-found";
            Object[] args = {s};
            throw new ParseException(
                    Translator.localize(msg, args),
                    0);
        }
        return objectFlowState;
    }

    /**
     * @see org.argouml.notation.NotationProvider4#getParsingHelp()
     */
    public String getParsingHelp() {
        return "parsing.help.fig-objectflowstate1";
    }

    /**
     * @see java.lang.Object#toString()
     */
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
