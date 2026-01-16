import httpx
from openai import OpenAI
import os

client = OpenAI(
    base_url="https://api.xty.app/v1",
    api_key="sk-SIdpFf5nFU0PEzJ0Ec8a10A1969747D6Be9858F2B7F9A522",
    http_client=httpx.Client(
        base_url="https://api.xty.app/v1",
        follow_redirects=True,
    ),
)

def refactor_code_smells_FeatureEnvy(example_java_code,java_code):

    try:
        response = client.chat.completions.create(
            model="gpt-4-1106-preview",
            messages=[
                {"role": "system", "content": "I am an AI trained to refactor code smells in Java code."},
                {"role": "user", "content": f'''In computer programming, a code smell is any characteristic in the 
                source code of a program that possibly indicates a deeper problem. I will now tell you the definition 
                about Feature Envy. Please read the definition and refactor the code according to the target to 
                eliminate this smell. The definition of Feature Envy is: Feature Envy occurs when a method in one 
                class can't seem to keep its eyes off the data of another class. This sneaky behavior hints that 
                there might be a better home for the method, where it fits in more naturally and keeps the codebase 
                cleaner and easier to manage. Here is an example for refactoring code which has Feature Envy smell, 
                Please read it carefully and learn to show only the most modified critical code after refactoring. 
                You can replace functional code with comments. You do not need to keep the structure, improve the 
                code's readability and maintainability. {example_java_code}Now based on the example, refactor the 
                following code to eliminate the feature envy code smell. You can achieve it by dividing the method in 
                original code into as many methods or classes as possible. Also make sure every methods you give in 
                the refactored_godclass code should not have feature envy smell.
                \n{java_code}
                '''}
            ],
            max_tokens=1500
        )
        if response.choices:
            model_response = response.choices[0].message.content if response.choices[0].message else ""
        else:
            model_response = ""

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


if __name__ == "__main__":

    java_code_file = r"../featureEnvyExample/5/java_code.txt"  # r"C:\Users\20560\Desktop\java_code.txt"
    example_code_file = r"../featureEnvyExample/example.txt"  # r"C:\Users\20560\Desktop\example.txt"
    

    with open(java_code_file, 'r', encoding='utf-8') as file:
         java_code = file.read()
         print(f"{java_code_file} read successfully!")

    with open(example_code_file, 'r', encoding='utf-8') as file:
         example_code = file.read()
         print(f"{example_code_file} read successfully!")

    model_response = refactor_code_smells_FeatureEnvy(example_code,java_code)
    print(model_response)

    output_dir = "../refactored_featureenvy"
    os.makedirs(output_dir, exist_ok=True)
    write_to_file("../refactored_featureenvy/refactored.java", clean_java_code(model_response))
