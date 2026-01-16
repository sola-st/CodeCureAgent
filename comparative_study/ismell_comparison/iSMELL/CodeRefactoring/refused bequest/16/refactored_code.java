// New class to handle FigJunctionState specific functionalities
class FigJunctionStateHandler {
    private static final int X = 0;
    private static final int Y = 0;
    private static final int WIDTH = 32;
    private static final int HEIGHT = 32;

    private FigDiamond bigPort;
    private FigDiamond head;

    public FigJunctionStateHandler() {
        bigPort = new FigDiamond(X, Y, WIDTH, HEIGHT, false, Color.cyan, Color.cyan);
        head = new FigDiamond(X, Y, WIDTH, HEIGHT, false, Color.black, Color.white);
    }

    public void setBigPort(FigDiamond bigPort) {
        this.bigPort = bigPort;
    }

    public FigDiamond getBigPort() {
        return bigPort;
    }

    public void setLineColor(Color col) {
        head.setLineColor(col);
    }

    public Color getLineColor() {
        return head.getLineColor();
    }

    public void setFillColor(Color col) {
        head.setFillColor(col);
    }

    public Color getFillColor() {
        return head.getFillColor();
    }

    public void setLineWidth(int w) {
        head.setLineWidth(w);
    }

    public int getLineWidth() {
        return head.getLineWidth();
    }

    public boolean isResizable() {
        return false;
    }

    public Point getClosestPoint(Point anotherPt, Rectangle r) {
        int[] xs = {r.x + r.width / 2, r.x + r.width, r.x + r.width / 2, r.x, r.x + r.width / 2};
        int[] ys = {r.y, r.y + r.height / 2, r.y + r.height, r.y + r.height / 2, r.y};
        return Geometry.ptClosestTo(xs, ys, 5, anotherPt);
    }
}

// Refactored FigJunctionState class without inheritance
public class FigJunctionState {
    private FigJunctionStateHandler handler;

    public FigJunctionState() {
        handler = new FigJunctionStateHandler();
        handler.setBigPort(new FigDiamond(0, 0, 32, 32, false, Color.cyan, Color.cyan));
    }

    public FigJunctionState(GraphModel gm, Object node) {
        this();
        // Handle setting owner if needed
    }

    public Object clone() {
        FigJunctionState figClone = new FigJunctionState();
        figClone.handler = this.handler; // Perform deep copy if necessary
        return figClone;
    }

    public void setLineColor(Color col) {
        handler.setLineColor(col);
    }

    public Color getLineColor() {
        return handler.getLineColor();
    }

    public void setFillColor(Color col) {
        handler.setFillColor(col);
    }

    public Color getFillColor() {
        return handler.getFillColor();
    }

    public void setLineWidth(int w) {
        handler.setLineWidth(w);
    }

    public int getLineWidth() {
        return handler.getLineWidth();
    }

    public boolean isResizable() {
        return handler.isResizable();
    }

    public Point getClosestPoint(Point anotherPt) {
        Rectangle r = new Rectangle(0, 0, 32, 32); // Example rectangle, should be the actual bounds
        return handler.getClosestPoint(anotherPt, r);
    }

    public void mouseClicked(MouseEvent me) {
        // Handle mouse clicked event
    }
}
