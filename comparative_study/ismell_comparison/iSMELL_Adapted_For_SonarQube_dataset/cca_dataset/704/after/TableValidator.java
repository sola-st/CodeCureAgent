package org.obolibrary.robot;

import com.google.common.collect.Lists;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.obolibrary.robot.export.*;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.obolibrary.robot.providers.QuotedAnnotationValueShortFormProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableValidator {

  /** Logger */
  private static final Logger logger = LoggerFactory.getLogger(ValidateOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "validate#";

  /** Error message for a rule that couldn't be parsed */
  private static final String malformedRuleError = NS + "MALFORMED RULE ERROR malformed rule: %s";

  /**
   * Error message for an invalid presence rule. Presence rules must be in the form of a truth
   * value.
   */
  private static final String invalidPresenceRuleError =
      NS
          + "INVALID PRESENCE RULE ERROR in column %d: invalid rule: \"%s\" for rule type: %s. Must be "
          + "one of: true, t, 1, yes, y, false, f, 0, no, n";

  /** Error message for invalid output format. */
  private static final String invalidFormatError =
      NS + "INVALID FORMAT ERROR '%s' must be one of: html, xlsx, or txt";

  /**
   * Error reported when a wildcard in a rule specifies a column greater than the number of columns
   * in the table.
   */
  private static final String columnOutOfRangeError =
      NS
          + "COLUMN OUT OF RANGE ERROR in column %d: rule \"%s\" indicates a column number that is "
          + "greater than the row length (%d).";

  /** Error reported when a when-clause does not have a corresponding main clause */
  private static final String noMainError =
      NS + "NO MAIN ERROR in column %d: rule: \"%s\" has when clause but no main clause.";

  /** Error reported when a when-clause can't be parsed */
  private static final String malformedWhenClauseError =
      NS + "MALFORMED WHEN CLAUSE ERROR in column %d: unable to decompose when-clause: \"%s\".";

  /** Error reported when a when-clause is of an invalid or inappropriate type */
  private static final String invalidWhenTypeError =
      NS
          + "INVALID WHEN TYPE ERROR in column %d: in clause: \"%s\": Only rules of type: %s are "
          + "allowed in a when clause.";

  /** Error reported when a query type is unrecognized */
  private static final String unrecognizedQueryTypeError =
      NS
          + "UNRECOGNIZED QUERY TYPE ERROR in column %d: query type \"%s\" not recognized in rule "
          + "\"%s\".";

  /** Error reported when a rule type is not recognized */
  private static final String unrecognizedRuleTypeError =
      NS + "UNRECOGNIZED RULE TYPE ERROR in column %d: unrecognized rule type \"%s\".";

  /** Reverse map from rule types (as Strings) to RTypeEnums, populated at load time */
  private static final Map<String, RTypeEnum> rule_type_to_rtenum_map = new HashMap<>();

  static {
    for (RTypeEnum r : RTypeEnum.values()) {
      rule_type_to_rtenum_map.put(r.getRuleType(), r);
    }
  }

  /**
   * Reverse map from rule types in the QUERY category (as Strings) to RTypeEnums, populated at load
   * time
   */
  private static final Map<String, RTypeEnum> query_type_to_rtenum_map = new HashMap<>();

  static {
    for (RTypeEnum r : RTypeEnum.values()) {
      if (r.getRuleCat() == RCatEnum.QUERY) {
        query_type_to_rtenum_map.put(r.getRuleType(), r);
      }
    }
  }

  private OWLOntology ontology;
  private String outFormat = null;
  private String outDir;

  /** The parser to use when validating class expressions */
  private ManchesterOWLSyntaxClassExpressionParser parser;

  private OWLReasoner reasoner;

  private static final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  private Map<IRI, String> iriToLabelMap;
  private Map<String, IRI> labelToIRIMap;

  private List<String> invalidTables = new ArrayList<>();
  private List<String> messages = new ArrayList<>();

  private Table outTable = null;
  private String currentTable;
  private int colNum;
  private int rowIdx;
  private int rowNum;
  private boolean valid;
  private boolean silent;

  private List<String[]> errors = new ArrayList<>();

  private Cell currentCell = null;

  private ShortFormProvider provider;

  public TableValidator(
      OWLOntology ontology,
      IOHelper ioHelper,
      ManchesterOWLSyntaxClassExpressionParser parser,
      OWLReasoner reasoner,
      String outFormat,
      String outDir) {
    this.ontology = ontology;
    this.parser = parser;
    this.reasoner = reasoner;
    if (outFormat != null) {
      // Add the format and validate it
      this.outFormat = outFormat.toLowerCase();
      if (!Lists.newArrayList("xlsx", "html", "txt").contains(this.outFormat)) {
        throw new IllegalArgumentException(String.format(invalidFormatError, outFormat));
      }
    }
    this.outDir = outDir;

    // Extract from the ontology two convenience maps from rdfs:labels to IRIs and vice versa:
    iriToLabelMap = OntologyHelper.getLabels(ontology);
    labelToIRIMap = reverseIRILabelMap(iriToLabelMap);

    // Create some providers for rendering entities
    ShortFormProvider oboProvider = new CURIEShortFormProvider(ioHelper.getPrefixes());
    provider =
        new QuotedAnnotationValueShortFormProvider(
            ontology.getOWLOntologyManager(),
            oboProvider,
            ioHelper.getPrefixManager(),
            Collections.singletonList(OWLManager.getOWLDataFactory().getRDFSLabel()),
            Collections.emptyMap());

    errors.add(new String[] {"table", "cell", "rule ID", "message"});
  }

  /** Turn logging on or off. */
  public void toggleLogging() {
    silent = !silent;
  }

  public List<String[]> getErrors() {
    return errors;
  }

  /**
   * Validate a set of tables.
   *
   * @param tables tables to validate (map of table name to table contents)
   * @param options map of validate options
   * @return List of invalid tables (or empty list on success)
   * @throws Exception on any problem
   */
  public List<String> validate(Map<String, List<List<String>>> tables, Map<String, String> options)
      throws Exception {

    int skippedRow = Integer.parseInt(OptionsHelper.getOption(options, "skip-row", "0"));

    // Validate all of the tables in turn:
    for (Map.Entry<String, List<List<String>>> table : tables.entrySet()) {
      // Reset valid for new table
      valid = true;
      outTable = new Table(outFormat);
      String tablePath = table.getKey();
      List<List<String>> data = table.getValue();

      currentTable =
          String.format(
              "%s.%s", FilenameUtils.getBaseName(tablePath), FilenameUtils.getExtension(tablePath));
      if (outFormat == null) {
        System.out.println(String.format("Validating %s ...", currentTable));
      }

      // Get the header and rules rows
      List<String> headerRow = data.remove(0);
      List<String> rulesRow = data.remove(0);

      // Get correct index for the rule row based on if a row was skipped
      int ruleRowIdx = 2;
      if (skippedRow < 3) {
        ruleRowIdx = 3;
      }

      // Get number to add to rowIdx to get true row number from input table
      // This will be either 3 or 4 (skipped row in header)
      // as rowIdx starts at 0 and does not include header and rule rows
      int addToRow;
      if (skippedRow > 0 && skippedRow <= 3) {
        // Skipped row is in header, add 1 to our reporting
        addToRow = 4;
      } else {
        addToRow = 3;
      }

      // Add header and rules rows to Table object
      for (int i = 0; i < headerRow.size(); i++) {
        String rawRule = i < rulesRow.size() ? rulesRow.get(i) : "";
        // TODO - allow different providers?
        Column c = new Column(headerRow.get(i), parseRules(rawRule), rawRule, provider);
        outTable.addColumn(c);
      }
      List<Column> columns = outTable.getColumns();

      // Validate data row by row, column by column
      for (rowIdx = 0; rowIdx < data.size(); rowIdx++) {
        rowNum = rowIdx + addToRow;
        if (rowNum == skippedRow) {
          // Skipped row occurs in the data
          addToRow = 4;
        }

        List<String> row = data.get(rowIdx);
        if (!hasContent(row)) {
          logger.debug(String.format("Skipping empty row %d", rowNum));
          continue;
        }

        Row outRow = null;
        if (outFormat != null) {
          outRow = new Row();
        }

        for (colNum = 0; colNum < columns.size(); colNum++) {
          Column c = columns.get(colNum);
          Map<String, List<String>> rules = c.getRules();

          // Get the contents of the current cell:
          String cellString = colNum < row.size() ? row.get(colNum) : "";

          // Extract all the data entries contained within the current cell:
          List<String> cellData = Lists.newArrayList(cellString.trim().split("\\|"));

          // Create the cell object
          currentCell = getCell(c, cellData);

          if (rules == null || rules.isEmpty()) {
            // No rules to validate, just add the cell exactly as is
            if (outRow != null) {
              outRow.add(currentCell);
            }
            continue;
          }

          // For each of the rules applicable to this column, validate each entry in the cell
          // against it:
          for (Map.Entry<String, List<String>> ruleEntry : rules.entrySet()) {
            for (String rule : ruleEntry.getValue()) {
              List<String> interpolatedRules = interpolateRule(rule, row);
              for (String interpolatedRule : interpolatedRules) {
                for (String d : cellData) {
                  String errorMsg = validateRule(d, interpolatedRule, row, ruleEntry.getKey());
                  if (errorMsg != null) {
                    // An error was returned, add to errors
                    errors.add(
                        new String[] {
                          currentTable,
                          IOHelper.cellToA1(rowNum, colNum + 1),
                          FilenameUtils.getBaseName(currentTable)
                              + "!"
                              + IOHelper.cellToA1(ruleRowIdx, colNum + 1),
                          errorMsg,
                        });
                  }
                }
              }
            }
          }
          if (outRow != null) {
            outRow.add(currentCell);
          }
        }
        if (outFormat != null) {
          outTable.addRow(outRow);
        }
      }

      if (!valid) {
        invalidTables.add(currentTable);
      }

      boolean standalone = OptionsHelper.optionIsTrue(options, "standalone");
      boolean writeAll = OptionsHelper.optionIsTrue(options, "write-all");

      // Write table if: write-all is true OR table is not valid (for non-null formats)
      if ((writeAll || !valid) && outFormat != null) {
        String outPath =
            outDir + "/" + FilenameUtils.getBaseName(tablePath) + "." + outFormat.toLowerCase();
        switch (outFormat.toLowerCase()) {
          case "xlsx":
            try (Workbook wb = outTable.asWorkbook("|");
                FileOutputStream fos = new FileOutputStream(outPath)) {
              wb.write(fos);
            }
            break;
          case "html":
            try (PrintWriter out = new PrintWriter(outPath)) {
              out.print(outTable.toHTML("|", standalone, true));
            }
            break;
          case "txt":
            try (PrintWriter out = new PrintWriter(outPath)) {
              for (String m : messages) {
                out.println(m);
              }
            }
            break;
        }
      }
    }
    return invalidTables;
  }

  /** Given a map from IRIs to strings, return its inverse. */
  private static Map<String, IRI> reverseIRILabelMap(Map<IRI, String> source) {
    HashMap<String, IRI> target = new HashMap<>();
    for (Map.Entry<IRI, String> entry : source.entrySet()) {
      String reverseKey = entry.getValue();
      IRI reverseValue = entry.getKey();
      if (target.containsKey(reverseKey)) {
        logger.warn(
            "Duplicate rdfs:label \"{}\". Overwriting value \"{}\" with \"{}\"",
            reverseKey,
            target.get(reverseKey),
            reverseValue);
      }
      target.put(reverseKey, reverseValue);
    }
    return target;
  }

  /**
   * Given an OWLClass describing a subject class from the ontology, an OWLClassExpression
   * describing a rule to query that subject class against, a string representing the query types to
   * use when evaluating the results of the query, and a list of strings describing a row from the
   * CSV: Determine whether, for any of the given query types, the given subject is in the result
   * set returned by the reasoner for that query type. Return true if it is in at least one of these
   * result sets, and false if it is not.
   */
  private boolean executeClassQuery(
      OWLClass subjectClass, OWLClassExpression ruleCE, List<String> row, String unsplitQueryType)
      throws Exception {

    logger.debug(
        String.format(
            "execute_class_query(): Called with parameters: "
                + "subjectClass: \"%s\", "
                + "ruleCE: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subjectClass, ruleCE, row, unsplitQueryType));

    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);

      if (isSubclassRelated(qType)) {
        if (checkSubclassConditions(subjectClass, ruleCE, qType)) return true;
      } else if (isSuperclassRelated(qType)) {
        if (checkSuperclassConditions(subjectClass, ruleCE, qType)) return true;
      } else if (qType == RTypeEnum.EQUIV || qType == RTypeEnum.NOT_EQUIV) {
        if (checkEquivalenceConditions(subjectClass, ruleCE, qType)) return true;
      } else {
        // Spit out an error in this case but continue validating the other rules:
        logger.error(
            String.format(
                "%s validation not possible for OWLClass %s.", qType.getRuleType(), subjectClass));
      }
    }
    return false;
  }

  private boolean isSubclassRelated(RTypeEnum qType) {
    return qType == RTypeEnum.SUB
        || qType == RTypeEnum.DIRECT_SUB
        || qType == RTypeEnum.NOT_SUB
        || qType == RTypeEnum.NOT_DIRECT_SUB;
  }

  private boolean isSuperclassRelated(RTypeEnum qType) {
    return qType == RTypeEnum.SUPER
        || qType == RTypeEnum.DIRECT_SUPER
        || qType == RTypeEnum.NOT_SUPER
        || qType == RTypeEnum.NOT_DIRECT_SUPER;
  }

  private boolean checkSubclassConditions(OWLClass subjectClass, OWLClassExpression ruleCE, RTypeEnum qType) {
    boolean direct = qType == RTypeEnum.DIRECT_SUB || qType == RTypeEnum.NOT_DIRECT_SUB;
    boolean not = qType == RTypeEnum.NOT_SUB || qType == RTypeEnum.NOT_DIRECT_SUB;
    NodeSet<OWLClass> subClassesFound = reasoner.getSubClasses(ruleCE, direct);
    return (not && !subClassesFound.containsEntity(subjectClass)) || (!not && subClassesFound.containsEntity(subjectClass));
  }

  private boolean checkSuperclassConditions(OWLClass subjectClass, OWLClassExpression ruleCE, RTypeEnum qType) {
    boolean direct = qType == RTypeEnum.DIRECT_SUPER || qType == RTypeEnum.NOT_DIRECT_SUPER;
    boolean not = qType == RTypeEnum.NOT_SUPER || qType == RTypeEnum.NOT_DIRECT_SUPER;
    NodeSet<OWLClass> superClassesFound = reasoner.getSuperClasses(ruleCE, direct);
    return (not && !superClassesFound.containsEntity(subjectClass)) || (!not && superClassesFound.containsEntity(subjectClass));
  }

  private boolean checkEquivalenceConditions(OWLClass subjectClass, OWLClassExpression ruleCE, RTypeEnum qType) {
    boolean not = qType == RTypeEnum.NOT_EQUIV;
    Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ruleCE);
    return (!not && equivClassesFound.contains(subjectClass)) || (not && !equivClassesFound.contains(subjectClass));
  }

  /**
   * Given an OWLClassExpression describing an unnamed subject class from the ontology, an
   * OWLClassExpression describing a rule to query that subject class against, a string representing
   * the query types to use when evaluating the results of the query, and a list of strings
   * describing a row from the CSV: Determine whether, for any of the given query types, the given
   * subject is in the result set returned by the reasoner for that query type. Return true if it is
   * in at least one of these result sets, and false if it is not.
   */
  private boolean executeGeneralizedClassQuery(
      OWLClassExpression subjectCE,
      OWLClassExpression ruleCE,
      List<String> row,
      String unsplitQueryType)
      throws Exception {

    logger.debug(
        String.format(
            "execute_generalized_class_query(): Called with parameters: "
                + "subjectCE: \"%s\", "
                + "ruleCE: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subjectCE, ruleCE, row, unsplitQueryType));

    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.SUB) {
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(subjectCE, ruleCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_SUB) {
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(subjectCE, ruleCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.SUPER) {
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(ruleCE, subjectCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_SUPER) {
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(ruleCE, subjectCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.EQUIV) {
        OWLEquivalentClassesAxiom axiom =
            dataFactory.getOWLEquivalentClassesAxiom(subjectCE, ruleCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_EQUIV) {
        OWLEquivalentClassesAxiom axiom =
            dataFactory.getOWLEquivalentClassesAxiom(subjectCE, ruleCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else {
        logger.error(
            String.format(
                "%s validation not possible for OWLClassExpression %s.",
                qType.getRuleType(), subjectCE));
      }
    }
    return false;
  }

  /**
   * Given an OWLNamedIndividual describing a subject individual from the ontology, an
   * OWLClassExpression describing a rule to query that subject individual against, a string
   * representing the query types to use when evaluating the results of the query, and a list of
   * strings describing a row from the CSV: Determine whether, for any of the given query types, the
   * given subject is in the result set returned by the reasoner for that query type. Return true if
   * it is in at least one of these result sets, and false if it is not.
   */
  private boolean executeIndividualQuery(
      OWLNamedIndividual subjectIndividual,
      OWLClassExpression ruleCE,
      List<String> row,
      String unsplitQueryType)
      throws Exception {

    logger.debug(
        String.format(
            "execute_individual_query(): Called with parameters: "
                + "subjectIndividual: \"%s\", "
                + "ruleCE: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subjectIndividual, ruleCE, row, unsplitQueryType));

    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.INSTANCE
          || qType == RTypeEnum.DIRECT_INSTANCE
          || qType == RTypeEnum.NOT_INSTANCE) {
        boolean not = qType == RTypeEnum.NOT_INSTANCE;
        NodeSet<OWLNamedIndividual> instancesFound =
            reasoner.getInstances(ruleCE, qType == RTypeEnum.DIRECT_INSTANCE);
        if ((not && !instancesFound.containsEntity(subjectIndividual))
            || (!not && instancesFound.containsEntity(subjectIndividual))) {
          return true;
        }
      } else {
        logger.error(
            String.format(
                "%s validation not possible for OWLNamedIndividual %s.",
                qType.getRuleType(), subjectIndividual));
      }
    }
    return false;
  }

  /**
   * Given a string describing a subject term, a string describing a rule to query that subject term
   * against, a string representing the query types to use when evaluating the results of the query,
   * and a list of strings describing a row from the CSV: Determine whether, for any of the given
   * query types, the given subject is in the result set returned by the reasoner for that query
   * type. Return true if it is in at least one of these result sets, and false if it is not.
   */
  private boolean executeQuery(
      String subject, String rule, List<String> row, String unsplitQueryType) throws Exception {
    logger.debug(
        String.format(
            "execute_query(): Called with parameters: "
                + "subject: \"%s\", "
                + "rule: \"%s\", "
                + "row: \"%s\", "
                + "query type: \"%s\".",
            subject, rule, row, unsplitQueryType));

    OWLClassExpression ruleCE = getClassExpression(rule);
    if (ruleCE == null) {
      report(
          String.format(
              "Unable to parse rule \"%s %s\" at column %d.", unsplitQueryType, rule, colNum + 1));
      return false;
    }

    String subjectLabel = getLabelFromTerm(subject);
    if (subjectLabel != null) {
      IRI subjectIri = labelToIRIMap.get(subjectLabel);
      OWLEntity subjectEntity = OntologyHelper.getEntity(ontology, subjectIri);
      try {
        OWLNamedIndividual subjectIndividual = subjectEntity.asOWLNamedIndividual();
        return executeIndividualQuery(subjectIndividual, ruleCE, row, unsplitQueryType);
      } catch (OWLRuntimeException e) {
        try {
          OWLClass subjectClass = subjectEntity.asOWLClass();
          return executeClassQuery(subjectClass, ruleCE, row, unsplitQueryType);
        } catch (OWLRuntimeException ee) {
          logger.error(
              String.format(
                  "While validating \"%s\" against \"%s %s\", encountered: %s",
                  subject, unsplitQueryType, rule, ee));
          return false;
        }
      }
    } else {
      OWLClassExpression subjectCE = getClassExpression(subject);
      if (subjectCE == null) {
        logger.error(String.format("Unable to parse subject \"%s\" at row %d.", subject, rowNum));
        return false;
      }
      try {
        return executeGeneralizedClassQuery(subjectCE, ruleCE, row, unsplitQueryType);
      } catch (UnsupportedOperationException e) {
        logger.error("Generalized class expression queries are not supported by this reasoner.");
        return false;
      }
    }
  }

  /**
   * Given a string describing a term from the ontology, parse it into a class expression expressed
   * in terms of the ontology. If the parsing fails, write a warning statement to the log.
   */
  private OWLClassExpression getClassExpression(String term) {
    OWLClassExpression ce;
    try {
      ce = parser.parse(term);
    } catch (OWLParserException e) {
      try {
        ce = parser.parse("'" + term + "'");
      } catch (OWLParserException ee) {
        logger.warn(
            String.format(
                "Could not determine class expression from \"%s\".\n\t%s.",
                term, e.getMessage().trim()));
        return null;
      }
    }
    return ce;
  }

  // Other existing methods remain unchanged...

  // The rest of the code remains unchanged below this point...

  /**
   * Given a string describing a rule type, return a boolean indicating whether it is one of the
   * rules recognized by ValidateOperation.
   */
  private boolean unknownRuleType(String ruleType) {
    return !rule_type_to_rtenum_map.containsKey(ruleType.split("\\|")[0]);
  }

  // ... RCatEnum and RTypeEnum as originally defined ...
  private enum RCatEnum {
    QUERY,
    PRESENCE
  }

  private enum RTypeEnum {
    DIRECT_SUPER("direct-superclass-of", RCatEnum.QUERY),
    NOT_SUPER("not-superclass-of", RCatEnum.QUERY),
    NOT_DIRECT_SUPER("not-direct-superclass-of", RCatEnum.QUERY),
    SUPER("superclass-of", RCatEnum.QUERY),
    EQUIV("equivalent-to", RCatEnum.QUERY),
    NOT_EQUIV("not-equivalent-to", RCatEnum.QUERY),
    DIRECT_SUB("direct-subclass-of", RCatEnum.QUERY),
    NOT_SUB("not-subclass-of", RCatEnum.QUERY),
    NOT_DIRECT_SUB("not-direct-subclass-of", RCatEnum.QUERY),
    SUB("subclass-of", RCatEnum.QUERY),
    DIRECT_INSTANCE("direct-instance-of", RCatEnum.QUERY),
    NOT_INSTANCE("not-instance-of", RCatEnum.QUERY),
    INSTANCE("instance-of", RCatEnum.QUERY),
    REQUIRED("is-required", RCatEnum.PRESENCE),
    EXCLUDED("is-excluded", RCatEnum.PRESENCE);

    private final String ruleType;
    private final RCatEnum ruleCat;

    RTypeEnum(String ruleType, RCatEnum ruleCat) {
      this.ruleType = ruleType;
      this.ruleCat = ruleCat;
    }

    private String getRuleType() {
      return ruleType;
    }

    private RCatEnum getRuleCat() {
      return ruleCat;
    }
  }
}