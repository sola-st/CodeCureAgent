/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.ontology.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import edu.cornell.mannlib.vitro.webapp.dao.jena.RDFServiceDataset;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ChangeSet;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService.ModelSerializationFormat;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFServiceException;
import edu.cornell.mannlib.vitro.webapp.rdfservice.impl.RDFServiceUtils;
import edu.cornell.mannlib.vitro.webapp.servlet.setup.JenaDataSourceSetupBase;
import edu.cornell.mannlib.vitro.webapp.utils.jena.JenaIngestUtils;

/**
 * Performs knowledge base updates necessary to align with a
 * new ontology version.
 */
public class KnowledgeBaseUpdater {

	private final Log log = LogFactory.getLog(KnowledgeBaseUpdater.class);

	private UpdateSettings settings;
	private ChangeLogger logger;
	private ChangeRecord record;

	public KnowledgeBaseUpdater(UpdateSettings settings) {
		this.settings = settings;
		this.logger = null;
		this.record = new SimpleChangeRecord(settings.getAddedDataFile(), settings.getRemovedDataFile());
	}

	public boolean update(ServletContext servletContext) throws IOException {

		if (this.logger == null) {
			this.logger = new SimpleChangeLogger(settings.getLogFile(),	settings.getErrorLogFile());
		}

		long startTime = System.currentTimeMillis();
        log.info("Performing any necessary data migration");
        logger.log("Started knowledge base migration");

        boolean changesPerformed = false;

		try {
		     changesPerformed = performUpdate(servletContext);
		} catch (Exception e) {
			 logger.logError(e.getMessage());
			 log.error(e,e);
		}

		if (!logger.errorsWritten()) {
			assertSuccess(servletContext);
	    	logger.logWithDate("Finished knowledge base migration");
		}

		record.writeChanges();
		logger.closeLogs();

		long elapsedSecs = (System.currentTimeMillis() - startTime)/1000;
		log.info("Finished checking knowledge base in " + elapsedSecs + " second" + (elapsedSecs != 1 ? "s" : ""));

		// The following was removed because it forced a recompute even if only
		// annotation values changed:
		// return record.hasRecordedChanges();

		return changesPerformed;
	}

	// returns true if ontology changes were found
	private boolean performUpdate(ServletContext servletContext) throws Exception {

		List<AtomicOntologyChange> rawChanges = getAtomicOntologyChanges();

		AtomicOntologyChangeLists changes = new AtomicOntologyChangeLists(rawChanges,settings.getNewTBoxModel(),settings.getOldTBoxModel());

		// update ABox data any time
    	log.debug("performing SPARQL CONSTRUCT additions");
    	performSparqlConstructs(settings.getSparqlConstructAdditionsDir(), settings.getRDFService(), ADD);

        log.debug("performing SPARQL CONSTRUCT retractions");
        performSparqlConstructs(settings.getSparqlConstructDeletionsDir(), settings.getRDFService(), RETRACT);

        log.info("\tchecking the abox");
        updateABox(changes);

        log.debug("performing post-processing SPARQL CONSTRUCT additions");
        performSparqlConstructs(settings.getSparqlConstructAdditionsDir() + "/post/",
                settings.getRDFService(), ADD);

        log.debug("performing post-processing SPARQL CONSTRUCT retractions");
        performSparqlConstructs(settings.getSparqlConstructDeletionsDir() + "/post/",
                settings.getRDFService(), RETRACT);


        // Only modify the TBox and migration metadata the first time
        if(updateRequired(servletContext)) {
            //process the TBox before the ABox
            try {
                log.debug("\tupdating tbox annotations");
                updateTBoxAnnotations();
            } catch (Exception e) {
                log.error(e,e);
            }
        }

        return !rawChanges.isEmpty();

	}

    private static final boolean ADD = true;
    private static final boolean RETRACT = !ADD;

