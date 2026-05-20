# Project Guidance for AI Assistants — HomeAutomation / home-controller

These guidelines tell Claude Code, Junie, and any other AI assistant how to work
with this repository: project layout, build/test instructions, coding
conventions, and safety rules. If anything here becomes outdated, prefer what is
in the code (Gradle files) and CI over this document.

## Language

**All code, identifiers, comments, commit messages, PR/MR titles and
descriptions, and AI guidance documents are written in English.** No Czech (or
any other language) in source files, JavaDoc, log strings, or this document.
Interactive chat with the project owner may be in Czech if the owner prefers,
but anything written to disk or git history must be English.

## Project overview

Gradle multi-project Java codebase with several modules:

- `base` — foundational utilities and shared types used by other modules.
- `controller` — core logic for device control/integration and runtime behavior.
- `app` — application/UI layer. Contains servlet/static resources under
  `app/src/main/resources/servlet/content` (HTML/JS/CSS/images such as floor
  plans).
- `extensions` — optional integrations with external services and devices
  (Solax inverter, recuperation unit, boiler, air-conditioning, heat pump) and
  related tests.
- `buildSrc` — Gradle build logic.

Other notable directories:

- `cfg`, `doc`, `script` — configuration, documentation, helper scripts.
- `doc/inverter/solax_modbus_spec_3.34.md` — Solax X3-Hybrid G4 Modbus register
  specification. Read-side and write-side maps are separate; see the Solax
  section below.
- `out`, `*/out` — compiled outputs; treat as generated. Never edit or place
  source files here.

Standard source layout per module:

- `src/main/java` — production Java code
- `src/main/resources` — resources
- `src/test/java` — unit/integration tests
- `src/test/resources` — test resources

## Build and test

Before submitting code changes, build the project and run relevant tests for
the impacted module(s). Documentation-only changes (Markdown, comments, static
resources without code changes) do not need a build.

Local Gradle commands:

- `./gradlew build` — full build
- `./gradlew test` — all tests (see safety note below — do NOT run blindly)
- `./gradlew :extensions:test --tests "org.chuma.*SolarPanelCalculatorTest"` —
  pattern
- `./gradlew compileJava compileTestJava` — build only, runs no tests (always
  safe)

In a JetBrains/Junie environment, prefer the provided Build and Test tools over
shell commands; the Gradle commands above are the equivalents.

### Test safety — never run tests that touch real devices

Some tests under `extensions/src/test/java/.../external/` are integration tests
that connect to and CHANGE STATE of real hardware in the user's house:

- Solax inverter (`SolaxInverterModbusClientTest` — calls
  `setSelfUseMinimalSoc`, `setExportControlUserLimit`, `setPgridBias`,
  `setInverterOn`, `setBatteryMode` against the real Solax X3-Hybrid G4)
- Recuperation unit, boiler, air-conditioning, heat pump
- Anything reachable via Modbus or Ethernet device control

Rules:

- Never run `./gradlew :extensions:test` (no `--tests` filter) without explicit
  user permission.
- For filtered runs (`--tests <Name>`), first read the test source to verify it
  does not touch external HW. If it does, ask before running.
- `./gradlew compileJava compileTestJava` is always safe — it runs no tests.
- When the user says "run the tests", clarify scope; do not interpret it as
  "run everything".
- After code changes in the inverter / recuperation / boiler / AC layer, report
  that the build passed and explicitly state you cannot run HW-touching tests —
  the user should run them.

Reason: running these blindly could switch the inverter into Standby mid-day,
change battery SoC limits, disable export, close the recuperation unit, or
overheat the boiler. Manual recovery would be required; there is risk of
damage or discomfort.

### Never propose shifting the system clock

When designing tests or verifications, never suggest changing the user's system
clock (`date`, `timedatectl`, manually changing time in Windows/macOS, etc.) —
not even as a "temporary" trick.

Why: shifting the clock can cause unintended side effects the user does not
expect — log timestamps, file timestamps, certificates, cron jobs, lock files,
NTP resync.

Use instead:

- TZ-dependent UI behavior — emulate timezone in DevTools (Sensors panel) or
  set `TZ=...` env var for Node processes.
- Clock-skew / server-time fixes — let the user verify manually with their own
  strategy; in your plan, limit yourself to "user verifies manually" and do not
  suggest how to reset the clock.

## Coding conventions

- Follow the existing style of the module you are editing. Mirror import order,
  spacing, and brace style.
- Java:
  - Descriptive names in `camelCase` for methods/variables, `UpperCamelCase`
    for classes.
  - Prefer explicit imports over wildcards.
  - Keep methods focused and small; avoid large, multi-responsibility methods.
  - Add tests in the corresponding module under `src/test/java` when fixing
    bugs or adding non-trivial features.
- Resources/Frontend (under `app/src/main/resources/servlet/content`):
  - Keep JS/CSS/HTML formatting consistent with neighboring files.
  - Large images (floor plans) should be optimized but lossless where possible.

## Working with Solax inverter / Modbus code

The header comment of `SolaxInverterModbusClient.java` references the spec at
`doc/inverter/solax_modbus_spec_3.34.md`. Always open that spec and grep for
the symbol before adding or modifying a getter/setter.

Key gotcha: Solax has separate read and write register maps. The same variable
lives at different addresses for read vs write:

- `SolarChargerUseMode` — read at `0x008B`, write at `0x001F`
- `SystemON_OFF` — write at `0x001C`
- `SelfUse_Discharge_MinSoC` — write at `0x0061`

Do not infer register addresses from commit messages, related code, or memory.
Look them up in the spec's "Write" section.

## Repository hygiene

- Do not edit anything in `out/` or `*/out/` folders — they are generated.
- Keep changes minimal and localized; avoid broad refactors unless requested.
- When adding a new module or dependency, update `settings.gradle` and the
  relevant Gradle build files.

## When unsure

- If the task is ambiguous, ask the user to clarify (which module, expected
  behavior, how to run a demo).
- If a non-standard test or run process is required, request exact instructions
  before proceeding.
