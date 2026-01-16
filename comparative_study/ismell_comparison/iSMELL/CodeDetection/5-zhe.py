import os
import random

import numpy as np
import pandas as pd
import torch
from torch.utils.data import Dataset, DataLoader, Subset, ConcatDataset
from sklearn.model_selection import KFold
from moe.core import custom_loss, MoE, generate_ground_truth
from torch.utils.tensorboard import SummaryWriter
from moe.newmodel import ExcelDataset, num_experts, top_k, top_k_per_column, flip_cols
from moe.augement_data import binary_interpolation, Linear_interpolation, augment_data


def evaluate_model(model, dataloader, device, flip_cols,fold):
    model.eval()
    val_losses = []
    total_correct = 0
    total_samples = 0

    smell_types = [ 'LongMethod', 'GodClass','FeatureEnvy', 'RefusedBequest']
    correct_counts = {smell: 0 for smell in smell_types}
    total_counts = {smell: 0 for smell in smell_types}

    with torch.no_grad():
        for inputs, labels, types, x_experts in dataloader:
            inputs, labels, types, x_experts = inputs.to(device), labels.to(device), types.to(device), x_experts.to(
                device)
            inputs = inputs.type(torch.float32)
            normalized_accuracies, top_k_per_column_indices = model(inputs)

            val_loss = criterion(normalized_accuracies, labels, types, x_experts, flip_cols, top_k_per_column_indices)
            val_losses.append(val_loss.item())

            for i in range(len(labels)):
                label = labels[i]
                type_index = types[i].item()
                predicted_index = top_k_per_column_indices[i, 0, type_index].item()  # 访问具体某一类的最高准确率索引

                expert_detection = x_experts[i, predicted_index].item()
                is_correct = (label == expert_detection)

                smell_type = smell_types[type_index]
                correct_counts[smell_type] += 1 if is_correct else 0
                total_counts[smell_type] += 1
                total_correct += 1 if is_correct else 0
                total_samples += 1

    val_loss_avg = sum(val_losses) / len(val_losses) if val_losses else 0
    overall_accuracy = total_correct / total_samples if total_samples > 0 else 0
    print(f"fold{fold}_Validation Loss = {val_loss_avg:.4f}, Overall Accuracy = {overall_accuracy:.2%}")

    for smell, count in correct_counts.items():
        if smell == 'LongMethod':
            continue
        accuracy = count / total_counts[smell] if total_counts[smell] > 0 else 0
        print(f"{smell} Accuracy: {accuracy:.2%}")
        writer.add_scalar(f'fold{fold}_Accuracy/{smell}', accuracy, epoch)

    writer.add_scalar(f"fold{fold}_Validation Loss", val_loss_avg, epoch)
    writer.add_scalar(f"fold{fold}_Overall Accuracy", overall_accuracy, epoch)

    return {
        "val_loss_avg": val_loss_avg,
        "overall_accuracy": overall_accuracy,
        "matches_by_type": {smell: {"correct": correct_counts[smell], "total": total_counts[smell]} for smell in
                            smell_types}
    }
