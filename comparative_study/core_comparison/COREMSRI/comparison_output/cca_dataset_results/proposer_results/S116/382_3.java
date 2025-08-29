```java
/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dao.jena;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;

import edu.cornell.mannlib.vitro.webapp.dao.VitroVocabulary;

public class JenaBaseDaoCon {

    public JenaBaseDaoCon() {
        LINK.addProperty(propertyCustomShortViewAnnot, _constModel.createTypedLiteral("linkShortView.jsp"));
        PRIMARY_LINK.setLabel("Primary Link", "en-US");
        PRIMARY_LINK.addProperty(propertyStubObjectPropertyAnnot, _constModel.createTypedLiteral(true));
        PRIMARY_LINK.addProperty(propertyCustomEntryFormAnnot, _constModel.createTypedLiteral("defaultLinkForm.jsp"));
        PRIMARY_LINK.setRange(LINK);
        PRIMARY_LINK.addProperty(propertyOfferCreateNewOptionAnnot, _constModel.createTypedLiteral(true));
        PRIMARY_LINK.addProperty(propertySelectFromExistingAnnot, _constModel.createTypedLiteral(false));

        ADDITIONAL_LINK.setLabel("Additional Link", "en-US");
        ADDITIONAL_LINK.setRange(LINK); //apparently does not work to have prop.getRangeVClass() return a non-null VClass
        ADDITIONAL_LINK.addProperty(propertyStubObjectPropertyAnnot, _constModel.createTypedLiteral(true));
        ADDITIONAL_LINK.addProperty(propertyCustomEntryFormAnnot, _constModel.createTypedLiteral("defaultLinkForm.jsp"));
        ADDITIONAL_LINK.addProperty(propertyOfferCreateNewOptionAnnot, _constModel.createTypedLiteral(true));
        ADDITIONAL_LINK.addProperty(propertySelectFromExistingAnnot, _constModel.createTypedLiteral(false));

        VITRO_PUBLIC_ONTOLOGY.setLabel("Vitro Public Ontology", null);
    }


    private OntModel _constModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    /* ***************** Vitro ontology constants ***************** */

    protected AnnotationProperty dataPropertyIsExternalId = _constModel.createAnnotationProperty(VitroVocabulary.DATAPROPERTY_ISEXTERNALID);

    protected AnnotationProperty hiddenFromDisplayBelowRoleLevelAnnot = _constModel.createAnnotationProperty(VitroVocabulary.HIDDEN_FROM_DISPLAY_BELOW_ROLE_LEVEL_ANNOT);
    protected AnnotationProperty prohibitedFromUpdateBelowRoleLevelAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROHIBITED_FROM_UPDATE_BELOW_ROLE_LEVEL_ANNOT);
    protected AnnotationProperty hiddenFromPublishBelowRoleLevelAnnot = _constModel.createAnnotationProperty(VitroVocabulary.HIDDEN_FROM_PUBLISH_BELOW_ROLE_LEVEL_ANNOT);

    protected AnnotationProperty searchBoostAnnot = _constModel.createAnnotationProperty(VitroVocabulary.SEARCH_BOOST_ANNOT);

    protected AnnotationProperty example = _constModel.createAnnotationProperty(VitroVocabulary.EXAMPLE_ANNOT);
    protected AnnotationProperty descriptionAnnot = _constModel.createAnnotationProperty(VitroVocabulary.DESCRIPTION_ANNOT);
    protected AnnotationProperty publicDescriptionAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PUBLIC_DESCRIPTION_ANNOT);
    protected AnnotationProperty shortDef = _constModel.createAnnotationProperty(VitroVocabulary.SHORTDEF);

    protected AnnotationProperty editing = _constModel.createAnnotationProperty(VitroVocabulary.EDITING);

    protected DatatypeProperty moniker = _constModel.createDatatypeProperty(VitroVocabulary.MONIKER);
    protected OntClass classGroup = _constModel.createClass(VitroVocabulary.CLASSGROUP);
    protected AnnotationProperty inClassGroup = _constModel.createAnnotationProperty(VitroVocabulary.IN_CLASSGROUP);
    protected DatatypeProperty modTime = _constModel.createDatatypeProperty(VitroVocabulary.MODTIME);

    protected DatatypeProperty displayRank = _constModel.createDatatypeProperty(VitroVocabulary.DISPLAY_RANK);
    protected AnnotationProperty displayRankAnnot = _constModel.createAnnotationProperty(VitroVocabulary.DISPLAY_RANK_ANNOT);
    protected AnnotationProperty displayLimit = _constModel.createAnnotationProperty(VitroVocabulary.DISPLAY_LIMIT);
    protected AnnotationProperty exampleAnnot = _constModel.createAnnotationProperty(VitroVocabulary.EXAMPLE_ANNOT);

    protected AnnotationProperty propertyEntitySortField = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_ENTITYSORTFIELD);
    protected AnnotationProperty propertyEntitySortDirection = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_ENTITYSORTDIRECTION);
    protected AnnotationProperty propertyObjectIndividualSortProperty = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_OBJECTINDIVIDUALSORTPROPERTY);
    protected AnnotationProperty propertyFullPropertyNameAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_FULLPROPERTYNAMEANNOT);
    protected AnnotationProperty propertyCustomEntryFormAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_CUSTOMENTRYFORMANNOT);
    protected AnnotationProperty propertyCustomDisplayViewAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_CUSTOMDISPLAYVIEWANNOT);
    protected AnnotationProperty propertyCustomShortViewAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_CUSTOMSHORTVIEWANNOT);
    protected AnnotationProperty propertyCustomListViewAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_CUSTOM_LIST_VIEW_ANNOT);
    protected AnnotationProperty propertyCustomSearchViewAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_CUSTOMSEARCHVIEWANNOT);
    protected AnnotationProperty propertySelectFromExistingAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_SELECTFROMEXISTINGANNOT);
    protected AnnotationProperty propertyOfferCreateNewOptionAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_OFFERCREATENEWOPTIONANNOT);
    protected AnnotationProperty propertyInPropertyGroupAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_INPROPERTYGROUPANNOT);
    protected AnnotationProperty propertyCollateBySubclassAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_COLLATEBYSUBCLASSANNOT);
    protected AnnotationProperty propertyStubObjectPropertyAnnot = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_STUBOBJECTPROPERTYANNOT);
    protected AnnotationProperty propertyEditLinksuppressed = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_EDITLINKSUPPRESSED);
    protected AnnotationProperty propertyAddLinksuppressed = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_ADDLINKSUPPRESSED);
    protected AnnotationProperty propertyDeleteLinksuppressed = _constModel.createAnnotationProperty(VitroVocabulary.PROPERTY_DELETELINKSUPPRESSED);

    protected OntClass propertyGroup = _constModel.createClass(VitroVocabulary.PROPERTYGROUP);

    protected OntClass link = _constModel.createClass(VitroVocabulary.LINK);
    protected ObjectProperty primaryLink = _constModel.createObjectProperty(VitroVocabulary.PRIMARY_LINK);
    protected ObjectProperty additionalLink = _constModel.createObjectProperty(VitroVocabulary.ADDITIONAL_LINK);
    protected DatatypeProperty linkAnchor = _constModel.createDatatypeProperty(VitroVocabulary.LINK_ANCHOR);
    protected DatatypeProperty linkUrl = _constModel.createDatatypeProperty(VitroVocabulary.LINK_URL);
    protected DatatypeProperty linkType = _constModel.createDatatypeProperty(VitroVocabulary.LINK_TYPE);
    protected DatatypeProperty linkDisplayRank = _constModel.createDatatypeProperty(VitroVocabulary.LINK_DISPLAYRANK_URL);

    protected OntClass portal = _constModel.createClass(VitroVocabulary.PORTAL);
    protected DatatypeProperty applicationThemeDir = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_THEMEDIR);
    protected DatatypeProperty applicationContactMail = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_CONTACTMAIL);
    protected DatatypeProperty applicationCorrectionMail = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_CORRECTIONMAIL);
    protected DatatypeProperty applicationAboutText = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_ABOUTTEXT);
    protected DatatypeProperty applicationAcknowlegeText = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_ACKNOWLEGETEXT);
    protected DatatypeProperty applicationCopyrightUrl = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_COPYRIGHTURL);
    protected DatatypeProperty applicationCopyrightAnchor = _constModel.createDatatypeProperty(VitroVocabulary.PORTAL_COPYRIGHTANCHOR);

    protected AnnotationProperty ontologyPrefixAnnot = _constModel.createAnnotationProperty(VitroVocabulary.ONTOLOGY_PREFIX_ANNOT);

    protected Ontology vitroPublicOntology = _constModel.createOntology(VitroVocabulary.VITRO_PUBLIC_ONTOLOGY);

    protected ObjectProperty indMainImage = _constModel.createObjectProperty(VitroVocabulary.IND_MAIN_IMAGE);

    /* ***************** User Account Model constants ***************** */

    protected OntClass userAccount = _constModel.createClass(VitroVocabulary.USERACCOUNT);
    protected OntClass userAccountRootUser = _constModel.createClass(VitroVocabulary.USERACCOUNT_ROOT_USER);
    protected DatatypeProperty userAccountEmailAddress = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_EMAIL_ADDRESS);
    protected DatatypeProperty userAccountFirstName = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_FIRST_NAME);
    protected DatatypeProperty userAccountLastName = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_LAST_NAME);
    protected DatatypeProperty userAccountArgon2Password = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_ARGON2_PASSWORD);
    protected DatatypeProperty userAccountMd5Password = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_MD5_PASSWORD);
    protected DatatypeProperty userAccountOldPassword = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_OLD_PASSWORD);
    protected DatatypeProperty userAccountLoginCount = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_LOGIN_COUNT);
    protected DatatypeProperty userAccountLastLoginTime = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_LAST_LOGIN_TIME);
    protected DatatypeProperty userAccountStatus = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_STATUS);
    protected DatatypeProperty userAccountPasswordLinkExpires = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_PASSWORD_LINK_EXPIRES);
    protected DatatypeProperty userAccountPasswordChangeRequired = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_PASSWORD_CHANGE_REQUIRED);
    protected DatatypeProperty userAccountExternalAuthId = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_EXTERNAL_AUTH_ID);
    protected DatatypeProperty userAccountExternalAuthOnly = _constModel.createDatatypeProperty(VitroVocabulary.USERACCOUNT_EXTERNAL_AUTH_ONLY);
    protected ObjectProperty userAccountHasPermissionSet = _constModel.createObjectProperty(VitroVocabulary.USERACCOUNT_HAS_PERMISSION_SET);
    protected ObjectProperty userAccountProxyEditorFor = _constModel.createObjectProperty(VitroVocabulary.USERACCOUNT_PROXY_EDITOR_FOR);

    protected OntClass permissionSet = _constModel.createClass(VitroVocabulary.PERMISSIONSET);
    protected OntClass permissionSetForNewUsers = _constModel.createClass(VitroVocabulary.PERMISSION_SET_FOR_NEW_USERS);
    protected OntClass permissionSetForPublic = _constModel.createClass(VitroVocabulary.PERMISSION_SET_FOR_PUBLIC);
    protected ObjectProperty permissionSetHasPermission = _constModel.createObjectProperty(VitroVocabulary.PERMISSIONSET_HAS_PERMISSION);

    protected OntClass permission = _constModel.createClass(VitroVocabulary.PERMISSION);


    public OntModel getConstModel() {
        return _constModel;
    }

}
