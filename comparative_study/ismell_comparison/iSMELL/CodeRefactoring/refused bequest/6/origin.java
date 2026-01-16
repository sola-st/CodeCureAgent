public class UMLClassAttributeListModel
        extends UMLModelElementOrderedListModel2 {

    /**
     * Constructor for UMLClassifierStructuralFeatureListModel.
     */
    public UMLClassAttributeListModel() {
        super("feature");
    }

    /**
     * @see org.argouml.uml.ui.UMLModelElementListModel2#buildModelList()
     */
    protected void buildModelList() {
        if (getTarget() != null) {

            setAllElements(Model.getFacade().getAttributes(getTarget()));
        }
    }

    /**
     * @see org.argouml.uml.ui.UMLModelElementListModel2#isValidElement(Object)
     */
    protected boolean isValidElement(Object/*MBase*/ element) {
        return (Model.getFacade().getAttributes(getTarget()).contains(element));
    }

    /**
     * @see org.argouml.uml.ui.UMLModelElementOrderedListModel2#swap(int, int)
     */
    public void swap(int index1, int index2) {
        Object clss = getTarget();
        List c = new ArrayList(Model.getFacade().getAttributes(clss));
        Object mem1 = c.get(index1);
        Object mem2 = c.get(index2);
        List cc = new ArrayList(c);
        cc.remove(mem1);
        cc.remove(mem2);
        Model.getCoreHelper().setAttributes(clss, cc);
        c.set(index1, mem2);
        c.set(index2, mem1);
        Model.getCoreHelper().setAttributes(clss, c);
        buildModelList();
    }
}
