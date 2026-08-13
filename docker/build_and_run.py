#!/usr/bin/env python3
"""Builds the server and producer projects, then brings up the full docker
stack (Kafka, Postgres, Redis, server, producer) and prints the REST API list.

Usage: python build_and_run.py
"""

import platform
import subprocess
import sys
from pathlib import Path

DOCKER_DIR = Path(__file__).resolve().parent
ROOT_DIR = DOCKER_DIR.parent
SERVER_DIR = ROOT_DIR / "server"
PRODUCER_DIR = ROOT_DIR / "producer"

API_BASE_URL = "http://localhost:3000/hometask/api/v1"

API_ENDPOINTS = [
    ("POST", "/messages", 'Create a message. Body: {"id": <int>, "msg": "<string>"}'),
    ("GET", "/messages/{id}", "Read a message by id"),
    ("PUT", "/messages/{id}", 'Update a message by id. Body: {"msg": "<string>"}'),
    ("DELETE", "/messages/{id}", "Delete a message by id"),
]


def run(command, cwd):
    print(f"\n$ {' '.join(command)}  (in {cwd})")
    result = subprocess.run(command, cwd=str(cwd))
    if result.returncode != 0:
        print(f"\nCommand failed with exit code {result.returncode}: {' '.join(command)}")
        sys.exit(result.returncode)


def gradlew(project_dir: Path) -> str:
    wrapper = "gradlew.bat" if platform.system() == "Windows" else "./gradlew"
    return str(project_dir / wrapper)


def build_project(name: str, project_dir: Path):
    print(f"\n=== Building {name} ===")
    run([gradlew(project_dir), "clean", "bootJar", "--no-daemon"], cwd=project_dir)


def start_docker_stack():
    print("\n=== Building docker images for server and producer ===")
    run(["docker", "compose", "build"], cwd=DOCKER_DIR)
    print("\n=== Starting the full stack (kafka, postgres, redis, server, producer) ===")
    run(["docker", "compose", "up", "-d"], cwd=DOCKER_DIR)


def print_api_list():
    print("\n" + "=" * 60)
    print("REST API")
    print("=" * 60)
    print(f"Base URL: {API_BASE_URL}\n")
    for method, path, description in API_ENDPOINTS:
        print(f"  {method:<6} {API_BASE_URL}{path}")
        print(f"         {description}\n")


def main():
    build_project("server", SERVER_DIR)
    build_project("producer", PRODUCER_DIR)
    start_docker_stack()

    print("\nAll components are up: kafka, postgres, redis, server, producer.")
    print("Check status with: docker compose ps")
    print_api_list()


if __name__ == "__main__":
    main()
