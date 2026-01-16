// Refactored class without inheritance from UMLModelElementOrderedListModel2
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
    }
}
