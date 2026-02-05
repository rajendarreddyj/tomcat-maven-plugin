# Contributing Guide

Thanks for your interest in contributing!

## Prerequisites
- JDK 21+
- Maven 3.9.6+
- Git

## Build & Test
```bash
mvn clean install        # compile + unit tests
mvn verify               # unit + integration tests (maven-invoker)
```
Test coverage is enforced by JaCoCo (bundle thresholds: LINE ≥ 90%, BRANCH ≥ 85%).

## Project Structure
- Maven plugin sources in `src/main/java`
- Unit tests in `src/test/java`
- Integration tests in `src/it`
- Plugin descriptor in `src/main/resources/META-INF/maven/plugin.xml`

## Coding Standards
- Follow project Java guidelines in `.github/instructions/java.instructions.md`
- Use meaningful names, avoid magic numbers, prefer immutability where practical
- Do not introduce deprecated or insecure APIs
- Keep changes minimal and focused; avoid unrelated refactors

## Developing
1. Create a feature branch from `main`
2. Implement changes with tests
3. Run `mvn verify` and ensure checks pass
4. Update docs if behavior or configuration changes (e.g., `README.md`, `docs/`)
5. Open a Pull Request with:
   - Problem statement and rationale
   - Summary of changes
   - Testing notes (how verified)

## Release Profile
The `release` profile signs artifacts and prepares sources/javadocs for Central. Do not enable it in PR CI.

```bash
mvn -P release verify    # for maintainers when preparing a release
```

## Communication
- Questions and feature requests: open a GitHub issue
- Bugs: open an issue with steps to reproduce, expected vs actual behavior, logs, and environment details
- Security concerns: do NOT open an issue; see `SECURITY.md`
