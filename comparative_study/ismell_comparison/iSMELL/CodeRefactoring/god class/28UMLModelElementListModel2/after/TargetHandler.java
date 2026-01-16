package org.argouml.uml.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.argouml.model.AddAssociationEvent;
import org.argouml.model.AssociationChangeEvent;
import org.argouml.model.AttributeChangeEvent;
import org.argouml.model.InvalidElementException;
import org.argouml.model.Model;
import org.argouml.model.RemoveAssociationEvent;
import org.argouml.ui.targetmanager.TargetEvent;
import org.argouml.ui.targetmanager.TargetListener;
import org.tigris.gef.base.Diagram;
import org.tigris.gef.presentation.Fig;

class TargetHandler {
    private Object target;

    public void setTarget(Object theNewTarget) {
        theNewTarget = theNewTarget instanceof Fig
            ? ((Fig) theNewTarget).getOwner() : theNewTarget;
        if (Model.getFacade().isAUMLElement(theNewTarget)
                || theNewTarget instanceof Diagram) {
            if (Model.getFacade().isAUMLElement(listTarget)) {
                Model.getPump().removeModelEventListener(this, listTarget,
                        eventName);
                // Allow listening to other elements:
                removeOtherModelEventListeners(listTarget);
            }

            if (Model.getFacade().isAUMLElement(theNewTarget)) {
                listTarget = theNewTarget;
                Model.getPump().addModelEventListener(this, listTarget,
                        eventName);
                // Allow listening to other elements:
                addOtherModelEventListeners(listTarget);

                rebuildModelList();

            } else {
                listTarget = null;
                removeAllElements();
            }
        }
    }

    public Object getTarget() {
        return target;
    }
}