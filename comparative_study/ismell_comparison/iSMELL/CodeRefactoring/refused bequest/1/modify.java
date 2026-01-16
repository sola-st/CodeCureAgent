// Factory class to handle property panel creation
class PropPanelFactory {
    private Map<Class, PropPanel> panelCache = new HashMap<>();

    // Method to create or retrieve a property panel
    public PropPanel getPropPanel(Object modelElement) {
        Class modelClass = modelElement.getClass();
        if (!panelCache.containsKey(modelClass)) {
            panelCache.put(modelClass, createPropPanel(modelElement));
        }
        return panelCache.get(modelClass);
    }

    // Method to encapsulate panel creation logic
    private PropPanel createPropPanel(Object modelElement) {
        if (modelElement instanceof UMLActivityDiagram) {
            return new PropPanelUMLActivityDiagram();
        }
        if (modelElement instanceof UMLClassDiagram) {
            return new PropPanelUMLClassDiagram();
        }
        if (modelElement instanceof UMLCollaborationDiagram) {
            return new PropPanelUMLCollaborationDiagram();
        }
        if (modelElement instanceof UMLDeploymentDiagram) {
            return new PropPanelUMLDeploymentDiagram();
        }
        if (modelElement instanceof UMLSequenceDiagram) {
            return new PropPanelUMLSequenceDiagram();
        }
        if (modelElement instanceof UMLStateDiagram) {
            return new PropPanelUMLStateDiagram();
        }
        if (modelElement instanceof UMLUseCaseDiagram) {
            return new PropPanelUMLUseCaseDiagram();
        }
        if (Model.getFacade().isASubmachineState(modelElement)) {
            return new PropPanelSubmachineState();
        }
        if (Model.getFacade().isASubactivityState(modelElement)) {
            return new PropPanelSubactivityState();
        }
        if (Model.getFacade().isAAbstraction(modelElement)) {
            return new PropPanelAbstraction();
        }
        if (Model.getFacade().isACallState(modelElement)) {
            return new PropPanelCallState();
        }
        if (Model.getFacade().isAActionState(modelElement)) {
            return new PropPanelActionState();
        }
        if (Model.getFacade().isAActivityGraph(modelElement)) {
            return new PropPanelActivityGraph();
        }
        if (Model.getFacade().isAActor(modelElement)) {
            return new PropPanelActor();
        }
        if (Model.getFacade().isAArgument(modelElement)) {
            return new PropPanelArgument();
        }
        if (Model.getFacade().isAAssociationClass(modelElement)) {
            return new PropPanelAssociationClass();
        }
        if (Model.getFacade().isAAssociationRole(modelElement)) {
            return new PropPanelAssociationRole();
        }
        if (Model.getFacade().isAAssociation(modelElement)) {
            return new PropPanelAssociation();
        }
        if (Model.getFacade().isAAssociationEndRole(modelElement)) {
            return new PropPanelAssociationEndRole();
        }
        if (Model.getFacade().isAAssociationEnd(modelElement)) {
            return new PropPanelAssociationEnd();
        }
        if (Model.getFacade().isAAttribute(modelElement)) {
            return new PropPanelAttribute();
        }
        if (Model.getFacade().isACallAction(modelElement)) {
            return new PropPanelCallAction();
        }
        if (Model.getFacade().isAClassifierInState(modelElement)) {
            return new PropPanelClassifierInState();
        }
        if (Model.getFacade().isAClass(modelElement)) {
            return new PropPanelClass();
        }
        if (Model.getFacade().isAClassifierRole(modelElement)) {
            return new PropPanelClassifierRole();
        }
        if (Model.getFacade().isACollaboration(modelElement)) {
            return new PropPanelCollaboration();
        }
        if (Model.getFacade().isAComment(modelElement)) {
            return new PropPanelComment();
        }
        if (Model.getFacade().isAComponent(modelElement)) {
            return new PropPanelComponent();
        }
        if (Model.getFacade().isAComponentInstance(modelElement)) {
            return new PropPanelComponentInstance();
        }
        if (Model.getFacade().isACompositeState(modelElement)) {
            return new PropPanelCompositeState();
        }
        if (Model.getFacade().isACreateAction(modelElement)) {
            return new PropPanelCreateAction();
        }
        if (Model.getFacade().isAEnumeration(modelElement)) {
            return new PropPanelEnumeration();
        }
        if (Model.getFacade().isADataType(modelElement)) {
            return new PropPanelDataType();
        }
        if (Model.getFacade().isADestroyAction(modelElement)) {
            return new PropPanelDestroyAction();
        }
        if (Model.getFacade().isAEnumerationLiteral(modelElement)) {
            return new PropPanelEnumerationLiteral();
        }
        if (Model.getFacade().isAExtend(modelElement)) {
            return new PropPanelExtend();
        }
        if (Model.getFacade().isAExtensionPoint(modelElement)) {
            return new PropPanelExtensionPoint();
        }
        if (Model.getFacade().isAFinalState(modelElement)) {
            return new PropPanelFinalState();
        }
        if (Model.getFacade().isAFlow(modelElement)) {
            return new PropPanelFlow();
        }
        if (Model.getFacade().isAGeneralization(modelElement)) {
            return new PropPanelGeneralization();
        }
        if (Model.getFacade().isAGuard(modelElement)) {
            return new PropPanelGuard();
        }
        if (Model.getFacade().isAInclude(modelElement)) {
            return new PropPanelInclude();
        }
        if (Model.getFacade().isAInteraction(modelElement)) {
            return new PropPanelInteraction();
        }
        if (Model.getFacade().isAInterface(modelElement)) {
            return new PropPanelInterface();
        }
        if (Model.getFacade().isALink(modelElement)) {
            return new PropPanelLink();
        }
        if (Model.getFacade().isALinkEnd(modelElement)) {
            return new PropPanelLinkEnd();
        }
        if (Model.getFacade().isAMessage(modelElement)) {
            return new PropPanelMessage();
        }
        if (Model.getFacade().isAMethod(modelElement)) {
            return new PropPanelMethod();
        }
        if (Model.getFacade().isAModel(modelElement)) {
            return new PropPanelModel();
        }
        if (Model.getFacade().isANode(modelElement)) {
            return new PropPanelNode();
        }
        if (Model.getFacade().isANodeInstance(modelElement)) {
            return new PropPanelNodeInstance();
        }
        if (Model.getFacade().isAObject(modelElement)) {
            return new PropPanelObject();
        }
        if (Model.getFacade().isAObjectFlowState(modelElement)) {
            return new PropPanelObjectFlowState();
        }
        if (Model.getFacade().isAOperation(modelElement)) {
            return new PropPanelOperation();
        }
        if (Model.getFacade().isAPackage(modelElement)) {
            return new PropPanelPackage();
        }
        if (Model.getFacade().isAParameter(modelElement)) {
            return new PropPanelParameter();
        }
        if (Model.getFacade().isAPartition(modelElement)) {
            return new PropPanelPartition();
        }
        if (Model.getFacade().isAPermission(modelElement)) {
            return new PropPanelPermission();
        }
        if (Model.getFacade().isAPseudostate(modelElement)) {
            return new PropPanelPseudostate();
        }
        if (Model.getFacade().isAReception(modelElement)) {
            return new PropPanelReception();
        }
        if (Model.getFacade().isAReturnAction(modelElement)) {
            return new PropPanelReturnAction();
        }
        if (Model.getFacade().isASendAction(modelElement)) {
            return new PropPanelSendAction();
        }
        if (Model.getFacade().isASignal(modelElement)) {
            return new PropPanelSignal();
        }
        if (Model.getFacade().isASimpleState(modelElement)) {
            return new PropPanelSimpleState();
        }
        if (Model.getFacade().isAStateMachine(modelElement)) {
            return new PropPanelStateMachine();
        }
        if (Model.getFacade().isAStereotype(modelElement)) {
            return new PropPanelStereotype();
        }
        if (Model.getFacade().isAStimulus(modelElement)) {
            return new PropPanelStimulus();
        }
        if (Model.getFacade().isAStubState(modelElement)) {
            return new PropPanelStubState();
        }
        if (Model.getFacade().isASubsystem(modelElement)) {
            return new PropPanelSubsystem();
        }
        if (Model.getFacade().isASynchState(modelElement)) {
            return new PropPanelSynchState();
        }
        if (Model.getFacade().isATaggedValue(modelElement)) {
            return new PropPanelTaggedValue();
        }
        if (Model.getFacade().isATagDefinition(modelElement)) {
            return new PropPanelTagDefinition();
        }
        if (Model.getFacade().isATerminateAction(modelElement)) {
            return new PropPanelTerminateAction();
        }
        if (Model.getFacade().isATransition(modelElement)) {
            return new PropPanelTransition();
        }
        if (Model.getFacade().isAUninterpretedAction(modelElement)) {
            return new PropPanelUninterpretedAction();
        }
        if (Model.getFacade().isAUsage(modelElement)) {
            return new PropPanelUsage();
        }
        if (Model.getFacade().isAUseCase(modelElement)) {
            return new PropPanelUseCase();
        }
        if (Model.getFacade().isACallEvent(modelElement)) {
            return new PropPanelCallEvent();
        }
        if (Model.getFacade().isAChangeEvent(modelElement)) {
            return new PropPanelChangeEvent();
        }
        if (Model.getFacade().isASignalEvent(modelElement)) {
            return new PropPanelSignalEvent();
        }
        if (Model.getFacade().isATimeEvent(modelElement)) {
            return new PropPanelTimeEvent();
        }
        if (Model.getFacade().isADependency(modelElement)) {
            return new PropPanelDependency();
        }
        if (modelElement instanceof FigText) {
            return new PropPanelString();
        }
        return null;
    }
}

