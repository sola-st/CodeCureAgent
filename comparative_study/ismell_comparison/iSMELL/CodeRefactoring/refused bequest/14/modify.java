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

    public Collection getChildren(Object parent) {
        if (parent instanceof Diagram) {
            return handler.getEdges();
        }
        return Collections.emptyList();
    }

    public Set getDependencies(Object parent) {
        if (parent instanceof Diagram) {
            return handler.getDependencies();
        }
        return Collections.emptySet();
    }
}
