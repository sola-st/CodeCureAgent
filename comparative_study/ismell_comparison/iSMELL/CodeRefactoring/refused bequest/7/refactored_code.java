public class FigActor {

    private ActorPortFigRect bigPort;
    private FigCircle head;
    private FigLine body, arms, leftLeg, rightLeg;
    private FigText nameFig;
    private FigStereoType stereotypeFig;

    public FigActor() {
        initializeComponents();
    }

    private void initializeComponents() {
        bigPort = new ActorPortFigRect(10, 10, 15, 60);
        bigPort.setVisible(false);
        head = new FigCircle(10, 10, 15, 15, Color.black, Color.white);
        body = new FigLine(20, 25, 20, 40, Color.black);
        arms = new FigLine(10, 30, 30, 30, Color.black);
        leftLeg = new FigLine(20, 40, 15, 55, Color.black);
        rightLeg = new FigLine(20, 40, 25, 55, Color.black);
        nameFig = new FigText(5, 55, 35, 20, "Actor Name", false, false, 0);
        stereotypeFig = new FigStereoType(getBigPort().getCenter().x,
                getBigPort().getCenter().y);

        // Add Figs to this composite Fig in back-to-front order
        addFig(bigPort);
        addFig(nameFig);
        addFig(head);
        addFig(body);
        addFig(arms);
        addFig(leftLeg);
        addFig(rightLeg);
        addFig(stereotypeFig);
    }

    public void setLineWidth(int width) {
        head.setLineWidth(width);
        body.setLineWidth(width);
        arms.setLineWidth(width);
        leftLeg.setLineWidth(width);
        rightLeg.setLineWidth(width);
    }

    public void setFilled(boolean filled) {
        head.setFilled(filled);  // Only the head should be filled
    }

    // Implement other necessary methods similar to those in FigNodeModelElement if needed
}
