from __future__ import annotations

import os
import time
from typing import List, Literal, Optional

from colorama import Fore

from agent_core.config import Config
from typing import TYPE_CHECKING

from agent_core.utils.path_utils.path_utils import sanitize_and_shorten_file_path

if TYPE_CHECKING:
    from agent_core.agents.base import BaseAgent

from ..api_manager import ApiManager
from ..base import (
    ChatModelResponse,
    ChatSequence,
    FunctionCallDict,
    Message,
    ResponseMessageDict,
)
from ..providers import openai as iopenai
from ..providers.openai import (
    OPEN_AI_CHAT_MODELS,
    OpenAIFunctionCall,
    OpenAIFunctionSpec,
    count_openai_functions_tokens,
)
from .token_counter import *


def call_ai_function(
    function: str,
    args: list,
    description: str,
    config: Config,
    model: Optional[str] = None,
) -> str:
    """Call an AI function

    This is a magic function that can do anything with no-code. See
    https://github.com/Torantulino/AI-Functions for more info.

    Args:
        function (str): The function to call
        args (list): The arguments to pass to the function
        description (str): The description of the function
        model (str, optional): The model to use. Defaults to None.

    Returns:
        str: The response from the function
    """
    if model is None:
        model = config.smart_llm
    # For each arg, if any are None, convert to "None":
    args = [str(arg) if arg is not None else "None" for arg in args]
    # parse args to comma separated string
    arg_str: str = ", ".join(args)

    prompt = ChatSequence.for_model(
        model,
        [
            Message(
                "system",
                f"You are now the following python function: ```# {description}"
                f"\n{function}```\n\nOnly respond with your `return` value.",
            ),
            Message("user", arg_str),
        ],
    )
    return create_chat_completion(prompt=prompt, temperature=0, config=config).content


def create_text_completion(
    prompt: str,
    config: Config,
    model: Optional[str],
    temperature: Optional[float],
    max_output_tokens: Optional[int],
) -> str:
    if model is None:
        model = config.fast_llm
    if temperature is None:
        temperature = config.temperature

    kwargs = {"model": model}
    kwargs.update(config.get_openai_credentials(model))

    response = iopenai.create_text_completion(
        prompt=prompt,
        **kwargs,
        temperature=temperature,
        max_tokens=max_output_tokens,
    )
    logger.debug(f"Response: {response}")

    return response.choices[0].text


# Overly simple abstraction until we create something better
def create_chat_completion(
    prompt: ChatSequence,
    config: Config,
    functions: Optional[List[OpenAIFunctionSpec]] = None,
    model: Optional[str] = None,
    temperature: Optional[float] = None,
    max_tokens: Optional[int] = None,
    agent: BaseAgent = None
) -> ChatModelResponse:
    """Create a chat completion using the OpenAI API

    Args:
        messages (List[Message]): The messages to send to the chat completion
        model (str, optional): The model to use. Defaults to None.
        temperature (float, optional): The temperature to use. Defaults to 0.9.
        max_tokens (int, optional): The max tokens to use. Defaults to None.

    Returns:
        str: The response from the chat completion
    """

    if model is None:
        model = prompt.model.name
    if temperature is None:
        temperature = config.temperature
    if max_tokens is None:
        prompt_tlength = prompt.token_length
        max_tokens = (
            min(OPEN_AI_CHAT_MODELS[model].max_tokens -
                prompt_tlength - 1, 4000)
        )  # the -1 is just here because we have a bug and we don't know how to fix it. When using gpt-4-0314 we get a token error.
        logger.debug(f"Prompt length: {prompt_tlength} tokens")
        if functions:
            functions_tlength = count_openai_functions_tokens(functions, model)
            max_tokens -= functions_tlength
            logger.debug(
                f"Functions take up {functions_tlength} tokens in API call")

    logger.debug(
        f"{Fore.GREEN}Creating chat completion with model {model}, temperature {temperature}, max_tokens {max_tokens}{Fore.RESET}"
    )
    with open("model_logging_temp.txt", "w") as mlt:
        mlt.write(
            f"Creating chat completion with model {model}, temperature {temperature}, max_tokens {max_tokens}")

    chat_completion_kwargs = {
        "model": model,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "response_format": {"type": "json_object"}
    }

    chat_completion_kwargs.update(config.get_openai_credentials(model))

    if functions:
        chat_completion_kwargs["functions"] = [
            function.schema for function in functions
        ]

    # Print full prompt to debug log
    logger.debug(prompt.dump())

    if agent is not None:
        # Write the chat completion start to the execution info file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                "!! AI Chat Completion startup timestamp: " + str(time.time_ns()) + "\n")
    response = iopenai.create_chat_completion(
        messages=prompt.raw(),
        **chat_completion_kwargs,
    )

    if agent is not None:
        # Write the chat completion end to the execution info file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                "!! AI Chat Completion end timestamp: " + str(time.time_ns()) + "\n")

        prompt_tokens_cached = response["usage"]["prompt_tokens_details"]["cached_tokens"]
        prompt_tokens_uncached = response["usage"]["prompt_tokens"] - \
            response["usage"]["prompt_tokens_details"]["cached_tokens"]
        completion_tokens = response["usage"]["completion_tokens"]

        # Write the chat completion token usage to the file
        with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, "execution_info", f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{agent.ai_config.warning_file_name}_line_{str(agent.ai_config.warning_start_line)}_execution_info"), "a+") as patf:
            patf.write(
                "!! AI Chat Completion prompt_tokens_cached: " + str(prompt_tokens_cached) + "\n")
            patf.write(
                "!! AI Chat Completion prompt_tokens_uncached: " + str(prompt_tokens_uncached) + "\n")
            patf.write(
                "!! AI Chat Completion completion_tokens: " + str(completion_tokens) + "\n")

    logger.debug(f"Response: {response}")

    if hasattr(response, "error"):
        logger.error(response.error)
        raise RuntimeError(response.error)

    first_message: ResponseMessageDict = response.choices[0].message
    content: str | None = first_message.get("content")
    function_call: FunctionCallDict | None = first_message.get("function_call")

    for plugin in config.plugins:
        if not plugin.can_handle_on_response():
            continue
        # TODO: function call support in plugin.on_response()
        content = plugin.on_response(content)

    return ChatModelResponse(
        model_info=OPEN_AI_CHAT_MODELS[model],
        content=content,
        function_call=OpenAIFunctionCall(
            name=function_call["name"], arguments=function_call["arguments"]
        )
        if function_call
        else None,
    )
