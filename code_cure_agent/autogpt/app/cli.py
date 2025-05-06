"""Main script for the autogpt package."""
from pathlib import Path
from typing import Optional

import click


@click.group(invoke_without_command=True)
@click.option("-c", "--continuous", is_flag=True, help="Enable Continuous Mode")
@click.option(
    "--skip-reprompt",
    "-y",
    is_flag=True,
    help="Skips the re-prompting messages at the beginning of the script",
)
@click.option(
    "--none-interactive",
    "-n",
    is_flag=True,
    help="If set the user will not be prompted for input when the last cycle was reached. This allows for running the agent without needing manual user feedback.",
)
@click.option(
    "--ai-settings",
    "-C",
    help=(
        "Specifies which ai_settings.yaml file to use, relative to the Auto-GPT"
        " root directory. Will also automatically skip the re-prompt."
    ),
)
@click.option(
    "--prompt-settings",
    "-P",
    help="Specifies which prompt_settings.yaml file to use.",
)
# We don't use this flag but instead set the commands_limit via the hyperparams.json file and overwrite this parameter with that value
@click.option(
    "-l",
    "--continuous-limit",
    type=int,
    help="Defines the number of times to run in continuous mode",
)
@click.option("--speak", is_flag=True, help="Enable Speak Mode")
@click.option("--debug", is_flag=True, help="Enable Debug Mode")


@click.option("--model-version", type=str, required=True, help="GPT model to use.")

@click.option(
    "--use-memory",
    "-m",
    "memory_type",
    type=str,
    help="Defines which Memory backend to use",
)
@click.option(
    "-b",
    "--browser-name",
    help="Specifies which web-browser to use when using selenium to scrape the web.",
)
@click.option(
    "--allow-downloads",
    is_flag=True,
    help="Dangerous: Allows Auto-GPT to download files natively.",
)
@click.option(
    "--skip-news",
    is_flag=True,
    help="Specifies whether to suppress the output of latest news on startup.",
)
@click.option(
    # TODO: this is a hidden option for now, necessary for integration testing.
    #   We should make this public once we're ready to roll out agent specific workspaces.
    "--workspace-directory",
    "-w",
    type=click.Path(),
    hidden=True,
)
@click.option(
    "--install-plugin-deps",
    is_flag=True,
    help="Installs external dependencies for 3rd party plugins.",
)
@click.option(
    "--ai-name",
    type=str,
    help="AI name override",
)
@click.option(
    "--ai-role",
    type=str,
    help="AI role override",
)
@click.option(
    "--warning-repository-url",
    type=str,
    help="The Git repository with the SonarQube warning to fix"
)
@click.option(
    "--warning-repository-commit",
    type=str,
    help="The commit of the Git repo to fix the warning on. Can be a commitId or 'MASTER' for the most current commit."
)
@click.option(
    "--warning-file-path",
    type=str,
    help="The file path to the file with the SonarQube warning. This has to be the relative path from the repository."
)
@click.option(
    "--warning-rule-key",
    type=str,
    help="The rule identifier of the SonarQube warning to fix. (Squid)"
)
@click.option(
    "--warning-start-line",
    type=int,
    help="The line where the SonarQube warning is located."
)
@click.option(
    "--warning-rule-name",
    type=str,
    help="The name of the SonarQube warning to fix (short description)"
)
@click.option(
    "--warning-specific-message",
    type=str,
    help="The context-specific message of the SonarQube warning."
)
@click.option(
    "--ai-goal",
    type=str,
    multiple=True,
    help="AI goal override; may be used multiple times to pass multiple goals",
)
@click.option(
    "--experiment-file",
    type=str,
    multiple=False,
    help="the path to the file containing the configuration of the agent for the experiment.",
)
@click.option(
    "--sorald-jar-path",
    type=str,
    help="Override path to the sorald executable for mining SonarQube warnings."
)
@click.pass_context
def main(
    ctx: click.Context,
    continuous: bool,
    continuous_limit: int,
    ai_settings: str,
    prompt_settings: str,
    skip_reprompt: bool,
    none_interactive: bool,
    speak: bool,
    debug: bool,
    model_version: str,
    memory_type: str,
    browser_name: str,
    allow_downloads: bool,
    skip_news: bool,
    workspace_directory: str,
    sorald_jar_path: str | None,
    install_plugin_deps: bool,
    ai_name: Optional[str],
    ai_role: Optional[str],
    warning_repository_url: Optional[str],
    warning_repository_commit: Optional[str],
    warning_file_path: Optional[str],
    warning_rule_key: Optional[str],
    warning_start_line: Optional[int],
    warning_rule_name: Optional[str],
    warning_specific_message: Optional[str],
    ai_goal: tuple[str],
    experiment_file: str
) -> None:
    """
    Welcome to AutoGPT an experimental open-source application showcasing the capabilities of the GPT-4 pushing the boundaries of AI.

    Start an Auto-GPT assistant.
    """
    # Put imports inside function to avoid importing everything when starting the CLI
    from autogpt.app.main import run_auto_gpt

    if ctx.invoked_subcommand is None:
        run_auto_gpt(
            continuous=continuous,
            continuous_limit=continuous_limit,
            ai_settings=ai_settings,
            prompt_settings=prompt_settings,
            skip_reprompt=skip_reprompt,
            none_interactive=none_interactive,
            speak=speak,
            debug=debug,
            model_version=model_version,
            memory_type=memory_type,
            browser_name=browser_name,
            allow_downloads=allow_downloads,
            skip_news=skip_news,
            working_directory=Path(
                __file__
            ).parent.parent.parent,  # TODO: make this an option
            workspace_directory=workspace_directory,
            sorald_jar_path= sorald_jar_path,
            install_plugin_deps=install_plugin_deps,
            ai_name=ai_name,
            ai_role=ai_role,
            warning_repository_URL=warning_repository_url,
            warning_repository_commit=warning_repository_commit,
            warning_file_path=warning_file_path,
            warning_rule_key=warning_rule_key,
            warning_start_line=warning_start_line,
            warning_rule_name=warning_rule_name,
            warning_specific_message=warning_specific_message,
            ai_goals=ai_goal,
            experiment_file=experiment_file
        )


if __name__ == "__main__":
    main()
