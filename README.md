# homeTask

An event-driven CRUD service for a simple `Message` resource (`id: int`, `msg: string`), split across two Spring Boot apps that communicate over Kafka:

- **`server`** — the REST API. Publishes each request as a Kafka event and blocks (on a virtual thread) until `producer` confirms the outcome, so the HTTP response reflects the real result of the database operation. Also caches reads in Redis.
- **`producer`** — a Kafka consumer. Listens for create/update/delete/read events, applies them to Postgres, and publishes the result back on a reply topic.
- **`docker`** — Kafka, Postgres, Redis, and the two apps above, wired together with Docker Compose, plus a script to build and launch everything.

```
Client → server (REST, :3000) → Kafka (command topics) → producer → Postgres
                ↑                                              │
                └──────────────── Kafka (reply topic) ─────────┘
server also checks/populates a Redis cache before hitting Kafka on reads.
```

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running)
- Python 3
- JDK 21 (for `server`) and JDK 17 (for `producer`) on your `PATH`, for the local build step

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

## REST API

Base URL: `http://localhost:3000/hometask/api/v1`

| Method | Path             | Description                                  |
|--------|------------------|-----------------------------------------------|
| POST   | `/messages`      | Create a message. Body: `{"id": <int>, "msg": "<string>"}` |
| GET    | `/messages/{id}` | Read a message by id                          |
| PUT    | `/messages/{id}` | Update a message by id. Body: `{"msg": "<string>"}` |
| DELETE | `/messages/{id}` | Delete a message by id                        |

### Examples

```bash
curl -X POST http://localhost:3000/hometask/api/v1/messages \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "msg": "hello"}'
# 201 Created -> {"id":1,"msg":"hello"}

curl http://localhost:3000/hometask/api/v1/messages/1
# 200 OK -> {"id":1,"msg":"hello"}

curl -X PUT http://localhost:3000/hometask/api/v1/messages/1 \
  -H "Content-Type: application/json" \
  -d '{"msg": "updated"}'
# 200 OK -> {"id":1,"msg":"updated"}

curl -X DELETE http://localhost:3000/hometask/api/v1/messages/1
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
