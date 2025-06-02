from __future__ import annotations


from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from autogpt.agents import BaseAgent

import os
import json
import subprocess
import time
import random
import select

import javalang
import javalang.tree

from autogpt.logs.logger import logger

from autogpt.command_decorator import command
from autogpt.utils.path_utils import path_utils
from autogpt.commands.repository_operations import checkout_project

COMMAND_CATEGORY = "method_lookup"
COMMAND_CATEGORY_TITLE = "Commands for looking up methods (go to definition and go to references)"

ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
    "go_to_definition",
    "Look up the definition of a project local symbol (method, class, variable etc.).",
    {
        "file_path": {
            "type": "string",
            "description": "The path to the file where the symbol to look up occurs.",
            "required": True,
        },
        "symbol_to_look_up": {
            "type": "string",
            "description": "The symbol that you want to look up. Exactly match the symbol name, but don't include braces and similar.",
            "required": True

        },
        "symbol_line": {
            "type": "integer",
            "description": "The line number where the symbol occurs.",
            "required": True

        }
    },
)
def go_to_definition(file_path: str, symbol_to_look_up: str, symbol_line: int, agent: BaseAgent) -> str:
    if symbol_line <= 0:
        return f"Lookup of definition failed. The symbol_line was {str(symbol_line)}, but must be greater than 0."

    # LSP expects 0-indexed line
    symbol_line_zero_indexed = int(symbol_line) - 1

    try:
        file_path = path_utils.preprocess_paths(
            agent.config.workspace_path, agent.ai_config.warning_repository_name, file_path)

        symbol_column_zero_indexed = find_column_of_symbol(
            file_path, symbol_to_look_up, symbol_line_zero_indexed, agent)

    except ValueError as ve:
        logger.error("LSP definition lookup failed ",
                     "Error was: " + str(ve))
        return "Lookup of definition failed. " + str(ve)

    try:
        lookup_result = lsp_definition_lookup(file_path, symbol_line_zero_indexed,
                                              symbol_column_zero_indexed, agent)
    except Exception as e:
        logger.error("LSP definition lookup failed ",
                     "Error was: " + str(e))
        checkout_project(agent)
        return "Lookup of definition failed. " + str(e)

    if "result" not in lookup_result or len(lookup_result["result"]) == 0:
        checkout_project(agent)
        return f"Lookup of definition failed. No definition could be found for '{symbol_to_look_up}' at line {str(symbol_line)} in file '{file_path}'. " \
            "\nDon't try the command with the same arguments again."

    # TODO: test whether returning the full method/class/field etc. makes sense (esp. for long classes)
    # or if we should instead only return the file and position of the definition. => let agent use read_range where it sees fit
    try:
        full_file_path: str = lookup_result["result"][0]["uri"]
        full_file_path = full_file_path.lstrip("file:///")
        full_file_path = "/" + full_file_path
        repo_path = os.path.join(agent.config.workspace_path,
                                 agent.ai_config.warning_repository_name)

        file_found_definition = full_file_path.replace(f"{repo_path}/", "").replace(
            f"{agent.config.workspace_path}/", "").replace(f"{agent.config.workspace_path}", "")

        start_line_definition = lookup_result["result"][0]["range"]["start"]["line"] + 1

        found_definition_code = extract_code_for_symbol(
            symbol_to_look_up, full_file_path, start_line_definition)

    except Exception as e:
        logger.error("Accessing definition failed ",
                     "Error was: " + str(e))
        checkout_project(agent)
        return "Lookup of definition failed. " + str(e)

    checkout_project(agent)

    return f"The definition of '{symbol_to_look_up}' was found in file '{file_found_definition}' starting at line {str(start_line_definition)}.  "\
        "\nThe code of the definition is the following:  \n" + found_definition_code


def find_column_of_symbol(file_path: str, symbol_to_look_up: str, symbol_line_zero_indexed: int, agent: BaseAgent) -> int:

    with open(os.path.join(agent.config.workspace_path, agent.ai_config.warning_repository_name, file_path)) as fp:
        file_lines = fp.readlines()

    if len(file_lines) <= symbol_line_zero_indexed:
        raise ValueError(
            f"The symbol_line {str(symbol_line_zero_indexed + 1)} was out of range for the file '{file_path}' with {str(len(file_lines))} lines.")

    target_line = file_lines[symbol_line_zero_indexed]

    try:
        symbol_column_zero_indexed = target_line.index(symbol_to_look_up)
    except ValueError:
        raise ValueError(
            f"The symbol_to_look_up '{symbol_to_look_up}' was not found in line {str(symbol_line_zero_indexed + 1)} of file '{file_path}'.")

    return symbol_column_zero_indexed


