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
        fBeginLineNumber = fCurrentEntity.getLineNumber();
        fBeginColumnNumber = fCurrentEntity.getColumnNumber();
        fBeginCharacterOffset = fCurrentEntity.getCharacterOffset();
        int c = fCurrentEntity.read();
        if (c == '<') {
            setScannerState(STATE_MARKUP_BRACKET);
            next = true;
        }
        else if (c == '&') {
            scanEntityRef(fStringBuffer, true);
        }
        else if (c == -1) {
            throw new EOFException();
        }
        else {
            fCurrentEntity.rewind();
            scanCharacters();
        }
    }

    public void handleStateMarkupBracket() throws IOException {
        // Your original code here
        int c = fCurrentEntity.read();
        if (c == '!') {
            if (skip("--", false)) {
                scanComment();
            }
            else if (skip("[CDATA[", false)) {
                scanCDATA();
            }
            else if (skip("DOCTYPE", false)) {
                scanDoctype();
            }
            else {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1002", null);
                }
                skipMarkup(true);
            }
        }
        else if (c == '?') {
            scanPI();
        }
        else if (c == '/') {
            scanEndElement();
        }
        else if (c == -1) {
            if (fReportErrors) {
                fErrorReporter.reportError("HTML1003", null);
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                fStringBuffer.clear();
                fStringBuffer.append('<');
                fDocumentHandler.characters(fStringBuffer, null);
            }
            throw new EOFException();
        }
        else {
            fCurrentEntity.rewind();
            fElementCount++;
            fSingleBoolean[0] = false;
            final String ename = scanStartElement(fSingleBoolean);
            fBeginLineNumber = fCurrentEntity.getLineNumber();
            fBeginColumnNumber = fCurrentEntity.getColumnNumber();
            fBeginCharacterOffset = fCurrentEntity.getCharacterOffset();
            if ("script".equalsIgnoreCase(ename)) {
                scanScriptContent();
            }
            else if (!fParseNoScriptContent && "noscript".equalsIgnoreCase(ename)) {
                scanNoXxxContent("noscript");
            }
            else if (!fParseNoFramesContent && "noframes".equalsIgnoreCase(ename)) {
                scanNoXxxContent("noframes");
            }
            else if (ename != null && !fSingleBoolean[0]
                    && HTMLElements.getElement(ename).isSpecial()
                    && (!ename.equalsIgnoreCase("TITLE") || isEnded(ename))) {
                setScanner(fSpecialScanner.setElementName(ename));
                setScannerState(STATE_CONTENT);
                return true;
            }
        }
        setScannerState(STATE_CONTENT);
    }

    public void handleStateStartDocument() {
        // Your original code here
        if (fDocumentHandler != null && fElementCount >= fElementDepth) {
            if (DEBUG_CALLBACKS) {
                System.out.println("startDocument()");
            }
            XMLLocator locator = HTMLScanner.this;
            String encoding = fIANAEncoding;
            Augmentations augs = locationAugs();
            NamespaceContext nscontext = new NamespaceSupport();
            XercesBridge.getInstance().XMLDocumentHandler_startDocument(fDocumentHandler, locator, encoding, nscontext, augs);
        }
        if (fInsertDoctype && fDocumentHandler != null) {
            String root = HTMLElements.getElement(HTMLElements.HTML).name;
            root = modifyName(root, fNamesElems);
            String pubid = fDoctypePubid;
            String sysid = fDoctypeSysid;
            fDocumentHandler.doctypeDecl(root, pubid, sysid,
                    synthesizedAugs());
        }
        setScannerState(STATE_CONTENT);
    }

    public void handleStateEndDocument() {
        // Your original code here
        if (fDocumentHandler != null && fElementCount >= fElementDepth && complete) {
            if (DEBUG_CALLBACKS) {
                System.out.println("endDocument()");
            }
            fEndLineNumber = fCurrentEntity.getLineNumber();
            fEndColumnNumber = fCurrentEntity.getColumnNumber();
            fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
            fDocumentHandler.endDocument(locationAugs());
        }
        return ;
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