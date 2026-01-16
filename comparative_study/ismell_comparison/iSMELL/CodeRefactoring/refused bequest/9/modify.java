import org.apache.log4j.Logger;
import org.tigris.gef.base.CmdCreateNode;
import org.tigris.gef.base.CmdSetMode;
import org.tigris.gef.graph.GraphModel;
import org.tigris.gef.presentation.FigNode;
import org.tigris.gef.ui.RadioAction;

import java.beans.PropertyVetoException;

public class UMLUseCaseDiagram extends UMLDiagram {

    private static final Logger LOG = Logger.getLogger(UMLUseCaseDiagram.class);

    private DiagramActions diagramActions;

    public UMLUseCaseDiagram() {
        try {
            setName(getNewDiagramName());
        } catch (PropertyVetoException pve) { }
        diagramActions = new DiagramActions();
    }

    public UMLUseCaseDiagram(Object m) {
        this();
        if (!Model.getFacade().isANamespace(m)) {
            throw new IllegalArgumentException("Object must be a namespace.");
        }
        setNamespace(m);
        initializeDiagram();
    }

    public UMLUseCaseDiagram(String name, Object namespace) {
        this(namespace);
        try {
            setName(name);
        } catch (PropertyVetoException v) { }
    }

    public void setNamespace(Object handle) {
        if (!Model.getFacade().isANamespace(handle)) {
            LOG.error("Illegal argument. Object " + handle + " is not a namespace");
            throw new IllegalArgumentException("Illegal argument. Object " + handle + " is not a namespace");
        }
        Object m = handle;
        super.setNamespace(m);

        UseCaseDiagramGraphModel gm = new UseCaseDiagramGraphModel();
        gm.setHomeModel(m);
        LayerPerspective lay = new LayerPerspectiveMutable(Model.getFacade().getName(m), gm);
        UseCaseDiagramRenderer rend = new UseCaseDiagramRenderer();
        lay.setGraphNodeRenderer(rend);
        lay.setGraphEdgeRenderer(rend);
        setLayer(lay);
    }

    private void initializeDiagram() {
        UseCaseDiagramGraphModel graphModel = new UseCaseDiagramGraphModel();
        graphModel.setHomeModel(getNamespace());
        setGraphModel(graphModel);

        LayerPerspective lay = new LayerPerspectiveMutable(Model.getFacade().getName(getNamespace()), graphModel);
        UseCaseDiagramRenderer renderer = new UseCaseDiagramRenderer();
        lay.setGraphNodeRenderer(renderer);
        lay.setGraphEdgeRenderer(renderer);
        setLayer(lay);
    }

    protected Object[] getUmlActions() {
        return diagramActions.getActions();
    }

    protected String getNewDiagramName() {
        String name = getLabelName() + " " + getNextDiagramSerial();
        if (!(ProjectManager.getManager().getCurrentProject().isValidDiagramName(name))) {
            name = getNewDiagramName();
        }
        return name;
    }

    public String getLabelName() {
        return Translator.localize("label.usecase-diagram");
    }

    public boolean isRelocationAllowed(Object base) {
        return false;
    }

    public boolean relocate(Object base) {
        return false;
    }
}

class DiagramActions {

    private Action actionActor;
    private Action actionUseCase;
    private Action actionAssociation;
    private Action actionAggregation;
    private Action actionComposition;
    private Action actionUniAssociation;
    private Action actionUniAggregation;
    private Action actionUniComposition;
    private Action actionGeneralize;
    private Action actionExtend;
    private Action actionInclude;
    private Action actionDependency;
    private Action actionExtensionPoint;

    Object[] getActions() {
        return new Object[]{
                getActionActor(),
                getActionUseCase(),
                null,
                getAssociationActions(),
                getActionDependency(),
                getActionGeneralize(),
                getActionExtend(),
                getActionInclude(),
                null,
                getActionExtensionPoint()
        };
    }

