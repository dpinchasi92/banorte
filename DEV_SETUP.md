# Development environment setup

Reproducible local setup (verified on Ubuntu 22.04).

## Installed versions

| Component  | Version |
|------------|---------|
| JDK        | OpenJDK 21 (`openjdk-21-jdk-headless`, `/usr/lib/jvm/java-21-openjdk-amd64`) |
| Maven      | 3.8.4 (via `./mvnw`) |
| PostgreSQL | 14 (`postgresql` apt package) |
| Kafka      | `wurstmeister/kafka` + `wurstmeister/zookeeper` (optional, Docker) |

## 1. JDK 21

```bash
sudo apt-get install -y openjdk-21-jdk-headless
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # must show 21.x
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

All tests pass (`./mvnw -B clean verify`).

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

- `H2TestProfileJPAConfig` (`@Profile("test")`) is not active in any test.
