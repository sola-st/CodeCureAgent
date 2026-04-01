# CodeCureAgent

CodeCureAgent is an autonomous LLM-based agent designed for automated static analysis warning repair.  
It can classify and fix arbitrary SonarQube rule violations in Java code.  
The public GitHub repository is located at <https://github.com/sola-st/CodeCureAgent>.

<div style="text-align: left;">
  <img src="./code_cure_agent/code_cure_agent_project_image.png" alt="Alt text" width="300" height="300">
</div>

---
## 0. Quick Overview

The repository is structured as follows:

- [code_cure_agent](code_cure_agent): CodeCureAgent code, experiment setup and experiment output
  - [code_cure_agent/agent_core](code_cure_agent/agent_core): Main parts of the CodeCureAgent implementation. The implemented tools are at: [code_cure_agent/agent_core/commands](code_cure_agent/agent_core/commands)
  - [code_cure_agent/agent_config_and_prompt_files](code_cure_agent/agent_config_and_prompt_files): Used prompt files for the two sub-agents and agent config
  - [code_cure_agent/evaluation_results](code_cure_agent/evaluation_results): Results of our evaluation on 1000 warnings. Contains markdown files with aggregated evaluation results, csv files with data on all warning runs, plots, and all log files created during the experiment run.
    - [code_cure_agent/evaluation_results/evaluation_outputs](code_cure_agent/evaluation_results/evaluation_outputs): This holds the log files of the full evaluation. It is split into multiple experiment batches.  
    The most interesting files in this log output are the files in the subfolder `code_cure_agent/evaluation_results/evaluation_outputs/experiment_X/run_summaries`. These show for each warning run: details about the warning, classification and fix results including a diff of made changes for successful fixes. (Multi-File Fix Example: [code_cure_agent/evaluation_results/evaluation_outputs/experiment_1/run_summaries/6_summary.diff](code_cure_agent/evaluation_results/evaluation_outputs/experiment_1/run_summaries/6_summary.diff))
  - [code_cure_agent/experimental_setups](code_cure_agent/experimental_setups): Contains the evaluation dataset and utility scripts for evaluating your own experiment 
    - [code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv): This is the input file with the 1000 warnings used for running our evaluation.