// Simplified version of TabProps using the factory for property panel management
public class TabProps extends AbstractArgoJPanel implements TabModelTarget, TargetListener, ArgoModuleEventListener {
    private PropPanelFactory panelFactory = new PropPanelFactory();
    private Object target;
    private JPanel blankPanel = new JPanel();
    private JPanel lastPanel;

    private EventListenerList listenerList = new EventListenerList();

    public TabProps() {
        this("tab.properties", "ui.PropPanel");
    }

    public TabProps(String tabName, String panelClassBase) {
        super(tabName);
        TargetManager.getInstance().addTarget(this);
        setOrientation(ConfigLoader.getTabPropsOrientation());
        setLayout(new BorderLayout());
        ArgoEventPump.addListener(ArgoEventTypes.ANY_MODULE_EVENT, this);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        ArgoEventPump.removeListener(ArgoEventTypes.ANY_MODULE_EVENT, this);
    }

    public void setOrientation(Orientation orientation) {
        super.setOrientation(orientation);
        Enumeration pps = Collections.enumeration(panelFactory.panelCache.values());
        while (pps.hasMoreElements()) {
            Object o = pps.nextElement();
            if (o instanceof Orientable) {
                Orientable orientable = (Orientable) o;
                orientable.setOrientation(orientation);
            }
        }
    }

