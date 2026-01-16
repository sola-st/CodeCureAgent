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

class MethodWithBody:
    def __init__(self, name, body):
        self.name = name
        self.body = body

    def to_code(self):
        return f"public void {self.name}() {{\n{self.body}\n}}"

def extract_methods(java_code):
    methods = []
    try:
        tree = parse.parse(java_code)
        for _, node in tree.filter(MethodDeclaration):
            method_name = node.name  # 获取方法名
            method_body = extract_method_body(node)  # 提取方法体
            methods.append(MethodWithBody(method_name, method_body))  # 使用自定义类存储方法信息
    except Exception as e:
        print(f"Error parsing Java code: {e}")
    return methods

def extract_method_body(node):
    # 假设 node.body 是一个表示方法体的节点列表
    body_lines = []
    for statement in node.body:
        if hasattr(statement, 'to_code'):
            body_lines.append(statement.to_code().strip())  # 转换为代码行并去掉空白
        else:
            # 处理其他可能的语句类型
            body_lines.append(str(statement).strip())  # 直接转换为字符串
    return '\n'.join(body_lines)  # 将所有行连接成一个字符串


def find_method_code(java_code, method_name):
    # 正则表达式匹配方法
    # pattern = rf'\b{method_name}\s*\(.*?\)\s*{{'
    pattern = rf'\b(public|private|protected|static)?\s+\S+\s+{method_name}\s*\(.*?\)\s*{{'
    
    # 查找所有方法的起始位置
    method_starts = [m.start() for m in re.finditer(pattern, java_code)]

    if not method_starts:
        return None
    
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

def clean_java_code(java_code):
    # Remove Markdown code fence lines
    lines = java_code.strip().splitlines()
    cleaned_lines = []
    
    # 找到第一个"```"的索引
    start_index = next((i for i, line in enumerate(lines) if line.startswith("```")), None)
    
    if start_index is not None:
        # 从"```"的下一行开始
        cleaned_lines = lines[start_index + 1:]

    # 移除结束的"```"行
    cleaned_lines = [line for line in cleaned_lines if not line.startswith("```")]
    
    return "\n".join(cleaned_lines)

def replace_methods(java_code):  #有点问题，我没法确定是哪个方法需要被替换，因为在不同class中有重名的方法；现在尝试检测那些方法中第一行是注释的
    methods = extract_methods(java_code)
    modified_code = java_code  # 用于存储修改后的代码

    for method in methods:
        # 假设 method 有一个 `body` 属性，包含方法体的语句
        if find_method_code(java_code, method.name) is not None:
            print(method.name)
            print(method.body)  # 直接打印 body

            if method.body:
                # 获取第一行代码
                first_statement = method.body.splitlines()[0].strip()  
                if first_statement.startswith("//"):
                    print("abcabc!!!!!!!!!!!!!!!!")
                    # 替换方法为 new_method
                    new_method = find_method_code(java_code, method.name)
                    # 在代码中替换原方法
                    modified_code = modified_code.replace(method.to_code(), new_method)

    return modified_code

def write_to_file(file_name, content):
    with open(file_name, 'w') as file:
        file.write(content)

        
if __name__ == "__main__":

    java_code_file = r"../refusedBequestExample/java_code.txt"  # r"C:\Users\20560\Desktop\java_code.txt"
    parent_code_file = r"../refusedBequestExample/parent_code.txt"  # r"C:\Users\20560\Desktop\parent_code.txt"
    example_code_file = r"../refusedBequestExample/example_code.txt"  # r"C:\Users\20560\Desktop\example.txt"
    

    with open(java_code_file, 'r', encoding='utf-8') as file:
         java_code = file.read()
         print(f"{java_code_file} read successfully!")

    with open(example_code_file, 'r', encoding='utf-8') as file:
         example_code = file.read()
         print(f"{example_code_file} read successfully!")

    with open(java_code_file, 'r', encoding='utf-8') as file:
         parent_code = file.read()
         print(f"{parent_code_file} read successfully!")

    model_response = refactor_code_smells_RefusedBequest(example_code,java_code,parent_code)
    #print(model_response)
    
    #response_file = r"./refusedBequestExample/sample.txt"
    # with open(response_file, 'r', encoding='utf-8') as file:
    #      model_response = file.read()
    #      print(f"{response_file} read successfully!")
    #print(model_response)
    modified_code = replace_methods(clean_java_code(model_response))  #extract all method name
    print(modified_code)

    output_dir = "../refactored_refusedbequest"
    os.makedirs(output_dir, exist_ok=True)
    write_to_file("../refactored_refusedbequest/refactored.java", clean_java_code(model_response))
    """
    for method in methods:
        if find_method_code(java_code,method.name) is not None:
            print(method.name)
            """
