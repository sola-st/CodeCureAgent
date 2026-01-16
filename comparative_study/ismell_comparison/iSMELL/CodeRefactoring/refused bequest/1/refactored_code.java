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
        // Use switch or if-else chain based on the instance type of modelElement
        // Return a new instance of the corresponding panel class
        if (modelElement instanceof UMLClass) {
            return new PropPanelUMLClass();
        }
        // Additional checks and creations for other model types
    }
}

// Simplified version of TabProps using the factory for property panel management
public class TabProps extends AbstractArgoJPanel implements TabModelTarget {
    private PropPanelFactory panelFactory = new PropPanelFactory();
    private Object target;

    // Constructor and other necessary methods remain with minor modifications
    public void setTarget(Object t) {
        this.target = t;  // Simplified target setting logic
        updatePanel();
    }

    // Method to update the current panel based on the target
    private void updatePanel() {
        removeAll();
        PropPanel currentPanel = panelFactory.getPropPanel(target);
        if (currentPanel != null) {
            add(currentPanel, BorderLayout.CENTER);
        }
        validate();
        repaint();
    }
}
