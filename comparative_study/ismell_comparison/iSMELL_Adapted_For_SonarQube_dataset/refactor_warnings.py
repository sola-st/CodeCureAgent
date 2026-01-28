import shutil
import time
import click
from dotenv import load_dotenv
from openai import OpenAI
import os
import csv
from iSMELL_utility_scripts.llm_logger import log_llm_interaction

load_dotenv(verbose=True, override=True)

client = OpenAI(
    base_url=None,
    api_key=os.environ["OPENAI_API_KEY"],
)

GPT_MODEL = "gpt-4.1-mini-2025-04-14"


class Warning:
    def __init__(self, warning_id, rule_type, rule_key, rule_name, file_name, specific_message, warning_line):
        self.warning_id = warning_id
        self.rule_type = rule_type
        self.rule_key = rule_key
        self.rule_name = rule_name
        self.file_name = file_name
        self.specific_message = specific_message
        self.warning_line: int = warning_line
        self.rule_docu = self.__retrieve_rule_docu()


    def __retrieve_rule_docu(self):
        
        rule_docu = ""
        
        rule_docu_file = "sonar_qube_rule_metadata/java/metadata_full_sonar_way_profile.json"
        try:
            with open(rule_docu_file, 'r', encoding='utf-8') as file:
                import json
                data = json.load(file)
                rule = data.get(self.rule_key, None)
                if rule:
                    rule_docu = rule.get("desc", "")
                else:
                    print(f"Rule with key {self.rule_key} not found in metadata.")

        except FileNotFoundError:
            print(f"Rule docu file not found: {rule_docu_file}")
        except Exception as e:
            print(f"Error reading rule docu file: {e}")

        example = self.__ask_llm_for_example_if_docu_does_not_contain_example(rule_docu)
        if example != "":
            rule_docu += f"\nCode example:\n{example}"
        return rule_docu
    
    def __ask_llm_for_example_if_docu_does_not_contain_example(self, rule_docu):
        if "code example" in rule_docu.lower():
            return ""
        
        prompt = f"""Provide a single concise Java code example that illustrates the SonarQube rule with ID '{self.warning_id}', 
which is of type '{self.rule_type}' and named '{self.rule_name}'. The SonarQube rule has the following SonarQube docu: \n'{rule_docu}'\n 
The example should clearly demonstrate the issue that this rule addresses and how it is fixed. Provide two versions: 'Noncompliant code example' and 'Compliant solution'. Don't output anything but the code examples."""

        
        example = ""
        try:
            plain_prompt_file = os.path.join("cca_dataset", str(self.warning_id), "after", str(self.warning_id) + "_example_generation_prompt.txt")
            write_to_file(plain_prompt_file, prompt)

            print("Sending example generation prompt to LLM...")

            response = client.chat.completions.create(
                model=GPT_MODEL,
                messages=[
                    {"role": "system", "content": "You are an AI assistant that provides code examples for SonarQube warnings."},
                    {"role": "user", "content": prompt}
                ]
            )

            if response.choices:
                example = response.choices[0].message.content if response.choices[0].message else ""
            else:
                example = ""

            print("Received example generation response from LLM.")
            
            plain_response_file = os.path.join("cca_dataset", str(self.warning_id), "after", str(self.warning_id) + "_example_generation_response.txt")
            write_to_file(plain_response_file, example)
            
            output_dir = f"cca_dataset/{self.warning_id}/after"
            log_llm_interaction(
                warning_id=self.warning_id,
                interaction_type="example_generation",
                prompt=prompt,
                response_content=example,
                usage=response.usage if response else None,
                output_dir=output_dir,
                model_name=GPT_MODEL
            )

        except Exception as e:
            print(f"Error while generating example for rule docu: {e}")
            example = ""

        return example
    

    


def create_prompt_text(warning: Warning,  java_code):

    warning_line_text = retrieve_warning_line_text(warning, java_code)

    llm_prompt = ""
    if warning.rule_type == "Code_Smell":
        llm_prompt += """In computer programming, a code smell is any characteristic in the source code of a program that possibly indicates a deeper problem."""
    elif warning.rule_type == "Bug":
        llm_prompt += """In computer programming, a bug is an error, flaw or fault in a computer program that causes it to produce an incorrect or unexpected result, or to behave in unintended ways."""
    elif warning.rule_type == "Vulnerability":
        llm_prompt += """In computer programming, a vulnerability is a weakness in a computer system or application that can be exploited by an attacker to gain unauthorized access or cause harm."""
    elif warning.rule_type == "Security_Hotspot":
        llm_prompt += """In computer programming, a security hotspot is a piece of code that is not necessarily a vulnerability, but it requires review and possibly refactoring to ensure that it is secure."""
    else:
        print(f"Unknown rule type: {warning.rule_type}")
        llm_prompt += """In computer programming, there are various types of code issues such as code smells, bugs, vulnerabilities, and security hotspots that can affect the quality and security of the codebase."""

    llm_prompt += f"""\nNow I will tell you the definition about SonarQube warning '{warning.rule_key}':'{warning.rule_name}' as given by the SonarQube docu, including an example to help you solve a similar warning:""" + \
        f"""\n'{warning.rule_docu}'\n\n""" + \
        f"""Now based on the example, refactor the following Java code to eliminate the '{warning.rule_key}':'{warning.rule_name}' warning, with specific warning message '{warning.specific_message}'.""" + \
        f"""\nOutput the full refactored code (the full file). Don't leave out any part of the code.\n""" + \
        f"""The warning is located at the line '{warning_line_text}' (line number {str(warning.warning_line)}). Give the changed code using: ```...```.\n""" + \
        f"""Java code:\n```java\n{java_code}```"""


    return llm_prompt

