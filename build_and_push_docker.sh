#!/usr/bin/env bash
# Build the CodeCureAgent Docker image and push it to a registry.
#
# Usage:
#   ./build_and_push_docker.sh <image-name> [tag]
#
# Examples:
#   ./build_and_push_docker.sh myuser/codecureagent
#   ./build_and_push_docker.sh myuser/codecureagent v1.0
#   ./build_and_push_docker.sh ghcr.io/myorg/codecureagent latest
#
# The script must be run from the repository root.

set -euo pipefail

IMAGE_NAME="${1:-}"
TAG="${2:-latest}"

if [[ -z "$IMAGE_NAME" ]]; then
    echo "Usage: $0 <image-name> [tag]"
    echo "  image-name  e.g. myuser/codecureagent or ghcr.io/myorg/codecureagent"
    echo "  tag         defaults to 'latest'"
    exit 1
fi

FULL_IMAGE="${IMAGE_NAME}:${TAG}"

echo "==> Building Docker image: ${FULL_IMAGE}"
docker build --platform linux/amd64 -t "${FULL_IMAGE}" .

echo "==> Pushing Docker image: ${FULL_IMAGE}"
docker push "${FULL_IMAGE}"

echo "==> Done. Image available as: ${FULL_IMAGE}"
