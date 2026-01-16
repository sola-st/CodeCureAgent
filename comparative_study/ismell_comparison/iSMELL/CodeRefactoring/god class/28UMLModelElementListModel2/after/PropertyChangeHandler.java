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

class PropertyChangeHandler {
    public void propertyChange(PropertyChangeEvent e) {
        if (e instanceof AttributeChangeEvent) {
            try {
                if (isValidEvent(e)) {
                    rebuildModelList();
                }
            } catch (InvalidElementException iee) {
                return;
            }
        } else if (e instanceof AddAssociationEvent) {
            if (isValidEvent(e)) {
                Object o = getChangedElement(e);
                if (o instanceof Collection) {
                    ArrayList tempList = new ArrayList((Collection) o);
                    Iterator it = tempList.iterator();
                    while (it.hasNext()) {
                        Object o2 = it.next();
                        addElement(o2);
                    }
                } else {
                    /* TODO: If this is an ordered list, then you have to 
                        add in the right location! */
                    addElement(o); 
                }
            }
        } else if (e instanceof RemoveAssociationEvent) {
            boolean valid = false;
            if (!(getChangedElement(e) instanceof Collection)) {
                valid = contains(getChangedElement(e));
            } else {
                Collection col = (Collection) getChangedElement(e);
                Iterator it = col.iterator();
                valid = true;
                while (it.hasNext()) {
                    Object o = it.next();
                    if (!contains(o)) {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                Object o = getChangedElement(e);
                if (o instanceof Collection) {
                    Iterator it = ((Collection) o).iterator();
                    while (it.hasNext()) {
                        Object o3 = it.next();
                        removeElement(o3);
                    }
                } else {
                    removeElement(o);
                }
            }
        }
    }
}