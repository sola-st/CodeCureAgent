public class SelectionActionState {

    private static final Logger LOG = Logger.getLogger(SelectionActionState.class);

    private static Icon trans = ResourceLoaderWrapper.lookupIconResource("Transition");
    private static Icon transDown = ResourceLoaderWrapper.lookupIconResource("TransitionDown");

    private Fig content;
    private boolean showIncomingLeft = true;
    private boolean showIncomingAbove = true;
    private boolean showOutgoingRight = true;
    private boolean showOutgoingBelow = true;

    public SelectionActionState(Fig f) {
        this.content = f;
    }

    public void setOutgoingButtonEnabled(boolean b) {
        this.showOutgoingRight = b;
        this.showIncomingAbove = b;
    }

    public void setIncomingButtonEnabled(boolean b) {
        this.showIncomingLeft = b;
        this.showOutgoingBelow = b;
    }

    // Simplified method to check hits for buttons and respond accordingly
    public Handle hitHandle(Rectangle r) {
        Handle h = new Handle();
        // Check if the point is within any of the defined button areas
        // Example: Outgoing right button
        if (showOutgoingRight && withinButtonBounds(r, calculateButtonPosition("right"))) {
            h.index = 12;
            h.instructions = "Add an outgoing transition";
        }
        // Additional conditions for other buttons...

        return h;
    }

    private Point calculateButtonPosition(String direction) {
        int x = content.getX();
        int y = content.getY();
        int width = content.getWidth();
        int height = content.getHeight();
        // Calculate and return the button position based on direction
        switch (direction) {
            case "right": return new Point(x + width, y + height / 2);
            // Handle other directions...
            default: return new Point(x, y);
        }
    }

    private boolean withinButtonBounds(Rectangle rect, Point btnPos) {
        // Check if the rectangle intersects with the button area
        // Assuming a fixed size for buttons for simplicity
        int buttonSize = 10; // Example size
        Rectangle buttonRect = new Rectangle(btnPos.x - buttonSize / 2, btnPos.y - buttonSize / 2, buttonSize, buttonSize);
        return rect.intersects(buttonRect);
    }

    public void paintButtons(Graphics g) {
        // Paint buttons based on visibility flags
        if (showOutgoingRight) paintButton(g, "right");
        // Similar for other buttons
    }

    private void paintButton(Graphics g, String direction) {
        Point pos = calculateButtonPosition(direction);
        // Paint a button at the given position
        g.fillRect(pos.x - 5, pos.y - 5, 10, 10);
    }
}
