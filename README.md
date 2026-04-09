<p align="center">
  <h1 align="center">CodeCureAgent</h1>
</p>
<p align="center">

CodeCureAgent is an autonomous LLM-based agent for automated static analysis warning repair.
It classifies and fixes SonarQube rule violations in Java projects.  
Please find our paper describing CodeCureAgent and its evaluation here: <https://arxiv.org/abs/2509.11787>.

<div style="text-align: center;">
  <img src="./code_cure_agent/code_cure_agent_project_image.png" alt="CodeCureAgent" width="300" height="300">
</div>

---

## 1. Setup CodeCureAgent

You have two options. Either set up CodeCureAgent using the provided Dev Container (requires VS Code), or use the pre-built Docker image.

### 1.1 Requirements

- User requirements:
  - Basic Docker and/or VS Code Dev Container familiarity.
  - OpenAI API key with credits. Create an account on the OpenAI website and purchase credits to use the API. Generate an API token on the same website.
- Hardware requirements:
  - At least 40 GB free disk space.
  - At least 8 GB free RAM (16 GB+ recommended).
  - Internet access.
- Software requirements:
  - Linux, macOS, or Windows with WSL2.
  - Docker 20.10+.
  - VS Code (optional, for Dev Container workflow).

### 1.2 Setup Option A: VS Code Dev Container

1. Ensure you have the **Dev Containers** extension installed in VS Code. You can install it from the [Visual Studio Code Marketplace](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).

2. Clone the CodeCureAgent repository:

    ```bash
    git clone https://github.com/sola-st/CodeCureAgent.git
    ```

3. Open the repository folder in VS Code.

4. Reopen in Container: When prompted by VS Code to "Reopen in Container," click it.  
If not prompted, open the Command Palette (Ctrl+Shift+P) and select "Dev Containers: Reopen in Container."  
VS Code will now build and start the Dev Container, setting up the environment for you. This will take roughly 4 minutes.  
After the Dev Container is built it will continue to run further setups in the terminal. Wait until this is completed too (roughly 2 more minutes).  
If the Dev Container opened in less than a few minutes it likely failed to create the container properly. Then rebuild the container via opening the Command Palette (Ctrl+Shift+P) and selecting "Dev Containers: Rebuild in Container."

5. Within your VS Code terminal, move to the folder code_cure_agent

    ```bash
    cd code_cure_agent
    ```

6. Set the OpenAI API Key

    Inside the Dev Container terminal, configure your OpenAI API key by running:

    ```bash
    python3 set_api_key.py
    ```

    The script will prompt you to paste your API token.

### 1.3 Setup Option B: Pre-built Docker Image

1. Clone the CodeCureAgent repository:

    ```bash
    git clone https://github.com/sola-st/CodeCureAgent.git
    ```

2. Move into the repository root:

    ```bash
    cd CodeCureAgent
    ```

3. Pull the pre-built image from Docker Hub:

    ```bash
    docker pull pascaljoos12d/codecureagent:latest
    ```

4. Start container with mounted experiment folders:

    ```bash
    docker run -it --rm \
      -v "$(pwd)/code_cure_agent/experimental_setups:/workspace/CodeCureAgent/code_cure_agent/experimental_setups" \
      -v "$(pwd)/code_cure_agent/evaluation_results:/workspace/CodeCureAgent/code_cure_agent/evaluation_results" \
      pascaljoos12d/codecureagent:latest
    ```

    Any experiment logs written inside the container are immediately visible on the host (and vice versa).  
    The container starts a bash shell inside the repository root.  

5. From the container shell, move into `code_cure_agent` before running any commands :

    ```bash
    cd code_cure_agent
    ```

6. Set the OpenAI API Key

    From the container shell, configure your OpenAI API key by running:

    ```bash
    python3 set_api_key.py
    ```

    The script will prompt you to paste your API token.

All commands in the following sections 2 to 6 must be run from the folder `code_cure_agent`.

---

## 2. Quick Run

Run CodeCureAgent on a small included example with 3 warnings:

```bash
./run_on_dataset.sh ./experimental_setups/example_dataset/example_dataset_input_file.csv hyperparams.json
```

