import json






## Understanding the Violated Rule

read_range_desc = """read_range: Reads a range of lines in a given file.  
    Required params: (project_name:string, bug_index:string, file_path:string, start_line: int, end_line:int)"""

formulate_plan_desc = """formulate_plan: Formulate a plan, with fine-grained steps, that describes how you want to approach collecting enough information about the specific rule violation and fixing it.  
    Call this command after you have collected enough information about the SonarQube rule.  
    By calling this command, you also automatically switch to the state `Gathering Context for a Fix`.  
    Required params: (plan: string)"""


## Gathering Context for a Fix

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
    Required params: (project_name: str, bug_index: str, file_path: str, method_name: str)"""

write_fix_desc = """write_fix: Use this command to implement the fix you came up with.  
    Only use this command if you think that you have collected all necessary information by using other commands.  
    The project will automatically be rebuilt and reanalyzed by SonarQube. Changes are reverted automatically if the build fails or if the rule violation remains.  
    Required params: (project_name: string, bug_index: integer, changes_dicts:list[dict])  
    The list should contain at least one non-empty dictionary of changes. Each dict must conform to the format defined in the section `## The format of the fix`.
    Note: If you're not in the `Trying out Fix Candidates` state, using this command will automatically switch you to it.  
    [RESPECT LINE NUMBERS AS GIVEN IN THE CODE SNIPPETS]"""

update_plan_desc = """update_plan: Change your previously formulated plan on how to approach fixing the rule violation.  
    Maybe you have found new information that requires a change of plan. If so use this command.  
    Required params: (plan: string)"""

go_back_to_understanding_rule_desc = """go_back_to_understanding_rule: Allows you to return back to the state `Understanding the Violated Rule` where you can collect more information about the specific rule.  
    Required Params: (reason_for_going_back: string)"""
### also read_range


## Trying out Fix Candidates

go_back_to_collect_more_context_info_desc = """go_back_to_collect_more_context_info: Allows you to go back to the state `Gathering Context for a Fix` where you can collect more information about the code.  
    Required Params: (reason_for_going_back: string)"""

goals_accomplished_desc = """goals_accomplished: Call this function when you are sure you fixed the bug and all tests hava passed and give the reason that made you believe that you fixed the bug successfully, params: (reason: string)"""

### also write_fix
### also read_range
### also back_to_understanding_rule_desc



commands_dict = {
    "Understanding the Violated Rule": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [read_range_desc, formulate_plan_desc])]),
    "Gathering Context for a Fix": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [search_code_desc, get_classes_desc, get_similar_desc, extract_method_desc, read_range_desc, generate_method_desc, write_fix_desc, update_plan_desc, go_back_to_understanding_rule_desc])]),
    "Trying out Fix Candidates": "\n".join(["{}. {}".format(i+1, t) for i, t in enumerate(
        [write_fix_desc, read_range_desc, go_back_to_collect_more_context_info_desc, go_back_to_understanding_rule_desc, goals_accomplished_desc])])
}

with open("agent_config_and_prompt_files/commands_by_state.json", "w") as cbs:
    json.dump(commands_dict, cbs)