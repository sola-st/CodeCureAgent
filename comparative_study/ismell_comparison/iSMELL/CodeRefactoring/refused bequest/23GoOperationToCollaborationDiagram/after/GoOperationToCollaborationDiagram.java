package org.argouml.ui.explorer.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.argouml.i18n.Translator;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.Model;
import org.argouml.uml.diagram.ArgoDiagram;
import org.argouml.uml.diagram.collaboration.ui.UMLCollaborationDiagram;

public class GoOperationToCollaborationDiagram {

    private final PerspectiveRuleDelegate delegate;

    public GoOperationToCollaborationDiagram(PerspectiveRuleDelegate delegate) {
        this.delegate = delegate;
    }

    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAOperation(parent)) {
            Object operation = parent;
            Collection col = Model.getFacade().getCollaborations(operation);
            Set<ArgoDiagram> ret = new HashSet<ArgoDiagram>();
            Project p = ProjectManager.getManager().getCurrentProject();
            for (ArgoDiagram diagram : p.getDiagramList()) {
                if (diagram instanceof UMLCollaborationDiagram
		    && col.contains(((UMLCollaborationDiagram) diagram)
				    .getNamespace())) {
                    ret.add(diagram);
                }

            }
            return ret;
        }
        return Collections.EMPTY_SET;
    }

    public Set getDependencies(Object parent) {
        return Collections.EMPTY_SET;
    }

    public String getRuleName() {
        return Translator.localize("misc.operation.collaboration-diagram");
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