def retrieve_warning_line_text(warning: Warning, java_code):
    lines = java_code.splitlines()
    line_index = warning.warning_line - 1  # Convert to 0-based index
    if 0 <= line_index < len(lines):
        return lines[line_index].strip()
    else:
        return str(warning.warning_line)

def refactor_warning(warning: Warning, java_code):

    llm_prompt = create_prompt_text(warning, java_code)

    model_response = ""
    try:
        plain_prompt_file = os.path.join("cca_dataset", str(warning.warning_id), "after", str(warning.warning_id) + "_refactoring_prompt.txt")
        write_to_file(plain_prompt_file, llm_prompt)
        
        print("Sending refactoring prompt to LLM...")

        response = client.chat.completions.create(
            model=GPT_MODEL,
            messages=[
                {"role": "system", "content": "I am an AI trained to refactor code smells and bugs in Java code."},
                {"role": "user", "content": llm_prompt}
            ]
        )
        
        if response.choices:
            model_response = response.choices[0].message.content if response.choices[0].message else ""
        else:
            model_response = ""

        print("Received refactoring response from LLM.")

        plain_response_file = os.path.join("cca_dataset", str(warning.warning_id), "after", str(warning.warning_id) + "_refactoring_response.txt")
        write_to_file(plain_response_file, model_response)
            
        output_dir = os.path.join("cca_dataset", str(warning.warning_id), "after")
        log_llm_interaction(
            warning_id=warning.warning_id,
            interaction_type="refactoring",
            prompt=llm_prompt,
            response_content=model_response,
            usage=response.usage if response else None,
            output_dir=output_dir,
            model_name=GPT_MODEL
        )

    except Exception as e:
        print(f"Error while detecting code smells: {e}")
        model_response = "error"  # 在异常情况下确保 model_response 是一个空字符串

    return model_response

def clean_java_code(java_code):
    # Remove Markdown code fence lines
    lines = java_code.strip().splitlines()
    cleaned_lines = []
    
    # 找到第一个"```"的索引
    start_index = next((i for i, line in enumerate(lines) if line.startswith("```")), None)
    
    end_index = next((i for i, line in enumerate(lines) if line.startswith("```") and i > start_index), None)

    if start_index is not None and end_index is not None:
        # 从第一个"```"的下一行到最后一个"```"的前一行
        cleaned_lines = lines[start_index + 1:end_index]

    return "\n".join(cleaned_lines)


def write_to_file(file_name, content):
    with open(file_name, 'w') as file:
        file.write(content)



def fix_warning(warning: Warning):
    print(f"Fixing warning ID: {warning.warning_id}, Rule: {warning.rule_key}, File: {warning.file_name}")
    java_code_file = f"cca_dataset/{warning.warning_id}/before/{warning.file_name}"
    

    with open(java_code_file, 'r', encoding='utf-8') as file:
         java_code = file.read()


    model_response = refactor_warning(warning, java_code)
    

    output_dir = os.path.join("cca_dataset", str(warning.warning_id), "after")
    os.makedirs(output_dir, exist_ok=True)
    write_to_file(os.path.join(output_dir, warning.file_name), clean_java_code(model_response))



@click.command()
@click.option(
    "--input-evaluation-dataset-csv-file",
    "-i",
    type=click.File(),
    default="./evaluation_dataset_filled_up_to_1000_input_file.csv",
    help="Path to the input evaluation dataset CSV file."
)
@click.option(
    "--start-instance-id",
    "-s",
    default=1,
    help="Instance ID to start processing from."
)
@click.option(
    "--end-instance-id",
    "-e",
    default=50,
    help="Instance ID to stop processing at."
)
def refactor_warnings(input_evaluation_dataset_csv_file, start_instance_id, end_instance_id):
    
    reader = csv.DictReader(input_evaluation_dataset_csv_file)
    for row in reader:
        if int(row['instanceID']) < start_instance_id or int(row['instanceID']) > end_instance_id:
            continue 

        # Extract warning details
        warning = Warning(
            warning_id=int(row['instanceID']),
            rule_type=row['ruleType'],
            rule_key=row['ruleKey'],
            rule_name=row['ruleName'],
            file_name=row['filePath'].split('/')[-1],
            specific_message=row['specificMessage'],
            warning_line=int(row['startLine'])
        )
        shutil.rmtree(os.path.join("cca_dataset", str(warning.warning_id), "after"), ignore_errors=True)
        os.makedirs(os.path.join("cca_dataset", str(warning.warning_id), "after"), exist_ok=True)

        time_log_file = os.path.join("cca_dataset", str(warning.warning_id), "after", "execution_time.log")

        with open(time_log_file, 'w') as log_file:
            log_file.write(f"!! Warning {warning.warning_id} fixing startup timestamp: " + str(time.time_ns()) + "\n")

        fix_warning(warning)

        with open(time_log_file, 'a+') as log_file:
            log_file.write(f"!! Warning {warning.warning_id} fixing end timestamp: " + str(time.time_ns()) + "\n")
        




if __name__ == "__main__":
    refactor_warnings()

    
    