def lsp_definition_lookup(file_path: str, symbol_line_zero_indexed: int, symbol_column_zero_indexed: int, agent: BaseAgent) -> str:

    request_id = random.randint(100, 2147483647)
    definition_request = {
        "jsonrpc": "2.0",
        "id": request_id,
        "method": "textDocument/definition", "params": {
            "textDocument": {
                "uri": "file://" + os.path.join(agent.config.workspace_path, agent.ai_config.warning_repository_name, file_path)
            },
            "position": {
                "line": symbol_line_zero_indexed,
                "character": symbol_column_zero_indexed
            }
        }}

    string_request = json.dumps(definition_request)
    command = prepare_command(agent.config.workspace_path)

    logger.debug("COMMAND: " + command)

    if not os.path.exists(os.path.join(agent.config.workspace_path, "lspeclipse")):
        prepare_lsp_env(agent.config.workspace_path)

    if not os.path.exists(os.path.join(agent.config.workspace_path, "lspeclipse", "lsp_init_file.json")):
        prepare_init_file(agent)

    with open(os.path.join(agent.config.workspace_path, "lspeclipse", "lsp_init_file.json")) as inif:
        init_content = inif.read()

    clean_project_of_gradle_build_files(agent)

    definition_lookup_result = execute_command(
        command, init_content, string_request, request_id)

    return definition_lookup_result


def prepare_command(workspace_path: str):
    cmd = [f'cd {str(os.path.join(workspace_path, "lspeclipse"))} &&',
           '/usr/lib/jvm/java-17-openjdk-amd64/bin/java',
           '-Declipse.application=org.eclipse.jdt.ls.core.id1',
           '-Dosgi.bundles.defaultStartLevel=4',
           '-Declipse.product=org.eclipse.jdt.ls.core.product',
           '-Dlog.level=WARN',
           '-Xmx1G',
           '--add-modules=ALL-SYSTEM',
           '--add-opens java.base/java.util=ALL-UNNAMED',
           '--add-opens java.base/java.lang=ALL-UNNAMED',
           '-jar',
           './plugins/org.eclipse.equinox.launcher_1.6.900.v20240613-2009.jar',
           '-configuration',
           './config_linux',
           '-data',
           str(workspace_path)
           ]
    return " ".join(cmd)


def prepare_lsp_env(destination_path: str):
    source_path = "lspeclipse"

    # Using subprocess to execute the cp command
    command = ["cp", "-r", source_path, destination_path]

    subprocess.run(command, check=True)


def prepare_init_file(agent: BaseAgent):
    with open(os.path.join(agent.config.workspace_path, "lspeclipse", "lsp_init_template.json")) as lit:
        lsp_template = json.load(lit)

    root_path = os.path.join(agent.config.workspace_path,
                             agent.ai_config.warning_repository_name)
    root_uri = "file://" + root_path

    lsp_template["params"]["rootPath"] = root_path
    lsp_template["params"]["rootUri"] = root_uri
    lsp_template["params"]["initializationOptions"]["workspaceFolders"] = [
        root_uri]
    lsp_template["params"]["workspaceFolders"][0]["uri"] = root_uri
    lsp_template["params"]["workspaceFolders"][0]["name"] = agent.ai_config.warning_repository_name

    with open(os.path.join(agent.config.workspace_path, "lspeclipse", "lsp_init_file.json"), "w") as json_handler:
        json.dump(lsp_template, json_handler)


def clean_project_of_gradle_build_files(agent: BaseAgent) -> None:
    # Remove gradle build files if present in the project to force using maven
    build_gradle_path = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name, "build.gradle")
    build_gradle_kts_path = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name, "build.gradle.kts")
    settings_gradle_path = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name, "settings.gradle")
    setting_gradle_kts_path = os.path.join(
        agent.config.workspace_path, agent.ai_config.warning_repository_name, "settings.gradle.kts")

    if os.path.exists(build_gradle_path):
        os.remove(build_gradle_path)
    if os.path.exists(build_gradle_kts_path):
        os.remove(build_gradle_kts_path)
    if os.path.exists(settings_gradle_path):
        os.remove(settings_gradle_path)
    if os.path.exists(setting_gradle_kts_path):
        os.remove(setting_gradle_kts_path)


