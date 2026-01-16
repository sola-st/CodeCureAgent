import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// 定义 PerspectiveRule 接口
interface PerspectiveRule {
    String getRuleName();
    Collection getChildren(Object parent);
    Set getDependencies(Object parent);
}

// Dummy implementation of Model and its Facade for illustration purposes
class Model {
    public static Facade getFacade() {
        return new Facade();
    }

    static class Facade {
        public boolean isAUseCase(Object obj) {
            // Dummy check for use case
            return obj instanceof UseCase;
        }

        public Collection getExtensionPoints(Object useCase) {
            // Dummy implementation for getting extension points
            return new HashSet();
        }
    }
}

// Dummy UseCase class for illustration purposes
class UseCase {}

// Dummy Translator class for illustration purposes
class Translator {
    public static String localize(String key) {
        return key;
    }
}

// Refactored class implementing PerspectiveRule
public class UseCaseExtensionPointProvider implements PerspectiveRule {
    @Override
    public String getRuleName() {
        return Translator.localize("misc.use-case.extension-point");
    }

    @Override
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAUseCase(parent)) {
            return Model.getFacade().getExtensionPoints(parent);
        }
        return new HashSet();
    }

    @Override
    public Set getDependencies(Object parent) {
        if (Model.getFacade().isAUseCase(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return new HashSet();
    }
}
