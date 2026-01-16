package org.argouml.ui.explorer.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.argouml.i18n.Translator;
import org.argouml.model.Model;

public class GoPackageToClass {

    private final PerspectiveRuleDelegate delegate;

    public GoPackageToClass(PerspectiveRuleDelegate delegate) {
        this.delegate = delegate;
    }

    public String getRuleName() {
        return Translator.localize("misc.package.class");
    }

    public Collection getChildren(Object parent) {
        if (Model.getFacade().isAPackage(parent)) {
            return Model.getModelManagementHelper()
                .getAllModelElementsOfKind(parent, Model.getMetaTypes().getUMLClass());
        }
        return Collections.EMPTY_SET;
    }

    public Set getDependencies(Object parent) {
        // Implementation remains the same, possibly refactored to use delegate if needed
        return Collections.EMPTY_SET;
    }
}
