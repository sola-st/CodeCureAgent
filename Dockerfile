# Standalone Docker image for CodeCureAgent.
# Replicates the devcontainer setup (.devcontainer/) with the project files baked in.

FROM mcr.microsoft.com/devcontainers/base:jammy

# Install system dependencies (matches .devcontainer/Dockerfile)
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    openjdk-17-jdk \
    dos2unix \
    bash-builtins \
    maven=3.6.3-5 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Install Python 3.10 with pip (matches devcontainer Python feature)
RUN apt-get update && apt-get install -y python3.10 python3.10-dev python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Copy project files (see .dockerignore for exclusions, notably .git)
WORKDIR /workspace
RUN mkdir CodeCureAgent
WORKDIR /workspace/CodeCureAgent
COPY . .

# Run setup equivalent to the devcontainer postCreateCommand:
# install Python deps and build the bundled Sorald jar
RUN cd code_cure_agent \
    && python3.10 -m pip install -r requirements.txt \
    && cd sorald \
    && mvn clean package -DskipTests \
    && cp sorald/target/sorald-*-jar-with-dependencies.jar sorald.jar

WORKDIR /workspace/CodeCureAgent
CMD ["/bin/bash"]
