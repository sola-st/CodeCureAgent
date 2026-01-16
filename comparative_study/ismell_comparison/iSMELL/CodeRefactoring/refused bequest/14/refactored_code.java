// Extracted DiagramEdgeHandler class for managing edges
class DiagramEdgeHandler {
    private Diagram diagram;

    public DiagramEdgeHandler(Diagram diagram) {
        this.diagram = diagram;
    }

    public Collection getEdges() {
        return Collections.unmodifiableCollection(diagram.getEdges());
    }

    public Set getDependencies() {
        return Collections.singleton(diagram);
    }
}

// Refactored GoDiagramToEdge class without inheritance
public class GoDiagramToEdge {
    private DiagramEdgeHandler handler;

    public GoDiagramToEdge(Diagram diagram) {
        this.handler = new DiagramEdgeHandler(diagram);
    }

    public String getRuleName() {
        return Translator.localize("misc.diagram.edge");
    }

    public Collection getChildren() {
        return handler.getEdges();
    }

    public Set getDependencies() {
        return handler.getDependencies();
    }
}
