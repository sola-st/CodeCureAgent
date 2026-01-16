public class UMLUseCaseDiagram extends UMLDiagram {

    private static final Logger LOG = Logger.getLogger(UMLUseCaseDiagram.class);

    /**
     * Action handling moved to a separate class to streamline UMLUseCaseDiagram and ensure
     * it uses its inheritance more appropriately without unused inheritance.
     */
    private DiagramActions diagramActions;

    /**
     * Construct a new UMLUseCaseDiagram within a defined namespace.
     * This constructor initializes the diagram with the specified model.
     *
     * @param m the namespace model for this diagram
     */
    public UMLUseCaseDiagram(Object m) {
        if (!Model.getFacade().isANamespace(m)) {
            throw new IllegalArgumentException("Object must be a namespace.");
        }
        setNamespace(m);
        diagramActions = new DiagramActions(m);
        initializeDiagram();
    }

    /**
     * Initializes the graph model and layers for the diagram.
     */
    private void initializeDiagram() {
        UseCaseDiagramGraphModel graphModel = new UseCaseDiagramGraphModel();
        graphModel.setHomeModel(getNamespace());
        setGraphModel(graphModel);

        LayerPerspective lay = new LayerPerspectiveMutable(
                Model.getFacade().getName(getNamespace()), graphModel);
        UseCaseDiagramRenderer renderer = new UseCaseDiagramRenderer();
        lay.setGraphNodeRenderer(renderer);
        lay.setGraphEdgeRenderer(renderer);
        setLayer(lay);
    }

    /**
     * Get the actions for building the diagram toolbar.
     * Delegates action creation to DiagramActions class.
     *
     * @return an array of toolbar actions
     */
    protected Object[] getUmlActions() {
        return diagramActions.getActions();
    }
}

/**
 * Handles the creation and management of actions related to the UMLUseCaseDiagram.
 */
class DiagramActions {
    private Action actionActor, actionUseCase, actionAssociation, actionDependency,
            actionGeneralize, actionExtend, actionInclude, actionExtensionPoint;

    private Object namespace;

    DiagramActions(Object namespace) {
        this.namespace = namespace;
    }

    Object[] getActions() {
        return new Object[] {
                getActionActor(), getActionUseCase(), null, getActionAssociation(),
                getActionDependency(), getActionGeneralize(), getActionExtend(),
                getActionInclude(), null, getActionExtensionPoint()
        };
    }

    // Methods to initialize actions (getActionActor, getActionUseCase, etc.)
}
