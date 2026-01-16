import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FigActor extends FigNodeModelElement {

    private static final long serialVersionUID = 7265843766314395713L;
    protected static final int MIN_VERT_PADDING = 4;

    private static final int HEAD_POSN = 2;
    private static final int BODY_POSN = 3;
    private static final int ARMS_POSN = 4;
    private static final int LEFT_LEG_POSN = 5;
    private static final int RIGHT_LEG_POSN = 6;

    private ActorPortFigRect bigPort;
    private FigCircle head;
    private FigLine body, arms, leftLeg, rightLeg;

    public FigActor() {
        initializeComponents();
    }

    public FigActor(GraphModel gm, Object node) {
        this();
        setOwner(node);
    }

    private void initializeComponents() {
        bigPort = new ActorPortFigRect(10, 10, 15, 60, this);
        bigPort.setVisible(false);
        head = new FigCircle(10, 10, 15, 15, Color.black, Color.white);
        body = new FigLine(20, 25, 20, 40, Color.black);
        arms = new FigLine(10, 30, 30, 30, Color.black);
        leftLeg = new FigLine(20, 40, 15, 55, Color.black);
        rightLeg = new FigLine(20, 40, 25, 55, Color.black);

        getNameFig().setBounds(5, 55, 35, 20);
        getNameFig().setTextFilled(false);
        getNameFig().setFilled(false);
        getNameFig().setLineWidth(0);

        getStereotypeFig().setBounds(getBigPort().getCenter().x, getBigPort().getCenter().y, 0, 0);

        addFig(bigPort);
        addFig(getNameFig());
        addFig(head);
        addFig(body);
        addFig(arms);
        addFig(leftLeg);
        addFig(rightLeg);
        addFig(getStereotypeFig());
        setBigPort(bigPort);
    }

    @Override
    public void setLineWidth(int width) {
        getFigAt(HEAD_POSN).setLineWidth(width);
        getFigAt(BODY_POSN).setLineWidth(width);
        getFigAt(ARMS_POSN).setLineWidth(width);
        getFigAt(LEFT_LEG_POSN).setLineWidth(width);
        getFigAt(RIGHT_LEG_POSN).setLineWidth(width);
    }

    @Override
    public void setFilled(boolean filled) {
        getFigAt(HEAD_POSN).setFilled(filled);
    }

    @Override
    public String placeString() {
        return "new Actor";
    }

    @Override
    public Selection makeSelection() {
        return new SelectionActor(this);
    }

    @Override
    public Vector getPopUpActions(MouseEvent me) {
        Vector popUpActions = super.getPopUpActions(me);
        popUpActions.insertElementAt(buildModifierPopUp(ABSTRACT | LEAF | ROOT), popUpActions.size() - getPopupAddOffset());
        return popUpActions;
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension nameDim = getNameFig().getMinimumSize();
        int w = nameDim.width;
        int h = nameDim.height + 65;
        return new Dimension(w, h);
    }

    @Override
    protected void setBoundsImpl(int x, int y, int w, int h) {
        int middle = w / 2;
        h = _h;
        Rectangle oldBounds = getBounds();
        getBigPort().setLocation(x + middle - getBigPort().getWidth() / 2, y + h - 65);
        getFigAt(HEAD_POSN).setLocation(x + middle - getFigAt(HEAD_POSN).getWidth() / 2, y + h - 65);
        getFigAt(BODY_POSN).setLocation(x + middle, y + h - 50);
        getFigAt(ARMS_POSN).setLocation(x + middle - getFigAt(ARMS_POSN).getWidth() / 2, y + h - 45);
        getFigAt(LEFT_LEG_POSN).setLocation(x + middle - getFigAt(LEFT_LEG_POSN).getWidth(), y + h - 35);
        getFigAt(RIGHT_LEG_POSN).setLocation(x + middle, y + h - 35);

        Dimension minTextSize = getNameFig().getMinimumSize();
        getNameFig().setBounds(x + middle - minTextSize.width / 2, y + h - minTextSize.height, minTextSize.width, minTextSize.height);

        updateEdges();
        _x = x;
        _y = y;
        _w = w;
        firePropChange("bounds", oldBounds, getBounds());
    }

    @Override
    public Object deepHitPort(int x, int y) {
        Object o = super.deepHitPort(x, y);
        if (o != null) {
            return o;
        }
        if (hit(new Rectangle(new Dimension(x, y)))) {
            return getOwner();
        }
        return null;
    }

    @Override
    public List getGravityPoints() {
        final int maxPoints = 20;
        List ret = new ArrayList();
        int cx = getFigAt(HEAD_POSN).getCenter().x;
        int cy = getFigAt(HEAD_POSN).getCenter().y;
        int radiusx = Math.round(getFigAt(HEAD_POSN).getWidth() / 2) + 1;
        int radiusy = Math.round(getFigAt(HEAD_POSN).getHeight() / 2) + 1;
        for (int i = 0; i < maxPoints; i++) {
            double angle = 2 * Math.PI / maxPoints * i;
            ret.add(new Point((int) (cx + Math.cos(angle) * radiusx), (int) (cy + Math.sin(angle) * radiusy)));
        }
        ret.add(new Point(((FigLine) getFigAt(LEFT_LEG_POSN)).getX2(), ((FigLine) getFigAt(LEFT_LEG_POSN)).getY2()));
        ret.add(new Point(((FigLine) getFigAt(RIGHT_LEG_POSN)).getX2(), ((FigLine) getFigAt(RIGHT_LEG_POSN)).getY2()));
        ret.add(new Point(((FigLine) getFigAt(ARMS_POSN)).getX1(), ((FigLine) getFigAt(ARMS_POSN)).getY1()));
        ret.add(new Point(((FigLine) getFigAt(ARMS_POSN)).getX2(), ((FigLine) getFigAt(ARMS_POSN)).getY2()));
        return ret;
    }

    @Override
    protected void modelChanged(PropertyChangeEvent mee) {
        super.modelChanged(mee);

        boolean damage = false;
        if (getOwner() == null) {
            return;
        }

        if (mee == null || "isAbstract".equals(mee.getPropertyName())) {
            updateAbstract();
            damage = true;
        }
        if (mee == null || "stereotype".equals(mee.getPropertyName())) {
            updateStereotypeText();
            damage = true;
        }
        if (mee != null && Model.getFacade().getStereotypes(getOwner()).contains(mee.getSource())) {
            updateStereotypeText();
            damage = true;
        }

        if (damage) {
            damage();
        }
    }

    @Override
    public void renderingChanged() {
        if (getOwner() != null) {
            updateAbstract();
        }
        super.renderingChanged();
        damage();
    }

    protected void updateAbstract() {
        Rectangle rect = getBounds();
        if (getOwner() == null) {
            return;
        }
        Object cls = getOwner();
        if (Model.getFacade().isAbstract(cls)) {
            getNameFig().setFont(getItalicLabelFont());
        } else {
            getNameFig().setFont(getLabelFont());
        }
        super.updateNameText();
        setBounds(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    protected void updateStereotypeText() {
        super.updateStereotypeText();
        if (!Model.getFacade().getStereotypes(getOwner()).isEmpty()) {
            Dimension stereoMin = getStereotypeFig().getMinimumSize();
            getStereotypeFig().setBounds(getBigPort().getCenter().x - getStereotypeFig().getWidth() / 2,
                    getBigPort().getY() + getBigPort().getHeight() + MIN_VERT_PADDING, stereoMin.width, stereoMin.height);
        } else {
            getStereotypeFig().setBounds(getBigPort().getCenter().x, getBigPort().getCenter().y, 0, 0);
        }
        damage();
    }

    static class ActorPortFigRect extends FigRect {
        private Fig parent;

        public ActorPortFigRect(int x, int y, int w, int h, Fig p) {
            super(x, y, w, h, null, null);
            parent = p;
        }

        @Override
        public List getGravityPoints() {
            return parent.getGravityPoints();
        }

        private static final long serialVersionUID = 5973857118854162659L;
    }
}
