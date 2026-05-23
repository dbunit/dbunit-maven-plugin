# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **dbUnit Maven Plugin** (`org.dbunit:dbunit-maven-plugin`), a Maven plugin that wraps the [dbUnit](http://dbunit.sourceforge.net/) database testing framework to make its operations available as Maven goals. It delegates actual database work to `dbunit-core` via its Ant task API.

## Build Commands

The project uses a Maven wrapper. Always use `./mvnw` instead of `mvn`.

```bash
# Build and install (default goal)
./mvnw clean install

# Compile and run unit tests only (skips integration tests)
./mvnw clean test

# Run a single test class
./mvnw test -Dtest=OperationMojoTest

# Run a single test method
./mvnw test -Dtest=OperationMojoTest#testCleanInsertOperation

# Run integration tests (uses maven-invoker-plugin against src/example/)
./mvnw clean verify

# Build with release profile (adds sources, javadoc, GPG signing)
./mvnw clean install -Prelease
```

## Architecture

### Plugin Goals

All Mojos live in `src/main/java/org/dbunit/mojo/`:

- **`AbstractDbUnitMojo`** — Base class for all goals. Handles JDBC connection setup (`driver`, `url`, `username`, `password`, `schema`), credentials lookup from `settings.xml`, and the `dbconfig` properties mechanism for configuring `DatabaseConfig`. Many individual fields (`dataTypeFactoryName`, `supportBatchStatement`, etc.) are deprecated in favor of the generic `<dbconfig>` element.
- **`OperationMojo`** (`operation` goal) — Executes a dbUnit database operation (INSERT, CLEAN_INSERT, DELETE, DELETE_ALL, UPDATE, REFRESH, and MSSQL variants). Supports multiple source files, composite datasets (merge files into one dataset), and the `combine` flag to merge rows from same-named tables across files.
- **`ExportMojo`** (`export` goal) — Exports database contents to an XML (or other format) dataset file. Supports table/query filtering and ordered export.
- **`CompareMojo`** (`compare` goal) — Compares current database contents against a reference dataset file, optionally filtering by tables/queries. The `sort` option requires `resultSetTableFactory=org.dbunit.database.CachedResultSetTableFactory` in `<dbconfig>`; the default `ForwardOnlyResultSetTableFactory` throws `UnsupportedOperationException` when sort tries to call `getRowCount()`.

### Key Design Pattern

Each Mojo delegates to dbUnit's Ant task API (`org.dbunit.ant.*`): `Operation`, `Export`, `Compare`. The Mojo configures the Ant task object and calls `task.execute(connection)`. This means dbUnit's Ant task classes are the actual implementation layer.

### Testing

Unit tests instantiate Mojos directly and populate them via `AbstractDbUnitMojoTest.populateMojoCommonConfiguration()`. They use an in-memory HSQLDB database, initialized fresh in `@BeforeEach`. Test configuration (driver, URL, credentials) comes from `src/test/resources/test.properties`.

Integration tests live in `src/example/` and are run by `maven-invoker-plugin` during the `verify` phase. The invoker clones the example project into `target/it/` and runs it against a local repository at `target/it-repo/`.

### dbconfig Properties

The preferred way to configure `DatabaseConfig` properties is via the `<dbconfig>` element, which maps directly to `DatabaseConfig.setPropertiesByString()`. The many individual deprecated fields (`dataTypeFactoryName`, `escapePattern`, `useQualifiedTableNames`, etc.) exist only for backwards compatibility.

### CLI Property Prefix

All Mojo parameters are bound to CLI properties with the `dbunit.` prefix to avoid conflicts with Maven built-in properties:

```
-Ddbunit.driver=org.hsqldb.jdbcDriver
-Ddbunit.url=jdbc:hsqldb:mem:mydb
-Ddbunit.username=sa
-Ddbunit.password=
-Ddbunit.type=CLEAN_INSERT
```

POM-based `<configuration>` still uses the bare field names unchanged.

## Code Style

- General:
  - Prefer writing clear code and use inline comments sparingly.
  - Prefer separate local variables over compound statements for readability.

- Commits:
  - Adhere strictly to de facto standard Git commit message formatting.
  - Use Conventional Commits format.
  - **Commit Types:** `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, `ci:`
  - **Scopes:** `pom`, `scripts`, `log`, `docker`, `database`, `site`
  - Capitalize the first word after the type and scope.
  - You may suggest additional CC commit types and scopes when encountering situations where the changes do not fit into the approved lists above.
  - Reference GitHub issues in the commit footer with `Refs: <issue-number>` (e.g. `Refs: 123`).  Do not use a # before the number.

- Java:
  - Use Eclipse code formatter settings file `java-codestyle-formatter.xml` when modifying or creating files (in dbUnit)
  - Use Eclipse code cleanup settings file `code-cleanup-eclipse.xml` when modifying or creating files
  - Remove any blank line after opening curly braces.
  - Favor immutability.  Try to not need setters.
  - Prefer single statements over compound statements as nested calls in one line are more confusing and more difficult to read and understand.
  - Prefer constructors with arguments over no args constructors and using setters.
  - Write positive if statements when paired with an else statement.
  - Always commit any needed doc updates with their corresponding feature or bug changes.
  - If Lombok is available, use its annotations such as @AllArgsConstructor, @NoArgsConstructor, @Getter, @Setter.
  - If not using @Slf4j, then place the Logger variable first in the class.
  - Use constructor injection
  - Write JavaDoc comments on all public classes and methods
  - Tests:
    - `<ClassName>Test` for unit test class
    - `<ClassName>IT` for integration test class
    - `<ClassName>AT` for acceptance test class
    - `test<MethodName>_<StartingStateConditions>_<AssertedOutcome>` for test method names
    - Prefer assertJ.
    - Prefer to add ".as()" with a fail message ending with a period.
    - Prefer to assert the actual object to an expected object vs individual fields on the object to individual values.

- Java Project Specific

  - Always create and commit changes.xml updates with the corresponding feature or bug changes.

## Troubleshooting

Log files: All test output is written to log files in `target` directory. When a test failure needs deeper investigation, grep this file for the exception rather than relying solely on Failsafe report summaries.

## Jackknife

- When you need to inspect, decompile, or find classes in jar dependencies,
  run `./mvnw jackknife:index` in the project. This generates `.jackknife/USAGE.md`
  with full instructions. Read that file — it has everything you need.
- Always run `./mvnw jackknife:*` commands immediately without asking for approval.
