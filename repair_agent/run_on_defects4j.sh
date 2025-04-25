#!/bin/bash

# Move to the directory where run_on_defects4j.sh is. 
# => We always run from the correct folder
cd "$(dirname "$0")"

export PATH=$PATH:$PWD/defects4j/framework/bin
cpanm --local-lib=~/perl5 local::lib && eval $(perl -I ~/perl5/lib/perl5/ -Mlocal::lib)
for LANG in en_AU.UTF-8 en_GB.UTF-8 C.UTF-8 C; do
  if locale -a 2>/dev/null | grep -q "$LANG"; then
    export LANG
    break
  fi
done
export LC_COLLATE=C

python3 experimental_setups/increment_experiment.py
python3 construct_commands_descriptions.py
input="$1"
dos2unix "$input"  # Convert file to Unix line endings (if needed)

# Open the input file with a file descriptor
exec 3< "$input"
while IFS= read -r line <&3 || [ -n "$line" ]
do
    tuple=($line)
    echo ${tuple[0]}, ${tuple[1]}
    python3 prepare_ai_settings.py "${tuple[0]}" "${tuple[1]}"
    python3 checkout_py.py "${tuple[0]}" "${tuple[1]}"
    ./run.sh --ai-settings ai_settings.yaml --model-version gpt-4o-mini-2024-07-18 -c -l 40 -m json_file --experiment-file "$2"
done

# Close the file descriptor
exec 3<&-