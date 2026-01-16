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

        if (Model.getFacade().isAClassifierInState(c)) {
            return parseClassifierInState(c, s);
        } else {
            return parseNormalClassifier(c, s);
        }
    }

    private Object parseClassifierInState(Object classifier, String input) throws ParseException {
        Collection<Object> states = getStates(classifier);
        updateStatesBasedOnInput(states, input, classifier);

        Model.getActivityGraphsHelper().setInStates(classifier, states);
        return objectFlowState;
    }

    private Object parseNormalClassifier(Object classifier, String input) throws ParseException {
        Collection<Object> statesToBeAdded = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(input, ",");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken().trim();
            Object state = findStateByName(classifier, name);
            if (state != null) {
                statesToBeAdded.add(state);
            } else {
                throw new ParseException("State not found: " + name, 0);
            }
        }

        Object cis = Model.getActivityGraphsFactory().buildClassifierInState(classifier, statesToBeAdded);
        Model.getCoreHelper().setType(objectFlowState, cis);
        return objectFlowState;
    }

    private Collection<Object> getStates(Object classifier) {
        return new ArrayList<>(Model.getFacade().getInStates(classifier));
    }

    private void updateStatesBasedOnInput(Collection<Object> states, String input, Object classifier) throws ParseException {
        Collection<Object> statesToBeRemoved = new ArrayList<>(states);
        Collection<String> namesToBeAdded = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(input, ",");

        while (tokenizer.hasMoreTokens()) {
            String nextToken = tokenizer.nextToken().trim();
            boolean found = false;
            Iterator<Object> i = states.iterator();
            while (i.hasNext()) {
                Object state = i.next();
                if (Model.getFacade().getName(state).equals(nextToken)) {
                    found = true;
                    statesToBeRemoved.remove(state);
                }
            }
            if (!found) {
                namesToBeAdded.add(nextToken);
            }
        }

        states.removeAll(statesToBeRemoved);

        for (String name : namesToBeAdded) {
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

    public void delete(Object obj) {
        if (obj != null) {
            ProjectManager.getManager().getCurrentProject().moveToTrash(obj);
        }
    }

    @Override
    public String toString() {
        StringBuffer theNewText = new StringBuffer("");
        Object cis = Model.getFacade().getType(objectFlowState);
        if (Model.getFacade().isAClassifierInState(cis)) {
            theNewText.append("[ ");
            Collection<Object> states = Model.getFacade().getInStates(cis);
            Iterator<Object> i = states.iterator();
            boolean first = true;
            while (i.hasNext()) {
                if (!first) {
                    theNewText.append(", ");
                }
                first = false;
                Object state = i.next();
                theNewText.append(Model.getFacade().getName(state));
            }
            theNewText.append(" ]");
        }
        return theNewText.toString();
    }
}

// Modified Class using the handler
public class ObjectFlowStateStateNotationUml extends ObjectFlowStateStateNotation {
    private ObjectFlowStateNotationHandler handler;

    public ObjectFlowStateStateNotationUml(Object objectFlowState) {
        super(objectFlowState);
        this.handler = new ObjectFlowStateNotationHandler(objectFlowState);
    }

    @Override
    public String getParsingHelp() {
        return "parsing.help.fig-objectflowstate2";
    }

    @Override
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

    @Override
    public String toString() {
        return handler.toString();
    }
}
