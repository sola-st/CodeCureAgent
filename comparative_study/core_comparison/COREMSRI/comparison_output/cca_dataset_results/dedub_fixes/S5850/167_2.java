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

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query type is unrecognized, inform the user but continue on.
    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.SUB
          || qType == RTypeEnum.DIRECT_SUB
          || qType == RTypeEnum.NOT_SUB
          || qType == RTypeEnum.NOT_DIRECT_SUB) {
        // Check to see if the subjectClass is a (direct) subclass of the given rule:
        // Get direct and not bools
        boolean direct = false;
        if (qType == RTypeEnum.DIRECT_SUB || qType == RTypeEnum.NOT_DIRECT_SUB) {
          direct = true;
        }
        boolean not = false;
        if (qType == RTypeEnum.NOT_SUB || qType == RTypeEnum.NOT_DIRECT_SUB) {
          not = true;
        }
        NodeSet<OWLClass> subClassesFound = reasoner.getSubClasses(ruleCE, direct);
        if (not && !subClassesFound.containsEntity(subjectClass)
            || !not && subClassesFound.containsEntity(subjectClass)) {
          // NOT and not in set OR in set
          return true;
        }

      } else if (qType == RTypeEnum.SUPER
          || qType == RTypeEnum.DIRECT_SUPER
          || qType == RTypeEnum.NOT_SUPER
          || qType == RTypeEnum.NOT_DIRECT_SUPER) {
        // Check to see if the subjectClass is a (direct) superclass of the given rule:
        // Get direct and not bools
        boolean direct = false;
        if (qType == RTypeEnum.DIRECT_SUPER || qType == RTypeEnum.NOT_DIRECT_SUPER) {
          direct = true;
        }
        boolean not = false;
        if (qType == RTypeEnum.NOT_SUPER || qType == RTypeEnum.NOT_DIRECT_SUPER) {
          not = true;
        }

        NodeSet<OWLClass> superClassesFound = reasoner.getSuperClasses(ruleCE, direct);
        if (not && !superClassesFound.containsEntity(subjectClass)
            || !not && superClassesFound.containsEntity(subjectClass)) {
          // NOT and not in set OR in set
          return true;
        }

      } else if (qType == RTypeEnum.EQUIV || qType == RTypeEnum.NOT_EQUIV) {
        // Check to see if the subjectClass is an equivalent of the given rule:
        boolean not = false;
        if (qType == RTypeEnum.NOT_EQUIV) {
          not = true;
        }
        Node<OWLClass> equivClassesFound = reasoner.getEquivalentClasses(ruleCE);
        if (!not && equivClassesFound.contains(subjectClass)
            || not && !equivClassesFound.contains(subjectClass)) {
          return true;
        }

      } else {
        // Spit out an error in this case but continue validating the other rules:
        logger.error(
            String.format(
                "%s validation not possible for OWLClass %s.", qType.getRuleType(), subjectClass));
      }
    }
    return false;
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

    // For each of the query types associated with the rule, check to see if the rule is satisfied
    // thus interpreted. If it is, then we return true, since multiple query types are interpreted
    // as a disjunction. If a query type is unrecognized, inform the user but continue on.
    String[] queryTypes = unsplitQueryType.split("\\|");
    for (String queryType : queryTypes) {
      if (unknownRuleType(queryType)) {
        throw new Exception(
            String.format(unrecognizedQueryTypeError, colNum + 1, queryType, unsplitQueryType));
      }

      RTypeEnum qType = query_type_to_rtenum_map.get(queryType);
      if (qType == RTypeEnum.SUB) {
        // Check to see if the subjectClass is a subclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(subjectCE, ruleCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_SUB) {
        // Check to see if the subjectClass is a subclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(subjectCE, ruleCE);
        if (!reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.SUPER) {
        // Check to see if the subjectClass is a superclass of the given rule:
        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(ruleCE, subjectCE);
        if (reasoner.isEntailed(axiom)) {
          return true;
        }
      } else if (qType == RTypeEnum.NOT_SUPER) {
        // Check to see if the subjectClass is a superclass of the given rule:
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
        // Spit out an error in this case but continue validating the other rules:
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
```java
    term = term.replaceAll("^(?:')|'$", "");

      // Print error if not silent
      System.out.println(outStr);
    }

    if (outFormat != null && !outFormat.equals("txt")) {
      // We want to put formatting on cells with errors
      if (outFormat.equals("xlsx")) {
        // Set the style of the current cell to a red background with a white font:
        currentCell.setFontColor(IndexedColors.WHITE);
        currentCell.setCellPattern(FillPatternType.FINE_DOTS);
        currentCell.setCellColor(IndexedColors.RED);
      } else {
        // Set the HTML class to bg-danger (red background with a white font)
        currentCell.setHTMLClass("bg-danger");
      }
      // Attach a comment to the cell
      // If one for this cell already exists, add new comment to existing comment
      String commentString = String.format(format, positionalArgs);
      String currentComment = currentCell.getComment();
      if (currentComment != null) {
        commentString = currentComment + "; " + commentString;
      }
      currentCell.setComment(commentString);
    } else if (outFormat != null) {
      // Add outStr to messages to be written to file
      messages.add(outStr);
    }
  }

  /**
   * Given a string describing a rule type, return a boolean indicating whether it is one of the
   * rules recognized by ValidateOperation.
   */
  private boolean unknownRuleType(String ruleType) {
    return !rule_type_to_rtenum_map.containsKey(ruleType.split("\\|")[0]);
  }

  /**
   * Given a string describing the content of a rule and a string describing its rule type, return a
   * simple map entry such that the `key` for the entry is the main clause of the rule, and the
   * `value` for the entry is a list of the rule's when-clauses. Each when-clause is itself stored
   * as an array of three strings, including the subject to which the when-clause is to be applied,
   * the rule type for the when clause, and the actual axiom to be validated against the subject.
   */
  private AbstractMap.SimpleEntry<String, List<String[]>> separateRule(String rule, String ruleType)
      throws Exception {

    // Check if there are any when clauses:
    Matcher m = Pattern.compile("(\\(\\s*when\\s+.+\\))(.*)").matcher(rule);
    String whenClauseStr;
    if (!m.find()) {
      // If there is no when clause, then just return back the rule string as it was passed with an
      // empty when clause list:
      logger.debug(String.format("No when-clauses found in rule: \"%s\".", rule));
      return new AbstractMap.SimpleEntry<>(rule, new ArrayList<>());
    }

    // Throw an exception if there is no main clause and this is not a PRESENCE rule:
    if (m.start() == 0 && rule_type_to_rtenum_map.get(ruleType).getRuleCat() != RCatEnum.PRESENCE) {
      throw new Exception(String.format(noMainError, colNum + 1, rule));
    }

    // Extract the actual content of the when-clause.
    whenClauseStr = m.group(1);
    whenClauseStr = whenClauseStr.substring("(when ".length(), whenClauseStr.length() - 1);

    // Don't fail just because there is some extra garbage at the end of the rule, but notify
    // the user about it:
    if (!m.group(2).trim().equals("")) {
      logger.warn(
          String.format("Ignoring string \"%s\" at end of rule \"%s\".", m.group(2).trim(), rule));
    }

    // Within each when clause, multiple subclauses separated by ampersands are allowed. Each
    // subclass must be of the form: <Entity> <Rule-Type> <Axiom>, where: <Entity> is a (not
    // necessarily interpolated) string describing either a label or a generalised DL class
    // expression involving labels, and any label names containing spaces are enclosed within
    // single quotes; <Rule-Type> is a possibly hyphenated alphanumeric string (which corresponds
    // to one of the rule types defined above in RTypeEnum); and <Axiom> can take any form.
    // Here we resolve each sub-clause of the when statement into a list of such triples.
    ArrayList<String[]> whenClauses = new ArrayList<>();
    for (String whenClause : whenClauseStr.split("\\s*&\\s*")) {
      m =
          Pattern.compile("^([^\'\\s()]+|\'[^\']+\'|\\(.+?\\))" + "\\s+([a-z\\-|]+)" + "\\s+(.*)$")
              .matcher(whenClause);

      if (!m.find()) {
        throw new Exception(String.format(malformedWhenClauseError, colNum + 1, whenClause));
      }
      // Add the triple to the list of when clauses:
      whenClauses.add(new String[] {m.group(1), m.group(2), m.group(3)});
    }

    // Now get the main part of the rule (i.e. the part before the when clause):
    m = Pattern.compile("^(.+)\\s+\\(when\\s").matcher(rule);
    if (m.find()) {
      return new AbstractMap.SimpleEntry<>(m.group(1), whenClauses);
    }

    // If no main clause is found, then if this is a PRESENCE rule, implicitly assume that the main
    // clause is "true":
    if (rule_type_to_rtenum_map.get(ruleType).getRuleCat() == RCatEnum.PRESENCE) {
      return new AbstractMap.SimpleEntry<>("true", whenClauses);
    }

    // We should never get here since we have already checked for an empty main clause earlier ...
    logger.error(
        String.format(
            "Encountered unknown error while looking for main clause of rule \"%s\".", rule));
    // Return the rule as passed with an empty when clause list:
    return new AbstractMap.SimpleEntry<>(rule, new ArrayList<>());
  }

  /**
   * Given a string describing a rule, a rule of the type PRESENCE, and a string representing a cell
   * from the CSV, determine whether the cell satisfies the given presence rule (e.g. is-required,
   * is-empty).
   */
  private String validatePresenceRule(String rule, RTypeEnum rType, String cell) throws Exception {

    logger.debug(
        String.format(
            "validate_presence_rule(): Called with parameters: "
                + "rule: \"%s\", "
                + "rule type: \"%s\", "
                + "cell: \"%s\".",
            rule, rType.getRuleType(), cell));

    // Presence-type rules (is-required, is-excluded) must be in the form of a truth value:
    if ((Arrays.asList("true", "t", "1", "yes", "y").indexOf(rule.toLowerCase()) == -1)
        && (Arrays.asList("false", "f", "0", "no", "n").indexOf(rule.toLowerCase()) == -1)) {
      throw new Exception(
          String.format(invalidPresenceRuleError, colNum + 1, rule, rType.getRuleType()));
    }

    // If the restriction isn't "true" then there is nothing to do. Just return:
    if (Arrays.asList("true", "t", "1", "yes", "y").indexOf(rule.toLowerCase()) == -1) {
      logger.debug(
          String.format("Nothing to validate for rule: \"%s %s\"", rType.getRuleType(), rule));
      return null;
    }

    String msg;
    switch (rType) {
      case REQUIRED:
        if (cell.trim().equals("")) {
          msg =
              String.format(
                  "Cell is empty but rule: \"%s %s\" does not allow this.",
                  rType.getRuleType(), rule);
          report(msg);
          return msg;
        }
        break;
      case EXCLUDED:
        if (!cell.trim().equals("")) {
          msg =
              String.format(
                  "Cell is non-empty (\"%s\") but rule: \"%s %s\" does not allow this.",
                  cell, rType.getRuleType(), rule);
          report(msg);
          return msg;
        }
        break;
      default:
        msg =
            String.format(
                "%s validation of rule type: \"%s\" is not yet implemented.",
                rType.getRuleCat(), rType.getRuleType());
        logger.error(msg);
        return msg;
    }
    logger.info(
        String.format("Validated \"%s %s\" against \"%s\".", rType.getRuleType(), rule, cell));
    return null;
  }

  /**
   * Given a string describing a cell from the CSV, a string describing a rule to be applied against
   * that cell, a string describing the type of that rule, and a list of strings describing the row
   * containing the given cell, validate the cell, indicating any validation errors via the output
   * writer (or XLSX workbook).
   */
  private String validateRule(String cell, String rule, List<String> row, String ruleType)
      throws Exception {

    logger.debug(
        String.format(
            "validate_rule(): Called with parameters: "
                + "cell: \"%s\", "
                + "rule: \"%s\", "
                + "row: \"%s\", "
                + "rule type: \"%s\".",
            cell, rule, row, ruleType));

    logger.info(String.format("Validating rule \"%s %s\" against \"%s\".", ruleType, rule, cell));
    if (unknownRuleType(ruleType)) {
      throw new Exception(String.format(unrecognizedRuleTypeError, colNum + 1, ruleType));
    }

    // Separate the given rule into its main clause and optional when clauses:
    AbstractMap.SimpleEntry<String, List<String[]>> separatedRule = separateRule(rule, ruleType);

    // Evaluate and validate any when clauses for this rule first:
    if (!validateWhenClauses(separatedRule.getValue(), row, colNum)) {
      logger.debug("Not all when clauses have been satisfied. Skipping main clause");
      return null;
    }

    // Once all of the when clauses have been validated, get the RTypeEnum representation of the
    // primary rule type of this rule:
    RTypeEnum primRType = rule_type_to_rtenum_map.get(ruleType.split("\\|")[0]);

    // If the primary rule type for this rule is not in the QUERY category, process it at this step
    // and return control to the caller. The further steps below are only needed when queries are
    // going to be sent to the reasoner.
    if (primRType.getRuleCat() != RCatEnum.QUERY) {
      return validatePresenceRule(separatedRule.getKey(), primRType, cell);
    }

    // If the cell contents are empty, just return to the caller silently (if the cell is not
    // expected to be empty, this will have been caught by one of the presence rules in the
    // previous step, assuming such a rule is constraining the column).
    if (cell.trim().equals("")) return null;

    // Get the axiom that the cell will be validated against:
    String axiom = separatedRule.getKey();

    // Send the query to the reasoner:
    // Comment may be null on exception, empty on success, or a non-empty String on validation
    // failure
    // Non-empty strings get added to the Cell
    boolean result = executeQuery(cell, axiom, row, ruleType);
    String msg = null;
    if (!result) {
      msg = String.format("Validation failed for rule: \"%s %s %s\".", cell, ruleType, axiom);
      report(msg);
    } else {
      logger.info(String.format("Validated: \"%s %s %s\".", cell, ruleType, axiom));
    }
    return msg;
  }

  /**
   * Given a list of String arrays describing a list of when-clauses, and a list of Strings
   * describing the row to which these when-clauses belong, validate the when-clauses one by one,
   * returning false if any of them fails to be satisfied, and true if they are all satisfied.
   */
  private boolean validateWhenClauses(List<String[]> whenClauses, List<String> row, int colNum)
      throws Exception {

    for (String[] whenClause : whenClauses) {
      String subject = whenClause[0].trim();
      // If the subject term is blank, then skip this clause:
      if (subject.equals("")) {
        continue;
      }

      // Make sure all of the rule types in the when clause are of the right category:
      String whenRuleType = whenClause[1];
      for (String whenRuleSubType : whenRuleType.split("\\|")) {
        RTypeEnum whenSubRType = rule_type_to_rtenum_map.get(whenRuleSubType);
        if (whenSubRType == null || whenSubRType.getRuleCat() != RCatEnum.QUERY) {
          throw new Exception(
              String.format(
                  invalidWhenTypeError,
                  colNum + 1,
                  String.join(" ", whenClause),
                  query_type_to_rtenum_map.keySet()));
        }
      }

      // Get the axiom to validate and send the query to the reasoner:
      String axiom = whenClause[2];
      if (!executeQuery(subject, axiom, row, whenRuleType)) {
        // If any of the when clauses fail to be satisfied, then we do not need to evaluate any
        // of the other when clauses, or the main clause, since the main clause may only be
        // evaluated when all of the when clauses are satisfied.
        logger.info(
            String.format(
                "When clause: \"%s %s %s\" is not satisfied.", subject, whenRuleType, axiom));
        return false;
      } else {
        logger.info(
            String.format("Validated when clause \"%s %s %s\".", subject, whenRuleType, axiom));
      }
    }
    // If we get to here, then all of the when clauses have been satisfied, so return true:
    return true;
  }

  /**
   * An enum representation of the different categories of rules. We distinguish between queries,
   * which involve queries to a reasoner, and presence rules, which check for the existence of
   * content in a cell.
   */
  private enum RCatEnum {
    QUERY,
    PRESENCE
  }

  /**
   * An enum representation of the different types of rules. Each rule type belongs to larger
   * category, and is identified within the CSV file by a particular string.
   */
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