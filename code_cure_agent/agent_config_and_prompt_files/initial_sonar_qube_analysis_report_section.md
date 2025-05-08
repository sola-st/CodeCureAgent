## Initial SonarQube Analysis Report

The following JSON object contains all violations of rules identified by SonarQube in the analyzed source file.  
Each entry under minedRules includes:  

* the ruleKey, identifying the violated rule,  
* the ruleName and ruleType,  
* one or more warningLocations, each specifying:  
  * the file path (filePath),  
  * the exact range (startLine, startColumn, endLine, endColumn),  
  * and a specificMessage explaining the violation in context.

```json
{analysis_report_json}
```