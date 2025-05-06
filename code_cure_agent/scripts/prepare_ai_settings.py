import argparse

template =\
    """ai_goals:
- Locate the Bug: Execute test cases and get info to systematically identify and isolate the bug within the project \"{name}\" and bug index \"{bug_index}\".
- Perform code Analysis: Analyze the lines of code associated with the bug to discern and comprehend the potentially faulty sections.
- Try simple Fixes: Attempt straightforward remedies, such as altering operators, changing identifiers, modifying numerical or boolean literals, adjusting function arguments, or refining conditional statements. Explore all plausible and elementary fixes relevant to the problematic code.
- Try complex Fixes: In instances where simple fixes prove ineffective, utilize the information gathered to propose more intricate solutions aimed at resolving the bug.
- Iterative Testing: Repeat the debugging process iteratively, incorporating the insights gained from each iteration, until all test cases pass successfully.
ai_name: RepairAgentV0.6.5
ai_role: |
  You are an AI assistant specialized in fixing bugs in Java code. 
  You will be given a buggy project, and your objective is to autonomously understand and fix the bug.
  You have three states, which each offer a unique set of commands,
   * 'collect information to understand the bug', where you gather information to understand a bug;
   * 'collect information to fix the bug', where you gather information to fix the bug;
   * 'trying out candidate fixes', where you suggest bug fixes that will be validated by a test suite.
api_budget: 0.0
warning_repository_URL: '{warning_repository_URL}'
warning_repository_commit: '{warning_repository_commit}'
warning_rule_key: '{warning_rule_key}'
warning_file_path: '{warning_file_path}'
warning_start_line: {warning_start_line}
warning_rule_name: '{warning_rule_name}'
warning_specific_message: '{warning_specific_message}'
"""

parser = argparse.ArgumentParser()
parser.add_argument("name")
parser.add_argument("index")
parser.add_argument("warning_repository_URL")
parser.add_argument("warning_repository_commit")
parser.add_argument("warning_rule_key")
parser.add_argument("warning_file_path")
parser.add_argument("warning_start_line")
parser.add_argument("warning_rule_name")
parser.add_argument("warning_specific_message")
args = parser.parse_args()

settings = template.format(name=args.name, bug_index=args.index, warning_repository_URL=args.warning_repository_URL, warning_repository_commit=args.warning_repository_commit, warning_rule_key=args.warning_rule_key,
                           warning_file_path=args.warning_file_path, warning_start_line=args.warning_start_line, warning_rule_name=args.warning_rule_name, warning_specific_message=args.warning_specific_message)

with open("agent_config_and_prompt_files/ai_settings.yaml", "w") as set_yaml:
    set_yaml.write(settings)
