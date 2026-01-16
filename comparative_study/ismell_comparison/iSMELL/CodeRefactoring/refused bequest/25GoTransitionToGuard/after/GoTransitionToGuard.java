package org.argouml.ui.explorer.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.argouml.i18n.Translator;
import org.argouml.model.Model;

public class GoTransitionToGuard {

    private final PerspectiveRuleDelegate delegate;

    public GoTransitionToGuard(PerspectiveRuleDelegate delegate) {
        this.delegate = delegate;
    }

    public String getRuleName() {
        return Translator.localize("misc.transition.guard");
    }

    public Collection getChildren(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Collection col = new ArrayList();
            col.add(Model.getFacade().getGuard(parent));
            return col;
        }
        return Collections.EMPTY_SET;
    }

    public Set getDependencies(Object parent) {
        if (Model.getFacade().isATransition(parent)) {
            Set set = new HashSet();
            set.add(parent);
            return set;
        }
        return Collections.EMPTY_SET;
    }
}
