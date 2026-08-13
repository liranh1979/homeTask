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


def discover_server_port() -> str:
    # server publishes no fixed host port (so it can be scaled with --scale server=N),
    # so the actual port has to be looked up after the container starts.
    result = subprocess.run(
        ["docker", "compose", "port", "server", "3000"],
        cwd=str(DOCKER_DIR), capture_output=True, text=True,
    )
    if result.returncode == 0 and ":" in result.stdout:
        return result.stdout.strip().rsplit(":", 1)[1]
    print("\nCould not auto-detect server's port; falling back to 3000.")
    print("Run 'docker compose port server 3000' to find it yourself.")
    return "3000"


def print_api_list(port: str):
    base_url = f"http://localhost:{port}/hometask/api/v1"
    print("\n" + "=" * 60)
    print("REST API")
    print("=" * 60)
    print(f"Base URL: {base_url}\n")
    for method, path, description in API_ENDPOINTS:
        print(f"  {method:<6} {base_url}{path}")
        print(f"         {description}\n")


def main():
    build_project("server", SERVER_DIR)
    build_project("producer", PRODUCER_DIR)
    start_docker_stack()

    print("\nAll components are up: kafka, postgres, redis, server, producer.")
    print("Check status with: docker compose ps")
    port = discover_server_port()
    print_api_list(port)
    print("Running multiple server instances? Each has its own port:")
    print("  docker ps --filter \"name=docker-server\"")


if __name__ == "__main__":
    main()
