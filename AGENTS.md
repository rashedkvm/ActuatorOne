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

## Commit & Pull Request Guidelines

The available history contains one descriptive, imperative commit: `Provide Spring Actuator Sample Application`. Follow that concise style; no formal commit convention is established. Pull requests should describe the change, include validation commands and results, and link relevant issues. Explain configuration or deployment effects when modifying properties, manifests, or Dockerfiles.

## Security & Configuration Tips

Keep credentials out of source files and deployment manifests. Review Actuator endpoint exposure before enabling additional endpoints; the example management settings in `application.properties` are commented out.
