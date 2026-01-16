import torch
from torch.utils.data import TensorDataset, DataLoader


def gaussian_scale(inputs, std_dev=0.1):
    """Apply Gaussian scaling to the input vector
    Args:
    inputs (torch.Tensor): The original vector
    std_dev (float): The standard deviation for the Gaussian noise

    Returns:
        torch.Tensor: The scaled vector
    """
    noise = torch.randn_like(inputs) * std_dev
    return inputs * (1 + noise)


def augment_data(loader, n_augmentations=5, std_dev=0.1, augment_types=[], batch_size=1024):
    """Apply Gaussian scaling data augmentation only to selected data types
    Args:
        loader (DataLoader): The original data loader
        n_augmentations (int): The number of augmented data points to generate per original data point
        std_dev (float): The standard deviation for the Gaussian scaling
        augment_types (List): A list of data types to be augmented

    Returns:
        DataLoader: A DataLoader containing only the augmented data
    """
    augmented_inputs_list = []
    augmented_labels_list = []
    augmented_types_list = []
    augmented_x_experts_list = []
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

    print(f'Initial data count: {len(loader.dataset)}')

    for inputs, labels, types, x_experts in loader:

        # Check the type of each data point; if the type is in the augment_types list, generate augmented data
        for i in range(inputs.size(0)):
            if types[i] in augment_types:
                for _ in range(n_augmentations):
                    augmented_input = gaussian_scale(inputs[i].unsqueeze(0), std_dev=std_dev)
                    augmented_inputs_list.append(augmented_input)
                    augmented_labels_list.append(labels[i].unsqueeze(0))
                    augmented_types_list.append(types[i].unsqueeze(0))
                    augmented_x_experts_list.append(x_experts[i].unsqueeze(0))

    # Convert the augmented data into a TensorDataset
    if augmented_inputs_list:  # 如果有生成增强数据
        augmented_inputs = torch.cat(augmented_inputs_list, dim=0)
        augmented_labels = torch.cat(augmented_labels_list, dim=0)
        augmented_types = torch.cat(augmented_types_list, dim=0)
        augmented_x_experts = torch.cat(augmented_x_experts_list, dim=0)
        augmented_dataset = TensorDataset(augmented_inputs, augmented_labels, augmented_types, augmented_x_experts)
    else:
        # If no augmented data is generated, create an empty TensorDataset
        augmented_dataset = TensorDataset(torch.empty(0), torch.empty(0), torch.empty(0), torch.empty(0))

    print(f'Augmented data count: {len(augmented_dataset)}')

    # Create a new DataLoader with the augmented data
    augmented_loader = DataLoader(augmented_dataset, batch_size=batch_size, shuffle=True)

    return augmented_loader


def binary_interpolation(loader, device, num, batch_size):
    # Initialize a list to store the augmented data
    all_inputs = []
    all_labels = []
    all_types = []
    all_x_experts = []

    print(f'Initial data count: {len(loader.dataset)}')

    for inputs, labels, types, x_experts in loader:
        augmentation_count = {}

        # Iterate over each row, find rows where x_expert and label are the same
        for i in range(len(labels)):
            for j in range(i+1, len(labels)):
                if torch.all(x_experts[i] == x_experts[j]).item() and labels[i] == labels[j] and types[i] == types[j]:
                    if i not in augmentation_count:
                        augmentation_count[i] = 0
                    if augmentation_count[i] < num:
                        p = 0.25
                        mask = torch.bernoulli(torch.full(inputs[i].size(), p))

                        # Apply binary interpolation
                        new_input = mask * inputs[i] + (1 - mask) * inputs[j]

                        # Add only the new data instances
                        all_inputs.append(new_input.unsqueeze(0))
                        all_labels.append(labels[i].unsqueeze(0))
                        all_types.append(types[i].unsqueeze(0))
                        all_x_experts.append(x_experts[i].unsqueeze(0))

                        augmentation_count[i] += 1

    # Concatenate all the data
    all_inputs = torch.cat(all_inputs, dim=0)
    all_labels = torch.cat(all_labels, dim=0)
    all_types = torch.cat(all_types, dim=0)
    all_x_experts = torch.cat(all_x_experts, dim=0)

    # Create a new TensorDataset and DataLoader
    augmented_dataset = TensorDataset(all_inputs, all_labels, all_types, all_x_experts)
    augmented_loader = DataLoader(augmented_dataset, batch_size=batch_size, shuffle=True)

    print(f'Augmented data count: {len(all_inputs)}')

    return augmented_loader

def Linear_interpolation(loader, device, num, batch_size, target_types):
    all_inputs = []
    all_labels = []
    all_types = []
    all_x_experts = []

    initial_data_count = len(loader.dataset)
    print(f'Initial data count: {initial_data_count}')

    for inputs, labels, types, x_experts in loader:

        augmentation_count = {}

        for i in range(len(labels)):
            if i not in augmentation_count:
                augmentation_count[i] = 0
            for j in range(i+1, len(labels)):
                if torch.all(x_experts[i] == x_experts[j]).item() and labels[i] == labels[j] and types[i] == types[j] and types[i].item() in target_types:

                    # Sample alpha from a uniform distribution U(0.9, 1.1)
                    alpha = torch.FloatTensor(1).uniform_(0.9, 1.1)

                    # Apply linear interpolation
                    new_input = alpha * inputs[i] + (1 - alpha) * inputs[j]

                    all_inputs.append(new_input.unsqueeze(0))
                    all_labels.append(labels[i].unsqueeze(0))
                    all_types.append(types[i].unsqueeze(0))
                    all_x_experts.append(x_experts[i].unsqueeze(0))

                    augmentation_count[i] += 1

                    # If the predetermined number of augmentations has been reached, stop iterating
                    if augmentation_count[i] >= num:
                        break

    all_inputs = torch.cat(all_inputs, dim=0)
    all_labels = torch.cat(all_labels, dim=0)
    all_types = torch.cat(all_types, dim=0)
    all_x_experts = torch.cat(all_x_experts, dim=0)

    # augmented_data_count = len(all_inputs)
    # print(f'Augmented data count: {augmented_data_count}')

    # Create a new TensorDataset and DataLoader
    augmented_dataset = TensorDataset(all_inputs, all_labels, all_types, all_x_experts)
    augmented_loader = DataLoader(augmented_dataset, batch_size=batch_size, shuffle=True)

    return augmented_loader