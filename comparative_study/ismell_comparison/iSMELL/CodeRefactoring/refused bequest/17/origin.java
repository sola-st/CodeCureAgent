public class SelectionClassifierRole extends SelectionNodeClarifiers {
    /**
     * Logger.
     */
    private static final Logger LOG =
            Logger.getLogger(SelectionClassifierRole.class);

    ////////////////////////////////////////////////////////////////
    // constants
    private static Icon assocrole =
            ResourceLoaderWrapper
                    .lookupIconResource("AssociationRole");

    private static Icon selfassoc =
            ResourceLoaderWrapper
                    .lookupIconResource("SelfAssociation");

    ////////////////////////////////////////////////////////////////
    // instance varables
    private boolean showIncoming = true;
    private boolean showOutgoing = true;

    ////////////////////////////////////////////////////////////////
    // constructors

    /**
     * Construct a new SelectionClassifierRole for the given Fig.
     *
     * @param f The given Fig.
     */
    public SelectionClassifierRole(Fig f) {
        super(f);
    }

    ////////////////////////////////////////////////////////////////
    // accessors

    /**
     * @param b true if the incoming button is enabled
     */
    public void setIncomingButtonEnabled(boolean b) {
        showIncoming = b;
    }

    /**
     * @param b true if the outgoing button is enabled
     */
    public void setOutgoingButtonEnabled(boolean b) {
        showOutgoing = b;
    }

    /**
     * @see org.tigris.gef.base.Selection#hitHandle(java.awt.Rectangle,
     *         org.tigris.gef.presentation.Handle)
     */
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
        int iw = assocrole.getIconWidth();
        int ih = assocrole.getIconHeight();
        if (showOutgoing && hitLeft(cx + cw, cy + ch / 2, iw, ih, r)) {
            h.index = 12;
            h.instructions = "Add an outgoing classifierrole";
        } else if (showIncoming && hitRight(cx, cy + ch / 2, iw, ih, r)) {
            h.index = 13;
            h.instructions = "Add an incoming classifierrole";
        } else if (hitRight(cx, cy + ch - 10, iw, ih, r)) {
            h.index = 14;
            h.instructions = "Add a associationrole to this";
        } else {
            h.index = -1;
            h.instructions = "Move object(s)";
        }
    }

    /**
     * @see org.tigris.gef.base.SelectionButtons#paintButtons(java.awt.Graphics)
     */
    public void paintButtons(Graphics g) {
        int cx = getContent().getX();
        int cy = getContent().getY();
        int cw = getContent().getWidth();
        int ch = getContent().getHeight();
        if (showOutgoing) {
            paintButtonLeft(assocrole, g, cx + cw, cy + ch / 2, 12);
        }
        if (showIncoming) {
            paintButtonRight(assocrole, g, cx, cy + ch / 2, 13);
        }
        if (showOutgoing || showIncoming) {
            paintButtonRight(selfassoc, g, cx, cy + ch - 10, 14);
        }
    }

    /**
     * @see org.tigris.gef.base.Selection#dragHandle(int, int, int, int,
     *         org.tigris.gef.presentation.Handle)
     */
    public void dragHandle(int mX, int mY, int anX, int anY, Handle hand) {
        if (hand.index < 10) {
            setPaintButtons(false);
            super.dragHandle(mX, mY, anX, anY, hand);
            return;
        }
        int cx = getContent().getX(), cy = getContent().getY();
        int cw = getContent().getWidth(), ch = getContent().getHeight();
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
            case 12 : //add outgoing
                edgeType = Model.getMetaTypes().getAssociationRole();
                by = cy + ch / 2;
                bx = cx + cw;
                break;
            case 13 : // add incoming
                edgeType = Model.getMetaTypes().getAssociationRole();
                reverse = true;
                by = cy + ch / 2;
                bx = cx;
                break;
            case 14: // add to self
                // do not want to drag this
                break;

            default :
                LOG.warn("invalid handle number");
                break;
        }
        if (edgeType != null && nodeType != null) {
            ModeCreateEdgeAndNode m =
                    new ModeCreateEdgeAndNode(ce, edgeType, false, this);
            m.setup((FigNode) getContent(), getContent().getOwner(),
                    bx, by, reverse);
            ce.pushMode(m);
        }
    }

    /**
     * Create a new ClassifierRole object.
     *
     * @see org.tigris.gef.base.SelectionButtons#getNewNode(int)
     */
    protected Object getNewNode(int buttonCode) {
        return Model.getCollaborationsFactory().createClassifierRole();
    }

    /**
     * @see org.tigris.gef.base.SelectionButtons#createEdgeAbove(
     *         org.tigris.gef.graph.MutableGraphModel, java.lang.Object)
     */
    protected Object createEdgeAbove(MutableGraphModel mgm, Object newNode) {
        return mgm.connect(newNode, getContent().getOwner(),
                (Class) Model.getMetaTypes().getAssociationRole());
    }

    /**
     * @see org.tigris.gef.base.SelectionButtons#createEdgeLeft(
     *         org.tigris.gef.graph.MutableGraphModel, java.lang.Object)
     */
    protected Object createEdgeLeft(MutableGraphModel gm, Object newNode) {
        return gm.connect(newNode, getContent().getOwner(),
                (Class) Model.getMetaTypes().getAssociationRole());
    }

    /**
     * @see org.tigris.gef.base.SelectionButtons#createEdgeRight(
     *         org.tigris.gef.graph.MutableGraphModel, java.lang.Object)
     */
    protected Object createEdgeRight(MutableGraphModel gm, Object newNode) {
        return gm.connect(getContent().getOwner(), newNode,
                (Class) Model.getMetaTypes().getAssociationRole());
    }

    /**
     * To enable this we need to add an icon.
     *
     * @see org.tigris.gef.base.SelectionButtons#createEdgeToSelf(
     *         org.tigris.gef.graph.MutableGraphModel)
     */
    protected Object createEdgeToSelf(MutableGraphModel gm) {
        return gm.connect(
                getContent().getOwner(),
                getContent().getOwner(),
                (Class) Model.getMetaTypes().getAssociationRole());
    }

    /**
     * @see org.tigris.gef.base.SelectionButtons#createEdgeUnder(
     *         org.tigris.gef.graph.MutableGraphModel, java.lang.Object)
     */
    protected Object createEdgeUnder(MutableGraphModel gm, Object newNode) {
        return gm.connect(getContent().getOwner(), newNode,
                (Class) Model.getMetaTypes().getAssociationRole());
    }

} /* end class SelectionClassifierRole */
