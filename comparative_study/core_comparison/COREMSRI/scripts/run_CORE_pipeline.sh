#!/bin/bash
DEBUGDIR=debug/cca_dataset_results
DATASET=dataset/cca_dataset
QUERY="S2111"

# Stage 2 and 3
python src/run_llm_proposer.py -e $DATASET -t $DEBUGDIR/proposer_results -r $DATASET -s $DEBUGDIR/files.pkl --Queries "$QUERY" --queries_meta_file "metadata/java/metadata_full_sonar_way_profile.json" --language "java" --model "gpt-4.1-mini-2025-04-14"

# Stage 4
#python src/run_codeql_verifier.py -i $DEBUGDIR/proposer_results -c codeql-home/codeql-repo/python/ql/src/ -d $DEBUGDIR/verifier_results/db/ -o $DEBUGDIR/verifier_results/res/ --Queries "$QUERY"

#python src/run_codeql_analysis.py -i $DATASET -g $DEBUGDIR/proposer_results/ -d $DEBUGDIR/verifier_results/db/ -r $DEBUGDIR/verifier_results/res/ -o $DEBUGDIR/analysis_results/ -s $DEBUGDIR/files.pkl --Queries "$QUERY"

#python src/get_diffs.py -p $DATASET -g $DEBUGDIR/proposer_results -a $DEBUGDIR/analysis_results/demo_filefix_types_Top10.pickle -s $DEBUGDIR/diff_folder --metadata_file "metadata/java/metadata.json"

# Stage 5
#python src/run_llm_ranker.py -d $DEBUGDIR/diff_folder -o $DEBUGDIR/ranker_results/ --Queries "$QUERY"

#python src/get_results.py -r $DEBUGDIR/ranker_results/ -p $DATASET -g $DEBUGDIR/proposer_results/ -d $DEBUGDIR/diff_folder/ -o $DEBUGDIR/results/ -j $DEBUGDIR/results.json
