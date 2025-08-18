# CodeCureAgent

CodeCureAgent is an autonomous LLM-based agent designed for automated static analysis warning repair.  
It can fix arbitrary SonarQube rule violations in Java code.

<div style="text-align: left;">
  <img src="./code_cure_agent/code_cure_agent_project_image.png" alt="Alt text" width="300" height="300">
</div>

---

## 📋 I. Requirements

Before you start using CodeCureAgent, ensure that your system meets the following requirements:

- **Docker**: Version 20.04 or higher. For installation instructions, see the [Docker documentation](https://docs.docker.com/get-docker).
- **VS Code**: Not a hard requirement but highly recommended. VS Code provides an easy way to interact with CodeCureAgent using Dev Containers (see the instructions below).
- **OpenAI Token and Credits**:
  - Create an account on the OpenAI website and purchase credits to use the API.
  - Generate an API token on the same website.
- **Disk Space**:
  - At least 40GB of available disk space on your machine. The code itself does not take 40GB. However, the dependencies might take up to 8GB, and files generated from running on different instances may use more. 40GB is a safe estimate.
- **Internet Access**: Required while running CodeCureAgent to connect to OpenAI's API.

---

## ⚙️ II. Setup CodeCureAgent

### **STEP 1: Open CodeCureAgent in a Dev Container**

1. Ensure you have the **Dev Containers** extension installed in VS Code. You can install it from the [Visual Studio Code Marketplace](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).

2. Clone the CodeCureAgent repository:

   ```bash
   git clone https://github.com/sola-st/master-thesis-pascal-joos.git
   ```

3. Open the repository folder in VS Code.

4. When prompted by VS Code to "Reopen in Container," click it. If not prompted, open the Command Palette (Ctrl+Shift+P) and select "Dev Containers: Reopen in Container."  
VS Code will now build and start the Dev Container, setting up the environment for you. This will take a while.

5. Within your VS Code terminal, move to the folder code_cure_agent

    ```bash
    cd code_cure_agent
    ```

### **STEP 2: Set the OpenAI API Key**

Inside the Dev Container terminal, configure your OpenAI API key by running:

```bash
python3.10 set_api_key.py
```

The script will prompt you to paste your API token.

---

## III. Run CodeCureAgent

CodeCureAgent takes a csv file as input where each line specifies a single violation of a SonarQube rule in a single Java file in a single Git repository.  

For an example on how the input file has to look like see [specific_commit_quality_profile_rules_input_file_single_rule_violations.csv](code_cure_agent/experimental_setups/dev_dataset/mining_results/specific_commit_quality_profile_rules_input_file_single_rule_violations.csv).  
You can create your own by following the steps described further down below in this paragraph.

To execute CodeCureAgent on an input file, run the following from the `code_cure_agent` folder:

  ```bash
   ./run_on_dataset.sh ./experimental_setups/dev_dataset/mining_results/specific_commit_quality_profile_rules_input_file_single_rule_violations.csv hyperparams.json
   ```

The first argument is the csv input file to run on. The second argument specifies hyperparameter settings.  
You can open the `hyperparams.json` file to review or customize its parameters (explained further in the customization section).  

### What Happens When You Start CodeCureAgent?

- CodeCureAgent goes through the input file line by line
- For each line CodeCureAgent checks out the project with the given URL and commit.
- It initiates the autonomous repair process, trying to fix the given rule violation in the given file.
- Logs detailing each step performed will be displayed in your terminal.

## IV. Experiment Setup and Evaluation

All utility scipts must be run from the folder [code_cure_agent](code_cure_agent).

### Creating your own csv input file, based on repositories you want to run CodeCureAgent on

1. Create a .csv file with one line per Git repository with three columns:
     - URL to the Git repository  
     - CommitID that you want to run on. If you want to use the most current commit on the master/main branch set the commitID to 'MASTER'.  
     - targetJavaVersion. The Java version the project is compiled to. Used to configure the SonarQube analyzer with the correct rules. Can be automatically inferred by using the script [infer_target_java_version_of_projects.py](code_cure_agent/experimental_setups/infer_target_java_version_of_projects.py)  

    For an example see [sampled_repos_with_commits_and_java_target_versions.csv](code_cure_agent/experimental_setups/dev_dataset/sampled_repos_with_commits_and_java_target_versions.csv). 
    Currently, CodeCureAgent only supports Maven projects that can be built by running a simple `mvn clean package` with Maven 3.6.3.  

2. Use the Sorald mining tool to mine SonarQube warnings on the repositories specified in the file.  
Example usage (run from `code_cure_agent` on the `sampled_repos_with_commits_and_java_target_versions.csv` file):  

   ```bash
   java -jar ./sorald/sorald.jar mine \
      --git-repos-list ./experimental_setups/dev_dataset/sampled_repos_with_commits_and_java_target_versions.csv \
      --miner-output-file ./experimental_setups/dev_dataset/mining_results/specific_commit_handled_rules_out.txt \
      --stats-output-file ./experimental_setups/dev_dataset/mining_results/specific_commit_handled_rules_mining_result.json \
      --temp-dir ./experimental_setups/dev_dataset/temp \
      --stats-on-git-repos \
      --rule-parameters ./sonarqube_quality_profile/quality_profile_rule_parameters.json \
      --handled-rules
   ```

   Remove the --handled-rules flag if you want to mine all warnings supported by the used SonarQube version.  
   If you only want to mine specific rules, pass the IDs of the rules via --rule-keys, or to only mine for specific types of rules use --rule-types.  
   For our experiments we use only rules that are part of the SonarWay quality profile, by using the keys from `code_cure_agent/sonarqube_quality_profile/quality_profile_rule_keys.txt`.  
   After running the mining tool the output is saved in a json file. In the example this is `specific_commit_handled_rules_mining_result.json`.

3. Finally you can create your csv input file from the json report by using `code_cure_agent/experimental_setups/prepare_experiment_input_file.py`.  
To this script provide the previously created json report as the first argument. Also --rule-violations-mode must be set to `single`.  
Additionally you can provide the path, the csv-file is to be saved to, via --target-csv-file-path.  
Example:

  ```bash
    python3 ./experimental_setups/prepare_experiment_input_file.py ./experimental_setups/dev_dataset/mining_results/specific_commit_handled_rules_mining_result.json \
      --target-csv-file-path ./experimental_setups/dev_dataset/mining_results/specific_commit_handled_rules_input_file_single_rule_violations.csv --rule-violations-mode single
  ```

### **Retrieve Repair Logs and History**

CodeCureAgent saves the output in multiple files.

- The primary logs are located in the folder `experimental_setups/experiment_X`, where `experiment_X` increments automatically with each run of the command `./run_on_dataset.sh`.

- Within this folder, you may find several subfolders, which each hold one file per rule violation:
  - **prompt_history**: Prompt history (prompts made to the model)
  - **responses**: History of agent answers and the results of running the commands
  - **all_messages**: Full message history (prompts to the agent, answers of the agent, and result of running a command)
  - **plausible_patches**: Any plausible patches generated
  - **implausible_patches**: Any implausible patches generated (rejected by the ChangeApprover steps)
  - further subfolders for debugging purposes

### Scripts for Evaluation

Within the `experimental_setups` folder, several scripts are available to post-process the logs:  
TODO: explain all the scripts for evaluation

## ✨ IV. Customize CodeCureAgent

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

- **Command Limits**: Control the maximum allowed cycles (budget) in the different phases of the agent.
  Default for our experiment:  
  ```json
  "classification_cycles_limit": 20,
  "fix_cycles_limit": 40 
  ```

- **Threshold of cycles left after which write_fix is prioratized**: Set the threshold of cycles left before the cycle budget is exhausted, where, when reached, the prompt is modified to force the agent to use write_fix.
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

## 📊 V. RepairAgent Data

In RepairAgent for our experiments, we utilized RepairAgent on the Defects4J dataset, successfully fixing 164 bugs. You can check our data under the folder data.
- The list of fixed bugs [here](./data/final_list_of_fixed_bugs). The list allows to compare with prior and future work.
  * For example, we compare to ChatRepair, SelfAPR, and ITER. The venn diagram of Figure 6 is produced using the command:
    ```bash
    python3.10 draw_venn_chatrepair_clean.py
    ```
  * The file [d4j12.csv](./code_cure_agent/experimental_setups/d4j12.csv) contains the list of bugs fixed by previous work. The script draw_venn_chatrepair_clean.py contains the list of fixes that we compare to.
- The implementation details of the patches in [this file](./data/fixes_implementation).
 
- The folder **data/root_patches** contains patches produced by CodeCureAgent in the main phase
- The folder **data/derivated_pathces** contains patches obtained by mutating **root_patches**


Note: RepairAgent encountered exceptions due to Middleware errors in 29 bugs, which were not re-run.

---

## 🧫 VI. Replicate Experiments
This part is about running RepairAgent on full evaluation datasets to replicate our experiments. The process is the same as above; We just provide ready-to-use input files and instructions for replication.

### Replicate Defects4J experiments
1. Create the execution batches for Defects4J which will create lists of bugs to run on.
    ```bash
    python3.10 get_defects4j_list.py
    ```
    The result of this command can be found in `experimental_setups/batches`

2. Run RepairAgent on each of the batches (either singularly or concurrently)
    ```bash
    ./run_on_defects4j.sh experimental_setups/batches/0 hyperparameters.json
    # replace 0 with the desired batch number
    ```

3. Refer to sections `4.2 Retrieve Repair Logs and History` and `4.3 Analyze Logs` on how to analyze logs and summarize the results of the experiments.

4. Furthermore, you can adapt the script `experimental_setups/generate_main_table.py` to generate the main comparative table (Table III in the paper)
   - 4.1. You can also use `experimental_setups/draw_venn_chatrepair_clean.py` to draw a venn diagram to compare different techniques (Figure 6 of the paper)  
5. You can use the script `experimental_setups/calculate_tokens.py` to calculate the costs of the agent (used to generate figure 9).

6. You can use the script `experimental_setups/collect_plausible_patches_files.py` to get the list of plausible patches to inspect.


### Replicate GitBugsJava Experiment
GitBugsJava is another dataset for program repair evaluation.
 
 1. First,prepare the GitBugsJava VM. Since this dataset requires a heavy VM (at least 140 GB of disk), we could not include it in this artifact. We added more detailed instruction on how to prepare such VM. Please check the step by step process here: https://github.com/gitbugactions/gitbug-java

 2. Copy the repository of RepairAgent inside the VM.

 3. Run RepairAgent on the list of bugs by specifying the file `experimental_setups/gitbuglist` as the target file.

 4. Use the same analysis scripts as part 1 (D4j replication) to analyse the results of the experiments.


--- 
