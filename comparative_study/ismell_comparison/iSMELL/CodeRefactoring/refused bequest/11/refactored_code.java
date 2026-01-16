public class GoClassifierToSequenceDiagram extends AbstractPerspectiveRule {

    /**
     * This method ensures the rule name is specific to sequence diagrams,
     * which specializes the generic functionality provided by the superclass.
     */
    @Override
    public String getRuleName() {
        return Translator.localize("misc.classifier.sequence-diagram");
    }

    /**
     * This overridden method extends the superclass by filtering the diagrams
     * specifically for sequence diagrams related to the classifier, enhancing
     * the generic functionality for a specific type of UML diagram.
     */
    @Override
    public Collection getChildren(Object parent) {
        if (!Model.getFacade().isAClassifier(parent)) {
            return Collections.emptyList();  // Returns an empty list instead of null for better safety.
        }

        Collection collaborations = Model.getFacade().getCollaborations(parent);
        Project project = ProjectManager.getManager().getCurrentProject();
        return project.getDiagrams().stream()
                .filter(diagram -> diagram instanceof UMLSequenceDiagram)
                .filter(diagram -> collaborations.contains(((SequenceDiagramGraphModel)
                        ((UMLSequenceDiagram) diagram).getGraphModel()).getCollaboration()))
                .collect(Collectors.toList());
    }

    /**
     * Inherited method from the superclass. If specific dependencies need to be
     * handled for sequence diagrams, they should be added here. Currently, returns
     * an empty set instead of null for consistency and to avoid potential null pointer exceptions.
     */
    @Override
    public Set getDependencies(Object parent) {
        return Collections.emptySet();
    }
}
