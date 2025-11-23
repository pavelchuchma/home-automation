# Project Guidelines — HomeAutomation / home-controller

These guidelines tell Junie how to work with this repository in future sessions: project overview, how to build and test, and coding conventions. If anything here becomes outdated, prefer what’s in the code (Gradle files) and CI over this document.

## Project overview
This is a Gradle multi-project Java codebase with several modules:

- base — foundational utilities and shared types used by other modules.
- controller — core logic for device control/integration and runtime behavior.
- app — application/UI layer. Contains servlet/static resources under `app/src/main/resources/servlet/content` (HTML/JS/CSS/images such as floor plans).
- extensions — optional integrations with external services/devices (e.g., Solax inverter) and related tests.
- buildSrc — Gradle build logic.

Other notable directories:
- cfg, doc, script — configuration, documentation, helper scripts.
- out, */out — compiled outputs; treat as generated. Do not edit or place source files here.

Standard source layout per module:
- `src/main/java` — production Java code
- `src/main/resources` — resources
- `src/test/java` — unit/integration tests
- `src/test/resources` — test resources

## What Junie should do before submitting changes
- Documentation-only changes (Markdown, comments, static resources without code changes): no build required.
- Any Java/Kotlin code change: build the project and run relevant tests for the impacted module(s) before submitting.
- If you touch the web resources under `app/src/main/resources/servlet/content` (JS/HTML/CSS) only, do not run Java builds unless Java code is affected.

## How to build and run tests in this environment
In this JetBrains/Junie environment, prefer the provided tools over shell commands.

- Build the entire project:
  - Use the Build tool: `functions.build()` (exposed as the “Build” action here).
- Run tests:
  - Use the Test tool: `functions.run_test` with a full path or fully qualified class name (FQN).
  - Examples:
    - Run all tests in a module:
      - `run_test extensions/src/test/java`
      - `run_test app/src/test/java`
    - Run a specific test class by FQN:
      - `run_test org.chuma.homecontroller.extensions.external.inverter.impl.SolarPanelCalculatorTest`
    - Run a specific test file by path:
      - `run_test extensions/src/test/java/org/chuma/homecontroller/extensions/external/inverter/impl/SolarPanelCalculatorTest.java`

If using Gradle locally, the equivalents are:
- `./gradlew build` (full build)
- `./gradlew test` (all tests)
- `./gradlew :extensions:test --tests "org.chuma.*SolarPanelCalculatorTest"` (pattern)

## Coding conventions
- Follow the existing style of the module you’re editing. Mirror import order, spacing, and brace style.
- Java:
  - Use descriptive names in `camelCase` for methods/variables, `UpperCamelCase` for classes.
  - Prefer explicit imports over wildcards.
  - Keep methods focused and small; avoid large, multi-responsibility methods.
  - Add tests in the corresponding module under `src/test/java` when fixing bugs or adding non-trivial features.
- Resources/Frontend (under `app/src/main/resources/servlet/content`):
  - Keep JS/CSS/HTML formatting consistent with neighboring files.
  - Large images (floor plans) should be optimized but lossless where possible.

## Repository hygiene
- Do not edit anything in `out/` or `*/out/` folders — they are generated.
- Keep changes minimal and localized; avoid broad refactors unless requested.
- When adding a new module or dependency, update `settings.gradle` and relevant Gradle build files accordingly.

## When unsure
- If the task is ambiguous, ask the user to clarify (e.g., which module to modify, expected behavior, or how to run a demo).
- If a non-standard test or run process is required, request exact instructions before proceeding.
