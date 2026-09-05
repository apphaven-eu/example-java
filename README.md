# Spring Boot with PostgreSQL on AppHaven

Deploy a Java Spring Boot application with managed PostgreSQL on [AppHaven](https://apphaven.eu). This example uses Spring JDBC and `JdbcClient` for a server-rendered todo list, with a Docker build and JDBC connection settings supplied by the platform.

Add, list, and delete tasks. Data persists across app restarts in PostgreSQL.
The repository includes the application, a `Dockerfile`, and an `apphaven.yaml` manifest.

## Stack

- Java 21, Spring Boot 3.5.16 (spring-boot-starter-web, spring-boot-starter-jdbc)
- PostgreSQL driver `org.postgresql:postgresql`, queries through `JdbcClient`
- PostgreSQL 17
- Build with Maven 3.9 in `maven:3.9-eclipse-temurin-21`, runtime image `eclipse-temurin:21-jre-alpine`

The schema lives in `src/main/resources/schema.sql` and is applied at startup by Spring's SQL
initialisation (`spring.sql.init.mode=always`). The statement is `CREATE TABLE IF NOT EXISTS`, so
repeated starts are safe.

Unlike the other examples in this series, this one does not read `DATABASE_URL`. JDBC cannot parse
the `postgres://user:password@host/db` URL that `${service.db.url}` provides, so `apphaven.yaml`
builds a `jdbc:postgresql://` URL from the individual `${service.db.host|port|database}` values and
passes the user and password separately.

## Run it locally

You need Docker for PostgreSQL and JDK 21 and Maven 3.9 or newer. Clone this repository first:

```sh
git clone https://github.com/apphaven-eu/example-java.git
cd example-java
```

You need JDK 21 and Maven 3.9 or newer. There is no Maven wrapper in this repository, use your own
`mvn`.

1. Start PostgreSQL for development:

   ```
   docker run -d --name todo-postgres -p 127.0.0.1:5432:5432 \
     -e POSTGRES_USER=todo -e POSTGRES_PASSWORD=devpass -e POSTGRES_DB=todo postgres:17
   ```

2. Point the application at it:

   ```
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/todo
   export SPRING_DATASOURCE_USERNAME=todo
   export SPRING_DATASOURCE_PASSWORD=devpass
   ```

3. Run it:

   ```
   mvn spring-boot:run
   ```

   To run the packaged jar instead: `mvn package` and then `java -jar target/app.jar`.

4. Open http://localhost:8080. The health endpoint is at http://localhost:8080/healthz.

Set `SERVER_PORT` to listen on a different port.

## Deploy Spring Boot on AppHaven

1. Fork this repository, or push a copy to a Git host reachable over HTTPS.
2. Open the [AppHaven console](https://console.apphaven.eu/), select a project, and create an app.
3. In **Source**, connect your repository and select the production branch (usually `main`).
4. Click **Deploy** and select that branch. Follow the build logs, then open the deployment URL.

You need an AppHaven account with console access. See the
[getting started guide](https://docs.apphaven.eu/getting-started) for account and repository setup.

`apphaven.yaml` declares the `web` service built from the `Dockerfile` and the managed `db` service
running PostgreSQL 17. The database connection settings are injected at deploy time from
`${service.db.host}`, `${service.db.port}`, `${service.db.database}`, `${service.db.user}` and
`${service.db.password}`, so production database credentials stay out of source control.

### Access and shared data

Apps are **private by default**: only members of the AppHaven project can open them.
This example has one shared todo list; it does not separate tasks by user.
For a public demo, a project administrator can select **Public** in the app's **Security**
section and redeploy. Anyone who can reach the app can add and delete tasks, so use demo data.
Production can be public while previews remain private. See [access control](https://docs.apphaven.eu/access).

### Preview a change

Push a new branch and deploy it from the console. AppHaven creates a preview with its own URL,
storage, and database, separate from production. Add a task in the preview, redeploy that branch,
and check that the task is still there before merging the change.

### Verify the deployment

Open the app, add a task, refresh, and delete it. The manifest waits for PostgreSQL to be healthy
before starting the web container. `/healthz` is a process liveness endpoint; it does not query
the database. The container's healthcheck runs internally, so it needs no public-path exemption.

The schema uses `CREATE TABLE IF NOT EXISTS` for the initial table. When extending the app,
use versioned migrations for changes to existing columns and tables.

## AppHaven

AppHaven builds this repository into an image and runs it, and provides the PostgreSQL database
declared as a `type: postgres` service in `apphaven.yaml`, including its storage and backups.

- Platform: https://apphaven.eu
- Managed PostgreSQL: https://docs.apphaven.eu/services/postgres
- Manifest reference: https://docs.apphaven.eu/reference/manifest

## Related examples

[Go](https://github.com/apphaven-eu/example-go), [Next.js](https://github.com/apphaven-eu/example-nextjs), [Express](https://github.com/apphaven-eu/example-node), [PHP](https://github.com/apphaven-eu/example-php), [FastAPI](https://github.com/apphaven-eu/example-python).

## License

[MIT](LICENSE).
