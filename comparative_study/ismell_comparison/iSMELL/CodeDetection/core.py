import torch
import torch.nn as nn
import torch.nn.functional as F
from moe.attention import MultiHeadSelfAttention

class Expert(nn.Module):
    def __init__(self, input_dim, num_heads=4, head_size=16):
        super(Expert, self).__init__()
        # Multi-Head Self-Attention mechanism layer
        self.multi_head_attention = MultiHeadSelfAttention(input_dim, num_heads, head_size)

        # Subsequent fully connected layers, incorporating additional Dropout layers
        self.fc = nn.Sequential(
            nn.Linear(input_dim, 256),
            nn.BatchNorm1d(256),  # BatchNormalization
            nn.ReLU(),
            nn.Dropout(0.5),
            nn.Linear(256, 64),
            nn.BatchNorm1d(64),  # BatchNormalization
            nn.Dropout(0.5),
            nn.ReLU(),
            nn.Linear(64, 4),
            nn.Dropout(0.5)  # 在最后一个Linear层后添加Dropout层
        )

    def forward(self, x):
        x = self.multi_head_attention(x)
        x = x.squeeze(1)
        x = self.fc(x)
        return x


class TopKGating(nn.Module):
    def __init__(self, input_dim, num_experts, top_k, noise_stddev=0.1):
        super(TopKGating, self).__init__()
        # Define the linear layers for the gating mechanism using nn.Sequential
        self.gate = nn.Sequential(
            nn.Linear(input_dim, 256),
            nn.ReLU(),
            nn.Linear(256, 128),
            nn.ReLU(),
            nn.Linear(128, num_experts)
        )

        # Define the linear layers for the noise layer using nn.Sequential
        self.noise_layer = nn.Sequential(
            nn.Linear(input_dim, 64),
            nn.ReLU(),
            nn.Linear(64, 128),
            nn.ReLU(),
            nn.Linear(128, num_experts)
        )

        self.top_k = top_k
        self.noise_stddev = noise_stddev
        self.multi_head_attention = MultiHeadSelfAttention(input_dim, num_heads = 4, head_size = 16)

    def forward(self, x):
        x = self.multi_head_attention(x)
        x = x.squeeze(1)  # 去掉序列长度的维度，x 的形状变为 [batch_size, feature_dimension]

        # Compute the original scores for each expert
        gating_scores = self.gate(x)

        # Apply noise and perform the Softplus operation
        noisy_scores = F.softplus(self.noise_layer(x))

        # Normalize the Softplus-transformed noisy scores
        standardized_noisy_scores = (noisy_scores - noisy_scores.mean(dim=1, keepdim=True)) / (
                noisy_scores.std(dim=1, keepdim=True) + 1e-8)

        # Add the normalized noisy scores to the original gating scores
        combined_scores = gating_scores + standardized_noisy_scores

        # Apply the top-k operation on the combined scores
        top_k_values, top_k_indices = torch.topk(combined_scores, self.top_k, dim=1)

        # Apply softmax to the selected expert scores to obtain weight distributions
        top_k_softmax_weights = F.softmax(top_k_values, dim=1)

        # Return the indices of the top-k experts for each sample along with their corresponding softmax weights
        return top_k_indices, top_k_softmax_weights

class MoE(nn.Module):
    def __init__(self, input_dim, num_experts, top_k, top_k_per_column):
        super(MoE, self).__init__()
        self.experts = nn.ModuleList([Expert(input_dim) for _ in range(num_experts)])
        self.gating = TopKGating(input_dim, num_experts, top_k)
        self.top_k_per_column = top_k_per_column

    def forward(self, x_features):
        # Obtain gating weights and indices
        indices, gating_weights = self.gating(x_features)

        # Retrieve the accuracy of each expert across different items
        expert_accuracies = torch.stack([expert(x_features) for expert in self.experts], dim=1)

        # Extend gating_weights to match the dimensions of expert_accuracies
        extended_gating_weights = gating_weights.unsqueeze(-1).expand_as(expert_accuracies)

        # Apply gating_weights to expert_accuracies
        weighted_accuracies = expert_accuracies * extended_gating_weights

        valid_indices_lists = [
            [0, 1, 2, 3, 4],
            [0, 1, 2, 3, 4],
            [1, 2, 4],
            [2, 4],
        ]

        # Set weighted_accuracies values to -1 for indices not in the valid index lists
        for column in range(weighted_accuracies.size(2)):
            valid_indices = valid_indices_lists[column]
            all_indices = set(range(weighted_accuracies.size(1)))  # Create a set of all row indices
            invalid_indices = all_indices - set(valid_indices)  # Identify invalid row indices
            for invalid_index in invalid_indices:
                weighted_accuracies[:, invalid_index, column] = -1

        # Sort the weighted accuracies to get the top_k_per_column indices
        _, top_k_per_column_indices = weighted_accuracies.topk(self.top_k_per_column, dim=1)

        # Return the matrix of weighted accuracies and the top_k_per_column_indices
        return weighted_accuracies, top_k_per_column_indices


def generate_ground_truth(label, type, x_expert, flip_cols):

    valid_indices_lists = [
        [0, 1, 2, 3, 4],
        [0, 1, 2, 3, 4],
        [1, 2, 4, 5, 6],
        [2, 4],
    ]

    gt_matrix = torch.zeros((len(x_expert), 4))

    if label == 1:
        gt_matrix[:, type] = x_expert
    elif label == 0:
        gt_matrix[:, type] = 1 - x_expert

    mask = torch.zeros_like(gt_matrix, dtype=torch.bool)
    for col, valid_indices in enumerate(valid_indices_lists):
        mask[valid_indices, col] = True

    gt_matrix[~mask] = -1

    return gt_matrix

def custom_loss(normalized_accuracies, labels, types, x_experts, flip_cols, chosen_experts_indices):
    device = torch.device('cuda:0')
    batch_size = labels.shape[0]
    losses = []

    for i in range(batch_size):
        # Generate ground truth matrix for the current sample
        gt_matrix = generate_ground_truth(labels[i], types[i], x_experts[i], flip_cols)
        gt_matrix = gt_matrix.to(device)

        # Extend chosen_experts_indices for broadcasting
        chosen_indices = chosen_experts_indices[i, 0].view(-1, 1)

        # Select corresponding rows from normalized_accuracies
        selected_accuracies = normalized_accuracies[i].index_select(0, chosen_indices.squeeze())

        # Select corresponding rows from gt_matrix
        selected_gt_matrix = gt_matrix.index_select(0, chosen_indices.squeeze())

        # Compute MSE loss element-wise without reduction
        individual_losses = F.smooth_l1_loss(selected_accuracies, selected_gt_matrix, reduction='none', beta=1.0)
        losses.append(individual_losses.sum())

    # Take the mean of all losses
    total_loss = torch.mean(torch.stack(losses))
    return total_loss











