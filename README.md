# homeTask

An event-driven CRUD service for a simple `Message` resource (`id: int`, `msg: string`), split across two Spring Boot apps that communicate over Kafka:

- **`server`** — the REST API. Publishes each request as a Kafka event and blocks (on a virtual thread) until `producer` confirms the outcome, so the HTTP response reflects the real result of the database operation. Also caches reads in Redis.
- **`producer`** — a Kafka consumer. Listens for create/update/delete/read events, applies them to Postgres, and publishes the result back on a reply topic.
- **`docker`** — Kafka, Postgres, Redis, and the two apps above, wired together with Docker Compose, plus a script to build and launch everything.

```
Client → server (REST) → Kafka (command topics) → producer → Postgres
                ↑                                        │
                └──────────────── Kafka (reply topic) ───┘
server also checks/populates a Redis cache before hitting Kafka on reads.
```

`server` and `producer` are both safe to run as multiple instances: `producer` is a stateless Kafka consumer-group member (partitions spread across instances automatically), and `server` gives each instance its own reply consumer group so replies always reach the instance that made the request.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running)
- Python 3
- JDK 21 on your `PATH`, for the local build step (both `server` and `producer` target Java 21)

## Quick start

```powershell
cd docker
py .\build_and_run.py
```

This will:
1. Build `server` and `producer` (`gradlew clean bootJar`).
2. Build the Docker images and start all 5 containers (Kafka, Postgres, Redis, `server`, `producer`).
3. Print the API list below.

> If you have `server`/`producer` already running locally (e.g. from IntelliJ), stop them first — they'd otherwise conflict with the Docker containers on the same ports.

### Useful commands

```powershell
docker compose ps            # check container status
docker compose logs -f server   # tail a service's logs
docker compose down          # stop and remove containers (images/data persist)
docker compose up -d         # bring the stack back up without rebuilding
```

### Scaling

`server` doesn't publish a fixed host port (so multiple instances can run side by side), so find the assigned port first:

```powershell
docker compose up -d --scale server=3    # run 3 instances of server
docker compose port server 3000          # -> e.g. 0.0.0.0:55789 (one instance's port)
docker ps --filter "name=docker-server"  # list every instance and its own port
```

Each instance is independently reachable on its own port — there's no load balancer in front by default, so pick one instance's port for a given request, or add a reverse proxy (nginx/Traefik) in front if you want a single stable entrypoint across replicas.

`producer` can be scaled the same way (`--scale producer=N`); it has no exposed port to worry about.

## REST API

Base URL: `http://localhost:<port>/hometask/api/v1`, where `<port>` is the host port Docker assigned to your `server` instance (see [Scaling](#scaling) above — with a single default instance, run `docker compose port server 3000` to find it).

| Method | Path             | Description                                  |
|--------|------------------|-----------------------------------------------|
| POST   | `/messages`      | Create a message. Body: `{"id": <int>, "msg": "<string>"}` |
| GET    | `/messages/{id}` | Read a message by id                          |
| PUT    | `/messages/{id}` | Update a message by id. Body: `{"msg": "<string>"}` |
| DELETE | `/messages/{id}` | Delete a message by id                        |

### Examples

```bash
# find the port once, then reuse it:
PORT=$(docker compose port server 3000 | cut -d: -f2)

curl -X POST http://localhost:$PORT/hometask/api/v1/messages \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "msg": "hello"}'
# 201 Created -> {"id":1,"msg":"hello"}

curl http://localhost:$PORT/hometask/api/v1/messages/1
# 200 OK -> {"id":1,"msg":"hello"}

curl -X PUT http://localhost:$PORT/hometask/api/v1/messages/1 \
  -H "Content-Type: application/json" \
  -d '{"msg": "updated"}'
# 200 OK -> {"id":1,"msg":"updated"}

curl -X DELETE http://localhost:$PORT/hometask/api/v1/messages/1
# 204 No Content
```

### Error responses

| Status | When |
|--------|------|
| 404 Not Found | update/delete/read on an id that doesn't exist |
| 409 Conflict | create with an id that already exists |
| 504 Gateway Timeout | no confirmation arrived from `producer` in time |
| 500 Internal Server Error | an unexpected failure while processing the event |

## Running the tests

```powershell
cd server
.\gradlew test

cd ..\producer
.\gradlew test
```

Both suites include unit tests (Mockito) and a couple of tests that spin up a real Spring context (for the `@Cacheable`/`@Retryable` proxy behavior), plus a full-context smoke test that needs the Docker stack (Kafka/Postgres/Redis) running.
