package org.obolibrary.robot;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience methods to get related entities for a set of IRIs. Allowed relation options are: -
 * ancestors - descendants - disjoints - domains - equivalents - inverses - ranges - types
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class RelatedObjectsHelper {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(RelatedObjectsHelper.class);

  /** Shared data factory. */
  private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

  /** Namespace for error messages. */
  private static final String NS = "errors#";

  /** Error message when --axioms is not a valid AxiomType. Expects: input string. */
  private static final String axiomTypeError = NS + "AXIOM TYPE ERROR %s is not a valid axiom type";

  /**
   * Error message when a datatype is given for an annotation, but the annotation value does not
   * match the datatype.
   */
  private static final String literalValueError = NS + "LITERAL VALUE ERROR %s is not a %s value";

  /** Error message when an invalid IRI is provided. Expects the entry field and term. */
  private static final String invalidIRIError =
      NS + "INVALID IRI ERROR %1$s \"%2$s\" is not a valid CURIE or IRI";

  /** Error message when an IRI pattern to match does not have a wildcard character. */
  private static final String invalidIRIPatternError =
      NS
          + "INVALID IRI PATTERN ERROR the pattern '%s' must contain at least one wildcard character.";

  /** String for subclass/property mapping */
  private static final String SUB = "sub";
  /** String for superclass/property mapping */
  private static final String SUPER = "super";

  /**
   * Given an ontology and a set of objects, return the annotation axioms for all objects.
   *
   * @param ontology OWLOntology to get annotations from
   * @param objects OWLObjects to get annotations of
   * @return set of OWLAxioms
   */
  public static Set<OWLAxiom> getAnnotationAxioms(OWLOntology ontology, Set<OWLObject> objects) {
    Set<OWLAxiom> axioms = new HashSet<>();
    for (OWLObject object : objects) {
      if (object instanceof OWLClass) {
        OWLClass cls = (OWLClass) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(cls.getIRI()));
      } else if (object instanceof OWLObjectProperty) {
        OWLObjectProperty prop = (OWLObjectProperty) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(prop.getIRI()));
      } else if (object instanceof OWLDataProperty) {
        OWLDataProperty prop = (OWLDataProperty) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(prop.getIRI()));
      } else if (object instanceof OWLAnnotationProperty) {
        OWLAnnotationProperty prop = (OWLAnnotationProperty) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(prop.getIRI()));
      } else if (object instanceof OWLDatatype) {
        OWLDatatype dt = (OWLDatatype) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(dt.getIRI()));
      } else if (object instanceof OWLNamedIndividual) {
        OWLNamedIndividual indiv = (OWLNamedIndividual) object;
        axioms.addAll(ontology.getAnnotationAssertionAxioms(indiv.getIRI()));
      }
    }
    return axioms;
  }

  /**
   * Filter a set of OWLAxioms based on the provided arguments.
   *
   * @param inputAxioms Set of OWLAxioms to filter
   * @param objects Set of OWLObjects to get axioms for
   * @param axiomSelectors List of string selectors for types of axioms
   * @param baseNamespaces List of string base namespaces used for internal/external selections
   * @param partial if true, get any axiom containing at least one OWLObject from objects
   * @param namedOnly if true, ignore anonymous OWLObjects in axioms
   * @return set of filtered OWLAxioms
   * @throws OWLOntologyCreationException on issue creating empty ontology for tautology checker
   */
  public static Set<OWLAxiom> filterAxioms(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      List<String> axiomSelectors,
      List<String> baseNamespaces,
      boolean partial,
      boolean namedOnly)
      throws OWLOntologyCreationException {

    // Go through the axiom selectors in order and process selections
    boolean internal = false;
    boolean external = false;
    Set<OWLAxiom> filteredAxioms = new HashSet<>();
    for (String axiomSelector : axiomSelectors) {
      if (axiomSelector.equalsIgnoreCase("internal")) {
        if (external) {
          logger.error(
              "ignoring 'internal' axiom selector - 'internal' and 'external' together will remove all axioms");
        }
        filteredAxioms.addAll(
            RelatedObjectsHelper.filterInternalAxioms(inputAxioms, baseNamespaces));
        internal = true;
      } else if (axiomSelector.equalsIgnoreCase("external")) {
        if (internal) {
          logger.error(
              "ignoring 'external' axiom selector - 'internal' and 'external' together will remove all axioms");
        }
        filteredAxioms.addAll(
            RelatedObjectsHelper.filterExternalAxioms(inputAxioms, baseNamespaces));
        external = true;
      } else if (axiomSelector.equalsIgnoreCase("tautologies")) {
        filteredAxioms.addAll(RelatedObjectsHelper.filterTautologicalAxioms(inputAxioms, false));
      } else if (axiomSelector.equalsIgnoreCase("structural-tautologies")) {
        filteredAxioms.addAll(RelatedObjectsHelper.filterTautologicalAxioms(inputAxioms, true));
      } else {
        // Assume this is a normal OWLAxiom type
        Set<Class<? extends OWLAxiom>> axiomTypes =
            RelatedObjectsHelper.getAxiomValues(axiomSelector);
        filteredAxioms.addAll(
            filterAxiomsByAxiomType(inputAxioms, objects, axiomTypes, partial, namedOnly));
      }
    }
    return filteredAxioms;
  }

  /**
   * Given a set of OWLAxioms, a set of OWLObjects, a set of OWLAxiom Classes, a partial boolean,
   * and a named-only boolean, return a set of OWLAxioms based on OWLAxiom type. The axiom is added
   * to the set if it is an instance of an OWLAxiom Class in the axiomTypes set. If partial, return
   * any axioms that use at least one object from the objects set. Otherwise, only return axioms
   * that use objects in the set. If namedOnly, only consider named OWLObjects in the axioms and
   * ignore any anonymous objects when selecting the axioms.
   *
   * @param axioms set of OWLAxioms to filter
   * @param objects set of OWLObjects to get axioms for
   * @param axiomTypes set of OWLAxiom Classes that determines what type of axioms will be selected
   * @param partial if true, include all axioms that use at least one OWLObject from objects
   * @param namedOnly if true, ignore anonymous OWLObjects used in axioms
   * @return set of filtered axioms
   */
  public static Set<OWLAxiom> filterAxiomsByAxiomType(
      Set<OWLAxiom> axioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean partial,
      boolean namedOnly) {
    if (partial) {
      return RelatedObjectsHelper.filterPartialAxioms(axioms, objects, axiomTypes, namedOnly);
    } else {
      return RelatedObjectsHelper.filterCompleteAxioms(axioms, objects, axiomTypes, namedOnly);
    }
  }

  /**
   * Given a st of axioms, a set of objects, and a set of axiom types, return a set of axioms where
   * all the objects in those axioms are in the set of objects.
   *
   * @param inputAxioms Set of OWLAxioms to filter
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, consider only named OWLObjects
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> filterCompleteAxioms(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    axiomTypes = setDefaultAxiomType(axiomTypes);
    if (namedOnly) {
      return getCompleteAxiomsNamedOnly(inputAxioms, objects, axiomTypes);
    } else {
      return getCompleteAxiomsIncludeAnonymous(inputAxioms, objects, axiomTypes);
    }
  }

  /**
   * Given a list of base namespaces and a set of axioms, return only the axioms that DO NOT have a
   * subject in the base namespaces.
   *
   * @param axioms set of OWLAxioms
   * @param baseNamespaces list of base namespaces
   * @return external OWLAxioms
   */
  public static Set<OWLAxiom> filterExternalAxioms(
      Set<OWLAxiom> axioms, List<String> baseNamespaces) {
    Set<OWLAxiom> externalAxioms = new HashSet<>();
    for (OWLAxiom axiom : axioms) {
      Set<IRI> subjects = getAxiomSubjects(axiom);
      if (!isBase(baseNamespaces, subjects)) {
        externalAxioms.add(axiom);
      }
    }
    return externalAxioms;
  }

  /**
   * Given a list of base namespaces and a set of axioms, return only the axioms that have a subject
   * in the base namespaces.
   *
   * @param axioms set of OWLAxioms
   * @param baseNamespaces list of base namespaces
   * @return internal OWLAxioms
   */
  public static Set<OWLAxiom> filterInternalAxioms(
      Set<OWLAxiom> axioms, List<String> baseNamespaces) {
    Set<OWLAxiom> internalAxioms = new HashSet<>();
    for (OWLAxiom axiom : axioms) {
      Set<IRI> subjects = getAxiomSubjects(axiom);
      if (isBase(baseNamespaces, subjects)) {
        internalAxioms.add(axiom);
      }
    }
    return internalAxioms;
  }

  /**
   * Given a set of axioms, a set of objects, and a set of axiom types, return a set of axioms where
   * at least one object in those axioms is also in the set of objects.
   *
   * @param inputAxioms Set of OWLAxioms to filter
   * @param objects Set of OWLObjects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, only consider named OWLObjects
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> filterPartialAxioms(
      Set<OWLAxiom> inputAxioms,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    axiomTypes = setDefaultAxiomType(axiomTypes);
    if (namedOnly) {
      return getPartialAxiomsNamedOnly(inputAxioms, objects, axiomTypes);
    } else {
      return getPartialAxiomsIncludeAnonymous(inputAxioms, objects, axiomTypes);
    }
  }

  /**
   * Given a set of OWLAxioms and a structural boolean, filter for tautological axioms, i.e., those
   * that would be true in any ontology.
   *
   * @param axioms set of OWLAxioms to filter
   * @param structural if true, only filter for structural tautological axioms based on a hard-coded
   *     set of patterns (e.g., X subClassOf owl:Thing, owl:Nothing subClassOf X, X subClassOf X)
   * @return tautological OWLAxioms from original set
   * @throws OWLOntologyCreationException on issue creating empty ontology for tautology checker
   */
  public static Set<OWLAxiom> filterTautologicalAxioms(Set<OWLAxiom> axioms, boolean structural)
      throws OWLOntologyCreationException {
    // If structural, checker will be null
    OWLReasoner tautologyChecker = ReasonOperation.getTautologyChecker(structural);
    Set<OWLAxiom> tautologies = new HashSet<>();
    for (OWLAxiom a : axioms) {
      if (ReasonOperation.isTautological(a, tautologyChecker, structural)) {
        tautologies.add(a);
      }
    }
    return tautologies;
  }

  /**
   * @deprecated Do not forget to remove this deprecated code someday.
   * Replaced by {@link #filterAxiomsByAxiomType(Set, Set, Set, boolean, boolean)}
   * @param ontology OWLOntology to get axioms from
   * @param objects OWLObjects to get axioms about
   * @param axiomTypes Set of OWLAxiom Classes that are the types of OWLAxioms to return
   * @param partial if true, return any OWLAxiom that has at least one OWLObject from objects
   * @param signature if true, ignore anonymous OWLObjects in axioms
   * @return axioms from ontology based on options
   */
  @Deprecated
  public static Set<OWLAxiom> getAxioms(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean partial,
      boolean signature) {
    // For backward compatibility, still return results using the recommended methods
    if (partial) {
      return RelatedObjectsHelper.getPartialAxioms(ontology, objects, axiomTypes, signature);
    } else {
      return RelatedObjectsHelper.getCompleteAxioms(ontology, objects, axiomTypes, signature);
    }
  }

  /**
   * Given a string axiom selector, return the OWLAxiom Class type(s) or null. The selector is
   * either a special keyword that represents a set of axiom classes or the name of an OWLAxiom
   * Class.
   *
   * @param axiomSelector string option to get the OWLAxiom Class type or types for
   * @return set of OWLAxiom Class types based on the string option
   */
  public static Set<Class<? extends OWLAxiom>> getAxiomValues(String axiomSelector) {
    Set<String> ignore =
        new HashSet<>(
            Arrays.asList("internal", "external", "tautologies", "structural-tautologies"));
    if (ignore.contains(axiomSelector)) {
      // Ignore special axiom options
      return null;
    }

    Set<Class<? extends OWLAxiom>> axiomTypes = new HashSet<>();
    if (axiomSelector.equalsIgnoreCase("all")) {
      axiomTypes.add(OWLAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("logical")) {
      axiomTypes.add(OWLLogicalAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("annotation")) {
      axiomTypes.add(OWLAnnotationAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("subclass")) {
      axiomTypes.add(OWLSubClassOfAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("subproperty")) {
      axiomTypes.add(OWLSubObjectPropertyOfAxiom.class);
      axiomTypes.add(OWLSubDataPropertyOfAxiom.class);
      axiomTypes.add(OWLSubAnnotationPropertyOfAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("equivalent")) {
      axiomTypes.add(OWLEquivalentClassesAxiom.class);
      axiomTypes.add(OWLEquivalentObjectPropertiesAxiom.class);
      axiomTypes.add(OWLEquivalentDataPropertiesAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("disjoint")) {
      axiomTypes.add(OWLDisjointClassesAxiom.class);
      axiomTypes.add(OWLDisjointObjectPropertiesAxiom.class);
      axiomTypes.add(OWLDisjointDataPropertiesAxiom.class);
      axiomTypes.add(OWLDisjointUnionAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("type")) {
      axiomTypes.add(OWLClassAssertionAxiom.class);
    } else if (axiomSelector.equalsIgnoreCase("abox")) {
      axiomTypes.addAll(
          AxiomType.ABoxAxiomTypes.stream()
              .map(AxiomType::getActualClass)
              .collect(Collectors.toSet()));
    } else if (axiomSelector.equalsIgnoreCase("tbox")) {
      axiomTypes.addAll(
          AxiomType.TBoxAxiomTypes.stream()
              .map(AxiomType::getActualClass)
              .collect(Collectors.toSet()));
    } else if (axiomSelector.equalsIgnoreCase("rbox")) {
      axiomTypes.addAll(
          AxiomType.RBoxAxiomTypes.stream()
              .map(AxiomType::getActualClass)
              .collect(Collectors.toSet()));
    } else if (axiomSelector.equalsIgnoreCase("declaration")) {
      axiomTypes.add(OWLDeclarationAxiom.class);
    } else {
      AxiomType<?> at = AxiomType.getAxiomType(axiomSelector);
      if (at != null) {
        // Attempt to get the axiom type based on AxiomType names
        axiomTypes.add(at.getActualClass());
      } else {
        throw new IllegalArgumentException(String.format(axiomTypeError, axiomSelector));
      }
    }
    return axiomTypes;
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where all
   * the objects in those axioms are in the set of objects. Prefer {@link #filterCompleteAxioms(Set,
   * Set, Set, boolean)}.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> getCompleteAxioms(
      OWLOntology ontology, Set<OWLObject> objects, Set<Class<? extends OWLAxiom>> axiomTypes) {
    return filterCompleteAxioms(ontology.getAxioms(), objects, axiomTypes, false);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where all
   * the objects in those axioms are in the set of objects. Prefer {@link #filterCompleteAxioms(Set,
   * Set, Set, boolean)}.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, consider only named OWLObjects
   * @return Set of OWLAxioms containing only the OWLObjects
   */
  public static Set<OWLAxiom> getCompleteAxioms(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    return filterCompleteAxioms(ontology.getAxioms(), objects, axiomTypes, namedOnly);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where at
   * least one object in those axioms is also in the set of objects. Prefer {@link
   * #filterPartialAxioms(Set, Set, Set, boolean)}.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> getPartialAxioms(
      OWLOntology ontology, Set<OWLObject> objects, Set<Class<? extends OWLAxiom>> axiomTypes) {
    return filterPartialAxioms(ontology.getAxioms(), objects, axiomTypes, false);
  }

  /**
   * Given an ontology, a set of objects, and a set of axiom types, return a set of axioms where at
   * least one object in those axioms is also in the set of objects. Prefer {@link
   * #filterPartialAxioms(Set, Set, Set, boolean)}.
   *
   * @param ontology OWLOntology to get axioms from
   * @param objects Set of objects to match in axioms
   * @param axiomTypes OWLAxiom types to return
   * @param namedOnly when true, only consider named OWLObjects
   * @return Set of OWLAxioms containing at least one of the OWLObjects
   */
  public static Set<OWLAxiom> getPartialAxioms(
      OWLOntology ontology,
      Set<OWLObject> objects,
      Set<Class<? extends OWLAxiom>> axiomTypes,
      boolean namedOnly) {
    return filterPartialAxioms(ontology.getAxioms(), objects, axiomTypes, namedOnly);
  }

  // The rest of the class unchanged ...

  // All other methods remain as is
}