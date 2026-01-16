// Refactored class focusing only on its unique functionalities
public class GoClassToAssociatedClass extends PerspectiveRule {

    @Override
    public String getRuleName() {
        return Translator.localize("misc.class.associated-class");
    }

    @Override
    public Collection getChildren(Object parent) {
        return Model.getFacade().isAClass(parent) ? Model.getFacade().getAssociatedClasses(parent) : Collections.emptyList();
    }

    @Override
    public Set getDependencies(Object parent) {
        return Model.getFacade().isAClass(parent) ? Collections.singleton(parent) : Collections.emptySet();
    }
}
