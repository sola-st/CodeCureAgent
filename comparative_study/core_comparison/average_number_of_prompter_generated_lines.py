import os
import statistics
import re


PROMPTER_RESULTS_DIRECTORY = "./COREMSRI/comparison_output/cca_dataset_results/proposer_results"

TEMP_FOLDER = "./temp"


def get_llm_output_line_counts_and_file_size(root_dir):
    if not os.path.exists(TEMP_FOLDER):
        os.makedirs(TEMP_FOLDER)

    line_counts = []
    file_sizes = []
    max_file_size = 0
    name_file_max_file_size = ""
    name_file_max_line_number = ""
    max_line_number = 0
    for folder_name in os.listdir(root_dir):
        folder_path = os.path.join(root_dir, folder_name)
        if os.path.isdir(folder_path):
            for sub_folder_name in os.listdir(folder_path):
                sub_folder_path = os.path.join(folder_path, sub_folder_name)
                if os.path.isdir(sub_folder_path):
                    for file_name in os.listdir(sub_folder_path):
                        if file_name.endswith('.log'):
                            file_path = os.path.join(
                                sub_folder_path, file_name)
                            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                                file_content = f.read()
                                pattern = re.compile(
                                    r"------------ Result for Prompt 0 ------------(.*?)------------ End Result for Prompt 0 ------------", re.DOTALL)
                                match = pattern.search(file_content)
                                if match:
                                    line_counts.append(
                                        len(match.group(1).splitlines()))
                                    with open(os.path.join(TEMP_FOLDER, file_name + ".java"), 'w', encoding='utf-8') as temp_file:
                                        temp_file.write(match.group(1))
                                        file_size = os.path.getsize(
                                            os.path.join(TEMP_FOLDER, file_name + ".java"))
                                        file_sizes.append(file_size)
                                        os.remove(os.path.join(
                                            TEMP_FOLDER, file_name + ".java"))
                                        if file_size > max_file_size:
                                            max_file_size = file_size
                                            name_file_max_file_size = file_path
                                    if line_counts[-1] > max_line_number:
                                        max_line_number = line_counts[-1]
                                        name_file_max_line_number = file_path

    return line_counts, file_sizes, name_file_max_file_size, name_file_max_line_number


def calc_average_number_of_prompter_generated_lines():
    line_counts, file_sizes, name_file_max_file_size, name_file_max_line_number = get_llm_output_line_counts_and_file_size(
        PROMPTER_RESULTS_DIRECTORY)
    if line_counts:
        mean_lines = statistics.mean(line_counts)
        median_lines = statistics.median(line_counts)
        min_lines = min(line_counts)
        max_lines = max(line_counts)
        print(f"Processed {len(line_counts)} Java files.")
        print(f"Average (mean) lines per file: {mean_lines:.2f}")
        print(f"Median lines per file: {median_lines}")
        print(f"Minimum lines per file: {min_lines}")
        print(f"Maximum lines per file: {max_lines}")
        print(
            f"File with the most lines: {name_file_max_line_number} with {max_lines} lines")
    else:
        print("No Java files found.")

    if file_sizes:
        mean_size = statistics.mean(file_sizes)
        median_size = statistics.median(file_sizes)
        mean_size /= 1024  # Convert to kB
        median_size /= 1024  # Convert to kB
        min_size = min(file_sizes) / 1024  # Convert to kB
        max_size = max(file_sizes) / 1024  # Convert to kB
        print(f"Average (mean) file size: {mean_size:.2f} kB")
        print(f"Median file size: {median_size:.2f} kB")
        print(f"Minimum file size: {min_size:.2f} kB")
        print(f"Maximum file size: {max_size:.2f} kB")
        print(
            f"Largest file: {name_file_max_file_size} with size {max_size:.2f} kB")
    else:
        print("No Java files found for file size calculation.")


if __name__ == "__main__":
    calc_average_number_of_prompter_generated_lines()
