import numpy as np
import pandas
import pandas as pd
import torch
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from torch.utils.data import Dataset, DataLoader, TensorDataset
from torch.utils.tensorboard import SummaryWriter

from moe.core import custom_loss, MoE, generate_ground_truth


class ExcelDataset(Dataset):

    def __init__(self, filepath="", sheet_name=""):
        # Print information about file and sheet being read
        print(f"Reading file from {filepath}, sheet = {sheet_name}")

        # Load Excel data into a DataFrame
        self.df = pandas.read_excel(
            filepath, header=0,
            sheet_name=sheet_name,
        )

        # Display the shape of the loaded DataFrame
        print(f"The shape of the dataframe is {self.df.shape}")

        # Identify columns related to expert features
        expert_feat_cols = self.df.columns[7:14]
        # Apply the 'process' method to each element within the expert feature columns
        self.df[expert_feat_cols] = self.df[expert_feat_cols].map(self.process)

        # Extract expert features and labels
        expert_feat = self.df.iloc[:, 7:14].values
        label = self.df.loc[:, "Label"].values

        # Split dataframe to separate vectors
        split_df = self.split_vectors(self.df)

        # Extract code smell features assuming columns 15 to 103 (104 in Python indexing is exclusive)
        code_smell_features = split_df.iloc[:, 15:104].values.astype(np.float64)
        # Extract CodeBERT features from the remaining columns
        codebert_features = split_df.iloc[:, 104:].values.astype(np.float64)

        # Normalize the code smell feature vectors
        scaler_smell = StandardScaler()
        code_smell_features = scaler_smell.fit_transform(code_smell_features)

        # Concatenate the normalized code smell features with CodeBERT features
        features = np.concatenate((code_smell_features, codebert_features), axis=1)

        # Convert numpy arrays to PyTorch tensors
        self.x_expert = torch.from_numpy(expert_feat)
        self.x_features = torch.from_numpy(features)

        # Create a mapping from code smells to indices
        self.code_smell_to_index = {'LongMethod': 0, 'GodClass': 1, 'FeatureEnvy': 2, 'RefusedBequest': 3}

        # Transform the 'Code Smell' column using the predefined mapping
        self.code_smell_index = self.df.loc[:, "Smell"].map(self.code_smell_to_index).values  # The map function can also utilize dictionary keys for mapping values
        self.y = torch.from_numpy(label)

    def split_vectors(self, df):
        # Check if there are non-string type values in the 'code_smell_vector' column and convert them to strings
        # if df['code_smell_vector'].apply(lambda x: isinstance(x, float)).any():
        #     df['code_smell_vector'] = df['code_smell_vector'].apply(lambda x: '' if isinstance(x, float) else x)
        #
        # Check if there are non-string type values in the 'codebert_vector' column and convert them to strings
        # if df['codebert_vector'].apply(lambda x: isinstance(x, float)).any():
        #     df['codebert_vector'] = df['codebert_vector'].apply(lambda x: '' if isinstance(x, float) else x)

        # Split the 'code_smell_vector' column; str.split() returns a list containing all substrings, with each element as a string
        # code_smell_vectors becomes a Series where each entry is a list
        code_smell_vectors = df['code_smell_vector'].str.split(',')
        # Generate column names for code smell features
        code_smell_columns = [f'code_smell_{i}' for i in range(len(code_smell_vectors.iloc[0]))]
        # Convert the list of lists (code_smell_vectors) into a DataFrame with column names from code_smell_columns
        code_smell_df = pd.DataFrame(code_smell_vectors.tolist(), columns=code_smell_columns)

        # Split the 'codebert_vector' column
        codebert_vectors = df['codebert_vector'].str.split(',')
        # Generate column names for CodeBERT features
        codebert_columns = [f'codebert_{i}' for i in range(len(codebert_vectors.iloc[0]))]
        # Convert the list of lists (codebert_vectors) into a DataFrame with column names from codebert_columns
        codebert_df = pd.DataFrame(codebert_vectors.tolist(), columns=codebert_columns)

        # Concatenate the original DataFrame with the newly created DataFrames after dropping the original vector columns
        result_df = pd.concat([df.drop(['code_smell_vector', 'codebert_vector'], axis=1), code_smell_df, codebert_df],
                              axis=1)

        return result_df

    def process(self, cell_value):
        if pd.isna(cell_value):  # Check if the cell value is NaN
            return 0  # Return 0 or any other desired value for NaN cells
        elif cell_value in ["Detected"]:
            return 1
        elif cell_value in ["Undetected", "Detection Not Supported"]:
            return 0
        else:
            print("No match found. Raising ValueError.")
            raise ValueError(f"Unknown expert status: {cell_value}")

    def __len__(self):
        return len(self.y)

    def __getitem__(self, item):
        return self.x_features[item], self.y[item], self.code_smell_index[item], self.x_expert[item],


