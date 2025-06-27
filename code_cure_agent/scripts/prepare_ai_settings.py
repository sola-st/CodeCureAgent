import argparse

template =\
    """ai_name: CodeCureAgent
warning_ID: {warning_ID}
warning_repository_URL: {warning_repository_URL}
warning_repository_commit: {warning_repository_commit}
warning_repository_target_java_version: {warning_repository_target_java_version}
warning_rule_key: {warning_rule_key}
warning_file_path: {warning_file_path}
warning_start_line: {warning_start_line}
warning_rule_name: >-
  {warning_rule_name}
warning_specific_message: >-
  {warning_specific_message}
warning_rule_type: {warning_rule_type}
"""

parser = argparse.ArgumentParser()
parser.add_argument("warning_ID")
parser.add_argument("warning_repository_URL")
parser.add_argument("warning_repository_commit")
parser.add_argument("warning_repository_target_java_version")
parser.add_argument("warning_rule_key")
parser.add_argument("warning_file_path")
parser.add_argument("warning_start_line")
parser.add_argument("warning_rule_name")
parser.add_argument("warning_specific_message")
parser.add_argument("warning_rule_type")
args = parser.parse_args()


settings = template.format(warning_ID=args.warning_ID, warning_repository_URL=args.warning_repository_URL, warning_repository_commit=args.warning_repository_commit, warning_repository_target_java_version=args.warning_repository_target_java_version, warning_rule_key=args.warning_rule_key,
                           warning_file_path=args.warning_file_path, warning_start_line=args.warning_start_line, warning_rule_name=args.warning_rule_name, warning_specific_message=args.warning_specific_message, warning_rule_type=args.warning_rule_type)

with open("agent_config_and_prompt_files/ai_settings.yaml", "w") as set_yaml:
    set_yaml.write(settings)
