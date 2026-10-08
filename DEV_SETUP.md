# Development environment setup

Reproducible local setup (verified on Ubuntu 22.04).

## Installed versions

| Component  | Version |
|------------|---------|
| JDK        | OpenJDK 11.0.32 (`openjdk-11-jdk`, `/usr/lib/jvm/java-11-openjdk-amd64`) |
| Maven      | 3.8.4 (via `./mvnw`) |
| PostgreSQL | 14 (`postgresql` apt package) |
| Kafka      | `wurstmeister/kafka` + `wurstmeister/zookeeper` (optional, Docker) |

## 1. JDK 11

```bash
sudo apt-get install -y openjdk-11-jdk
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # must show 11.x
```

If `repo.maven.apache.org` returns HTTP 429 (IP rate limit), point Maven at the
Google mirror of Maven Central:

```bash
export MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2
```

and add a `<mirror>` for `central` with the same URL to `~/.m2/settings.xml`.

## 2. Tests (H2 only, no Postgres needed)

`src/test/resources/application.properties` overrides the main config with an
in-memory H2 datasource, so `./mvnw test` does not connect to PostgreSQL.

```bash
./mvnw test
```

Current result: 140 tests, 46 failures, 26 errors. These are pre-existing failures in
the test code (Mockito stubbing/NPEs in service tests, integration tests that
expect pre-seeded rows with fixed IDs). The original code run against a live
Postgres gives exactly the same per-class counts.

## 3. PostgreSQL

```bash
sudo apt-get install -y postgresql
sudo pg_ctlcluster 14 main start
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'barorkar99';"
sudo -u postgres psql -c 'CREATE DATABASE "online-banking-rest-api";'
```

The credentials match `src/main/resources/application.properties`. The schema is
created by Hibernate (`ddl-auto=create`) when the app starts.

## 4. Kafka (optional)

Without a broker on `localhost:9092` the app still starts; it only logs connection
warnings. To run one, save the `docker-compose.yml` from README.md and run
`docker compose up -d`.

## 5. Run

```bash
./mvnw -DskipTests package
java -jar target/onlinebankingrestapi-0.0.1-SNAPSHOT.jar   # or ./mvnw spring-boot:run
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui/index.html   # 200
```

## Known issues

- `Dockerfile` is broken: the base image `adoptenjdk/openjdk11:ubi` is misspelled
  and deprecated (it should be something like `eclipse-temurin:11-jre`), and it copies
  `build/libs/*.jar`, which is a Gradle path (Maven writes to `target/`).
- `H2TestProfileJPAConfig` (`@Profile("test")`) is not active in any test.
