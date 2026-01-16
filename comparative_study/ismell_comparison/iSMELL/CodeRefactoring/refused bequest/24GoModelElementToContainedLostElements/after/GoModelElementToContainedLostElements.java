package org.argouml.ui.explorer.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.argouml.i18n.Translator;
import org.argouml.model.Model;

public class GoModelElementToContainedLostElements {

    private final PerspectiveRule perspectiveDelegate;

    public GoModelElementToContainedLostElements(PerspectiveRule perspectiveDelegate) {
        this.perspectiveDelegate = perspectiveDelegate;
    }

    public String getRuleName() {
        return Translator.localize("misc.model-element.contained-lost-elements");
    }

    public Collection getChildren(Object parent) {
        Collection ret = new ArrayList();
        if (Model.getFacade().isANamespace(parent)) {
            Collection col =
                Model.getModelManagementHelper().getAllModelElementsOfKind(
                        parent,
                        Model.getMetaTypes().getStateMachine());
            Iterator it = col.iterator();
            while (it.hasNext()) {
                Object machine = it.next();
                if (Model.getFacade().getNamespace(machine) == parent) {
                    Object context = Model.getFacade().getContext(machine);
                    if (context == null) {
                        ret.add(machine);
                    }
                }
            }
        }
        return ret;
    }

    public Set getDependencies(Object parent) {
        Set set = new HashSet();
        if (Model.getFacade().isANamespace(parent)) {
            set.add(parent);
        }
        return set;
    }

    // Delegate methods to perspectiveDelegate if needed
    public String toString() {
        return perspectiveDelegate.toString();
    }
}
