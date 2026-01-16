import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Dummy implementation of Model and its Facade for illustration purposes
class Model {
    public static Facade getFacade() {
        return new Facade();
    }

    public static CoreHelper getCoreHelper() {
        return new CoreHelper();
    }

    static class Facade {
        public List getAttributes(Object obj) {
            // Dummy implementation for getting attributes
            return new ArrayList();
        }
    }

    static class CoreHelper {
        public void setAttributes(Object target, List attributes) {
            // Dummy implementation for setting attributes
        }
    }
}

// Refactored class implementing attribute management
public class ClassAttributeManager {
    private Object target; // Assuming this replaces getTarget() method usage

    public ClassAttributeManager(Object target) {
        this.target = target;
    }

    /**
     * Builds the list of attributes for the class.
     * @return List of attributes
     */
    public List buildAttributeList() {
        if (this.target != null) {
            return new ArrayList(Model.getFacade().getAttributes(this.target));
        }
        return new ArrayList();
    }

    /**
     * Checks if an element is a valid attribute of the class.
     * @param element The element to check
     * @return true if the element is a valid attribute, false otherwise
     */
    public boolean isValidAttribute(Object element) {
        return buildAttributeList().contains(element);
    }

    /**
     * Swaps two attributes in the list.
     * @param index1 Index of the first attribute
     * @param index2 Index of the second attribute
     */
    public void swapAttributes(int index1, int index2) {
        List attributes = buildAttributeList();
        Collections.swap(attributes, index1, index2);
        Model.getCoreHelper().setAttributes(this.target, attributes);
        buildAttributeList();  // Refresh the list after swapping
    }

    /**
     * Sets a new target object.
     * @param newTarget The new target object
     */
    public void setTarget(Object newTarget) {
        this.target = newTarget;
    }

    /**
     * Gets the current target object.
     * @return The current target object
     */
    public Object getTarget() {
        return this.target;
    }
}
