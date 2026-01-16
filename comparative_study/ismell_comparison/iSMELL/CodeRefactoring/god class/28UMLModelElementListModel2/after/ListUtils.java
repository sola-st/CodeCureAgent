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

class ListUtils extends DefaultListModel {
    public void addAll(Collection col) {
        if (col.size() == 0) {
            return;
        }
        Iterator it = col.iterator();
        fireListEvents = false;
        int intervalStart = getSize() == 0 ? 0 : getSize() - 1;
        while (it.hasNext()) {
            Object o = it.next();
            addElement(o);
        }
        fireListEvents = true;
        fireIntervalAdded(this, intervalStart, getSize() - 1);
    }

    protected void setAllElements(Collection col) {
        if (!isEmpty()) {
            removeAllElements();
        }
        addAll(col);
    }

    public boolean contains(Object elem) {
        if (super.contains(elem)) {
            return true;
        }
        if (elem instanceof Collection) {
            Iterator it = ((Collection) elem).iterator();
            while (it.hasNext()) {
                if (!super.contains(it.next())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void addElement(Object obj) {
        if (obj != null && !contains(obj)) {
            super.addElement(obj);
        }
    }
}