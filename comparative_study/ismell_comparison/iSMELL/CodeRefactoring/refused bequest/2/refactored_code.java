public class ObjectFlowStateNotationHandler {
    private Object objectFlowState;

    public ObjectFlowStateNotationHandler(Object objectFlowState) {
        this.objectFlowState = objectFlowState;
    }

    /**
     * Do the actual parsing.
     *
     * @param s the new string
     * @return the altered ObjectFlowState
     * @throws ParseException when the given text was rejected
     */
    public Object parse(String s) throws ParseException {
        s = s.trim();
        if (s.startsWith("[")) {
            s = s.substring(1, s.length() - 1).trim();
        }

        Object c = Model.getFacade().getType(objectFlowState);
        if (c == null) {
            throw new ParseException("Classifier not found", 0);
        }

        return handleParsing(c, s);
    }

    private Object handleParsing(Object classifier, String input) throws ParseException {
        Collection<Object> states = getStates(classifier);
        updateStatesBasedOnInput(states, input, classifier);

        Model.getActivityGraphsHelper().setInStates(classifier, states);
        return objectFlowState;
    }

    private Collection<Object> getStates(Object classifier) {
        return new ArrayList<>(Model.getFacade().getInStates(classifier));
    }

    private void updateStatesBasedOnInput(Collection<Object> states, String input, Object classifier) throws ParseException {
        StringTokenizer tokenizer = new StringTokenizer(input, ",");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken().trim();
            Object state = findStateByName(classifier, name);
            if (state != null) {
                states.add(state);
            } else {
                throw new ParseException("State not found: " + name, 0);
            }
        }
    }

    private Object findStateByName(Object classifier, String name) {
        return Model.getActivityGraphsHelper().findStateByName(classifier, name);
    }
}

// Modified Class using the handler
public class ObjectFlowStateStateNotationUml extends ObjectFlowStateStateNotation {
    private ObjectFlowStateNotationHandler handler;

    public ObjectFlowStateStateNotationUml(Object objectFlowState) {
        super(objectFlowState);
        this.handler = new ObjectFlowStateNotationHandler(objectFlowState);
    }

    public String parse(String text) {
        try {
            handler.parse(text);
        } catch (ParseException pe) {
            String msg = "statusmsg.bar.error.parsing.objectflowstate";
            Object[] args = {pe.getLocalizedMessage(), pe.getErrorOffset()};
            ProjectBrowser.getInstance().getStatusBar().showStatus(Translator.messageFormat(msg, args));
        }
        return toString();
    }
}
