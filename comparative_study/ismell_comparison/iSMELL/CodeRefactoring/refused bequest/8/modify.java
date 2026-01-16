import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

public class SelectionActionState extends SelectionNodeClarifiers {

    private static final Logger LOG = Logger.getLogger(SelectionActionState.class);

    private static Icon trans = ResourceLoaderWrapper.lookupIconResource("Transition");
    private static Icon transDown = ResourceLoaderWrapper.lookupIconResource("TransitionDown");

    private boolean showIncomingLeft = true;
    private boolean showIncomingAbove = true;
    private boolean showOutgoingRight = true;
    private boolean showOutgoingBelow = true;

    public SelectionActionState(Fig f) {
        super(f);
    }

    public void setOutgoingButtonEnabled(boolean b) {
        setOutgoingRightButtonEnabled(b);
        setIncomingAboveButtonEnabled(b);
    }

    public void setIncomingButtonEnabled(boolean b) {
        setIncomingLeftButtonEnabled(b);
        setOutgoingBelowButtonEnabled(b);
    }

    public void setIncomingLeftButtonEnabled(boolean b) {
        showIncomingLeft = b;
    }

    public void setOutgoingRightButtonEnabled(boolean b) {
        showOutgoingRight = b;
    }

    public void setIncomingAboveButtonEnabled(boolean b) {
        showIncomingAbove = b;
    }

    public void setOutgoingBelowButtonEnabled(boolean b) {
        showOutgoingBelow = b;
    }

    @Override
    public void hitHandle(Rectangle r, Handle h) {
        super.hitHandle(r, h);
        if (h.index != -1) {
            return;
        }
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
        int cx = getContent().getX();
        int cy = getContent().getY();
        int cw = getContent().getWidth();
        int ch = getContent().getHeight();
        int iw = trans.getIconWidth();
        int ih = trans.getIconHeight();
        int iwd = transDown.getIconWidth();
        int ihd = transDown.getIconHeight();
        if (showOutgoingRight && hitLeft(cx + cw, cy + ch / 2, iw, ih, r)) {
            h.index = 12;
            h.instructions = "Add an outgoing transition";
        } else if (showIncomingLeft && hitRight(cx, cy + ch / 2, iw, ih, r)) {
            h.index = 13;
            h.instructions = "Add an incoming transition";
        } else if (showOutgoingBelow && hitAbove(cx + cw / 2, cy, iwd, ihd, r)) {
            h.index = 10;
            h.instructions = "Add an incoming transaction";
        } else if (showIncomingAbove && hitBelow(cx + cw / 2, cy + ch, iwd, ihd, r)) {
            h.index = 11;
            h.instructions = "Add an outgoing transaction";
        } else {
            h.index = -1;
            h.instructions = "Move object(s)";
        }
    }

    @Override
    public void paintButtons(Graphics g) {
        int cx = getContent().getX();
        int cy = getContent().getY();
        int cw = getContent().getWidth();
        int ch = getContent().getHeight();
        if (showOutgoingRight) {
            paintButtonLeft(trans, g, cx + cw, cy + ch / 2, 12);
        }
        if (showIncomingLeft) {
            paintButtonRight(trans, g, cx, cy + ch / 2, 13);
        }
        if (showOutgoingBelow) {
            paintButtonAbove(transDown, g, cx + cw / 2, cy, 14);
        }
        if (showIncomingAbove) {
            paintButtonBelow(transDown, g, cx + cw / 2, cy + ch, 15);
        }
    }

    @Override
    public void dragHandle(int mX, int mY, int anX, int anY, Handle hand) {
        if (hand.index < 10) {
            setPaintButtons(false);
            super.dragHandle(mX, mY, anX, anY, hand);
            return;
        }
        int cx = getContent().getX(), cy = getContent().getY();
        int cw = getContent().getWidth(), ch = getContent().getHeight();
        Object edgeType = null;
        Object nodeType = getNewNodeType(hand.index);

        Editor ce = Globals.curEditor();
        GraphModel gm = ce.getGraphModel();
        if (!(gm instanceof MutableGraphModel)) {
            return;
        }

        int bx = mX, by = mY;
        boolean reverse = false;
        switch (hand.index) {
            case 12: //add incoming
                edgeType = Model.getMetaTypes().getTransition();
                by = cy + ch / 2;
                bx = cx + cw;
                break;
            case 13: // add outgoing
                edgeType = Model.getMetaTypes().getTransition();
                reverse = true;
                by = cy + ch / 2;
                bx = cx;
                break;
            case 10: // add incoming on top
                edgeType = Model.getMetaTypes().getTransition();
                reverse = true;
                by = cy;
                bx = cx + cw / 2;
                break;
            case 11: // add outgoing below
                edgeType = Model.getMetaTypes().getTransition();
                by = cy + ch;
                bx = cx + cw / 2;
                break;
            default:
                LOG.warn("invalid handle number");
                break;
        }
        if (edgeType != null && nodeType != null) {
            ModeCreateEdgeAndNode m = new ModeCreateEdgeAndNode(ce, edgeType, false, this);
            m.setup((FigNode) getContent(), getContent().getOwner(), bx, by, reverse);
            ce.pushMode(m);
        }
    }

    @Override
    protected Object getNewNodeType(int buttonCode) {
        return Model.getMetaTypes().getActionState();
    }

    @Override
    protected Object getNewNode(int buttonCode) {
        return Model.getActivityGraphsFactory().createActionState();
    }

    @Override
    protected Object createEdgeAbove(MutableGraphModel mgm, Object newNode) {
        return mgm.connect(newNode, getContent().getOwner(), (Class) Model.getMetaTypes().getTransition());
    }

    @Override
    protected Object createEdgeLeft(MutableGraphModel gm, Object newNode) {
        return gm.connect(newNode, getContent().getOwner(), (Class) Model.getMetaTypes().getTransition());
    }

    @Override
    protected Object createEdgeRight(MutableGraphModel gm, Object newNode) {
        return gm.connect(getContent().getOwner(), newNode, (Class) Model.getMetaTypes().getTransition());
    }

    @Override
    protected Object createEdgeToSelf(MutableGraphModel gm) {
        return gm.connect(getContent().getOwner(), getContent().getOwner(), (Class) Model.getMetaTypes().getTransition());
    }

    @Override
    protected Object createEdgeUnder(MutableGraphModel gm, Object newNode) {
        return gm.connect(getContent().getOwner(), newNode, (Class) Model.getMetaTypes().getTransition());
    }
}