What happens:

- Input file is processed warning-by-warning.
- For each warning, CodeCureAgent checks out the target repository and commit.
- It initiates the autonomous repair process, first classifying the warning as true positive or false positive and then fixing or suppressing the warning accordingly.
- Detailed live logs are shown in terminal.
- Run logs are stored in `code_cure_agent/experimental_setups/experiment_X` (auto-incrementing index).

Expected terminal output (shortened):

```text
...
<Info on the configuration of the run>
...
Project checkout procedure starting.  
...
CodeCureAgent is now running the Classification-Sub-Agent.
AUTHORISED COMMANDS LEFT:   20
CODECUREAGENT THOUGHTS:  <Some agent thoughts>
  
NEXT ACTION:   COMMAND = <Some command selected by the agent>  ARGUMENTS = {<Arguments to the command>}
  
SYSTEM:   Command `<command_name>` returned:  
<Output from the command>
    
AUTHORISED COMMANDS LEFT:   19

... and so on, until the agent finishes its run.
```

---

## 3. Inspect All Logs and Data from Our Experiments

Review the included logs, summaries, CSVs, Markdown files, and plots from our 1000-warning evaluation.

### 3.1 Experiment Input Files

The experiment input files are located in [code_cure_agent/experimental_setups/evaluation_dataset](code_cure_agent/experimental_setups/evaluation_dataset).  
Most relevant is here the file [code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv), which is the full input file to CodeCureAgent.

### 3.2 Experiment Logs

All log files from running the experiment on the 1000 warnings are located in [code_cure_agent/evaluation_results/evaluation_outputs](code_cure_agent/evaluation_results/evaluation_outputs) (split into multiple batches of experiment runs).

The most interesting files in this log output are the files in the subfolders `code_cure_agent/evaluation_results/evaluation_outputs/experiment_X/run_summaries`. These show for each warning run: details about the warning, classification and fix results including a diff of made changes for successful fixes. (Multi-File Fix Example: [code_cure_agent/evaluation_results/evaluation_outputs/experiment_1/run_summaries/6_summary.diff](code_cure_agent/evaluation_results/evaluation_outputs/experiment_1/run_summaries/6_summary.diff))

### 3.3 Aggregated Evaluation Outputs (RQ1, RQ2, RQ4)

The extracted and aggregated evaluation results are located in [code_cure_agent/evaluation_results](code_cure_agent/evaluation_results).  
Important evaluation result files:

- [code_cure_agent/evaluation_results/analysis_results_overview_all.md](code_cure_agent/evaluation_results/analysis_results_overview_all.md): Aggregated results for all 1000 warnings. Includes effectiveness stats (with manual inspection) (RQ1), efficiency stats (RQ2), and ablation stats (RQ4).
- [code_cure_agent/evaluation_results/analysis_results_overview_first_291.md](code_cure_agent/evaluation_results/analysis_results_overview_first_291.md): Aggregated results for the subset of 291 warnings with distinct SonarQube rules.
- [code_cure_agent/evaluation_results/analysis_results_overview_random_samples.md](code_cure_agent/evaluation_results/analysis_results_overview_random_samples.md): Aggregated results for the subset of 709 random samples.
- [code_cure_agent/evaluation_results/plots](code_cure_agent/evaluation_results/plots): Plots visualizing the evaluation results.
- [code_cure_agent/evaluation_results/evaluation_results.csv](code_cure_agent/evaluation_results/evaluation_results.csv): CSV file with extracted evaluation results for all 1000 warnings (used to create the aggregated Markdown files and plots). This file includes the manual inspection results and reasoning for each warning.

### 3.4 Aggregated Evaluation Outputs for Baseline Comparisons (RQ3)

