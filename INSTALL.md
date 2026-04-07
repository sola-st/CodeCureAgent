Refer to `II. Setup CodeCureAgent` and `III. Run CodeCureAgent` in [`README.md`](README.md) for the detailed instructions.

This file provides a quick start guide to get CodeCureAgent up and running on a small example batch of 3 warnings, using the pre-built Docker image.

Start from the root of the downloaded repository:

```bash
# Pull the pre-built Docker image from Docker Hub
docker pull pascaljoos12d/codecureagent:latest

# Run the Docker container, mounting the necessary directories
docker run -it --rm \
  -v "$(pwd)/code_cure_agent/experimental_setups:/workspace/CodeCureAgent/code_cure_agent/experimental_setups" \
  -v "$(pwd)/code_cure_agent/evaluation_results:/workspace/CodeCureAgent/code_cure_agent/evaluation_results" \
  pascaljoos12d/codecureagent:latest
```

Then run the following commands inside the Docker container:

```bash
# Navigate to the code_cure_agent directory
cd code_cure_agent

# Set the OpenAI API key (you will be prompted to paste it)
python3 set_api_key.py


# Run the small example batch
./run_on_dataset.sh ./experimental_setups/example_dataset/example_dataset_input_file.csv hyperparams.json
```

Expected displayed output, if setup was successful (shortened):

```
Creating experiment folder: 1
dos2unix: converting file ./experimental_setups/example_dataset/example_dataset_input_file.csv to Unix format...
Current run input:  1, https://github.com/simplenlg/simplenlg.git, ...
All packages are installed.
LLM set to  gpt-4.1-mini-2025-04-14
Using AI Settings File:  agent_config_and_prompt_files/ai_settings.yaml
LEGAL:  
LEGAL:  DISCLAIMER AND INDEMNIFICATION AGREEMENT
LEGAL:  ...
LEGAL:              
...
<Info on the configuration of the run>
...
Project checkout procedure starting.  
...
CodeCureAgent is now running the Classification-Sub-Agent.  This is the sub-agent dealing with the task of classifying the violation as TP or FP.
AUTHORISED COMMANDS LEFT:   20
CODECUREAGENT THOUGHTS:  <Some agent thoughts>
  

NEXT ACTION:   COMMAND = <Some command selected by the agent>  ARGUMENTS = {<Arguments to the command>}
  
  
SYSTEM:   Command `<command_name>` returned:  

<Output from the command>
    
AUTHORISED COMMANDS LEFT:   19

... and so on, until the agent finishes its run.
```

The logs of the run will be saved in `code_cure_agent/experimental_setups/experiment_1`.
