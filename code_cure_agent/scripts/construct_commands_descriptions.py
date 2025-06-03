import json

# The no_state_machine state corresponds to not using a state machine at all. It therefore includes all of the relevant commands (except commands for transitioning states)


# Understanding the Violated Rule

read_range_desc = """read_range: Reads a range of lines in a given file.  
    Required params: (file_path: string, start_line: int, end_line: int)"""

read_sonarqube_docu_desc = """read_sonarqube_docu: Returns the documentation for the given SonarQube rule.  
The documentation can contain relevant details about the rule, when it applies, and how it can be fixed.  
Required params: (rule_key: string)"""

go_to_gather_context_for_fix_desc = """go_to_gather_context_for_fix: Transitions to the state `Gathering Context for a Fix`.  
    Call this command after you have collected enough information about the specific SonarQube rule.  
    Required params: ()"""


# Gathering Context for a Fix

find_definition_desc = """find_definition: Look up the definition of a project-local symbol. For example you can look up the implementation of a called method or of a class.  
    The symbol can either be a method, class, field or variable.  
    You can use this command to inform yourself about f.e. what a relevant method or class that is being referenced in the target file does.  
    Required params:  
        file_path: string - path to the file where the symbol is referenced  
        symbol: string - the symbol that you want to look up. Needs to exactly match the symbol name, but without any braces or similar  
        symbol_line: int - line number where the symbol is referenced"""

find_references_desc = """find_references: Look up all references (f.e. call-sites) of a project-local symbol (method, class, variable etc.).  
    You can use this command to find out where and how a method/class etc. is used in the project.  
    Use this command before changing the return value/return type or parameters of a method, to check if there are any call-sites that call the method that need to be updated accordingly.  
    But there might also be other scenarios where this can be useful.  
    Required params:  
        file_path: string - path to the file where the symbol is defined  
        symbol: string - the symbol that you want to look up. Needs to exactly match the symbol name, but without any braces or similar  
        symbol_line: int - line number where the symbol is defined"""

formulate_plan_desc = """formulate_plan: Formulate or update a plan, with fine-grained steps, about how you want to fix the rule violation (i.e. which lines in which files to change and to what).  
    Call this command before calling write_fix for the first time. You can call it again at any time, if you received new information that requires a change of plan.  
    Required params: (plan: string)"""

search_code_desc = """search_code_base: Scans all Java files in a project for a list of keywords.  
    Returns a dictionary structured as: { file_name: { class_name: { method_name: [matched keywords] } } }.  
    This helps identify reusable methods or locate similar code to inform your fix strategy.  
    Note: This function does not return source code. Use extract_method_code for that. (only do it for the ones that are relevant)  
    Required params: (project_name: string, bug_index: integer, key_words: list)"""

get_classes_desc = """get_classes_and_methods: Returns all class names and their methods in a file.  
    It returns a dictionary where keys are class names and values are lists of method names within each class.  
    Required params: (project_name: string, bug_index: integer, file_path: string)"""

get_similar_desc = """extract_similar_functions_calls: Given a buggy code snippet and its file path, extracts similar function calls to help identify appropriate parameter usage.
    Required params: (project_name: string, bug_index: string, file_path: string, code_snippet: string)"""

extract_method_desc = """extract_method_code: Retrieves possible implementations of a method by name in a file.
    Required params: (project_name: string, bug_index: integer, file_path: string, method_name: string)"""

generate_method_desc = """AI_generates_method_code:  Uses an AI model to generate a method implementation.  
    This helps see another implementation of that method given the context before it, which would help in 'probably' inferring a fix but no guarantee.  
    Required params: (project_name: string, bug_index: string, file_path: string, method_name: string)"""

write_fix_desc = """write_fix: Use this command to implement the fix you came up with.  
    Only use this command if you think that you have collected all necessary information by using other commands.  
    The project will automatically be rebuilt and reanalyzed by SonarQube, to check if your fix solves the rule violation. Afterwards the project is restored to its original state. 
    Required params: (changes_dicts:list[dict])  
    The list should contain at least one non-empty dictionary of changes. Each dict must conform to the format defined in the section `## The format of the fix`.  
    [RESPECT LINE NUMBERS AS GIVEN IN THE CODE SNIPPETS]"""


go_back_to_understanding_rule_desc = """go_back_to_understanding_rule: Allows you to return back to the state `Understanding the Violated Rule` where you can collect more information about the specific rule.  
    Required Params: (reason_for_going_back: string)"""
# also read_range


# Trying out Fix Candidates

go_back_to_gather_context_for_fix_desc = """go_back_to_gather_context_for_fix: Allows you to go back to the state `Gathering Context for a Fix` where you can collect more information about the code.  
    Required Params: (reason_for_going_back: string)"""

goals_accomplished_desc = """goals_accomplished: Call this function when you are sure you fixed the bug and all tests hava passed and give the reason that made you believe that you fixed the bug successfully, params: (reason: string)"""

# also write_fix
# also read_range
# also back_to_understanding_rule_desc


commands_dict = {
    "Understanding the Violated Rule": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [read_sonarqube_docu_desc, read_range_desc, go_to_gather_context_for_fix_desc])]),
    "Gathering Context for a Fix": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [find_definition_desc, find_references_desc, formulate_plan_desc, read_range_desc, write_fix_desc, go_back_to_understanding_rule_desc])]),
    "Trying out Fix Candidates": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [write_fix_desc, read_range_desc, go_back_to_gather_context_for_fix_desc, go_back_to_understanding_rule_desc, goals_accomplished_desc])]),
    "no_state_machine": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [read_sonarqube_docu_desc, read_range_desc, find_definition_desc, find_references_desc, formulate_plan_desc, write_fix_desc, goals_accomplished_desc])])
}

with open("agent_config_and_prompt_files/commands_by_state.json", "w") as cbs:
    json.dump(commands_dict, cbs)
