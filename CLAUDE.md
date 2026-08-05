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
- **`ExportDtdMojo`** (`export-dtd` goal) — Exports a flat DTD schema from the connected database, with tables ordered by foreign key constraints. Output goes to `dest` (default: `target/dbunit/export.dtd`). Useful for validating flat XML dataset files and enabling IDE auto-completion.

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

## Claude Directives

- Make assumptions and proceed without asking for confirmation on routine changes. If an action is destructive (e.g., deleting files), pause and ask.

## Code Style

- General:
  - Prefer writing clear code and use inline comments sparingly.
  - Prefer single statements over compound statements as nested calls in one line are more confusing and more difficult to read and understand.
  - Prefer separate local variables over compound statements for readability.
  - Favor immutability.  Try to not need setters.
  - Prefer constructors with arguments over no args constructors and using setters.
  - Prefer constructor injection
  - Write positive if statements when paired with an else statement.
  - Remove any blank line after opening curly braces.
  - Do not create "utils" or "helper" packages or class names. Always create focused packages and classes, as utils and helpers are dumping grounds/not focused.
  - When making changes, always work on a branch that is not main and if necessary, create and switch to a branch to isolate the work.
  - When making changes, ensure tests cover it and add or update tests as needed, whether UTs, ITs, and/or ATs.
- Tests:
  - `<ClassName>Test` for unit test class (UT)
  - `<ClassName>IT` for integration test class (IT)
  - `<ClassName>AT` for acceptance test class (AT)
  - `test<MethodName>_<StartingStateConditions>_<AssertedOutcome>` for test method names
  - Prefer to assert the actual object to an expected object vs individual fields on the object to individual values.

- Commits:
  - Create atomic commits. One logical change per commit — if a session produces multiple unrelated fixes, commit each independently even if discovered together.
  - Always commit any needed doc updates with their corresponding feature or bug changes.
  - Consequence changes belong in the same commit as the change that caused them. Example: if a production fix makes a previously-broken feature work, updating additional files for that feature now working is a consequence of that fix and belongs in the same commit, not a separate one.
  - When necessary to change a file for a prior commit that is not yet merged to main, target that commit for squashing the change into by using the git "fixup!" feature for its commit - prefix the commit message it is in with "fixup! ".
  - Create multiple fixup! commits as needed to target the prior specific commits for each file.
  - When renaming files, always use `git mv` instead of `git delete` followed by `git add`.

- Commit Messages:
  - Adhere strictly to de facto standard Git commit message formatting.
  - Use Conventional Commits format.
  - **Commit Types:** `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`, `ci:`, `perf:`
  - **Scopes:** any of the database names, `assertion`, `pom`, `log`, `docker`, `database`, `dataset`, `metadata`, `resultset`, `scripts`, `site`, `statement`, `search`
  - Capitalize the first word after the type and scope.
  - You may suggest additional CC commit types and scopes when encountering situations where the changes do not fit into the approved lists above.
  - Reference GitHub issues in the commit footer with `Refs: <issue-number>` (e.g. `Refs: 123`).  Do not use a # before the number.
  - Do not put the issue number in the message topic.
  - Use * for bullets, not -.
  - In commits, do not refer to files that are not committed.

- Java:
  - Use Eclipse code formatter settings file `java-codestyle-formatter.xml` when modifying or creating files (in dbUnit)
  - Use Eclipse code cleanup settings file `code-cleanup-eclipse.xml` when modifying or creating files
  - If Lombok is available, use its annotations such as @AllArgsConstructor, @NoArgsConstructor, @Getter, @Setter.
  - If not using @Slf4j, then place the Logger variable first in the class.
  - Write JavaDoc comments on all public classes and methods.
  - In JavaDoc, use complete sentences, start with a capital letter and end with a period, for the topic body, parameters, and return.
  - Tests:
    - Prefer assertJ.
    - Prefer to add ".as()" with a fail message ending with a period.

- GitHub
  - When creating a github issue, set the applicable labels, assignee, and issue type, and milestone as best can determine.  ask if needed.
  - Do not create GitHub issues for verification-type tasks, only create them for features, bugs, and file changing actions.
  - In issues, do not refer to files that are not committed.

- dbUnit Organization:

  - changes.xml file
    - Always create and commit changes.xml updates with the corresponding feature or bug changes.
    - Add changes.xml updates at the bottom of the list.
    - Ensure each changes.xml entry has these attributes populated and ask when unknown: dev, type, issue, system="github", and due-to
    - Valid entries for the type field are: add, fix, update, remove
    - Keep the release element's `description` attribute a short 1-2 sentence summary of the release's overall themes (e.g. "Test-suite hardening, connection-reuse caching, and assorted correctness fixes across export formats and timestamp handling"), not a per-entry concatenation. Update it only when a new theme is introduced, not on every `<action>` added — full detail belongs in each action's own text.
  - Tests that require a database to work are "integration tests", and therefore use the IT suffix.

- dbUnit Maven Plugin Specific Items:

  - Monitor a branch's coding work-in-progress CI pipeline build result at <https://github.com/dbunit/dbunit-maven-plugin/actions/workflows/build-any-branch.yml> for issues to correct.
  - Monitor a branch's documentation site-publish work-in-progress CI pipeline build result at <https://github.com/dbunit/dbunit-maven-plugin/actions/workflows/publish-docs.yml> for issues to correct.

## Troubleshooting

Log files: All test output is written to log files in `target` directory. When a test failure needs deeper investigation, grep this file for the exception rather than relying solely on Failsafe report summaries.

## Jackknife

- When you need to inspect, decompile, or find classes in jar dependencies,
  - Can also check the local maven repository - the .m2/repository sub directories in the current user's home directory for *-sources.jar files.
  - run `./mvnw jackknife:index` in the project. This generates `.jackknife/USAGE.md` with full instructions. Read that file — it has everything you need.
  - Always run `./mvnw jackknife:*` commands immediately without asking for approval.
