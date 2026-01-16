public class FigJunctionState extends FigStateVertex {

    private static final int X = 0;
    private static final int Y = 0;
    private static final int WIDTH = 32;
    private static final int HEIGHT = 32;

    private FigDiamond head;

    /**
     * The constructor.
     */
    public FigJunctionState() {
        setEditable(false);
        setBigPort(new FigDiamond(X, Y, WIDTH, HEIGHT, false,
                Color.cyan, Color.cyan));
        head = new FigDiamond(X, Y, WIDTH, HEIGHT, false,
                Color.black, Color.white);

        addFig(getBigPort());
        addFig(head);

        setBlinkPorts(false); //make port invisble unless mouse enters
    }

    /**
     * Constructor.
     *
     * @param gm ignored
     * @param node the owner
     */
    public FigJunctionState(GraphModel gm, Object node) {
        this();
        setOwner(node);
    }

    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        FigJunctionState figClone = (FigJunctionState) super.clone();
        Iterator it = figClone.getFigs().iterator();
        figClone.setBigPort((FigDiamond) it.next());
        figClone.head = (FigDiamond) it.next();
        return figClone;
    }

    ////////////////////////////////////////////////////////////////
    // Fig accesors

    /**
     * Initial states are fixed size.
     *
     * @see org.tigris.gef.presentation.Fig#isResizable()
     */
    public boolean isResizable() { return false; }

    /**
     * @see org.tigris.gef.presentation.Fig#setLineColor(java.awt.Color)
     */
    public void setLineColor(Color col) {
        head.setLineColor(col);
    }

    /**
     * @see org.tigris.gef.presentation.Fig#getLineColor()
     */
    public Color getLineColor() {
        return head.getLineColor();
    }

    /**
     * @see org.tigris.gef.presentation.Fig#setFillColor(java.awt.Color)
     */
    public void setFillColor(Color col) {
        head.setFillColor(col);
    }

    /**
     * @see org.tigris.gef.presentation.Fig#getFillColor()
     */
    public Color getFillColor() {
        return head.getFillColor();
    }

    /**
     * @see org.tigris.gef.presentation.Fig#setFilled(boolean)
     */
    public void setFilled(boolean f) {
    }

    /**
     * @see org.tigris.gef.presentation.Fig#getFilled()
     */
    public boolean getFilled() {
        return true;
    }

    /**
     * @see org.tigris.gef.presentation.Fig#setLineWidth(int)
     */
    public void setLineWidth(int w) {
        head.setLineWidth(w);
    }

    /**
     * @see org.tigris.gef.presentation.Fig#getLineWidth()
     */
    public int getLineWidth() {
        return head.getLineWidth();
    }

    ////////////////////////////////////////////////////////////////
    // Event handlers

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent me) { }

    /**
     * @see org.tigris.gef.presentation.Fig#getClosestPoint(java.awt.Point)
     */
    public Point getClosestPoint(Point anotherPt) {
        Rectangle r = getBounds();
        int[] xs = {r.x + r.width / 2,
                r.x + r.width,
                r.x + r.width / 2,
                r.x,
                r.x + r.width / 2,
        };
        int[] ys = {r.y,
                r.y + r.height / 2,
                r.y + r.height,
                r.y + r.height / 2,
                r.y,
        };
        Point p =
                Geometry.ptClosestTo(
                        xs,
                        ys,
                        5,
                        anotherPt);
        return p;
    }

    /**
     * The UID.
     */
    private static final long serialVersionUID = -5845934640541945686L;
} /* end class FigJunctionState */