    /**
     * Performs a set of arbitrary SPARQL CONSTRUCT queries on the
     * data, for changes that cannot be expressed as simple property
     * or class additions, deletions, or renamings.
     * Blank nodes created by the queries are given random URIs.
     * @param sparqlConstructDir Sparql CONSTRUCT
	 * @param rdfService RDF Service to use
	 * @param add (add = true; retract = false)
	 */
    private void performSparqlConstructs(String sparqlConstructDir,
            RDFService rdfService,
            boolean add)   throws IOException {
        Dataset dataset = new RDFServiceDataset(rdfService);
        File sparqlConstructDirectory = new File(sparqlConstructDir);
        log.debug("Using SPARQL CONSTRUCT directory " + sparqlConstructDirectory);
        if (!sparqlConstructDirectory.isDirectory()) {
            String logMsg = this.getClass().getName() +
                    "performSparqlConstructs() expected to find a directory " +
                    " at " + sparqlConstructDir + ". Unable to execute " +
                    " SPARQL CONSTRUCTS.";
            logger.logError(logMsg);
            log.error(logMsg);
            return;
        }
        List<File> sparqlFiles = Arrays.asList(sparqlConstructDirectory.listFiles());
        Collections.sort(sparqlFiles); // queries may depend on being run in a certain order
        JenaIngestUtils jiu = new JenaIngestUtils();
        for (File sparqlFile : sparqlFiles) {
            if(sparqlFile.isDirectory()) {
                continue;
            }
            StringBuilder fileContents = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(sparqlFile));
                String ln;
                while ( (ln = reader.readLine()) != null) {
                    fileContents.append(ln).append('\n');
                }
            } catch (FileNotFoundException fnfe) {
                String logMsg = "WARNING: performSparqlConstructs() could not find " +
		private class AtomicOntologyChangeLists {

			private List<AtomicOntologyChange> atomicClassChanges =
					new ArrayList<AtomicOntologyChange>();

			private List<AtomicOntologyChange> atomicPropertyChanges =
					new ArrayList<AtomicOntologyChange>();

			public AtomicOntologyChangeLists (
					List<AtomicOntologyChange> changeList, OntModel newTboxModel,
					OntModel oldTboxModel) throws IOException {

				for (AtomicOntologyChange changeObj : changeList) {
					if (changeObj.getSourceURI() != null) {
						triageChangeBySource(changeObj, oldTboxModel);
					} else if (changeObj.getDestinationURI() != null) {
						triageChangeByDestination(changeObj, newTboxModel);
					} else {
						logger.log("WARNING: Source and Destination URI can't be null. " + "Change Object skipped");
					}
				}
				//logger.log("Property and Class change Object lists have been created");
			}

			private void triageChangeBySource(AtomicOntologyChange changeObj, OntModel oldTboxModel) {
				log.debug("triaging " + changeObj);
				if (oldTboxModel.getOntProperty(changeObj.getSourceURI()) != null) {
					atomicPropertyChanges.add(changeObj);
					log.debug("added to property changes");
				} else if (oldTboxModel.getOntClass(changeObj.getSourceURI()) != null) {
					atomicClassChanges.add(changeObj);
					log.debug("added to class changes");
				} else if ("Prop".equals(changeObj.getNotes())) {
					atomicPropertyChanges.add(changeObj);
				} else if ("Class".equals(changeObj.getNotes())) {
					atomicClassChanges.add(changeObj);
				} else {
					logger.log("WARNING: Source URI is neither a Property nor a Class. Change Object skipped for sourceURI: " + changeObj.getSourceURI());
				}
			}

			private void triageChangeByDestination(AtomicOntologyChange changeObj, OntModel newTboxModel) {
				if (newTboxModel.getOntProperty(changeObj.getDestinationURI()) != null) {
					atomicPropertyChanges.add(changeObj);
				} else if (newTboxModel.getOntClass(changeObj.getDestinationURI()) != null) {
					atomicClassChanges.add(changeObj);
				} else {
					logger.log("WARNING: Destination URI is neither a Property nor a Class. Change Object skipped for destinationURI: " + changeObj.getDestinationURI());
				}
			}

			public List<AtomicOntologyChange> getAtomicClassChanges() {
				return atomicClassChanges;
			}

			public List<AtomicOntologyChange> getAtomicPropertyChanges() {
				return atomicPropertyChanges;
			}

		}
