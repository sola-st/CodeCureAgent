from agent_core.agents.agent import Agent
from agent_core.command_decorator import command
from agent_core.logs.logger import logger
import os

COMMAND_CATEGORY = "CLASSIFICATION_TASKS"
COMMAND_CATEGORY_TITLE = "CLASSIFICATION_TASKS"
ALLOWLIST_CONTROL = "allowlist"
DENYLIST_CONTROL = "denylist"


@command(
    "answer_question",
    "Use this command to answer the currently posed question in the 'Current Question to answer' section. "
    "Only call this command when you have collected enough information to answer the question. "
    "Give the answer to the question and also state how certain you are about your answer.",
    {"answer": {
        "type": "string",
        "description": "Your answer to the question.",
        "required": True,
    },
    }
)
def answer_question(answer: str,  agent: Agent) -> str:
    agent.question_answers.append(answer)
    agent_output = f"Your answer to question {str(agent.current_question)} has been recorded."

    with open(f"agent_config_and_prompt_files/classification_prompt_files/questions_prompt_parts/question_{str(agent.current_question)}_text.md") as question_file:
        question = question_file.readline()
    answer_output = f"Question {agent.current_question}: {question}Answer:  \n{answer}  \n\n"
    sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
        "/", ".")
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}_classification_result"), "a+") as patf:
        patf.write(answer_output)

    agent.current_question += 1
    if agent.current_question <= agent.number_of_questions:
        agent_output += f" Try to answer question {str(agent.current_question)} now."
    else:
        agent_output += " Give a final verdict now, based on your collected information and answered questions."

    return agent_output


@command(
    "give_final_verdict",
    "Use this command to formulate a final verdict about whether the potential rule violation is a True Positive (should fix) or a False Positive (should not fix). "
    "Give an explanation why you decided for one or the other. "
    "Only use this command after answering all three questions, or if you only have one command left.",
    {"verdict": {
        "type": "string",
        "description": " Either 'TP' or 'FP'.",
        "required": True
    },
        "reason": {
        "type": "string",
        "description": "Explanation of what led you to your decision.",
        "required": True
    }
    }
)
def give_final_verdict(verdict: str, reason: str, agent: Agent) -> str:
    if verdict.lower() == "tp" or verdict.lower() == "'fp'" or verdict.lower() == "true positive" or verdict.lower() == "should fix":
        classification = "TP"
    elif verdict.lower() == "fp" or verdict.lower() == "'fp'" or verdict.lower() == "false positive" or verdict.lower() == "should not fix":
        classification = "FP"
    else:
        logger.error("Processing the agent-provided final verdict failed.",
                     f"Verdict was '{verdict}', but should be either 'TP' or 'FP'. The agent can try again.")
        return f"Processing the final verdict failed. You gave as `verdict`: '{verdict}'. However, only either 'TP' or 'FP' is allowed."

    agent.final_verdict_reason = reason

    # Add result of classification to an output file
    classification_output = "Final Verdict:  \n"
    classification_output += classification
    classification_output += "\n\nReason:  \n"
    classification_output += agent.final_verdict_reason
    sanitized_warning_file_path = agent.ai_config.warning_file_path.replace(
        "/", ".")
    with open(os.path.join("experimental_setups", agent.exps[-1], agent.current_state, f"{str(agent.ai_config.warning_ID)}_{agent.ai_config.warning_repository_name}_{agent.ai_config.warning_rule_key}_{sanitized_warning_file_path}_line_{str(agent.ai_config.warning_start_line)}_classification_result"), "a+") as patf:
        patf.write(classification_output)

    return "Final verdict submitted:" + classification
