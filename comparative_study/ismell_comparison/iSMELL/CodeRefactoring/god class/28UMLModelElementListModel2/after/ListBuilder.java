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

class ListBuilder {
    private void rebuildModelList() {
        removeAllElements();
        buildingModel = true;
        try {
            buildModelList();
        } catch (InvalidElementException exception) {
            /*
             * This can throw an exception if the target has been
             * deleted. We don't want to try locking the repository
             * because this is called from the event delivery thread and
             * could cause a deadlock. Instead catch the exception and
             * leave the model empty.
             */
            LOG.debug("buildModelList threw exception for target " 
                    + getTarget() + ": "
                    + exception);
        } finally {
            buildingModel = false;
        }
        if (getSize() > 0) {
            fireIntervalAdded(this, 0, getSize() - 1);
        }
    }
}