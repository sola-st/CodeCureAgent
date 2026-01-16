import pandas as pd
import torch
from torch.utils.tensorboard import SummaryWriter

from moe.core import MoE
from moe.newmodel import ExcelDataset


def load_model(model_path, input_dim, num_experts, top_k, top_k_per_column):
    model = MoE(input_dim, num_experts, top_k=top_k, top_k_per_column=top_k_per_column)
    model.load_state_dict(torch.load(model_path))
    model.eval()
    return model


def main(writer, i, j):

    print(f"fold:{i}***************************************************************************************************")
    model_path = f"../modeltest/best_model_fold_{i}_epoch_{j}.pth"
    input_dim = 857
    num_experts = 7
    top_k = 7
    top_k_per_column = 1
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    model = load_model(model_path, input_dim, num_experts, top_k, top_k_per_column).to(device)
    dataset = ExcelDataset(f"../test_data/fold_{i}_test_data.xlsx", sheet_name="Sheet1")
    df = pd.read_excel(f"../test_data/fold_{i}_test_data.xlsx", sheet_name="Sheet1")

    # 新增列
    df['Output'] = ''
    df['Processed Indices'] = ''
    df['Is Prediction Correct'] = ''
    df['Prediction'] = ''

    smell_types = [ 'LongMethod','GodClass', 'FeatureEnvy', 'RefusedBequest']
    correct_counts = {smell: 0 for smell in smell_types}
    total_counts = {smell: 0 for smell in smell_types}
    general_counts = {smell: 0 for smell in smell_types}
    smell_accuracy_counts = {smell: 0 for smell in smell_types}  # 用于准确率计算的计数
    true_positives = {smell: 0 for smell in smell_types}  # 真正例
    false_positives = {smell: 0 for smell in smell_types}  # 假正例
    total_correct = 0
    total_samples = 0
    TP = 0  # True Positives
    FP = 0  # False Positives
    FN = 0  # False Negatives
    wrong_lines = []
    start_line = None
    end_line = None
    line_diff = None
    for index, data in enumerate(dataset):
        input_features, label, types, experts_label = data
        input_features = input_features.unsqueeze(0).to(device)
        input_features = input_features.type(torch.float32)

        if types == 0:
            continue

        with torch.no_grad():
            normalized_accuracies, top_k_per_column_indices = model(input_features)

        smell_index = types
        tool_index = top_k_per_column_indices[0, 0, smell_index].item()
        expert_detection = experts_label[tool_index].item()
        is_correct_general = (label == expert_detection)
        is_positive_prediction = expert_detection

        smell_type = smell_types[smell_index]
        if label == 1 and expert_detection == 1:
            true_positives[smell_type] += 1
        if label == 0 and expert_detection == 1:
            false_positives[smell_type] += 1

        # 更新DataFrame
        df.loc[index, 'Output'] =normalized_accuracies
        df.loc[index, 'Processed Indices'] = top_k_per_column_indices
        df.loc[index, 'Is Prediction Correct'] = 'Correct' if is_correct_general else 'Incorrect'
        df.loc[index, 'Prediction'] = is_positive_prediction

        # 更新统计
        smell_type = smell_types[smell_index]
        general_counts[smell_type] += 1
        if label == 1:
            total_counts[smell_type] += 1
            if is_correct_general:
                correct_counts[smell_type] += 1
                TP += 1
            else:
                FN += 1
        else:
            if not is_correct_general:
                FP += 1

        if is_correct_general:
            smell_accuracy_counts[smell_type] += 1
            total_correct += 1

        total_samples += 1

        if is_correct_general ==0 and smell_type =='LongMethod':
            start_line = df.loc[index, '开始行号']
            end_line = df.loc[index, '结束行号']
            line_diff = end_line - start_line
            wrong_lines.append(line_diff)

    print(wrong_lines)

    df.to_excel(f"../fold{i}moe.xlsx", index=False)

    overall_precision = TP / (TP + FP) if TP + FP > 0 else 0
    overall_recall = TP / (TP + FN) if TP + FN > 0 else 0
    overall_f1 = 2 * (overall_precision * overall_recall) / (overall_precision + overall_recall) if (overall_precision + overall_recall) > 0 else 0

    print(f"Overall accuracy: {total_correct / total_samples:.2%}")
    print(f"Overall recall: {overall_recall:.2%}")
    print(f"Overall precision: {overall_precision:.2%}")
    print(f"Overall F1: {overall_f1:.2%}")
    print("---------------------")
    writer.add_scalar(tag=f'fold{i}', scalar_value=(total_correct / total_samples), global_step=j)

    for smell in smell_types:
        recall = correct_counts[smell] / total_counts[smell] if total_counts[smell] > 0 else 0
        precision = true_positives[smell] / (true_positives[smell] + false_positives[smell]) if (true_positives[smell] +
                                                                                                 false_positives[
                                                                                                     smell]) > 0 else 0
        accuracy = smell_accuracy_counts[smell] / general_counts[smell] if general_counts[smell] > 0 else 0
        f1_score = 2 * (precision * recall) / (precision + recall) if (precision + recall) > 0 else 0

        print(f"{smell} accuracy: {accuracy:.2%}")
        print(f"{smell} recall: {recall:.2%}")
        print(f"{smell} precision: {precision:.2%}")
        print(f"{smell} F1 Score: {f1_score:.2%}")
        print("---------------------")

    return overall_f1

writer = SummaryWriter("../logs")

df = pd.read_excel(r"fold_epoch_accuracy.xlsx", sheet_name="Sheet1")

# if __name__ == "__main__":
#
#     df = pd.DataFrame(columns=['epoch'] + [f'fold{i}' for i in range(5)])
#     df['epoch'] = range(1, 101)
#
#     for i in range(5):
#         for j in range(1, 101, 1):
#             F1 = main(writer, i, j)
#             df.loc[j-1, f'fold{i}'] = F1
#
#     df.to_excel(r"fold_epoch_accuracy_F1_-1_723_150epoch_5zhe_5experts_0.10gaosi.xlsx", index=False)

if __name__ == "__main__":
    for i in range(5):
        accuracy = main(writer, i,70)

    writer.close()
