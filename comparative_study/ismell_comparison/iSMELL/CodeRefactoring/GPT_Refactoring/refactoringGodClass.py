import httpx
from openai import OpenAI
from javalang import parse
from javalang.tree import MethodDeclaration
import re
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
                the refactored code should not have feature envy smell.
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

def refactor_code_smells_GodClass(example_java_code,java_code):
    try:
        response = client.chat.completions.create(
            model="gpt-4-1106-preview",
            messages=[
                {"role": "system", "content": "I am an AI trained to refactor code smells in Java code."},
                {"role": "user", "content": f'''In computer programming, a code smell is any characteristic in the 
                source code of a program that possibly indicates a deeper problem. I will now tell you the definition 
                about God Class. Please read the definition and refactor the code according to the target to 
                eliminate this smell. The definition of God Class is: God Class is a large and unwieldy class that 
                takes on too many responsibilities within an application. It concentrates a multitude of functions, 
                oversees numerous objects, and effectively tries to do everything. Here is an example for refactoring 
                code which has God Class smell, Please read it carefully and learn to show only the most modified 
                critical code after refactoring. You can replace functional code with comments. You do not need to 
                keep the structure, improve the code's readability and maintainability {example_java_code}Now based 
                on the example, refactor the following code. You don't need to give the full code, just give the most 
                modified part of it. You should not output extra content.
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

def refactor_code_smells_RefusedBequest(example_java_code,java_code, parent_code):
    try:
        response = client.chat.completions.create(
            model="gpt-4-1106-preview",
            messages=[
                {"role": "system", "content": "I am an AI trained to refactor code smells in Java code."},
                {"role": "user", "content": f'''In computer programming, a code smell is any characteristic in the 
                source code of a program that possibly indicates a deeper problem. I will now tell you the definition 
                about Refused Bequest. Please read the definition and refactor the code according to the target to 
                eliminate this smell. The definition of Refused Bequest is: Refused Bequest occurs if a subclass uses 
                only some of the methods and properties inherited from its parents. This is an indication that the 
                class should not be a subclass of that parent class, since child classes should be adding or 
                modifying functionality. Here is an example for refactoring code which has Refused Bequest smell, 
                Please read it carefully and learn to show only the most modified critical code after refactoring. 
                You can replace functional code with comments. You do not need to keep the structure, improve the 
                code's readability and maintainability {example_java_code}Now I will give you a piece of code that 
                needs refactoring and the superclass of that code. Please based on the example, refactor the 
                following code that needs refactoring. You don't need to give the full code, just give the most 
                modified part of it. You should not output extra content. This is the superclass of the code that 
                needs refactoring: {parent_code}
                This is the code that needs refactoring:
                {java_code}
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


def extract_methods(java_code):
    methods = []
    try:
        tree = parse.parse(java_code)
        for _, node in tree.filter(MethodDeclaration):
            methods.append(node)
    except Exception as e:
        print(f"Error parsing Java code: {e}")
    return methods

def classify(model_response,methodname):
    try:
        response = client.chat.completions.create(
            model="gpt-4-1106-preview",
            messages=[
                {"role": "system", "content": "I am an AI trained to refactor code smells in Java code."},
                {"role": "user", "content": f'''I will provide you with all the method names from the code before 
                refactoring. Please advise on which classes in the refactored code these methods should be assigned 
                to. {methodname}\n\n{model_response}
                Output the suggestion for class distribution after refactoring in the following normalized format:
                `ClassName: [cn1], MethodName: [mn1, mn2, mn3]`.
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

def find_method_code(java_code, method_name):
    # 正则表达式匹配方法
    # pattern = rf'\b{method_name}\s*\(.*?\)\s*{{'
    pattern = rf'\b(public|private|protected|static)?\s+\S+\s+{method_name}\s*\(.*?\)\s*{{'
    
    # 查找所有方法的起始位置
    method_starts = [m.start() for m in re.finditer(pattern, java_code)]
    
    methods_code = []
    
    for start in method_starts:
        # 从方法起始位置开始查找结束大括号
        balance = 1
        end = start + java_code[start:].find('{') + 1
        
        while balance > 0 and end < len(java_code):
            if java_code[end] == '{':
                balance += 1
            elif java_code[end] == '}':
                balance -= 1
            end += 1
        
        # 提取方法代码
        methods_code.append(java_code[start:end])
    
    return methods_code

def generate_java_code(class_name, method_names, java_code):
    # 生成的 Java 代码
    generated_code = f"public class {class_name} {{\n"
    
    for method_name in method_names:
        # 使用 find_method_code 查找方法代码
        method_code = find_method_code(java_code, method_name)
        
        if method_code:
            generated_code += f"    // Method: {method_name}\n"
            generated_code += f"    {method_code[0].strip()}\n\n"  # 只取第一个匹配
        
    generated_code += "}"
    
    return generated_code

def write_to_file(file_name, content):
    with open(file_name, 'w') as file:
        file.write(content)

if __name__ == "__main__":

    java_code_file = r"../godClassExample/java_code.txt"  # r"C:\Users\20560\Desktop\java_code.txt"
    example_code_file = r"../godClassExample/example.txt"  # r"C:\Users\20560\Desktop\example.txt"
    

    with open(java_code_file, 'r', encoding='utf-8') as file:
         java_code = file.read()
         print(f"{java_code_file} read successfully!")

    with open(example_code_file, 'r', encoding='utf-8') as file:
         example_code = file.read()
         print(f"{example_code_file} read successfully!")


    methods = extract_methods(java_code)
    for method in methods:
        print(f"Method name: {method.name}")
    model_response = refactor_code_smells_GodClass(example_code,java_code)
    classified_methods = classify(model_response,methods)
    # model_response = refactor_code_smells_FeatureEnvy(example_code,java_code)
    # model_response = refactor_code_smells_RefusedBequest(example_code,java_code,parent_code)
    print(model_response)
    print(classified_methods)

    output_dir = "../refactored_godclass"
    os.makedirs(output_dir, exist_ok=True)
    for line in classified_methods.strip().split('\n'):   # 使用正则表达式提取 className 和 methodName
        match = re.match(r'(\w+): \[(.*?)\]', line)
        if match:
            class_name = match.group(1)
            method_names = match.group(2).split(', ')
            generated_java_code = generate_java_code(class_name, method_names, java_code)
            #print(generated_java_code)
            file_name = os.path.join(output_dir, f"{class_name}.java")  # 在 refactored_godclass 文件夹中创建文件
            write_to_file(file_name, generated_java_code)

