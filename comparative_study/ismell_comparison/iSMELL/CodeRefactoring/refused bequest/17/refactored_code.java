// Separate class to handle SelectionClassifierRole specific functionalities
class ClassifierRoleHandler {
    private boolean showIncoming = true;
    private boolean showOutgoing = true;
    private static final Icon assocrole = ResourceLoaderWrapper.lookupIconResource("AssociationRole");
    private static final Icon selfassoc = ResourceLoaderWrapper.lookupIconResource("SelfAssociation");

    public void setIncomingButtonEnabled(boolean b) {
        showIncoming = b;
    }

    public void setOutgoingButtonEnabled(boolean b) {
        showOutgoing = b;
    }

    public boolean isShowIncoming() {
        return showIncoming;
    }

    public boolean isShowOutgoing() {
        return showOutgoing;
    }

    public Icon getAssocRoleIcon() {
        return assocrole;
    }

    public Icon getSelfAssocIcon() {
        return selfassoc;
    }

    public void handleDragHandle(int mX, int mY, int anX, int anY, Handle hand, Fig content, Logger log) {
        if (hand.index < 10) {
            // Original super call logic for drag handle
            return;
        }

        int cx = content.getX(), cy = content.getY();
        int cw = content.getWidth(), ch = content.getHeight();
        Object edgeType = null;
        Object nodeType = Model.getMetaTypes().getClassifierRole();

        Editor ce = Globals.curEditor();
        GraphModel gm = ce.getGraphModel();
        if (!(gm instanceof MutableGraphModel)) {
            return;
        }

        int bx = mX, by = mY;
        boolean reverse = false;
        switch (hand.index) {
            case 12:
                edgeType = Model.getMetaTypes().getAssociationRole();
                by = cy + ch / 2;
                bx = cx + cw;
                break;
            case 13:
                edgeType = Model.getMetaTypes().getAssociationRole();
                reverse = true;
                by = cy + ch / 2;
                bx = cx;
                break;
            case 14:
                break;
            default:
                log.warn("invalid handle number");
                break;
        }
        if (edgeType != null && nodeType != null) {
            ModeCreateEdgeAndNode m = new ModeCreateEdgeAndNode(ce, edgeType, false, content);
            m.setup((FigNode) content, content.getOwner(), bx, by, reverse);
            ce.pushMode(m);
        }
    }

    // Other methods related to painting buttons and hitting handles...
}

// Refactored SelectionClassifierRole class without inheritance
public class SelectionClassifierRole {
    private static final Logger LOG = Logger.getLogger(SelectionClassifierRole.class);
    private ClassifierRoleHandler handler;
    private Fig content;

    public SelectionClassifierRole(Fig f) {
        this.handler = new ClassifierRoleHandler();
        this.content = f;
    }

    public void setIncomingButtonEnabled(boolean b) {
        handler.setIncomingButtonEnabled(b);
    }

    public void setOutgoingButtonEnabled(boolean b) {
        handler.setOutgoingButtonEnabled(b);
    }

    public void hitHandle(Rectangle r, Handle h) {
        // Simplified logic for handling hits
        if (!isPaintButtons()) {
            return;
        }
        Editor ce = Globals.curEditor();
        SelectionManager sm = ce.getSelectionManager();
        if (sm.size() != 1) {
            return;
        }
        ModeManager mm = ce.getModeManager();
        if (mm.includes(ModeModify.class) && getPressedButton() == -1) {
            return;
        }
        int cx = content.getX();
        int cy = content.getY();
        int cw = content.getWidth();
        int ch = content.getHeight();
        int iw = handler.getAssocRoleIcon().getIconWidth();
        int ih = handler.getAssocRoleIcon().getIconHeight();
        if (handler.isShowOutgoing() && hitLeft(cx + cw, cy + ch / 2, iw, ih, r)) {
            h.index = 12;
            h.instructions = "Add an outgoing classifierrole";
        } else if (handler.isShowIncoming() && hitRight(cx, cy + ch / 2, iw, ih, r)) {
            h.index = 13;
            h.instructions = "Add an incoming classifierrole";
        } else if (hitRight(cx, cy + ch - 10, iw, ih, r)) {
            h.index = 14;
            h.instructions = "Add an associationrole to this";
        } else {
            h.index = -1;
            h.instructions = "Move object(s)";
        }
    }

    public void paintButtons(Graphics g) {
        int cx = content.getX();
        int cy = content.getY();
        int cw = content.getWidth();
        int ch = content.getHeight();
        if (handler.isShowOutgoing()) {
            paintButtonLeft(handler.getAssocRoleIcon(), g, cx + cw, cy + ch / 2, 12);
        }
        if (handler.isShowIncoming()) {
            paintButtonRight(handler.getAssocRoleIcon(), g, cx, cy + ch / 2, 13);
        }
        if (handler.isShowOutgoing() || handler.isShowIncoming()) {
            paintButtonRight(handler.getSelfAssocIcon(), g, cx, cy + ch - 10, 14);
        }
    }

    public void dragHandle(int mX, int mY, int anX, int anY, Handle hand) {
        handler.handleDragHandle(mX, mY, anX, anY, hand, content, LOG);
    }

    // Other methods...
}