    private Object[] getAssociationActions() {
        return new Object[]{
                getActionAssociation(),
                getActionUniAssociation(),
                getActionAggregation(),
                getActionUniAggregation(),
                getActionComposition(),
                getActionUniComposition(),
        };
    }

    protected Action getActionActor() {
        if (actionActor == null) {
            actionActor = new RadioAction(new CmdCreateNode(
                    Model.getMetaTypes().getActor(), "button.new-actor"));
        }
        return actionActor;
    }

    protected Action getActionUseCase() {
        if (actionUseCase == null) {
            actionUseCase = new RadioAction(new CmdCreateNode(
                    Model.getMetaTypes().getUseCase(), "button.new-usecase"));
        }
        return actionUseCase;
    }

    protected Action getActionAssociation() {
        if (actionAssociation == null) {
            actionAssociation = new RadioAction(
                    new ActionSetAddAssociationMode(
                            Model.getAggregationKind().getNone(),
                            false,
                            "button.new-association"));
        }
        return actionAssociation;
    }

    protected Action getActionAggregation() {
        if (actionAggregation == null) {
            actionAggregation = new RadioAction(
                    new ActionSetAddAssociationMode(
                            Model.getAggregationKind().getAggregate(),
                            false,
                            "button.new-aggregation"));
        }
        return actionAggregation;
    }

    protected Action getActionComposition() {
        if (actionComposition == null) {
            actionComposition = new RadioAction(
                    new ActionSetAddAssociationMode(
                            Model.getAggregationKind().getComposite(),
                            false,
                            "button.new-composition"));
        }
        return actionComposition;
    }

    protected Action getActionUniAssociation() {
        if (actionUniAssociation == null) {
            actionUniAssociation = new RadioAction(
                    new ActionSetAddAssociationMode(
                            Model.getAggregationKind().getNone(),
                            true,
                            "button.new-uniassociation"));
        }
        return actionUniAssociation;
    }

    protected Action getActionUniAggregation() {
        if (actionUniAggregation == null) {
            actionUniAggregation = new RadioAction(
                    new ActionSetAddAssociationMode(
                            Model.getAggregationKind().getAggregate(),
                            true,
                            "button.new-uniaggregation"));
        }
        return actionUniAggregation;
    }

    protected Action getActionUniComposition() {
        if (actionUniComposition == null) {
            actionUniComposition = new RadioAction(
                    new ActionSetAddAssociationMode(
                            Model.getAggregationKind().getComposite(),
                            true,
                            "button.new-unicomposition"));
        }
        return actionUniComposition;
    }

    protected Action getActionGeneralize() {
        if (actionGeneralize == null) {
            actionGeneralize = new RadioAction(
                    new CmdSetMode(
                            ModeCreatePolyEdge.class,
                            "edgeClass",
                            Model.getMetaTypes().getGeneralization(),
                            "button.new-generalization"));
        }
        return actionGeneralize;
    }

    protected Action getActionExtend() {
        if (actionExtend == null) {
            actionExtend = new RadioAction(
                    new CmdSetMode(
                            ModeCreatePolyEdge.class,
                            "edgeClass",
                            Model.getMetaTypes().getExtend(),
                            "button.new-extend"));
        }
        return actionExtend;
    }

    protected Action getActionInclude() {
        if (actionInclude == null) {
            actionInclude = new RadioAction(
                    new CmdSetMode(
                            ModeCreatePolyEdge.class,
                            "edgeClass",
                            Model.getMetaTypes().getInclude(),
                            "button.new-include"));
        }
        return actionInclude;
    }

    protected Action getActionDependency() {
        if (actionDependency == null) {
            actionDependency = new RadioAction(
                    new CmdSetMode(
                            ModeCreatePolyEdge.class,
                            "edgeClass",
                            Model.getMetaTypes().getDependency(),
                            "button.new-dependency"));
        }
        return actionDependency;
    }

    protected Action getActionExtensionPoint() {
        if (actionExtensionPoint == null) {
            actionExtensionPoint = ActionAddExtensionPoint.singleton();
        }
        return actionExtensionPoint;
    }
}
