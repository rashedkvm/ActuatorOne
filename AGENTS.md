# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Spring Boot 3.3.4 sample for exploring Actuator settings.
- `src/main/java/io/spring/actone/`: application entry point (`ActoneApplication`) and REST controller (`HelloWorldController`).
- `src/main/resources/application.properties`: application and optional Actuator configuration.
- `src/test/java/io/spring/actone/`: Spring Boot tests, mirroring the source package.
- `pom.xml` and `.mvn/wrapper/`: Maven build configuration and wrapper settings.
- Root-level YAML manifests: Cloud Foundry deployment variants. Dockerfiles exist at the root and in `src/main/`.
- `target/`: generated build output; keep it out of commits.

## Build, Test, and Development Commands

Use Java 17 or above and the Maven wrapper (`mvnw.cmd` on Windows).
- `./mvnw clean package`: compile, run tests, and package the executable JAR.
- `./mvnw test`: run the test suite.
- `./mvnw spring-boot:run`: start locally on port 8080 by default.
- `java -jar target/actone-0.0.3-SNAPSHOT.jar`: run the packaged application; match the version in `pom.xml`.
- `./mvnw spring-boot:build-image`: build a container image; requires Docker.

Check `/hello` for the greeting and `/actuator/health` for application health.

## Coding Style & Naming Conventions

Use package `io.spring.actone`, PascalCase class names, and camelCase methods and fields. Existing Java files mix tabs and four-space indentation; preserve the surrounding file's style. Keep Spring annotations adjacent to their classes or methods. No formatter or lint plugin is configured in `pom.xml`.

## Testing Guidelines

Tests use JUnit Jupiter through `spring-boot-starter-test`. Follow the existing `*Tests.java` naming convention. Use `@SpringBootTest` for application-context checks and add endpoint assertions when changing controller behavior. Run `./mvnw test` before submitting changes. No coverage threshold is configured.

## Platform Environment Variables (`ACTONE_ENV_ENABLED`)

`EnvController` serves `/api/env` and the home page's Environment section. It is **off by default**.
- Enable it by setting `ACTONE_ENV_ENABLED` to exactly lowercase `true` (`manifest.yml` ships `'false'`). Any other value, including `TRUE`, `1` or unset, disables it; `/api/env` then returns 404 and is not listed in `/api/routes`.
- Set it in the manifest `env:` block, or use `cf set-env actuator-one ACTONE_ENV_ENABLED true`. The value is read at startup, so restage or restart afterward; during a rolling restart old and new instances can briefly disagree.
- Output is an allowlist: `VCAP_*`, `CF_*`, `PORT`, `MEMORY_LIMIT`, `INSTANCE_INDEX`, `INSTANCE_GUID`, `LANG`, `TMPDIR`, `USER`, `HOME`. `DATABASE_URL` is excluded.
- Only `credentials` members of `VCAP_SERVICES` bindings and `CF_INSTANCE_KEY` are masked as `****`; unparseable `VCAP_SERVICES` is masked entirely. Other values (for example `VCAP_APPLICATION`, service metadata) are returned raw, and `/api/runtime` is unchanged.
- The app has no authentication and the manifests request routes (`random-route: true`). Enable this only on a restricted route, with no bound services holding real secrets.

## Commit & Pull Request Guidelines

The available history contains one descriptive, imperative commit: `Provide Spring Actuator Sample Application`. Follow that concise style; no formal commit convention is established. Pull requests should describe the change, include validation commands and results, and link relevant issues. Explain configuration or deployment effects when modifying properties, manifests, or Dockerfiles.

## Security & Configuration Tips

Keep credentials out of source files and deployment manifests. Review Actuator endpoint exposure before enabling additional endpoints; the example management settings in `application.properties` are commented out.
