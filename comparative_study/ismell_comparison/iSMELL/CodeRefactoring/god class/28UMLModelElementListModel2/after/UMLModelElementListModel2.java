/* $Id: UMLModelElementListModel2.java 19614 2011-07-20 12:10:13Z linus $
 *****************************************************************************
 * Copyright (c) 2009 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    mvw
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2002-2008 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

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

/**
 * The model for a list that contains ModelElements. The state of the Element is
 * still kept in the model subsystem itself. This list is only to be used as the
 * model for some GUI element like UMLLinkedList.
 *
 * @since Oct 2, 2002
 * @author jaap.branderhorst@xs4all.nl
 * @deprecated in 0.31.2 by Bob Tarling  This is replaced by the XML property
 * panels module
 */
@Deprecated
public abstract class UMLModelElementListModel2 extends ListUtils
        implements TargetListener, PropertyChangeListener {
    private String eventName = null;
    private Object listTarget = null;

    /**
     * The type of model elements this list model is designed to hold.
     */
    private Object metaType;

    private static final Logger LOG = Logger.getLogger(UMLModelElementListModel2.class);

    private EventNameHandler eventNameHandler;
    private MetaTypeHandler metaTypeHandler;
    private TargetHandler targetHandler;
    private PropertyChangeHandler propertyChangeHandler;
    private ListBuilder listBuilder;
    private PopupMenuBuilder popupMenuBuilder;

    private boolean fireListEvents = true;
    private boolean buildingModel = false;
    private boolean reverseDropConnection;

    /**
     * Constructor to be used if the subclass does not depend on the
     * MELementListener methods and setTarget method implemented in this
     * class.
     */
    public UMLModelElementListModel2() {
        super();
    }

    /**
     * Constructor for UMLModelElementListModel2.
     *
     * @param name the name of the event to listen to, which triggers us
     *             to update the list model from the UML data
     */
    public UMLModelElementListModel2(String name) {
        super();
        eventName = name;
    }
    
    /**
     * Constructor for UMLModelElementListModel2.
     *
     * @param name the name of the event to listen to, which triggers us
     *             to update the list model from the UML data
     * @param theMetaType the type of model element that the list model
     *                 is designed to contain.
     */
    public UMLModelElementListModel2(String name, Object theMetaType) {
        super();
        this.metaType = theMetaType;
        eventName = name;
    }
    
    /**
     * Constructor for UMLModelElementListModel2.
     *
     * @param name the name of the event to listen to, which triggers us
     *             to update the list model from the UML data
     * @param theMetaType the type of model element that the list model
     *                 is designed to contain.
     * @param reverseTheDropConnection tells the JList to reverse the
     *              connection made and drop during dnd.
     */
    public UMLModelElementListModel2(
	    String name, 
	    Object theMetaType, 
	    boolean reverseTheDropConnection) {
        super();
        this.metaType = theMetaType;
        eventName = name;
        this.reverseDropConnection = reverseTheDropConnection;
    }
    
    public boolean isReverseDropConnection() {
	return reverseDropConnection;
    }

    /**
     * @param building The buildingModel to set.
     */
    protected void setBuildingModel(boolean building) {
        this.buildingModel = building;
    }

    /**
     * Builds the list of elements. Called from targetChanged every time the
     * target of the proppanel is changed. Usually the method setAllElements is
     * called with the result.
     */
    protected abstract void buildModelList();

    /**
     * Utility method to get the changed element from some event e
     * @param e the event
     * @return Object the changed element
     */
    protected Object getChangedElement(PropertyChangeEvent e) {
        if (e instanceof AssociationChangeEvent) {
            return ((AssociationChangeEvent) e).getChangedValue();
        }
        if (e instanceof AttributeChangeEvent) {
            return ((AttributeChangeEvent) e).getSource();
        }
        return e.getNewValue();
    }

    /**
     * This function allows subclasses to listen to more modelelements.
     * The given target is guaranteed to be a UML modelelement.
     * 
     * @param oldTarget the UML modelelement
     */
    protected void removeOtherModelEventListeners(Object oldTarget) {
        /* Do nothing by default. */
    }

    /**
     * This function allows subclasses to listen to more modelelements.
     * The given target is guaranteed to be a UML modelelement.
     * 
     * @param newTarget the UML modelelement
     */
    protected void addOtherModelEventListeners(Object newTarget) {
        /* Do nothing by default. */
    }

    /**
     * Returns true if the given element is valid, i.e. it may be added to the
     * list of elements.
     *
     * @param element the element to be tested
     * @return true if valid
     */
    protected abstract boolean isValidElement(Object element);

    /**
     * Returns true if some event is valid. An event is valid if the
     * element changed in the event is valid. This is determined via a
     * call to isValidElement.  This method can be overriden by
     * subclasses if they cannot determine if it is a valid event just
     * by checking the changed element.
     *
     * @param e the event
     * @return boolean true if valid
     */
    protected boolean isValidEvent(PropertyChangeEvent e) {
        boolean valid = false;
        if (!(getChangedElement(e) instanceof Collection)) {
            // TODO: Considering all delete events to be valid like below
            // is going to cause lots of unecessary work and some problems
            if ((e.getNewValue() == null && e.getOldValue() != null)
                    // Don't test changed element if it was deleted
                    || isValidElement(getChangedElement(e))) {
                valid = true; // we tried to remove a value
            }
        } else {
            Collection col = (Collection) getChangedElement(e);
            Iterator it = col.iterator();
            if (!col.isEmpty()) {
                valid = true;
                while (it.hasNext()) {
                    Object o = it.next();
                    if (!isValidElement(o)) {
                        valid = false;
                        break;
                    }
                }
            } else {
                if (e.getOldValue() instanceof Collection
                    && !((Collection) e.getOldValue()).isEmpty()) {
                    valid = true;
                }
            }
        }
        return valid;
    }

    /*
     * @see TargetListener#targetAdded(TargetEvent)
     */
    public void targetAdded(TargetEvent e) {
        setTarget(e.getNewTarget());
    }

    /*
     * @see TargetListener#targetRemoved(TargetEvent)
     */
    public void targetRemoved(TargetEvent e) {
        setTarget(e.getNewTarget());
    }

    /*
     * @see TargetListener#targetSet(TargetEvent)
     */
    public void targetSet(TargetEvent e) {
        setTarget(e.getNewTarget());
    }

    /*
     * @see javax.swing.AbstractListModel#fireContentsChanged(
     *          Object, int, int)
     */
    protected void fireContentsChanged(Object source, int index0, int index1) {
        if (fireListEvents && !buildingModel) {
            super.fireContentsChanged(source, index0, index1);
        }
    }

    /*
     * @see javax.swing.AbstractListModel#fireIntervalAdded(
     *          Object, int, int)
     */
    protected void fireIntervalAdded(Object source, int index0, int index1) {
        if (fireListEvents && !buildingModel) {
            super.fireIntervalAdded(source, index0, index1);
        }
    }

    /*
     * @see javax.swing.AbstractListModel#fireIntervalRemoved(
     *          Object, int, int)
     */
    protected void fireIntervalRemoved(Object source, int index0, int index1) {
        if (fireListEvents && !buildingModel) {
            super.fireIntervalRemoved(source, index0, index1);
        }
    }
}
