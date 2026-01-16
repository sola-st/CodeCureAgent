public class GoClassifierToSequenceDiagram extends AbstractPerspectiveRule {

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getRuleName()
     */
    @Override
    public String getRuleName() {
        return Translator.localize("misc.classifier.sequence-diagram");
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getChildren(java.lang.Object)
     */
    @Override
    public Collection getChildren(Object parent) {
        if (!Model.getFacade().isAClassifier(parent)) {
            return Collections.emptyList();
        }

        Collection collaborations = Model.getFacade().getCollaborations(parent);
        Project project = ProjectManager.getManager().getCurrentProject();
        List<ArgoDiagram> result = new ArrayList<>();

        for (ArgoDiagram diagram : project.getDiagrams()) {
            if (diagram instanceof UMLSequenceDiagram) {
                SequenceDiagramGraphModel graphModel = (SequenceDiagramGraphModel) ((UMLSequenceDiagram) diagram).getGraphModel();
                if (collaborations.contains(graphModel.getCollaboration())) {
                    result.add(diagram);
                }
            }
        }

        return result;
    }

    /**
     * @see org.argouml.ui.explorer.rules.PerspectiveRule#getDependencies(java.lang.Object)
     */
    @Override
    public Set getDependencies(Object parent) {
        // The original code has a TODO comment indicating an incomplete implementation.
        // This method currently returns an empty set to maintain consistency and safety.
        return Collections.emptySet();
    }
}
