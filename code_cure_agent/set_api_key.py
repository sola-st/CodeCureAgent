import os

# Overwrites the .env files with the OPENAI_API_KEY provided via a user prompt
# If new environment variables are to add to .env, add them here


def main():
    file_paths = ["agent_core/.env", ".env"]

    print("Please provide your OpenAI API-KEY.")
    openai_api_key = input("OpenAI API-KEY: ").strip()

    for file_path in file_paths:
        with open(file_path, 'w+') as file:
            file.write("OPENAI_API_KEY=" + openai_api_key)


if __name__ == "__main__":
    main()
