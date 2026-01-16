public class HTMLScannerStateManager {
    private int fScannerState;
    private boolean fReportErrors;
    private CurrentEntity fCurrentEntity;
    private ErrorReporter fErrorReporter;
    private DocumentHandler fDocumentHandler;
    private int fElementCount;
    private int fElementDepth;
    private boolean fParseNoScriptContent;
    private boolean fParseNoFramesContent;
    private SpecialScanner fSpecialScanner;
    private boolean[] fSingleBoolean;

    public HTMLScannerStateManager(int fScannerState, boolean fReportErrors, CurrentEntity fCurrentEntity, ErrorReporter fErrorReporter, DocumentHandler fDocumentHandler, int fElementCount, int fElementDepth, boolean fParseNoScriptContent, boolean fParseNoFramesContent, SpecialScanner fSpecialScanner, boolean[] fSingleBoolean) {
        this.fScannerState = fScannerState;
        this.fReportErrors = fReportErrors;
        this.fCurrentEntity = fCurrentEntity;
        this.fErrorReporter = fErrorReporter;
        this.fDocumentHandler = fDocumentHandler;
        this.fElementCount = fElementCount;
        this.fElementDepth = fElementDepth;
        this.fParseNoScriptContent = fParseNoScriptContent;
        this.fParseNoFramesContent = fParseNoFramesContent;
        this.fSpecialScanner = fSpecialScanner;
        this.fSingleBoolean = fSingleBoolean;
    }

    public void handleStateContent() throws IOException {
        // Your original code here
    }

    public void handleStateMarkupBracket() throws IOException {
        // Your original code here
    }

    public void handleStateStartDocument() {
        // Your original code here
    }

    public void handleStateEndDocument() {
        // Your original code here
    }
}

    public boolean scan(boolean complete) throws IOException {
        boolean next;
        do {
            try {
                next = false;
                HTMLScannerStateManager manager = new HTMLScannerStateManager(fScannerState, fReportErrors, fCurrentEntity, fErrorReporter, fDocumentHandler, fElementCount, fElementDepth, fParseNoScriptContent, fParseNoFramesContent, fSpecialScanner, fSingleBoolean);
                switch (fScannerState) {
                    case STATE_CONTENT:
                        manager.handleStateContent();
                        break;
                    case STATE_MARKUP_BRACKET:
                        manager.handleStateMarkupBracket();
                        break;
                    case STATE_START_DOCUMENT:
                        manager.handleStateStartDocument();
                        break;
                    case STATE_END_DOCUMENT:
                        manager.handleStateEndDocument();
                        break;
                    default:
                        throw new RuntimeException("unknown scanner state: " + fScannerState);
                }
            } catch (EOFException e) {
                if (fCurrentEntityStack.empty()) {
                    setScannerState(STATE_END_DOCUMENT);
                } else {
                    fCurrentEntity = (CurrentEntity) fCurrentEntityStack.pop();
                }
                next = true;
            }
        } while (next || complete);
        return true;
    } // scan(boolean):boolean