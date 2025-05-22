#!/bin/bash

enable -f /usr/lib/bash/csv csv

# Move to the directory where run_on_dataset.sh is.
# => We always run from the correct folder
cd "$(dirname "$0")"


python3 experimental_setups/increment_experiment.py
python3 scripts/construct_commands_descriptions.py
input="$1"
dos2unix "$input" # Convert file to Unix line endings (if needed)

# Ensure the input file ends with a newline
if [ -n "$input" ] && [ -f "$input" ]; then
    tail -c1 "$input" | read -r _ || echo >> "$input"
fi

skip_header=1


# Open the input file with a file descriptor. 
# Using a file descriptor instead of redirecting stdout ensures that autogpt runs in an interactive shell, 
# so if needed, the user could be asked for input.
exec 3< "$input"
while IFS= read -r line <&3; do
    if ((skip_header)); then
        ((skip_header--))
    else
        csv -a fields "$line"
        echo "Current run input: " ${fields[0]}, ${fields[1]}, ${fields[2]}, ${fields[3]}, ${fields[4]}, ${fields[5]}, ${fields[6]} , ${fields[7]}
        python3 scripts/prepare_ai_settings.py "${fields[0]}" "${fields[1]}" "${fields[2]}" "${fields[3]}" "${fields[4]}" "${fields[5]}" "${fields[6]}" "${fields[7]}"

        ./run.sh --ai-settings agent_config_and_prompt_files/ai_settings.yaml --model-version gpt-4.1-mini-2025-04-14 -c --none-interactive -m json_file --experiment-file "$2"
    fi
done

# Close the file descriptor
exec 3<&-