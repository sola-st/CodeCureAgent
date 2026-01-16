public class GoClassifierToSequenceDiagram extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getRuleName()
     */
    public String getRuleName() {
        return Translator.localize ("misc.classifier.sequence-diagram");
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(java.lang.Object)
     */
    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAClassifier(parent)) {
            Collection col = Model.getFacade().getCollaborations(parent);
            List ret = new ArrayList();
            Project p = ProjectManager.getManager().getCurrentProject();
            Iterator it = p.getDiagrams().iterator();

            while (it.hasNext()) {
                ArgoDiagram diagram = (ArgoDiagram) it.next();
                if (diagram instanceof UMLSequenceDiagram
                        && col.contains(((SequenceDiagramGraphModel)
                        ((UMLSequenceDiagram) diagram).getGraphModel())
                        .getCollaboration())) {
                    ret.add(diagram);
                }
            }

            return ret;
        }

        return null;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    public Set getDependencies(Object parent) {
        // TODO: What?
        return null;
    }
}