    public void setTarget(Object t) {
        this.target = t;
        updatePanel();
    }

    private void updatePanel() {
        removeAll();
        PropPanel currentPanel = panelFactory.getPropPanel(target);
        if (currentPanel != null) {
            add(currentPanel, BorderLayout.CENTER);
            lastPanel = currentPanel;
        } else {
            add(blankPanel, BorderLayout.CENTER);
            lastPanel = blankPanel;
        }
        validate();
        repaint();
    }

    public void refresh() {
        setTarget(TargetManager.getInstance().getTarget());
    }

    public boolean shouldBeEnabled(Object t) {
        t = (t instanceof Fig) ? ((Fig) t).getOwner() : t;
        return (t instanceof Diagram || Model.getFacade().isAModelElement(t));
    }

    // Event listener methods
    public void targetAdded(TargetEvent e) {
        setTarget(TargetManager.getInstance().getSingleTarget());
        fireTargetAdded(e);
    }

    public void targetRemoved(TargetEvent e) {
        setTarget(TargetManager.getInstance().getSingleTarget());
        fireTargetRemoved(e);
    }

    public void targetSet(TargetEvent e) {
        setTarget(TargetManager.getInstance().getSingleTarget());
        fireTargetSet(e);
    }

    private void addTargetListener(TargetListener listener) {
        listenerList.add(TargetListener.class, listener);
    }

    private void removeTargetListener(TargetListener listener) {
        listenerList.remove(TargetListener.class, listener);
    }

    private void fireTargetSet(TargetEvent targetEvent) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TargetListener.class) {
                ((TargetListener) listeners[i + 1]).targetSet(targetEvent);
            }
        }
    }

    private void fireTargetAdded(TargetEvent targetEvent) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TargetListener.class) {
                ((TargetListener) listeners[i + 1]).targetAdded(targetEvent);
            }
        }
    }

    private void fireTargetRemoved(TargetEvent targetEvent) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TargetListener.class) {
                ((TargetListener) listeners[i + 1]).targetRemoved(targetEvent);
            }
        }
    }

    public void moduleLoaded(ArgoModuleEvent event) {
        if (event.getSource() instanceof PluggablePropertyPanel) {
            PluggablePropertyPanel p = (PluggablePropertyPanel) event.getSource();
            panelFactory.getPropPanel(p.getClassForPanel()); // Ensure panel is created and cached
        }
    }

    public void moduleUnloaded(ArgoModuleEvent event) {
        if (event.getSource() instanceof PluggablePropertyPanel) {
            PluggablePropertyPanel p = (PluggablePropertyPanel) event.getSource();
            panelFactory.panelCache.remove(p.getClassForPanel());
        }
    }

    public void moduleEnabled(ArgoModuleEvent event) {
        if (event.getSource() instanceof PluggablePropertyPanel) {
            PluggablePropertyPanel p = (PluggablePropertyPanel) event.getSource();
            PropPanel panel = panelFactory.getPropPanel(p.getClassForPanel());
            if (panel != null) {
                panel.setEnabled(true);
            }
        }
    }

    public void moduleDisabled(ArgoModuleEvent event) {
        if (event.getSource() instanceof PluggablePropertyPanel) {
            PluggablePropertyPanel p = (PluggablePropertyPanel) event.getSource();
            PropPanel panel = panelFactory.getPropPanel(p.getClassForPanel());
            if (panel != null) {
                panel.setEnabled(false);
            }
        }
    }
}