if __name__ == '__main__':
    seed_value = 808
    torch.manual_seed(seed_value)
    torch.cuda.manual_seed_all(seed_value)
    np.random.seed(seed_value)  # Numpy module.
    random.seed(seed_value)  # Python random module.

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    writer = SummaryWriter("../logs")

    # 加载数据
    excel_dataset = ExcelDataset("../updated_dataset.xlsx", sheet_name="Sheet1")
    df = excel_dataset.df

    if not os.path.exists('../test_data'):
        os.makedirs('../test_data')

    kfold = KFold(n_splits=5, shuffle=True, random_state=seed_value)
    num_epochs = 70
    val_epoch = 70
    results = []
    overall_accuracies = []
    smell_type_accuracies = {smell: [] for smell in ['GodClass', 'LongMethod', 'FeatureEnvy', 'RefusedBequest']}
    for fold, (train_ids, test_ids) in enumerate(kfold.split(df)):

        last_epoch_data = {}

        print(f'FOLD {fold}')
        test_data = df.iloc[test_ids]
        test_data.to_excel(f'../test_data/fold_{fold}_test_data.xlsx', index=False)

        train_dataset = Subset(excel_dataset, train_ids)
        test_dataset = Subset(excel_dataset, test_ids)
        # 使用增强数据
        train_loader = DataLoader(train_dataset, batch_size=1024, shuffle=True)
        augmented_train_data1 = binary_interpolation(train_loader, device,num=5,batch_size=2048)
        augmented_train_data2 = Linear_interpolation(train_loader, device,num=5,batch_size=2048,target_types = [0,1,2,3])
        augmented_train_data3 = augment_data(train_loader, n_augmentations=5, std_dev=0.10,augment_types = [0,1,2,3],batch_size =2048)
        test_loader = DataLoader(test_dataset, batch_size=2048, shuffle=False)

        dataset0 = train_loader.dataset
        dataset1 = augmented_train_data1.dataset
        dataset2 = augmented_train_data2.dataset
        dataset3 = augmented_train_data3.dataset

        # Merge datasets
        combined_dataset = ConcatDataset([dataset0,dataset1, dataset2, dataset3])

        # Create a new data loader
        combined_loader = DataLoader(combined_dataset, batch_size=2048)

        # Fetch a batch of training data to obtain the dimensions of x_features
        inputs, labels, types, x_experts = next(iter(train_loader))
        input_dim = inputs.shape[1]

        # Initialize the model and optimizer
        model = MoE(input_dim, num_experts, top_k=top_k, top_k_per_column=top_k_per_column)
        model.to(device)

        criterion = custom_loss
        optimizer = torch.optim.Adam(model.parameters(), lr=0.00003)
        scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, 'min', patience=3)

        best_match = 20
        best_epoch = 0

        for epoch in range(num_epochs):
            model.train()
            train_losses = []
            for inputs, labels, types, x_experts in combined_loader:

                inputs, labels, x_experts = inputs.to(device), labels.to(device), x_experts.to(device)
                inputs = inputs.type(torch.float32)
                optimizer.zero_grad()
                normalized_accuracies, top_k_per_column_indices = model(inputs)
                loss = criterion(normalized_accuracies, labels, types, x_experts, flip_cols, top_k_per_column_indices)
                loss.backward()
                optimizer.step()
                train_losses.append(loss.item())

            if (epoch + 1) % val_epoch == 0:
                    metrics = evaluate_model(model, test_loader, device,flip_cols,fold)
                    if metrics['val_loss_avg'] < best_match:
                        best_loss = metrics['val_loss_avg']
                        best_epoch = epoch
                        torch.save(model.state_dict(), f"../modeltest/best_model_fold_{fold}_epoch_{epoch+1}.pth")
                        print(f'Fold {fold}, Epoch {epoch+1} - New best model saved with accuracy: {best_loss:.4f}')

            if  len(train_losses)!=0:
                train_loss_avg = sum(train_losses) / len(train_losses)
            print(f"Epoch {epoch + 1}: Train Loss = {train_loss_avg:.4f}")
            writer.add_scalar(f'fold{fold}_: Train Loss ', train_loss_avg, epoch)

            if epoch == num_epochs-1 :
                last_epoch_data = metrics
                overall_accuracies.append(last_epoch_data['overall_accuracy'])

        for smell in smell_type_accuracies:
            smell_type_accuracies[smell].append(
                last_epoch_data['matches_by_type'][smell]['correct'] / last_epoch_data['matches_by_type'][smell][
                    'total'] if last_epoch_data['matches_by_type'][smell]['total'] > 0 else 0)

        results.append((best_epoch, best_loss))
        print(f'Fold {fold} Best at Epoch {best_epoch+1} - Loss: {best_loss:.4f}')

    average_accuracy = sum(overall_accuracies) / len(overall_accuracies)
    average_smell_accuracies = {smell: sum(accuracies) / len(accuracies) for smell, accuracies in
                                smell_type_accuracies.items()}
    print(f'Average Overall Accuracy: {average_accuracy:.4f}')
    for smell, acc in average_smell_accuracies.items():
        print(f'Average Accuracy for {smell}: {acc:.4f}')
