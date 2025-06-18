import os

experiments_list = "experimental_setups/experiments_list.txt"

# Create experiments_list.txt if not yet created
if not os.path.isfile(experiments_list):
    try:
        with open(experiments_list, "x"):
            pass
    except FileExistsError:
        pass


with open(experiments_list, "r+") as expl:
    exps = expl.read().splitlines()
    if exps:
        last_exp = int(exps[-1].split("_")[1])
    else:
        last_exp = 0

    print("Creating experiment folder:", last_exp+1)
    os.mkdir("experimental_setups/experiment_{}".format(last_exp + 1))

    os.mkdir(
        "experimental_setups/experiment_{}/classification".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp".format(last_exp + 1))

    os.mkdir(
        "experimental_setups/experiment_{}/classification/prompt_history".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/classification/all_messages".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/classification/responses".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/classification/saved_contexts".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/classification/analysis_reports".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/classification/docu_tool_outputs".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/classification/execution_info".format(last_exp + 1))

    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/prompt_history".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/all_messages".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/responses".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/saved_contexts".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/plausible_patches".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/implausible_patches".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/analysis_reports".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/docu_tool_outputs".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_tp/execution_info".format(last_exp + 1))

    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/prompt_history".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/all_messages".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/responses".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/saved_contexts".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/plausible_patches".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/implausible_patches".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/analysis_reports".format(last_exp + 1))
    os.mkdir(
        "experimental_setups/experiment_{}/fix_fp/execution_info".format(last_exp + 1))

    expl.write("experiment_{}\n".format(last_exp + 1))
