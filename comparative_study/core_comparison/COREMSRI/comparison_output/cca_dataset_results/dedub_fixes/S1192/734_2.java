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

	private static final String ACTION_INSERT = "insert";

	private static final String EDIT_ACTION_REFACTOR_OP = "refactorOp";

	private static final String TITLE_RENAME_RESOURCE = "Rename Resource";
	private static final String TITLE_MOVE_PROPERTY_STATEMENTS = "Move Property Statements";
	private static final String TITLE_MOVE_CLASS_INSTANCES = "Move Class Instances";

	private static final String FORM_JSP_RENAME_RESOURCE = "/templates/edit/specific/renameResource_retry.jsp";
	private static final String FORM_JSP_MOVE_PROPERTY_STATEMENTS = "/templates/edit/specific/movePropertyStatements_retry.jsp";
	private static final String FORM_JSP_MOVE_INSTANCES = "/templates/edit/specific/moveInstances_retry.jsp";

	private static final String BODY_JSP_FORM_BASIC = "/templates/edit/formBasic.jsp";

	private static final String SCRIPTS_FORM_BASIC_JS = "/templates/edit/formBasic.js";

	private static final String CANCEL_BUTTON_DISABLED = "disabled";

	private void doRenameResource(VitroRequest request, HttpServletResponse response, EditProcessObject epo) {
		epo.setAttribute("modeStr", MODE_STR_RENAME_RESOURCE);
		epo.setAttribute("_action", ACTION_INSERT);
		epo.setAttribute("oldURI", request.getParameter("oldURI"));
		request.setAttribute("editAction", EDIT_ACTION_REFACTOR_OP);
		request.setAttribute("title", TITLE_RENAME_RESOURCE);
		request.setAttribute("formJsp", FORM_JSP_RENAME_RESOURCE);
		request.setAttribute("_cancelButtonDisabled", CANCEL_BUTTON_DISABLED);
		request.setAttribute("bodyJsp", BODY_JSP_FORM_BASIC);
        request.setAttribute("scripts", SCRIPTS_FORM_BASIC_JS);
	}

	private void doMovePropertyStatements(VitroRequest request, HttpServletResponse response, EditProcessObject epo) {
		epo.setAttribute("modeStr", MODE_STR_MOVE_PROPERTY_STATEMENTS);
		String propertyURI = request.getParameter("propertyURI");
		epo.setAttribute("propertyURI", propertyURI);
		epo.setAttribute("propertyType", request.getParameter("propertyType"));
		FormObject foo = new FormObject();
		epo.setFormObject(foo);
		HashMap<String, List<Option>> optMap = new HashMap<String, List<Option>>();
		foo.setOptionLists(optMap);
		List<Option> subjectClassOpts = FormUtils.makeOptionListFromBeans(request.getUnfilteredWebappDaoFactory().getVClassDao().getAllVclasses(), "URI", "PickListName", null, null);
		subjectClassOpts.add(0, new Option("", "? wildcard", true));
		optMap.put("SubjectClassURI", subjectClassOpts);
		optMap.put("ObjectClassURI", subjectClassOpts);

		List newPropertyOpts;
		if (epo.getAttribute("propertyType").equals("ObjectProperty")) {
			List<ObjectProperty> opList = request.getUnfilteredWebappDaoFactory().getObjectPropertyDao().getAllObjectProperties();
			Collections.sort(opList);
			newPropertyOpts = FormUtils.makeOptionListFromBeans(opList, "URI", "PickListName", null, null);
		} else {
			List<DataProperty> dpList = request.getUnfilteredWebappDaoFactory().getDataPropertyDao().getAllDataProperties();
			Collections.sort(dpList);
			newPropertyOpts = FormUtils.makeOptionListFromBeans(dpList, "URI", "PickListName", null, null);
		}
		HashMap<String, Option> hashMap = new HashMap<String, Option>();
        newPropertyOpts = getSortedList(hashMap, newPropertyOpts, request);
		newPropertyOpts.add(new Option("", "(move to trash)"));
		optMap.put("NewPropertyURI", newPropertyOpts);

		request.setAttribute("editAction", EDIT_ACTION_REFACTOR_OP);
		request.setAttribute("title", TITLE_MOVE_PROPERTY_STATEMENTS);
		request.setAttribute("formJsp", FORM_JSP_MOVE_PROPERTY_STATEMENTS);
		request.setAttribute("_cancelButtonDisabled", CANCEL_BUTTON_DISABLED);
		request.setAttribute("bodyJsp", BODY_JSP_FORM_BASIC);
        request.setAttribute("scripts", SCRIPTS_FORM_BASIC_JS);
	}

	public void doMoveInstances(VitroRequest request, HttpServletResponse response, EditProcessObject epo) {
		epo.setAttribute("modeStr", MODE_STR_MOVE_INSTANCES);
		String propertyURI = request.getParameter("VClassURI");
		epo.setAttribute("VClassURI", propertyURI);
		FormObject foo = new FormObject();
		epo.setFormObject(foo);
		HashMap<String, List<Option>> optMap = new HashMap<String, List<Option>>();
		foo.setOptionLists(optMap);
		List<Option> newClassURIopts = FormUtils.makeOptionListFromBeans(request.getUnfilteredWebappDaoFactory().getVClassDao().getAllVclasses(), "URI", "PickListName", null, null);
		newClassURIopts.add(new Option("", "move to trash"));
		optMap.put("NewVClassURI", newClassURIopts);
		request.setAttribute("editAction", EDIT_ACTION_REFACTOR_OP);
		request.setAttribute("title", TITLE_MOVE_CLASS_INSTANCES);
		request.setAttribute("formJsp", FORM_JSP_MOVE_INSTANCES);
		request.setAttribute("_cancelButtonDisabled", CANCEL_BUTTON_DISABLED);
		request.setAttribute("bodyJsp", BODY_JSP_FORM_BASIC);
        request.setAttribute("scripts", SCRIPTS_FORM_BASIC_JS);
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

        setRequestAttributes(request, epo);

        try {
			JSPPageHandler.renderBasicPage(request, response, BODY_JSP_FORM_BASIC);
        } catch (Exception e) {
            log.error(this.getClass().getName() + " could not forward to view.");
            log.error(e.getMessage());
            log.error(e.getStackTrace());
        }


	}



}
