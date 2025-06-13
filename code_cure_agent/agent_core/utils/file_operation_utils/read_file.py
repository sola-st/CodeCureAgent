

def read_file(file_relative_path: str) -> str:
    with open(file_relative_path) as file:
        return file.read()