- [comparative_study](comparative_study): All files and results of the comparison to baselines [Sorald](https://github.com/ASSERT-KTH/sorald), [iSMELL](https://github.com/iSMELL2024/iSMELL), and [CORE](https://github.com/microsoft/COREMSRI).

---

## I. Requirements

Before you start using CodeCureAgent, ensure that your system meets the following requirements:

- **Docker**: Version 20.04 or higher. For installation instructions, see the [Docker documentation](https://docs.docker.com/get-docker).
- **VS Code**: VS Code provides an easy way to interact with CodeCureAgent using Dev Containers (see the instructions below).
- **OpenAI Token and Credits**:
  - Create an account on the OpenAI website and purchase credits to use the API.
  - Generate an API token on the same website.
- **Disk Space**:
  - At least 40GB of available disk space on your machine. The code itself does not take 40GB. However, the dependencies might take up to 8GB, and files generated from running on different instances may use more. 40GB is a safe estimate.
- **Internet Access**: Required while running CodeCureAgent to connect to OpenAI's API.

---

## II. Setup CodeCureAgent

You have two options. Either set up CodeCureAgent using the provided Dev Container (requires VS Code), or use the pre-built Docker container.

### Option 1: Dev Container

#### STEP 1: Open CodeCureAgent in a Dev Container

1. Ensure you have the **Dev Containers** extension installed in VS Code. You can install it from the [Visual Studio Code Marketplace](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).

2. Download the CodeCureAgent repository and unpack it.

3. Open the repository folder in VS Code.

4. When prompted by VS Code to "Reopen in Container," click it. If not prompted, open the Command Palette (Ctrl+Shift+P) and select "Dev Containers: Reopen in Container."  
VS Code will now build and start the Dev Container, setting up the environment for you. This will take a few minutes.  
After the Dev Container is built it will continue to run further setups in the terminal. Wait until this is completed too.  
If the Dev Container opened in less than a few minutes it likely failed to create the container properly. Then rebuild the container via opening the Command Palette (Ctrl+Shift+P) and selecting "Dev Containers: Rebuild in Container."

5. Within your VS Code terminal, move to the folder code_cure_agent

    ```bash
    cd code_cure_agent
    ```

#### STEP 2: Set the OpenAI API Key

Inside the Dev Container terminal, configure your OpenAI API key by running:

```bash
python3.10 set_api_key.py
```

The script will prompt you to paste your API token.

### Option 2: Pre-built Docker Container

TODO: Add instructions


---

## III. Run CodeCureAgent

Run all commands from the `code_cure_agent` folder!

Running CodeCureAgent on a small example batch of 4 warnings:

```bash
./run_on_dataset.sh ./experimental_setups/example_dataset/example_dataset_input_file.csv hyperparams.json
```

The first argument is the csv input file to run on, in which each line specifies a single SonarQube warning in a single Java file in a single Git repository.  
The second argument specifies hyperparameter settings.  
You can open the `hyperparams.json` file to review or customize its parameters (explained further in the customization section).  

### What Happens When You Start CodeCureAgent?

- CodeCureAgent goes through the input file line by line
- For each line CodeCureAgent checks out the project with the given URL and commit.
- It initiates the autonomous repair process, first classifying the warning as true positive or false positive and then fixing or suppressing the warning accordingly.
- The terminal output shows detailed live logs of the process.
- Log output of the runs is saved in the folder `code_cure_agent/experimental_setups/experiment_X`, where `experiment_X` increments automatically with each run of the command `./run_on_dataset.sh`.

## IV. Results from Experiment on 1000 Warnings (RQ1, RQ2, RQ4)

For our experiments, we utilized CodeCureAgent on a dataset of 1000 warnings, successfully creating plausible fixes for 968 of them.  

The experiment input files are located in [code_cure_agent/experimental_setups/evaluation_dataset](code_cure_agent/experimental_setups/evaluation_dataset).  
Most relevant is here the file [code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv), which is the full input file to CodeCureAgent.

All log files from running the experiment on the 1000 warnings are located in [code_cure_agent/evaluation_results/evaluation_outputs](code_cure_agent/evaluation_results/evaluation_outputs) (split into multiple batches of experiment runs).

The extracted and aggregated evaluation results are located in [code_cure_agent/evaluation_results](code_cure_agent/evaluation_results).  
Important evaluation result files:

- [code_cure_agent/evaluation_results/analysis_results_overview_all.csv](code_cure_agent/evaluation_results/analysis_results_overview_all.csv): Aggregated results for all 1000 warnings. Includes effectiveness stats (with manual inspection) (RQ1), efficiency stats (RQ2), and ablation stats (RQ4).
- [code_cure_agent/evaluation_results/analysis_results_overview_first_291.csv](code_cure_agent/evaluation_results/analysis_results_overview_first_291.csv): Aggregated results for the subset of 291 warnings with distinct SonarQube rules.
- [code_cure_agent/evaluation_results/analysis_results_overview_random_samples.csv](code_cure_agent/evaluation_results/analysis_results_overview_random_samples.csv): Aggregated results for the subset of 709 random samples.
- [code_cure_agent/evaluation_results/plots](code_cure_agent/evaluation_results/plots): Plots visualizing the evaluation results.

---

## V. Replicate Experiments

### Replicate CodeCureAgent experiment on 1000 warnings dataset

1. Run CodeCureAgent on the [code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv) input file as described in `III. Run CodeCureAgent`.

2. Post-process the created log files as described in `VI. Running and Evaluating your own Experiment: 3. Scripts for Evaluation` to receive aggregated results (Markdown file and plots).

### Replicate Comparison to Sorald (RQ3)

We ran Sorald on the same dataset of 1000 warnings, of which Sorald supports 62 warnings.  

The scripts and results are found in [comparative_study/sorald_comparison](comparative_study/sorald_comparison).  
Refer to the dedicated README for more information: [comparative_study/sorald_comparison/README.md](comparative_study/sorald_comparison/README.md).  

### Replicate Comparison to CORE (RQ3)

We ran CORE on the same dataset of 1000 warnings.  

The scripts and results are found in [comparative_study/core_comparison](comparative_study/core_comparison).  
Refer to the dedicated README for more information: [comparative_study/core_comparison/README.md](comparative_study/core_comparison/README.md).  

### Replicate Comparison to iSMELL (RQ3)

We ran iSMELL on the same dataset of 1000 warnings.  

The scripts and results are found in [comparative_study/ismell_comparison](comparative_study/ismell_comparison).  
Refer to the dedicated README for more information: [comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/README.md](comparative_study/ismell_comparison/iSMELL_Adapted_For_SonarQube_dataset/README.md).  

---

## VI. Running and Evaluating your own Experiment

All utility scripts must be run from the folder [code_cure_agent](code_cure_agent).

If you want to rerun the experiment from the paper, skip 1. and run on the provided input file [code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_filled_up_to_1000_input_file.csv).

### 1. Creating your own csv input file, based on repositories you want to run CodeCureAgent on

1. Create a .csv file with one line per Git repository with three columns (without header):
     - URL to the Git repository  
     - CommitID that you want to run on. If you want to use the most current commit on the master/main branch set the commitID to 'MASTER'.  
     - targetJavaVersion. The Java version the project compiles to. Used to configure the SonarQube analyzer with the correct rules. Can be automatically inferred by using the script [code_cure_agent/experimental_setups/infer_target_java_version_of_projects.py](code_cure_agent/experimental_setups/infer_target_java_version_of_projects.py)  

    For an example see [evaluation_dataset_repos_list_with_java_versions.csv](code_cure_agent/experimental_setups/evaluation_dataset/evaluation_dataset_repos_list_with_java_versions.csv). 
    Currently, CodeCureAgent only supports Maven projects that can be built by running a simple `mvn clean package` with Maven 3.6.3.  

2. Use the Sorald mining tool to mine SonarQube warnings on the repositories specified in the file.  
Example usage (run from `code_cure_agent` on the `evaluation_dataset_repos_list_with_java_versions.csv` file):  

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

   Remove the --handled-rules flag if you want to mine all warnings supported by the used SonarQube version.  
   If you only want to mine specific rules, pass the IDs of the rules via --rule-keys, or to only mine for specific types of rules use --rule-types.  
   For our experiments we use only rules that are part of the SonarWay quality profile, by using the keys from `code_cure_agent/sonarqube_quality_profile/quality_profile_rule_keys.txt`.  
   After running the mining tool the output is saved in a json file. In the example this is `evaluation_dataset_mining_result.json`.

3. Finally, you can create your csv input file from the json report by using `code_cure_agent/experimental_setups/prepare_experiment_input_file.py`.  
To this script provide the previously created json report as the first argument. Also --rule-violations-mode must be set to `single`.  
Additionally, you can provide the path, the csv-file is to be saved to, via --target-csv-file-path.  
Example:

  ```bash
    python3 ./experimental_setups/prepare_experiment_input_file.py ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_mining_result.json \
      --target-csv-file-path ./experimental_setups/evaluation_dataset/mining_results/evaluation_dataset_input_file_all_violations.csv --rule-violations-mode single
  ```

4. Optionally, you can sample from the input file to run only on some of the warnings using [code_cure_agent/experimental_setups/sample_rule_violations_from_input_file.py](code_cure_agent/experimental_setups/sample_rule_violations_from_input_file.py).

### 2. CodeCureAgent Experiment Logs

Run CodeCureAgent on your created input file as described in `III. Run CodeCureAgent`.

When running CodeCureAgent the output is saved in multiple files.

The primary logs are created in the folder `code_cure_agent/experimental_setups/experiment_X`, where `experiment_X` increments automatically with each run of the command `./run_on_dataset.sh`.

The folder is structured into subfolders `classification`, `fix_fp`, `fix_tp` and `tasks`.

Most interesting are the `classification_result` files in the `classification` folder and the `prompt_history` files in the `prompt_history` subfolders of `classification`, `fix_fp` and `fix_tp`.


### 3. Scripts for Evaluation

Within the [code_cure_agent/experimental_setups](code_cure_agent/experimental_setups) folder, several scripts are available to calculate evaluation results from one or multiple executed CodeCureAgent runs.  
All scripts are expected to be run from the `code_cure_agent` folder.  

1. Create evaluation results file  
    After running one or multiple experiments, logs are located in the folders `code_cure_agent/experimental_setups/experiment_X`.  
    If you do not want to run your own experiments, but calculate evaluation results on the log files of our experiment runs, copy all folders and files from [code_cure_agent/evaluation_results/evaluation_outputs](code_cure_agent/evaluation_results/evaluation_outputs) to [code_cure_agent/experimental_setups](code_cure_agent/experimental_setups).  
    The script [code_cure_agent/experimental_setups/write_experiment_results_to_csv_file.py](code_cure_agent/experimental_setups/write_experiment_results_to_csv_file.py) can be used to extract the experiment run results from the experiment logs into a csv file.  
    By default, the evaluation results are appended to the csv file [code_cure_agent/evaluation_results/evaluation_results.csv](code_cure_agent/evaluation_results/evaluation_results.csv).  

2. Create extended evaluation results file with further info  
    An extended version of the evaluation results file can be created by using the [code_cure_agent/experimental_setups/extend_evaluation_results_with_more_stats.py](code_cure_agent/experimental_setups/extend_evaluation_results_with_more_stats.py) script. It expects the previously created evaluation results file as input.  
    By default, the extended evaluation results are written to the csv file [code_cure_agent/evaluation_results/evaluation_results_extended.csv](code_cure_agent/evaluation_results/evaluation_results_extended.csv).  

3. Aggregate results into a Markdown  
    The evaluation results can be aggregated into a Markdown file that presents relevant stats.  
    Use the script [code_cure_agent/experimental_setups/calculate_stats_from_evaluation_results.py](code_cure_agent/experimental_setups/calculate_stats_from_evaluation_results.py) for this.  
    It expects the extended evaluation results csv file as first argument.  
    By default, the Markdown is written to `code_cure_agent/evaluation_results/analysis_results_overview.md`.  
    See an example result Markdown here: [code_cure_agent/evaluation_results/analysis_results_overview_all.md](code_cure_agent/evaluation_results/analysis_results_overview_all.md)

4. Create summaries for each fixed/unfixed warning
    You can create summaries that show the most important information on a CodeCureAgent run on a warning, including a diff of all made changes.
    Use the script [code_cure_agent/experimental_setups/create_warning_summaries.py](code_cure_agent/experimental_setups/create_warning_summaries.py). It requires that the extended evaluation results csv file has been created before (2.).
    The summaries are added to the experiment logs (`code_cure_agent/experimental_setups/experiment_X`) in a subfolder `run_summaries`.

5. Manually inspect a repaired warning  
    We provide a further script [code_cure_agent/experimental_setups/show_next_warning_for_manual_inspection.py](code_cure_agent/experimental_setups/show_next_warning_for_manual_inspection.py) that can be used to quickly open relevant files for a specified warning, including a VS Code diff between the unfixed and fixed versions of the warning.  
    The instanceID of the warning that is to be looked at can be provided via option `--id-to-show`.  
    This script also requires that logs are located in the folders `code_cure_agent/experimental_setups/experiment_X`. (see 1.)

6. Create plots  
    We provide further Jupyter notebooks for creating plots, including a Venn diagram.

## VI. Customize CodeCureAgent

### 1. Modify `hyperparams.json`

- **Budget Control Strategy**: Defines how the agent views the remaining cycles, suggested fixes, and minimum required fixes:
  - **FULL-TRACK**: Put the max, consumed and left budget in the prompt (default for our experiments).
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

- **Repetition Handling**: Default settings restrict repetitions.
  ```json
  "repetition_handling": "RESTRICT",
  ```

- **Cycle Limits**: Control the maximum allowed cycles (budget) in the different sub-agents.
  Default for our experiment:  
  ```json
  "classification_cycles_limit": 20,
  "fix_cycles_limit": 40 
  ```

- **Threshold of cycles left after which write_fix is prioritized**: Set the threshold of cycles left before the cycle budget is exhausted, where, when reached, the prompt is modified to force the agent to use write_fix.
  Default for our experiment:  
  ```json
  "prioritize_write_fix_cycle_threshold": 5
  ```

### 2. Switching the used GPT model

In the `run_on_dataset.sh` file, locate the line:
```bash
./run.sh --ai-settings agent_config_and_prompt_files/ai_settings.yaml --model-version gpt-4.1-mini-2025-04-14 -m json_file --experiment-file "$2"
```
Change the model_version to one of the following supported models:  

- gpt-3.5-turbo-0125
- gpt-4-turbo-2024-04-09
- gpt-4o-mini-2024-07-18
- gpt-4o-2024-08-06
- gpt-4.1-nano-2025-04-14
- gpt-4.1-mini-2025-04-14
- gpt-4.1-2025-04-14

Reasoning models are not supported by the used OpenAI API version.

---
