# 
# Create a small development dataset:
# Samples 5 repositories from the list of all repositories used by Sorald
# and saves the sampled repos to sampled_repos.csv.
#


import random
import csv
import os

# Sample 5 unique numbers from the range 2 to 162
sampled_numbers = random.sample(range(2, 163), 5)
sorted_samples = sorted(sampled_numbers)
print("Sampled numbers:", sorted_samples)

input_csv_path = os.path.join(os.path.dirname(__file__), "original_sorald_considered_repos_stats.csv")
rows_to_write = []

# Read sampled rows in the csv file of all repositories
with open(input_csv_path, "r") as csv_file:
    csv_reader = csv.reader(csv_file)
    for i, row in enumerate(csv_reader):
        if i+1 in sorted_samples:  
            row_to_write = list()
            row_to_write.append(i+1)
            row_to_write.extend(row[:2])
            rows_to_write.append(row_to_write)  

# Write the sampled rows to a new file
output_csv_path = os.path.join(os.path.dirname(__file__), "sampled_repos.csv")
with open(output_csv_path, "w", newline="") as csv_file:
    csv_writer = csv.writer(csv_file)
    csv_writer.writerow(["original_row_number", "repository_url", "commit_id"]) 
    csv_writer.writerows(rows_to_write)