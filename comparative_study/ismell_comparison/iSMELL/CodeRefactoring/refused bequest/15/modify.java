// Separate class to handle model element contents and dependencies
class ModelElementContentHandler {
    public Collection getModelElementContents(Object parent) {
        if (Model.getFacade().isAModelElement(parent)) {
            return Model.getFacade().getModelElementContents(parent);
        }
        return Collections.emptyList();
    }

    public Set getModelElementDependencies(Object parent) {
        Set set = new HashSet();
        if (Model.getFacade().isAModelElement(parent)) {
            set.add(parent);
            set.addAll(Model.getFacade().getModelElementContents(parent));
        }
        return set;
    }
}

// Refactored GoModelElementToContents class without inheritance
public class GoModelElementToContents {
    private ModelElementContentHandler handler;

    public GoModelElementToContents() {
        this.handler = new ModelElementContentHandler();
    }

    public String getRuleName() {
        return Translator.localize("misc.model-element.contents");
    }

    public Collection getChildren(Object parent) {
        return handler.getModelElementContents(parent);
    }

    public Set getDependencies(Object parent) {
        return handler.getModelElementDependencies(parent);
    }
}
