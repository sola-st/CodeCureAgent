#!/bin/bash

enable -f /usr/lib/bash/csv csv

# Move to the directory where run_on_dataset.sh is.
# => We always run from the correct folder
cd "$(dirname "$0")"

#TODO: To be removed as soon as not needed anymore
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
dos2unix "$input" # Convert file to Unix line endings (if needed)

skip_header=1

while IFS= read -r line; do
    if ((skip_header)); then
        ((skip_header--))
    else
        csv -a fields "$line"
        echo "Current run input: " ${fields[0]}, ${fields[1]}, ${fields[2]}, ${fields[3]}, ${fields[4]}
        python3 prepare_ai_settings.py "Codec" "4" "${fields[0]}" "${fields[1]}" "${fields[3]}" "${fields[2]}" "${fields[4]}"
        # TODO: To be replaced or removed as soon as we implemented our own setup procedure
        python3 checkout_py.py "Codec" "4"

        ./run.sh --ai-settings ai_settings.yaml --model-version gpt-4o-mini-2024-07-18 -c -l 40 -m json_file --experiment-file "$2"
    fi
done <"$input"
