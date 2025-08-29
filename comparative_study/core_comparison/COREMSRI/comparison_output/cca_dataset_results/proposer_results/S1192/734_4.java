```java
/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.controller.edit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.cornell.mannlib.vitro.webapp.utils.JSPPageHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.cornell.mannlib.vedit.beans.EditProcessObject;
import edu.cornell.mannlib.vedit.beans.FormObject;
import edu.cornell.mannlib.vedit.beans.Option;
import edu.cornell.mannlib.vedit.controller.BaseEditController;
import edu.cornell.mannlib.vedit.util.FormUtils;
import edu.cornell.mannlib.vitro.webapp.auth.permissions.SimplePermission;
import edu.cornell.mannlib.vitro.webapp.beans.DataProperty;
import edu.cornell.mannlib.vitro.webapp.beans.ObjectProperty;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;

public class RefactorRetryController extends BaseEditController {

	private static final Log log = LogFactory.getLog(RefactorRetryController.class.getName());

	private static final String MODE_STR_RENAME_RESOURCE = "renameResource";
	private static final String MODE_STR_MOVE_PROPERTY_STATEMENTS = "movePropertyStatements";
	private static final String MODE_STR_MOVE_INSTANCES = "moveInstances";

	private static final String ATTRIBUTE_MODE_STR = "modeStr";
	private static final String ATTRIBUTE_ACTION = "_action";
	private static final String ATTRIBUTE_OLD_URI = "oldURI";
	private static final String ATTRIBUTE_PROPERTY_URI = "propertyURI";
	private static final String ATTRIBUTE_PROPERTY_TYPE = "propertyType";
	private static final String ATTRIBUTE_VCLASS_URI = "VClassURI";

	private static final String REQUEST_ATTR_EDIT_ACTION = "editAction";
	private static final String REQUEST_ATTR_TITLE = "title";
	private static final String REQUEST_ATTR_FORM_JSP = "formJsp";
	private static final String REQUEST_ATTR_CANCEL_BUTTON_DISABLED = "_cancelButtonDisabled";
	private static final String REQUEST_ATTR_BODY_JSP = "bodyJsp";
	private static final String REQUEST_ATTR_SCRIPTS = "scripts";

	private static final String EDIT_ACTION_REFACTOR_OP = "refactorOp";
	private static final String CANCEL_BUTTON_DISABLED = "disabled";

	private static final String TITLE_RENAME_RESOURCE = "Rename Resource";
	private static final String TITLE_MOVE_PROPERTY_STATEMENTS = "Move Property Statements";
	private static final String TITLE_MOVE_CLASS_INSTANCES = "Move Class Instances";

	private static final String FORM_JSP_RENAME_RESOURCE = "/templates/edit/specific/renameResource_retry.jsp";
	private static final String FORM_JSP_MOVE_PROPERTY_STATEMENTS = "/templates/edit/specific/movePropertyStatements_retry.jsp";
	private static final String FORM_JSP_MOVE_INSTANCES = "/templates/edit/specific/moveInstances_retry.jsp";

	private static final String BODY_JSP_FORM_BASIC = "/templates/edit/formBasic.jsp";
	private static final String SCRIPTS_FORM_BASIC_JS = "/templates/edit/formBasic.js";

	private static final String OPTION_EMPTY_VALUE = "";
	private static final String OPTION_PICKLIST_WILDCARD = "? wildcard";
	private static final String OPTION_MOVE_TO_TRASH = "(move to trash)";
	private static final String OPTION_MOVE_TO_TRASH_SHORT = "move to trash";

	private void doRenameResource(VitroRequest request, HttpServletResponse response, EditProcessObject epo) {
		epo.setAttribute(ATTRIBUTE_MODE_STR, MODE_STR_RENAME_RESOURCE);
		epo.setAttribute(ATTRIBUTE_ACTION, "insert");
		epo.setAttribute(ATTRIBUTE_OLD_URI, request.getParameter(ATTRIBUTE_OLD_URI));
		request.setAttribute(REQUEST_ATTR_EDIT_ACTION, EDIT_ACTION_REFACTOR_OP);
		request.setAttribute(REQUEST_ATTR_TITLE, TITLE_RENAME_RESOURCE);
		request.setAttribute(REQUEST_ATTR_FORM_JSP, FORM_JSP_RENAME_RESOURCE);
		request.setAttribute(REQUEST_ATTR_CANCEL_BUTTON_DISABLED, CANCEL_BUTTON_DISABLED);
		request.setAttribute(REQUEST_ATTR_BODY_JSP, BODY_JSP_FORM_BASIC);
        request.setAttribute(REQUEST_ATTR_SCRIPTS, SCRIPTS_FORM_BASIC_JS);
	}

	private void doMovePropertyStatements(VitroRequest request, HttpServletResponse response, EditProcessObject epo) {
		epo.setAttribute(ATTRIBUTE_MODE_STR, MODE_STR_MOVE_PROPERTY_STATEMENTS);
		String propertyURI = request.getParameter(ATTRIBUTE_PROPERTY_URI);
		epo.setAttribute(ATTRIBUTE_PROPERTY_URI, propertyURI);
		epo.setAttribute(ATTRIBUTE_PROPERTY_TYPE, request.getParameter(ATTRIBUTE_PROPERTY_TYPE));
		FormObject foo = new FormObject();
		epo.setFormObject(foo);
		HashMap<String,List<Option>> optMap = new HashMap<String,List<Option>>();
		foo.setOptionLists(optMap);
		List<Option> subjectClassOpts = FormUtils.makeOptionListFromBeans(request.getUnfilteredWebappDaoFactory().getVClassDao().getAllVclasses(),"URI","PickListName", null, null);
		subjectClassOpts.add(0,new Option(OPTION_EMPTY_VALUE, OPTION_PICKLIST_WILDCARD,true));
		optMap.put("SubjectClassURI", subjectClassOpts);
		optMap.put("ObjectClassURI", subjectClassOpts);

		List newPropertyOpts;
		if (epo.getAttribute(ATTRIBUTE_PROPERTY_TYPE).equals("ObjectProperty"))  {
			List<ObjectProperty> opList = request.getUnfilteredWebappDaoFactory().getObjectPropertyDao().getAllObjectProperties();
			Collections.sort(opList);
			newPropertyOpts = FormUtils.makeOptionListFromBeans(opList,"URI","PickListName", null, null);
		} else {
			List<DataProperty> dpList = request.getUnfilteredWebappDaoFactory().getDataPropertyDao().getAllDataProperties();
			Collections.sort(dpList);
			newPropertyOpts = FormUtils.makeOptionListFromBeans(dpList,"URI","PickListName", null, null);
		}
		HashMap<String,Option> hashMap = new HashMap<String,Option>();
        newPropertyOpts = getSortedList(hashMap,newPropertyOpts,request);
		newPropertyOpts.add(new Option(OPTION_EMPTY_VALUE, OPTION_MOVE_TO_TRASH));
		optMap.put("NewPropertyURI", newPropertyOpts);

		request.setAttribute(REQUEST_ATTR_EDIT_ACTION, EDIT_ACTION_REFACTOR_OP);
		request.setAttribute(REQUEST_ATTR_TITLE, TITLE_MOVE_PROPERTY_STATEMENTS);
		request.setAttribute(REQUEST_ATTR_FORM_JSP, FORM_JSP_MOVE_PROPERTY_STATEMENTS);
		request.setAttribute(REQUEST_ATTR_CANCEL_BUTTON_DISABLED, CANCEL_BUTTON_DISABLED);
		request.setAttribute(REQUEST_ATTR_BODY_JSP, BODY_JSP_FORM_BASIC);
        request.setAttribute(REQUEST_ATTR_SCRIPTS, SCRIPTS_FORM_BASIC_JS);
	}

	public void doMoveInstances(VitroRequest request, HttpServletResponse response, EditProcessObject epo) {
		epo.setAttribute(ATTRIBUTE_MODE_STR, MODE_STR_MOVE_INSTANCES);
		String propertyURI = request.getParameter(ATTRIBUTE_VCLASS_URI);
		epo.setAttribute(ATTRIBUTE_VCLASS_URI, propertyURI);
		FormObject foo = new FormObject();
		epo.setFormObject(foo);
		HashMap<String,List<Option>> optMap = new HashMap<String,List<Option>>();
		foo.setOptionLists(optMap);
		List<Option> newClassURIopts = FormUtils.makeOptionListFromBeans(request.getUnfilteredWebappDaoFactory().getVClassDao().getAllVclasses(),"URI","PickListName", null, null);
		newClassURIopts.add(new Option (OPTION_EMPTY_VALUE, OPTION_MOVE_TO_TRASH_SHORT));
		optMap.put("NewVClassURI", newClassURIopts);
		request.setAttribute(REQUEST_ATTR_EDIT_ACTION, EDIT_ACTION_REFACTOR_OP);
		request.setAttribute(REQUEST_ATTR_TITLE, TITLE_MOVE_CLASS_INSTANCES);
		request.setAttribute(REQUEST_ATTR_FORM_JSP, FORM_JSP_MOVE_INSTANCES);
		request.setAttribute(REQUEST_ATTR_CANCEL_BUTTON_DISABLED, CANCEL_BUTTON_DISABLED);
		request.setAttribute(REQUEST_ATTR_BODY_JSP, BODY_JSP_FORM_BASIC);
        request.setAttribute(REQUEST_ATTR_SCRIPTS, SCRIPTS_FORM_BASIC_JS);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
        if (!isAuthorizedToDisplayPage(request, response, SimplePermission.EDIT_ONTOLOGY.ACTION)) {
        	return;
        }

	    //create an EditProcessObject for this and put it in the session
	    EditProcessObject epo = super.createEpo(request);

	    VitroRequest vreq = new VitroRequest(request);

	    String modeStr = request.getParameter("mode");

	    if (modeStr != null) {
			switch (modeStr) {
				case MODE_STR_RENAME_RESOURCE:
					doRenameResource(vreq, response, epo);
					break;
				case MODE_STR_MOVE_PROPERTY_STATEMENTS:
					doMovePropertyStatements(vreq, response, epo);
					break;
				case MODE_STR_MOVE_INSTANCES:
					doMoveInstances(vreq, response, epo);
					break;
			}
	    }

        setRequestAttributes(request,epo);

        try {
			JSPPageHandler.renderBasicPage(request, response, BODY_JSP_FORM_BASIC);
        } catch (Exception e) {
            log.error(this.getClass().getName()+" could not forward to view.");
            log.error(e.getMessage());
            log.error(e.getStackTrace());
        }


	}



}
