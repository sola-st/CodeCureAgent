import os

# TODO: test and understand what this does in detail. Is it what we need?

def preprocess_paths(workspace, project_name: str, file_path):
    project_dir = os.path.join(workspace, project_name)
    
    if file_path.endswith(".java"):
        file_path = file_path[:-5]
        file_path = file_path.replace(".", "/")
        file_path += ".java"
    else:
        file_path = file_path.replace(".", "/")
    
    if not os.path.exists(os.path.join(project_dir,file_path)):
        if not os.path.exists(os.path.join(project_dir, "files_index.txt")):
            with open(os.path.join(project_dir, "files_index.txt"), "w") as fit:
                fit.write("\n".join(list_java_files(project_dir)))
            
        with open(os.path.join(project_dir, "files_index.txt")) as fit:
            files_index = [f for f in fit.read().splitlines() if file_path in f]
        
        if len(files_index) == 1:
            file_path = files_index[0]
        elif len(files_index) >= 1:
            raise ValueError("Multiple Candidate Paths. We do not handle this yet!")
        else:
            return "The file_path {} does not exist.".format(file_path)
    return file_path


def list_java_files(main_dir) -> list:
    directory = main_dir
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root.replace("{}/".format(main_dir), ""), file))

    return java_files