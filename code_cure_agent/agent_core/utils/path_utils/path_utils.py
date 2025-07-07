import os
import uuid


def preprocess_paths(workspace, project_name: str, file_path):
    project_dir = os.path.join(workspace, project_name)

    if not os.path.exists(os.path.join(project_dir, file_path)):

        file_index_file_name = "cca_files_index_java_only.txt"

        if not os.path.exists(os.path.join(project_dir, file_index_file_name)):
            with open(os.path.join(project_dir, file_index_file_name), "w") as fit:
                fit.write("\n".join(list_files(project_dir)))

        with open(os.path.join(project_dir, file_index_file_name)) as fit:
            files_index = [f for f in fit.read().splitlines()
                           if file_path in f]

        if len(files_index) == 1:
            file_path = files_index[0]
        elif len(files_index) >= 1:
            raise ValueError(
                "Multiple Candidate Paths. We do not handle this yet!")
        else:
            raise ValueError(
                "The file_path {} does not exist.".format(file_path))
    return file_path


def list_files(main_dir, java_only=True) -> list:
    directory = main_dir
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if not java_only or file.endswith(".java"):
                java_files.append(os.path.join(
                    root.replace("{}/".format(main_dir), ""), file))

    return java_files


def find_all_folders(workspace: str, project_name: str, folder_sub_path: str):
    """
    Finds the paths of all the folders that match the folder_sub_path in their full path
    """
    project_dir = os.path.join(workspace, project_name)

    all_folders = []

    for root, dirs, _ in os.walk(project_dir):
        for dir in dirs:
            if folder_sub_path in os.path.join(root, dir):
                all_folders.append(os.path.join(root, dir))

    return all_folders


def sanitize_and_shorten_file_path(file_path: str):
    sanitized_warning_file_path = file_path.replace(
        "/", ".")

    # Replace the file_path with a unique id if it becomes to long for the output files
    if len(sanitized_warning_file_path) > 45:
        sanitized_warning_file_path = str(uuid.uuid4().hex)
    return sanitized_warning_file_path