def execute_command(command: str, init_req: str, req: str, request_id: int) -> dict:

    # Open the subprocess with stdout redirected to a file
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stdin=subprocess.PIPE, text=True, shell=True)
    try:
        content_length = len(init_req)
        request = "Content-Length: {}\r\n\r\n{}".format(
            content_length, init_req)
        # Send the input to the subprocess
        process.stdin.write(request)
        process.stdin.flush()

        initalization_msg_id = 1
        timeout_initialization = 30
        init_result = read_message_from_subprocess(
            process, initalization_msg_id, timeout_initialization, True)
        logger.debug(json.dumps(
            init_result), title="LSP initialization result when running look_up_definition: ")

        content_length = len(req)
        request = "Content-Length: {}\r\n\r\n{}".format(content_length, req)
        # Send the input to the subprocess
        process.stdin.write(request)
        process.stdin.flush()

        timeout = 10
        definition_lookup_result = read_message_from_subprocess(
            process, request_id, timeout, False)

        logger.debug(json.dumps(
            definition_lookup_result), title="LSP definition lookup result: ")

        process.kill()

        return definition_lookup_result

    # Be save that the process is killed if any unexpected exception occured
    except Exception as e:
        logger.error("LSP definition lookup failed ",
                     "During running the subprocess an excpetion occured. " + str(e))
        process.kill()
        raise e


def read_message_from_subprocess(process: subprocess.Popen, target_message_id: int, timeout: int, init_req: bool) -> dict:
    start_time = time.time()
    while True:
        ready, _, _ = select.select([process.stdout], [], [], 1)
        if ready:
            chunk = os.read(process.stdout.fileno(), 4096).decode()
            next_lines = chunk.splitlines()
            for next_line in next_lines:
                try:
                    parsed_next_line = json.loads(next_line)
                    if init_req:
                        # Look for the "Started" message if it is an init request
                        if "method" in parsed_next_line and parsed_next_line["method"] == "language/status" and "params" in parsed_next_line:
                            params_init = parsed_next_line["params"]
                            if "type" in params_init and params_init["type"] == "Started":
                                return parsed_next_line
                    else:
                        # Look for the message with the correct id if it was not an init request
                        if "id" in parsed_next_line:
                            message_id = parsed_next_line["id"]
                            if int(message_id) == target_message_id:
                                return parsed_next_line

                except ValueError:
                    # The read line was not one of the json-messages, so wait for the next
                    pass

        if time.time() - start_time > timeout:
            process.kill()
            raise TimeoutError(
                f"The lookup ran into a timeout after {timeout} seconds.")


def extract_code_for_symbol(symbol_to_look_up: str, full_file_path: str, start_line_definition: int) -> str:

    with open(full_file_path, encoding='utf-8', errors='ignore') as jf:
        content = jf.read()
    tree = javalang.parse.parse(content)
    content_split = content.splitlines(True)

    for _, node in tree.filter(javalang.tree.Declaration):
        if searched_tree_node(node, symbol_to_look_up, start_line_definition):
            start, end = get_start_end_for_node(
                node, tree, len(content_split))
            output = ""
            len_preamble = 0
            # We can't handle annotations for now
            if hasattr(node, "documentation") and node.documentation:
                len_preamble = len(node.documentation.splitlines())

            for i in range(start - 1 - len_preamble, end):
                output += f"Line {str(i + 1)}:{content_split[i]}"

            return output

    raise Exception(
        f"Code could not be extracted from the found definition location.")


def searched_tree_node(node: javalang.tree.Declaration, symbol_to_look_up: str, start_line_definition: int) -> bool:
    # Most Declarations types have a name field
    if hasattr(node, "name") and node.name == symbol_to_look_up and node.position[0] == start_line_definition:
        return True

    # VariableDeclaration or FieldDeclaration type nodes don't have a name field directly
    elif hasattr(node, "declarators") and len(node.declarators) > 0 and node.declarators[0].name == symbol_to_look_up and node.position[0] == start_line_definition:
        return True

    else:
        return False


def get_start_end_for_node(node_to_find, tree, max_end):
    start = None
    end = None
    for path, node in tree:
        if start is not None and node_to_find not in path:
            # Save upper bound of the end position (might include comments of following lines)
            end = node.position[0]
            return start, end
        if start is None and node == node_to_find:
            start = node.position[0]
    if start is not None:
        end = max_end
    return start, end
