package org.linkeddatafragments.util;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author mielvandersande
 */
@SuppressWarnings("javadoc")
/**
 * All common URIs needed for parsing and serializations
 */
public class CommonResources {

    /**
     *
     */
    public static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     *
     */
    public static final Property RDF_TYPE = createProperty(RDF + "type");

    /**
     *
     */
    public static final Property RDF_SUBJECT = createProperty(RDF + "subject");

    /**
     *
     */
    public static final Property RDF_PREDICATE = createProperty(RDF + "predicate");

    /**
     *
     */
    public static final Property RDF_OBJECT = createProperty(RDF + "object");

    /**
     *
     */
    public static final String VOID = "http://rdfs.org/ns/void#";

    /**
     *
     */
    public static final Property VOID_TRIPLES = createProperty(VOID + "triples");

    /**
     *
     */
    public static final Property VOID_SUBSET = createProperty(VOID + "subset");

    /**
     *
     */
    public static final Property VOID_DATASET = createProperty(VOID + "Dataset");

    /**
     *
     */
    public static final String HYDRA = "http://www.w3.org/ns/hydra/core#";

    /**
     *
     */
    public static final Property HYDRA_TOTALITEMS = createProperty(HYDRA + "totalItems");

    /**
     *
     */
    public static final Property HYDRA_ITEMSPERPAGE = createProperty(HYDRA + "itemsPerPage");

    /**
     *
     */
    public static final Property HYDRA_SEARCH = createProperty(HYDRA + "search");

    /**
     *
     */
    public static final Property HYDRA_TEMPLATE = createProperty(HYDRA + "template");

    /**
     *
     */
    public static final Property HYDRA_MAPPING = createProperty(HYDRA + "mapping");

    /**
     *
     */
    public static final Property HYDRA_VARIABLE = createProperty(HYDRA + "variable");

    /**
     *
     */
    public static final Property HYDRA_PROPERTY = createProperty(HYDRA + "property");

    /**
     *
     */
    public static final Property HYDRA_COLLECTION = createProperty(HYDRA + "Collection");

    /**
     *
     */
    public static final Property HYDRA_PAGEDCOLLECTION = createProperty(HYDRA + "PagedCollection");

    /**
     *
     */
    public static final Property HYDRA_FIRSTPAGE = createProperty(HYDRA + "firstPage");

    /**
     *
     */
    public static final Property HYDRA_LASTPAGE = createProperty(HYDRA + "lastPage");

    /**
     *
     */
    public static final Property HYDRA_NEXTPAGE = createProperty(HYDRA + "nextPage");

    /**
     *
     */
    public static final Property HYDRA_PREVIOUSPAGE = createProperty(HYDRA + "previousPage");

    /**
     *
     */
    public static final Property INVALID_URI = createProperty("urn:invalid");

    private static Property createProperty(String uri) {
        return ResourceFactory.createProperty(uri);
    }
}