The baseline comparisons are located in the `comparative_study` folder. Each baseline has its own subfolder with README and assets.
- Sorald comparison assets:
  - [comparative_study/sorald_comparison](comparative_study/sorald_comparison)
  - [comparative_study/sorald_comparison/README.md](comparative_study/sorald_comparison/README.md)
  - [Original repo](https://github.com/ASSERT-KTH/sorald)
  - [Sorald paper](https://ieeexplore.ieee.org/document/9756950)
- iSMELL comparison assets:
  - [comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset](comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset)
  - [comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/README.md](comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/README.md)
  - [Original repo](https://github.com/iSMELL2024/iSMELL/tree/main)
  - [iSMELL paper](https://dl.acm.org/doi/10.1145/3691620.3695508)
- CORE comparison assets:
  - [comparative_study/core_comparison](comparative_study/core_comparison)
  - [comparative_study/core_comparison/README.md](comparative_study/core_comparison/README.md)
  - [Original repo](https://github.com/microsoft/COREMSRI)
  - [CORE paper](https://dl.acm.org/doi/10.1145/3643762)

---

## 4. Reproduce Tables and Figures of the Paper from Data

This section details how to regenerate result files and plots from included experiment outputs (via scripts).

### 4.1 Prepare Logs for Processing

If you want to recompute metrics from the provided full logs, copy all folders/files from:  
[code_cure_agent/evaluation_results/evaluation_outputs](code_cure_agent/evaluation_results/evaluation_outputs)  
into:  
[code_cure_agent/experimental_setups](code_cure_agent/experimental_setups)

### 4.2 Run Evaluation Scripts (from `code_cure_agent`)

1. Create evaluation results CSV:

    ```bash
    python3 experimental_setups/write_experiment_results_to_csv_file.py -t evaluation_results/new_experiment_results.csv
    ```

2. Create extended results CSV:

    ```bash
    python3 experimental_setups/extend_evaluation_results_with_more_stats.py evaluation_results/new_experiment_results.csv -t evaluation_results/new_experiment_results_extended.csv
    ```

3. Aggregate stats to Markdown (for table-ready summaries):

    ```bash
    python3 experimental_setups/calculate_stats_from_evaluation_results.py evaluation_results/new_experiment_results_extended.csv -t evaluation_results/new_experiment_results_analysis.md
    ```

4. Create per-warning run-summaries (the `x_summary.diff` files):

    ```bash
    python3 experimental_setups/create_warning_summaries.py -e evaluation_results/new_experiment_results_extended.csv
    ```

5. Open all relevant resources for a repaired warning to perform a manual inspection (Requires VS Code):

    ```bash
    python3 experimental_setups/show_next_warning_for_manual_inspection.py -e evaluation_results/new_experiment_results.csv --id-to-show 1
    ```

    Provide the ID of the warning to inspect via `--id-to-show`.

6. Regenerate plots via notebooks in [code_cure_agent/experimental_setups](code_cure_agent/experimental_setups)

---

## 5. Run a Large-Scale Experiment Like Ours

### 5.1 Re-run CodeCureAgent on the 1000-Warning Dataset

Run:

```bash
./run_on_dataset.sh ./experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv hyperparams.json
```

### 5.2 Evaluate the Generated Logs

Use the same script chain from Section 4.2 to create:

- evaluation CSV
- extended CSV
- aggregated Markdown stats
- per-warning summaries
- plots

---

## 6. Run on Your Own Repositories

1. Create a CSV listing the target repositories (no header), one line per repository:
   - repository URL
   - commit ID (or `MASTER` for latest commit on master/main branch)
   - target Java version.  
    The Java version the project compiles to. Used to configure the SonarQube analyzer with the correct rules. Can be automatically inferred by using the script [code_cure_agent/experimental_setups/infer_target_java_version_of_projects.py](code_cure_agent/experimental_setups/infer_target_java_version_of_projects.py)

    Example repo list (used in the following):
    [code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_repos_list_with_java_versions.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_repos_list_with_java_versions.csv)

2. Mine SonarQube warnings via Sorald:

    ```bash
    java -jar ./sorald/sorald.jar mine \
        --git-repos-list ./experimental_setups/evaluation_dataset/evaluation_dataset_repos_list_with_java_versions.csv \
        --miner-output-file ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_out.txt \
        --stats-output-file ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_mining_result.json \
        --temp-dir ./experimental_setups/evaluation_dataset/temp \
        --stats-on-git-repos \
        --rule-parameters ./sonarqube_quality_profile/quality_profile_rule_parameters.json \
        --handled-rules
    ```

    Notes:

    - Run from `code_cure_agent`.
    - Remove `--handled-rules` to mine all supported rules.
    - Filter by rule IDs with `--rule-keys` or by rule type with `--rule-types`.
    - SonarWay quality profile used in our experiments (pass to `--rule-keys`):
      - [code_cure_agent/sonarqube_quality_profile/quality_profile_rule_keys.txt](code_cure_agent/sonarqube_quality_profile/quality_profile_rule_keys.txt)

3. Convert mining JSON to CodeCureAgent input CSV:

    ```bash
      python3 ./experimental_setups/prepare_experiment_input_file.py ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_mining_result.json \
        --target-csv-file-path ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_input_file_all_violations.csv --rule-violations-mode single
    ```

4. Optionally sample from the warnings using [code_cure_agent/experimental_setups/sample_rule_violations_from_input_file.py](code_cure_agent/experimental_setups/sample_rule_violations_from_input_file.py)

5. Run CodeCureAgent on the created input file:

    ```bash
    ./run_on_dataset.sh ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_input_file_all_violations.csv hyperparams.json
    ```

Current limitation:

- Only Maven projects are supported that build with `mvn clean package` using Maven 3.6.3.

---

## 7. Customize CodeCureAgent

### 7.1 Modify Hyperparameters in: [code_cure_agent/hyperparams.json](code_cure_agent/hyperparams.json)

- **Budget Control Strategy**:  
  Defines how the agent views the remaining cycles, suggested fixes, and minimum required fixes:
  - **FULL-TRACK**: Put the max, consumed, and left budget in the prompt (default for our experiments).
  - **NO-TRACK**: Suppresses budget information.
  
  Example Configuration:
  
  ```json
  "budget_control": {
      "name": "FULL-TRACK",
      "params": {
          "#fixes": 4 //The agent should suggest at least 4 patches within the given budget, the number is updated based on agent progress (4 is default).
      }
  }
  ```

- **Repetition Handling**:  
  Default settings restrict repetitions (reprompting the LLM for repeated actions).

  ```json
  "repetition_handling": "RESTRICT",
  ```

  Change to "NONE" to allow unrestricted repetitions.

- **Cycle Limits**:  
  Control the maximum allowed cycles (budget) in the different sub-agents.  
  Default for our experiment:  

  ```json
  "classification_cycles_limit": 20,
  "fix_cycles_limit": 40 
  ```

- **Prioritize `write_fix` When Few Cycles Are Left**:  
    Default for our experiment:

    ```json
    "prioritize_write_fix_cycle_threshold": 5
    ```

### 7.2 Switching the Used GPT Model

In the [code_cure_agent/run_on_dataset.sh](code_cure_agent/run_on_dataset.sh) file, locate the line:

```bash
./run.sh --ai-settings agent_config_and_prompt_files/ai_settings.yaml --model-version gpt-4.1-mini-2025-04-14 -m json_file --experiment-file "$2"
```

Replace `--model-version` with one of:

- `gpt-3.5-turbo-0125`
- `gpt-4-turbo-2024-04-09`
- `gpt-4o-mini-2024-07-18`
- `gpt-4o-2024-08-06`
- `gpt-4.1-nano-2025-04-14`
- `gpt-4.1-mini-2025-04-14`
- `gpt-4.1-2025-04-14`

Reasoning models are not supported by the API version currently used in this project.

---

## 8. CodeCureAgent Implementation

The following are the most important folders for the CodeCureAgent implementation:

- [code_cure_agent/agent_core](code_cure_agent/agent_core): Main parts of the CodeCureAgent implementation
  - [code_cure_agent/agent_core/agents](code_cure_agent/agent_core/agents): Implementation of the cyclic agent with two sub-agents
  - [code_cure_agent/agent_core/commands](code_cure_agent/agent_core/commands): Implemented tools that the agent can use
- [code_cure_agent/agent_config_and_prompt_files](code_cure_agent/agent_config_and_prompt_files): Used prompt files for the two sub-agents and agent configuration
