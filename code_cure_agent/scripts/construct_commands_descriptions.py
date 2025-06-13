import json


read_range_desc = """read_range:  
    Reads a range of lines in a given file.  
    Required params:  
    - file_path (string)
    - start_line (int)
    - end_line (int)"""

read_sonarqube_docu_desc = """read_sonarqube_docu:  
    Returns the documentation for the given SonarQube rule.  
    The documentation can contain relevant details about the rule, when it applies, and how it can be fixed.  
    This command can only look up docu for SonarQube rules. It supports no other kind of documentation.  
    Required params:  
    - rule_key (string)"""


find_definition_desc = """find_definition:  
    Retrieve the definition of a project-local symbol (method, class, field, or variable) referenced in a file.  
    Use it to understand what a referenced symbol does by locating its implementation or declaration.  
    Only works for symbols defined in the project. Not for external libraries or standard Java classes. The symbol must not be a keyword.  
    Required params:  
    - file_path (string): Path to the file where the symbol is referenced.
    - symbol (string): Exact name of the referenced symbol (e.g., getUser, MAX_COUNT) without parantheses or qualifiers (e.g., write getUser, not getUser()).
    - symbol_line (int): Line number where the symbol is referenced in the file."""

find_references_desc = """find_references:  
    Find all project-local references (e.g., call sites or usages) of a symbol such as a method, class, field, or variable.  
    Use this to understand where and how a symbol is used across the project.  
    Use this before changing a method’s return value, return type or parameters to identify all call sites that may need updating. But there are also other situations where this can be helpful.  
    Only works for symbols defined in the project. Not for external libraries or standard Java classes. The symbol must not be a keyword.  
    Required params:
    - file_path (string): Path to the file where the symbol occurs.
    - symbol (string): Exact name of the symbol (e.g., getUser, MAX_COUNT) without parantheses or qualifiers (e.g., write getUser, not getUser()).
    - symbol_line (int): Line number where the symbol occurs in the file."""

formulate_plan_desc = """formulate_plan:  
    Formulate or update a plan, with fine-grained steps, about how you want to fix the rule violation (i.e. which lines in which files to change and to what).  
    Call this command before calling write_fix for the first time. You can call it again at any time, if you received new information that requires a change of plan.  
    Required params:  
    - plan (string)"""

write_fix_desc = """write_fix:  
    Use this command to implement the fix you came up with.  
    Only use this command if you think that you have collected all necessary information by using other commands.  
    The project will automatically be rebuilt and reanalyzed by SonarQube, to check if your fix solves the rule violation. Afterwards the project is restored to its original state.  
    Required params:  
    - changes_dicts (list[dict]): The list should contain at least one non-empty dictionary of changes. Each dict must conform to the format defined in the section `## The format of the fix`.  
    [RESPECT LINE NUMBERS AS GIVEN IN THE CODE SNIPPETS]"""


goals_accomplished_desc = """goals_accomplished:  
    Call this command if you are sure you fixed the rule violation and your write_fix attempt has been approved.  
    Give a reason why you think the rule violation was fixed successfully.  
    Required Params:  
    - reason (string)"""


answer_question_desc = """answer_question:
    Use this command to answer the currently posed question in the 'Current Question to answer' section.  
    Only call this command when you have collected enough information to answer the question.  
    Give the answer to the question and also state how certain you are about your answer.
    Required Params:  
    - answer (string): Your answer to the question."""

give_final_verdict_desc = """give_final_verdict:
    Use this command to formulate a final verdict about whether the potential rule violation is a True Positive (should fix) or a False Positive (should not fix).  
    Give an explanation why you decided for one or the other.  
    Only use this command after answering all three questions, or if you only have one command left.  
    Required Params:  
    - verdict (string): Either 'TP' or 'FP'.
    - reason (string): Explanation of what led you to your decision."""


# RepairAgent unused commands:

search_code_desc = """search_code_base:  
    Scans all Java files in a project for a list of keywords.  
    Returns a dictionary structured as: { file_name: { class_name: { method_name: [matched keywords] } } }.  
    This helps identify reusable methods or locate similar code to inform your fix strategy.  
    Note: This function does not return source code. Use extract_method_code for that. (only do it for the ones that are relevant)  
    Required params:  
    - project_name (string)
    - bug_index (int)
    - key_words (list)"""

get_classes_desc = """get_classes_and_methods:  
    Returns all class names and their methods in a file.  
    It returns a dictionary where keys are class names and values are lists of method names within each class.  
    Required params:  
    - project_name (string)
    - bug_index: (int)
    - file_path: (string)"""

get_similar_desc = """extract_similar_functions_calls:  
    Given a buggy code snippet and its file path, extracts similar function calls to help identify appropriate parameter usage.
    Required params:  
    - project_name (string)
    - bug_index (string)
    - file_path (string)
    - code_snippet (string)"""

extract_method_desc = """extract_method_code:  
    Retrieves possible implementations of a method by name in a file.
    Required params:  
    - project_name (string) 
    - bug_index (int)
    - file_path (string)
    - method_name (string)"""

generate_method_desc = """AI_generates_method_code:  
    Uses an AI model to generate a method implementation.  
    This helps see another implementation of that method given the context before it, which would help in 'probably' inferring a fix but no guarantee.  
    Required params:  
    - project_name (string)
    - bug_index (string)
    - file_path (string)
    - method_name (string)"""


commands_dict = {
    "classification": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [read_sonarqube_docu_desc, read_range_desc, find_definition_desc, find_references_desc, answer_question_desc, give_final_verdict_desc])]),
    "fix_tp": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [read_sonarqube_docu_desc, read_range_desc, find_definition_desc, find_references_desc, formulate_plan_desc, write_fix_desc, goals_accomplished_desc])]),
    "fix_fp": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [read_range_desc, write_fix_desc, goals_accomplished_desc])])
}

with open("agent_config_and_prompt_files/commands_by_state.json", "w") as cbs:
    json.dump(commands_dict, cbs)