def evaluate_model(model, dataloader, device, flip_cols):
    model.eval()
    val_losses = []  # List to store validation losses
    total_correct = 0  # Count of correctly predicted samples
    total_samples = 0  # Total number of samples evaluated

    # Assume smell_types encompasses all possible categories of labels
    smell_types = ['LongMethod', 'GodClass', 'FeatureEnvy', 'RefusedBequest']
    correct_counts = {smell: 0 for smell in smell_types}  # Correct predictions per smell type
    total_counts = {smell: 0 for smell in smell_types}  # Total predictions per smell type

    with torch.no_grad():  # Disable gradient calculation for evaluation
        for inputs, labels, types, x_experts in dataloader:
            # Move tensors to the specified device and ensure the input data is of type float32
            inputs, labels, types, x_experts = inputs.to(device), labels.to(device), types.to(device), x_experts.to(
                device)
            inputs = inputs.type(torch.float32)

            # Forward pass through the model to get normalized accuracies and top indices
            normalized_accuracies, top_k_per_column_indices = model(inputs)

            # Calculate validation loss
            val_loss = criterion(normalized_accuracies, labels, types, x_experts, flip_cols, top_k_per_column_indices)
            val_losses.append(val_loss.item())  # Collect individual losses

            # Loop over each sample in the batch
            for i in range(len(labels)):
                # Get true label, type index, and predicted expert index
                label = labels[i]
                type_index = types[i].item()
                predicted_index = top_k_per_column_indices[
                    i, 0, type_index].item()  # Top-1 prediction index for this type

                # Check if the prediction matches the true expert detection
                expert_detection = x_experts[i, predicted_index].item()
                is_correct = (label == expert_detection)

                # Update counts for accuracy calculation
                smell_type = smell_types[type_index]
                correct_counts[smell_type] += int(is_correct)
                total_counts[smell_type] += 1
                total_correct += int(is_correct)
                total_samples += 1

    # Compute average validation loss and overall accuracy
    val_loss_avg = sum(val_losses) / len(val_losses) if val_losses else 0
    overall_accuracy = total_correct / total_samples if total_samples > 0 else 0
    print(f"Validation Loss = {val_loss_avg:.4f}, Overall Accuracy = {overall_accuracy:.2%}")

    # Print and log accuracy per smell type
    for smell, count in correct_counts.items():
        accuracy = count / total_counts[smell] if total_counts[smell] > 0 else 0
        print(f"{smell} Accuracy: {accuracy:.2%}")
        writer.add_scalar(f'Accuracy/{smell}', accuracy, epoch)  # Assuming 'epoch' is defined elsewhere

    # Log overall metrics
    writer.add_scalar("Validation Loss", val_loss_avg, epoch)
    writer.add_scalar("Overall Accuracy", overall_accuracy, epoch)

    # Return summary of evaluation metrics
    return {
        "val_loss_avg": val_loss_avg,
        "overall_accuracy": overall_accuracy,
        "matches_by_type": {smell: {"correct": correct_counts[smell], "total": total_counts[smell]} for smell in
                            smell_types}
    }

num_experts = 7
top_k = 7
flip_cols = [0, 1]
top_k_per_column = 1
val_epoch = 1
best_exact_one_off = 1
criterion = custom_loss
writer = SummaryWriter("../logs")
device = torch.device('cuda:0')

if __name__ == '__main__':
    # Set random seed for reproducibility
    seed_value = 520
    torch.manual_seed(seed_value)  # For CPU operations
    torch.cuda.manual_seed_all(seed_value)  # If using GPU, set the random seed for all GPUs

    print("Testing ExcelDataset")
    excel_dataset = ExcelDataset("../updated_dataset.xlsx", sheet_name="Sheet1")

    # Split dataset into training and validation sets
    train_dataset, val_dataset = train_test_split(excel_dataset, test_size=0.1, random_state=42)
    # Further split validation into validation and test sets (commented out)

    train_loader = DataLoader(train_dataset, batch_size=2, shuffle=True)
    # Example of data augmentation steps (commented out)

    val_dataloader = DataLoader(val_dataset, batch_size=512, shuffle=False)
    #test_dataloader = DataLoader(test_dataset, batch_size=256, shuffle=False)

    # Fetch a batch of training data to determine x_features dimension
    inputs, labels, types, x_experts = next(iter(train_loader))
    input_dim = inputs.shape[1]  # The 1st dimension represents the feature dimension

    # Initialize the Mixture of Experts (MoE) model
    model = MoE(input_dim, num_experts, top_k=top_k, top_k_per_column=top_k_per_column)

    # Define loss function and optimizer
    criterion = custom_loss  # Using the custom loss function
    optimizer = torch.optim.Adam(model.parameters(), lr=0.00003)
    scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, 'min', patience=3)

    num_epochs = 10000
    model.to(device)

    for epoch in range(num_epochs):
        model.train()
        train_losses = []

        for inputs, labels, types, x_experts in train_loader:
            inputs, labels, types, x_experts = inputs.to(device), labels.to(device), types.to(device), x_experts.to(device)
            optimizer.zero_grad()

            # Assert no NaNs in inputs
            assert not torch.isnan(inputs).any(), "Input data contains NaN!"
            inputs = inputs.type(torch.float32)

            # Forward pass
            normalized_accuracies, top_k_per_column_indices = model(inputs)

            # Compute and backpropagate the loss
            loss = criterion(normalized_accuracies, labels, types, x_experts, flip_cols, top_k_per_column_indices)
            loss.backward()
            optimizer.step()

            train_losses.append(loss.item())

        train_loss_avg = sum(train_losses) / len(train_losses)
        print(f"Epoch {epoch + 1}: Average Training Loss = {train_loss_avg:.4f}")
        writer.add_scalar("Loss", train_loss_avg, epoch)  # Assuming 'writer' is defined elsewhere

        # Evaluate the model and save the best one
        if (epoch + 1) % val_epoch == 0:
            metrics = evaluate_model(model, val_dataloader, device, flip_cols)
            if metrics['val_loss_avg'] < best_exact_one_off:
                best_exact_one_off = metrics['val_loss_avg']
                print(metrics)
                torch.save(model.state_dict(), f"../model/best_model_{epoch + 1}.pth")  # Save the model
                print(f"Model saved at epoch {epoch + 1}")

    print("Training Finished.")
