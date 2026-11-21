# Cascade — Product Requirements Document

Nov 19, 2026 · Dhananjay

## 1. Overview

Cascade is a desktop automation engine written in Java 25 with a JavaFX interface: the user chains a trigger, optional conditions and ordered actions into a workflow, and Cascade runs it automatically while the app is open.

It addresses repetitive manual work, such as sorting downloaded files or sending the same notification. Existing tools are web services or heavy platforms. Cascade is local, small and extensible: every trigger, condition and action is a plugin class behind a shared interface, so new behaviour is added without touching the engine or the interface.

| Item | Decision |
| --- | --- |
| Repository | [djaysharma007/Cascade](https://github.com/djaysharma007/Cascade) |
| Course | Advanced Object Oriented Programming (Java) |
| Developer | One person, building with an AI coding agent |
| Stack | Java 25 (LTS), JavaFX 25, Maven with the Maven wrapper, Jackson, JUnit 5 |
| Launch | `java -jar target/cascade.jar` or `mvn javafx:run` opens the GUI |
| Storage | JSON files under `~/.cascade/` |

### Success criteria

1. A three-minute demo works end to end: create a workflow, drop a PDF into a folder, watch it move, and see a retried failure in the log.
2. Every Must requirement in section 4 is implemented and tested.
3. `mvn verify` and the GitHub Actions build pass on `main`.
4. The interface meets the quality bar in section 12, with no default JavaFX styling left anywhere.
5. The repository carries a README with screenshots, three UML diagrams and five ADRs.

## 2. Scope

Version 1 is a single-user desktop app whose workflows run only while it is open. Everything in the second list is deferred on purpose.

**In scope**

- Workflow builder, enable/disable, test run, activity log and settings
- Three triggers, three conditions plus composite grouping, and three actions (section 7)
- Retry, timeout, idempotency and bounded concurrency in the engine
- JSON persistence behind a repository interface
- Tests, architecture rules, CI, UML diagrams and ADRs

**Out of scope for v1**

- Web or REST interface, React, Spring Boot
- Database, message queue, microservices, accounts or multiple users
- Loading plugins from external JAR files, starting at system boot
- Branching, loops and nested condition groups in the interface
- Light theme, localization, auto-update and cloud sync

## 3. Users and core flow

The primary user is one person automating tasks on their own computer. The second audience is the course evaluator, who must be able to see the design both in the running app and in the repository.

1. The user runs `mvn javafx:run` or the JAR. The window opens with the engine stopped.
2. The user selects New workflow, names it, and picks a trigger from the catalog. A form generated from the trigger's parameter specs appears.
3. The user adds conditions and actions the same way and reorders the actions.
4. Save validates every parameter. Errors appear beside the field and block saving.
5. The user turns the workflow on and starts the engine. The sidebar shows the workflow as Enabled.
6. When the trigger fires, Cascade builds an event, checks the conditions, runs the actions in order and writes a run record.
7. The Activity view shows the run live, with each step's result, attempts and duration.

**Reference workflow for the demo**

| Part | Configuration |
| --- | --- |
| Trigger | File created in the Downloads folder |
| Conditions | All of: extension is `.pdf`, name contains `assignment` |
| Action 1 | Move file to `Documents/College/Assignments` |
| Action 2 | Print message `Saved {fileName}` |

## 4. Functional requirements

Must items are required for the course submission; Should items are built if time allows, in the order listed.

| ID | Requirement | Priority |
| --- | --- | --- |
| FR-01 | Create a workflow with a name, one trigger, any number of conditions and at least one action | Must |
| FR-02 | Edit, duplicate and delete workflows; delete asks for confirmation | Must |
| FR-03 | Saving validates every parameter; invalid input shows inline errors and blocks the save | Must |
| FR-04 | Enable or disable each workflow; the state persists and is restored on launch | Must |
| FR-05 | Start and stop the engine from the top bar; stopping releases every trigger cleanly | Must |
| FR-06 | The plugin catalog is built from the registry; adding a plugin class needs no change to the engine or the UI | Must |
| FR-07 | Parameter forms are generated from each plugin's parameter specs, one control per parameter type | Must |
| FR-08 | Every plugin carries a `@Plugin` annotation and is registered; a build-time test fails otherwise | Must |
| FR-09 | A fired trigger creates an event, evaluates conditions, runs actions in order and records the run | Must |
| FR-10 | Text parameters support placeholders such as `{fileName}`, `{filePath}`, `{time}`, `{workflowName}` and `{runId}`; unknown placeholders fail validation at save | Must |
| FR-11 | A failed step stops the run; remaining steps are recorded as Not run | Must |
| FR-12 | Transient failures are retried with exponential backoff and jitter by a retry decorator | Must |
| FR-13 | Each action attempt has a timeout; expiry produces the Unknown outcome state | Must |
| FR-14 | Retried steps are idempotent per run and step (idempotency key) | Should |
| FR-15 | Concurrent runs are capped and queued; overflow is dropped and shown in the UI | Should |
| FR-16 | The Activity view shows runs live with a per-step timeline (result, attempts, duration) | Must |
| FR-17 | Structured log lines carry workflow id and run id | Should |
| FR-18 | The last 500 runs are kept and survive restart | Should |
| FR-19 | Workflows are stored as JSON with a `schemaVersion`; writes are atomic | Must |
| FR-20 | A corrupt workflow file never crashes the app; it is listed under Needs attention with the reason | Must |
| FR-21 | Test run executes a workflow once with a sample event, without waiting for the real trigger | Should |
| FR-22 | Every action is reachable by keyboard, with visible focus and logical tab order | Should |
| FR-23 | A Settings view edits concurrency cap, default timeout and retry defaults | Should |

### Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-01 | The UI thread never performs file, network or engine work; all UI updates run on the JavaFX Application Thread |
| NFR-02 | Cold start to an interactive window in under 2 seconds on a typical laptop |
| NFR-03 | A local file-created event reaches its first action in under 200 ms |
| NFR-04 | 100 enabled workflows with the Activity view open produce no visible UI stutter |
| NFR-05 | Runs on Windows, macOS and Linux with JDK 25; the Maven wrapper fetches Maven, so nothing else needs installing |
| NFR-06 | Core and engine packages reach at least 80% line coverage; architecture rules pass |

## 5. Architecture and package structure

Cascade is a layered application in one Maven module. The dependency rule is strict: every arrow points toward `core`, and `core` imports nothing but the JDK. An ArchUnit test enforces it.

Dependency direction (diagram in the original document, summarised here):

- `app` (composition root) builds the registry, repositories and engine, then opens the window. It depends on every module so it can wire them together.
- `ui` depends on `core` and `engine` (only through `EngineFacade`).
- `engine` depends on `core` and `registry`.
- `storage`, `registry` and `plugins` each depend only on `core`.
- `core` depends on nothing but the JDK.

The `engine` knows nothing about JavaFX, which keeps it testable and lets it run without a window later. The `ui` layer reaches it only through `EngineFacade`. `storage` implements the `WorkflowRepository` interface that lives in `core`, so the engine sees storage only as an interface and deleting `storage` still compiles it.

### Packages

Root package: `io.github.djaysharma007.cascade`.

```text
cascade/
├── pom.xml
├── AGENTS.md
├── docs/             adr/, uml/, screenshots/, architecture.md, open-questions.md, illegal-states.md
└── src/
    ├── main/java/.../cascade/
    │   ├── app/        Launcher, CascadeApplication, CompositionRoot
    │   ├── core/       ids, Workflow, Event, ParamSpec, results, failures, contracts, annotations
    │   ├── plugins/    triggers/, conditions/, actions/
    │   ├── registry/   PluginRegistry, PluginDescriptor
    │   ├── engine/     WorkflowEngine, lifecycle, executor, decorators, idempotency, event bus
    │   ├── storage/    JsonWorkflowRepository, RunLogRepository, migrators
    │   └── ui/         views/, viewmodels/, components/, FxEngineAdapter, EngineFacade
    ├── main/resources/ theme/cascade.css, fonts/, icons/, fxml/
    └── test/java/...   mirrors main, plus architecture/ and contract/ suites
```

**Rules**

- `Launcher` is a plain class with `main` that starts `CascadeApplication`, so the packaged JAR runs without JavaFX module-path flags.
- `CompositionRoot` is the only place that constructs concrete classes and wires them together.
- `plugins` depends only on `core`; `engine` depends on `core` and `registry`; `storage` and `registry` depend on `core`; `ui` depends on `core` and `engine`.
- Ports are interfaces in `core` (such as `WorkflowRepository`); adapters live in `storage`. Nothing outside `app` references a concrete adapter.

## 6. Domain model, invariants and contracts

Every type states its guarantee as a sentence before any field is written. Value types are immutable records with validating constructors; there are no public setters, and collections are copied on the way in and out (`List.copyOf`, `Map.copyOf`).

| Type | Kind | Invariant |
| --- | --- | --- |
| `WorkflowId`, `RunId`, `EventId` | Record over a UUID | An id is never null and two ids are equal only if their UUIDs are equal; they are never interchangeable |
| `PluginId` | Record over a string | A plugin id is 3 to 40 lowercase letters, digits or hyphens and starts with a letter |
| `Workflow` | Immutable record, built only through `WorkflowBuilder` | A workflow has a non-blank name of at most 80 characters, exactly one trigger spec and at least one action spec, and none of its lists can change after construction |
| `PluginSpec` | Record | A spec names a plugin id and holds an unmodifiable copy of its parameters |
| `ParamSpec` | Record | A parameter has a name, a type, a required flag and a default that matches its type |
| `Event` | Record | An event has an id, a timestamp and an unmodifiable payload, and is never mutated after creation |
| Condition tree | Leaf conditions plus `AllOf`, `AnyOf`, `Not` | A composite has at least one child; `Not` has exactly one |
| `ExecutionResult` | Sealed: `Success`, `Skipped`, `Failed` | Every result is exactly one of these three, and `Failed` always carries a `Failure` |
| `Failure` | Sealed: `Expected`, `Defect`, `Transient`, `UnknownOutcome` | Every failure is exactly one species (section 8) |
| `WorkflowState` | Enum with a total transition table | Every state and event pair is defined (section 8) |

### Contracts

Contracts are written as Javadoc on the interface before any implementation exists. A plugin that breaks a clause is a defect in the implementer; a caller that breaks a precondition is a defect in the caller.

| Interface | Precondition | Postcondition | Invariant |
| --- | --- | --- | --- |
| `Trigger.start(listener)` | Not already started; parameters validated; listener not null | Events reach the listener on a non-UI thread until `stop()` | After `stop()` returns, the listener is never called again |
| `Trigger.stop()` | None | Releases every thread and file handle the trigger holds | Idempotent and never throws |
| `Condition.test(event)` | Event not null | Returns true or false | Pure: no side effects, no blocking I/O, deterministic for the same event |
| `Action.execute(event, context)` | Event and context not null; parameters validated | Returns an `ExecutionResult`; expected failures are returned, not thrown | Does not mutate the event; safe to call from several threads; repeating a call with the same idempotency key has no extra effect |

### Evidence required

- An invariant-breaker test per record that tries each of the five ways a guarantee leaks: a public setter, an escaping reference, a mutable element inside an immutable collection, a constructor that validates nothing, and equality that lies.
- A shared contract test suite (`ActionContractTest`, `ConditionContractTest`, `TriggerContractTest`) that every plugin extends, plus a deliberately broken saboteur plugin that the suite must reject.
- `docs/illegal-states.md` lists expressions that must not compile, and a test compiles each one and asserts failure. Examples: passing a `WorkflowId` where a `PluginId` is expected; a non-exhaustive `switch` over `Failure`; a class outside the permits list extending `ExecutionResult`.
- `equals` and `hashCode` tests for every id and value type, including use as `HashMap` keys.

## 7. Plugin catalog (v1)

Each plugin declares a `@Plugin(id, displayName, category)` annotation and a list of `ParamSpec`s. Triggers also declare the payload keys they provide, and conditions and actions declare the keys they require, so the editor can warn at save time when a condition needs data the chosen trigger never supplies (for example, Extension is after a Timer trigger).

| Kind | Id | Display name | Parameters | Payload |
| --- | --- | --- | --- | --- |
| Trigger | `manual` | Manual | `label` (text, optional) | provides none |
| Trigger | `timer` | Timer | `intervalSeconds` (number, 1 or more), `fireImmediately` (yes/no, default no) | provides `firedAt` |
| Trigger | `file-created` | File created | `folder` (folder, must exist), `recursive` (yes/no, default no) | provides `fileName`, `filePath`, `fileSize` |
| Condition | `extension-is` | Extension is | `value` (text, such as `.pdf`, case-insensitive) | requires `fileName` |
| Condition | `name-contains` | Name contains | `text` (text), `caseSensitive` (yes/no, default no) | requires `fileName` |
| Condition | `file-size-over` | File size over | `megabytes` (number, above 0) | requires `fileSize` |
| Condition | `all-of`, `any-of`, `not` | All of, Any of, Not | `children` (conditions) | union of children |
| Action | `move-file` | Move file | `destinationFolder` (folder), `onConflict` (choice: rename, skip, overwrite; default rename) | requires `filePath` |
| Action | `print-message` | Print message | `text` (template) | none |
| Action | `send-email` | Send email | `smtpHost`, `smtpPort`, `username`, `passwordEnvVar`, `to`, `subject`, `body` | none |

**Rules**

- The interface exposes composite conditions as a single Match all or Match any control over the condition list, plus a Not toggle per row. Nested groups exist in the domain and JSON but not in the v1 interface.
- Send email never stores a password. The `passwordEnvVar` parameter names an environment variable that is read at run time.
- Template text may use `{fileName}`, `{filePath}`, `{fileSize}`, `{time}`, `{workflowName}` and `{runId}`. A placeholder the trigger cannot supply is a validation error at save.
- Test doubles used only in tests: `FlakyAction` (injects transient failures, timeouts and defects at a configurable rate) and `SaboteurAction` (violates the contract on purpose).

## 8. Failure handling and concurrency

### Failure taxonomy

Every failure is one of four species, modelled as the sealed type `Failure`. The species decides the handling; no code decides it with `instanceof` or message text.

| Species | Type | Example | Handling |
| --- | --- | --- | --- |
| Expected outcome | `Failure.Expected` | Move file with conflict policy skip and a file already present | Not retried; the step is recorded with a plain-language reason |
| Programmer error | `Failure.Defect` | Null pointer or contract breach inside a plugin | Not retried; run fails; full stack trace logged; engine keeps running |
| Transient infrastructure | `Failure.Transient` | SMTP connection refused, file locked by another process | Retried by the retry decorator |
| Unknown outcome | `Failure.UnknownOutcome` | Timeout after the mail server may have accepted the message | Retried only through the idempotency key; otherwise the run ends as Unknown and is flagged in the UI |

A timeout is never reported as a plain failure, because the caller cannot know whether the effect happened. A run ends in exactly one of: Succeeded, Skipped (conditions false), Failed or Unknown.

### Retry, timeout and idempotency

| Setting | Default |
| --- | --- |
| Attempts per action | 3 |
| Backoff | 500 ms first delay, doubling, full jitter, capped at 10 s |
| Retry budget per action | 30 s in total |
| Timeout per attempt | 15 s |

- `RetryingAction` is a decorator over any `Action`. `TimeoutAction` and `LoggingAction` are decorators too; the layer order (logging, retry, timeout, action) is documented because it changes what is observable.
- The idempotency key is `runId:stepIndex`. An `IdempotencyStore` records completed steps, so a retried or replayed step that already finished does nothing. Send email derives its `Message-ID` header from the key.
- A circuit breaker is out of scope for v1 and is listed under future scope.

### Workflow lifecycle state machine

The enabled flag is replaced by an explicit state held in an `AtomicReference` and changed by compare-and-set, so enabling or disabling while a trigger fires is atomic. Reject means the call is refused with a clear exception; discard means the event is dropped and counted.

| State | enable | triggerStarted | triggerStartFailed | disable | triggerStopped | triggerFired |
| --- | --- | --- | --- | --- | --- | --- |
| DISABLED | STARTING | reject | reject | no change | reject | discard |
| STARTING | no change | ENABLED | FAULTED | STOPPING | reject | discard |
| ENABLED | no change | reject | reject | STOPPING | reject | run |
| STOPPING | reject | reject | reject | no change | DISABLED | discard |
| FAULTED | STARTING | reject | reject | DISABLED | reject | discard |

### Concurrency design

- Runs execute on a virtual-thread executor. A semaphore caps concurrent runs (default 8) and a bounded queue (100) provides back-pressure; overflow drops the newest event, logs it and shows a warning in the UI.
- Runs of the same workflow are serialized by a per-workflow lock, so two events cannot interleave one workflow's steps.
- Every mutable field is classified as immutable, confined or guarded, and carries a `@ThreadSafety` annotation naming its category (and the guard, if guarded). An architecture test fails the build when a non-final field has no annotation.
- Events are immutable records handed to the executor, which is the safe publication point. UI view-models receive engine updates only through `FxEngineAdapter`, which marshals them with `Platform.runLater`.

### Chaos test

A test feeds 500 events through a workflow whose action is a `FlakyAction` with a 30% injection rate split across transient errors, timeouts and defects. It must show zero duplicate effects per idempotency key, and the same workflow with injection off must show no added latency.

## 9. Persistence and JSON schema

Workflows are stored as one JSON file each, behind a `WorkflowRepository` interface (`save`, `findById`, `findAll`, `delete`). `JsonWorkflowRepository` is the only implementation in v1; a SQLite implementation can replace it later without touching the engine or the UI.

| Path under `~/.cascade/` | Contents |
| --- | --- |
| `workflows/<workflowId>.json` | One workflow, including its enabled state |
| `runs.jsonl` | Run history as JSON lines, rotated at 5 MB; the last 500 runs are loaded at start |
| `settings.json` | Concurrency cap, default timeout, retry defaults |
| `logs/cascade.log` | Structured application log |

```json
{
  "schemaVersion": 1,
  "id": "3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
  "name": "Sort assignments",
  "enabled": true,
  "trigger": { "type": "file-created", "params": { "folder": "C:/Users/me/Downloads" } },
  "conditions": {
    "type": "all-of",
    "children": [
      { "type": "extension-is", "params": { "value": ".pdf" } },
      { "type": "name-contains", "params": { "text": "assignment" } }
    ]
  },
  "actions": [
    { "type": "move-file", "params": { "destinationFolder": "C:/College/Assignments" } },
    { "type": "print-message", "params": { "text": "Saved {fileName}" } }
  ]
}
```

**Rules**

- The serialized form is a published contract. Adding an optional field with a default is allowed within a version; renaming, removing or changing the meaning of a field bumps `schemaVersion` and adds a migrator (`WorkflowMigrator`, one step per version) with a test using a stored v1 file.
- Writes go to a temporary file in the same folder and are moved over the target with an atomic move, so a crash never leaves a half-written file.
- Reads and writes use try-with-resources. Unknown fields are ignored with a warning; a file that fails to parse or validate is skipped, logged, and listed under Needs attention with the reason (FR-20).
- Java serialization is not used anywhere.

## 10. Design system

Cascade uses a single dark theme built from the teal palette (Ink Black, Dark Teal, Tropical Teal, Electric Aqua, Sky Blue). The family fits the name, stays cohesive because every colour is a neighbour of the others, and gives high contrast for a tool people leave open. All colours live as named tokens in one `cascade.css`; no hex value appears anywhere else.

### Colour tokens

| Token | Hex | Source | Use |
| --- | --- | --- | --- |
| `bg-base` | `#001011` | Ink Black | Window background |
| `bg-surface` | `#061E21` | Derived | Sidebar, panels |
| `bg-raised` | `#093A3E` | Dark Teal | Cards, inputs, hovered rows |
| `border` | `#124B50` | Derived | 1 px dividers and outlines |
| `accent` | `#3AAFB9` | Tropical Teal | Primary buttons, selection, links |
| `accent-bright` | `#64E9EE` | Electric Aqua | Focus ring, live indicator, active toggle; at most 5% of any screen |
| `text-primary` | `#E6F2F4` | Derived | Main text |
| `text-secondary` | `#97C8EB` | Sky Blue | Labels, secondary text, info |
| `text-muted` | `#86B0B7` | Derived | Placeholders, disabled text |
| `success` | `#5BD68A` | Semantic | Succeeded only |
| `warning` | `#F2B84B` | Semantic | Unknown outcome, retries, warnings |
| `error` | `#F2677A` | Semantic | Failed, destructive actions |

Semantic colours are used only for status and never as decoration. Status is never shown by colour alone: every status chip carries an icon and a text label. A unit test computes the contrast ratio of every text and background token pair used in the app and fails below 4.5:1.

### Typography

| Role | Font | Size and weight |
| --- | --- | --- |
| Page title | Inter | 20 px, 600 |
| Section title | Inter | 16 px, 600 |
| Body | Inter | 14 px, 400 |
| Table, secondary | Inter | 13 px, 400 |
| Caption | Inter | 12 px, 500 |
| Paths, log, JSON | JetBrains Mono | 13 px, 400 |

Both fonts (SIL Open Font License) are bundled in resources and loaded at startup, so the look is identical on every machine.

### Layout, shape and motion

- 4 px base grid. Spacing scale: 4, 8, 12, 16, 24, 32, 48. No other values.
- Radius: 6 px for controls, 10 px for cards, 14 px for dialogs. Borders 1 px. Separation comes from surface colour and borders, not shadows; only dialogs and toasts have one soft shadow.
- Control height 32 px, primary button 36 px, list row 44 px, sidebar 264 px wide.
- Motion: 120 ms for hover and press, 180 ms for panels and dialogs, ease-out, fade and short translate only. No bounce, no spinning decorations. The only looping animation is a slow pulse on the live-run indicator.
- One outline icon set (Lucide, ISC licence) at 16 and 20 px with a 1.5 px stroke, bundled as SVG paths. Emoji and Unicode symbols are never used as icons.
- App mark: three descending rounded bars in `accent-bright` on `bg-base`, drawn by hand as SVG, with the wordmark Cascade in Inter 600.

### Components

| Component | Specification |
| --- | --- |
| Primary button | `accent` fill, `bg-base` text, 36 px; hover lightens, pressed darkens, 2 px `accent-bright` focus ring |
| Secondary and ghost buttons | Transparent with `border` outline or no outline; `text-primary` label |
| Danger button | `error` text and outline; filled only inside the delete confirmation |
| Text input, select, number field | `bg-raised` fill, `border` outline, `accent-bright` outline on focus, inline error text in `error` below the field |
| Toggle switch | 36 by 20 px track; `accent-bright` when on; custom-drawn |
| Segmented control | Used for Match all or any; selected segment uses `accent` |
| Status chip | Dot or icon plus text; Enabled, Disabled, Faulted, Succeeded, Skipped, Failed, Unknown |
| Table | 12 px header in `text-secondary`, no zebra stripes, hover row in `bg-raised` |
| Toast | Bottom right, 4 s, at most 3 stacked, never blocks input |
| Dialog | 480 px wide, 60% black scrim, one primary action, Escape closes |

## 11. Screens and interaction

The window opens at 1280 by 800 and cannot shrink below 1024 by 680. Layout is FXML, styling is CSS only, state lives in view-models, and controllers contain no logic (MVVM). View-models reach the engine through one `EngineFacade`.

### Application shell

- **Top bar:** app mark, current view title, and the engine control: a single button that reads Start engine or Stop engine, with a status chip beside it (Stopped, Running, Stopping).
- **Sidebar:** New workflow button, then navigation for Workflows, Activity and Settings.
- **Status bar:** enabled workflow count, runs in flight, and any back-pressure warning.

### Workflows

- A list of rows, 44 px tall: name, trigger summary, state chip, last result with relative time, an enable toggle and an overflow menu (Edit, Duplicate, Delete).
- A Needs attention section appears above the list only when a file failed to load, with the reason and the file name.
- Empty state: one sentence saying what a workflow is and a New workflow button. No illustration.

### Workflow editor

- Three stacked sections in this order: Trigger (one card), Conditions (list with a Match all or any control and an Add condition button), Actions (ordered list with drag reordering and an Add action button).
- Choosing a plugin opens its generated form inside the card. Controls follow the parameter type: text, number, yes/no switch, choice, folder picker with a Browse button.
- Validation runs on field blur and on Save. Errors are written under the field in plain language: what is wrong and what to enter instead.
- A sticky footer holds Save, Discard changes and Test run. Unsaved changes are indicated in the footer, and leaving with unsaved changes asks for confirmation.

### Activity

- A table of runs: time, workflow, trigger summary, result chip, duration. New runs appear at the top live.
- Selecting a run opens a side panel with a step timeline: each step's result, number of attempts, duration and failure reason, with Not run steps greyed.
- Filters for workflow and result. Empty state names what will appear here and when.

### Settings

- Concurrency cap, default action timeout, retry attempts and base delay, with defaults shown and a Reset to defaults action.

### States every screen must define

Loading (skeleton rows, not spinners over blank space), empty, error (what failed, why, what to do next) and populated. The agent must show each state in the visual check described in section 12.

### Keyboard

`Ctrl+N` new workflow, `Ctrl+S` save, `Ctrl+Enter` test run, `Ctrl+E` start or stop the engine, `Esc` closes dialogs and panels. On macOS the same shortcuts use `Cmd`.

## 12. Rules for the AI coding agent

These rules apply to every task. In phase 0 the agent copies this section and section 13 verbatim into `AGENTS.md` at the repository root (and `CLAUDE.md` if the agent is Claude Code) so they are read at the start of every session.

### Environment and autonomy

- Target machine: Windows with PowerShell and JDK 25 already installed (`C:\Program Files\Java\jdk-25.0.2`). Java 25 is the required version. Never ask for, install or switch to another JDK, and never downgrade Java to make a tool work.
- The agent runs every command itself and installs anything missing without asking: Maven, Git, GitHub CLI or anything else the build needs. Preferred order: (1) a user-level install that needs no administrator rights, such as downloading the official zip into `%USERPROFILE%\tools`, adding it to PATH for the session and persisting it in the user environment variables; (2) `winget install` with `--silent --accept-package-agreements --accept-source-agreements`; (3) Chocolatey only if it is already present.
- After Maven is available once, run `mvn wrapper:wrapper` and commit `mvnw`, `mvnw.cmd` and `.mvn/`. From then on every build uses `mvnw`, so nobody needs Maven installed.
- Set `JAVA_HOME` to the JDK 25 folder for the session and verify with `java -version`, `javac -version` and `mvnw -version`.
- Use the newest stable versions of the Maven plugins and libraries that support Java 25 (compiler plugin, JaCoCo, ArchUnit, Mockito, Jackson, JUnit, JavaFX plugin). If a tool fails because of the Java version, update that tool.
- Ask the user only when blocked by something only they can do: a password or login prompt, an administrator elevation the agent cannot obtain, a GitHub authentication failure, or a decision that changes the PRD. Then send one message with exactly what is needed and the exact command, and continue with any work that is not blocked.
- Never end a turn with a list of manual install steps the agent could run itself. If a download fails, retry once, try the next install route, then report the exact error.

### How to work

1. Read this whole PRD before writing code. It is the source of truth; if code and PRD disagree, fix the code or ask, never silently diverge.
2. Build in the phase order of section 16, one feature at a time. Do not start a feature whose phase gate has not passed.
3. Implement only what the PRD lists. Do not add features, screens, settings or dependencies that are not in it. If something is missing or ambiguous, write the question in `docs/open-questions.md`, pick the simplest option that matches the PRD, and note the choice there.
4. Every pattern, interface and abstraction must trace to a requirement. A pattern with no requirement behind it is removed, not defended.
5. Before reporting a feature done: run `mvn verify`, launch the app, exercise the feature, and complete the visual check below.

### Code quality

- Follow the package rules in section 5. Dependencies point inward only; architecture tests enforce this.
- No public setters, no mutable value types, no raw `String` ids, no `null` returned for "nothing" (use `Optional` or a result type), no wildcard imports, no unused imports.
- Methods stay under about 30 lines and classes under about 300. Split by responsibility, not by line count alone.
- No `System.out` and no `printStackTrace()`; use SLF4J. No swallowed exceptions; every catch either handles, translates to a `Failure`, or rethrows with context.
- No magic numbers or repeated string literals; name them as constants or settings.
- Comments explain why, never what. No commented-out code, no `TODO` or `FIXME` left in a commit, no leftover debug code.
- Javadoc is mandatory on every contract interface and on every public type in `core`, and states pre- and postconditions.
- Every behaviour change ships with a test in the same commit.

### Interface quality (what separates this from template output)

- Use only the tokens in section 10. No raw hex, no default JavaFX (Modena) styling visible, no default focus ring, no stock blue.
- Spacing comes from the 4 px scale only. Align everything to the grid; equal gaps for equal relationships. Misaligned edges are defects.
- No gradients, glows, glassmorphism, neon outlines, drop shadows on cards, decorative blobs or illustrations. Electric Aqua stays within its 5% budget.
- One icon set, one weight. No emoji anywhere: UI, logs, comments, commit messages, README.
- Every list, table and form has designed empty, loading, error and populated states.
- Controls look and behave consistently: same height, radius, hover, pressed, focus and disabled states everywhere. Build each component once in `ui/components` and reuse it.
- Every interactive element is keyboard reachable with a visible focus state and a meaningful accessible name.
- Animations follow section 10 only; nothing moves without a purpose.

### Copy

- Sentence case, plain and specific. Say what happened and what to do next.
- No exclamation marks, no filler such as "Oops", "Awesome", "Let's get started", "Seamless", "Powerful" or "Supercharge".
- No placeholder text: no lorem ipsum, no `foo`, `bar`, `test123` or `My Workflow 1` in shipped screens or sample data. Sample data uses realistic names and paths.
- Error messages never show raw exception text or stack traces to the user; they are logged instead.
- The same concept always has the same name. Use the terms in this PRD: workflow, trigger, condition, action, run, engine.

### Visual check before a feature is done

1. Run the app and capture a screenshot of each screen the feature touched, in every state (empty, loading, error, populated), using a JavaFX node snapshot saved to `docs/screenshots/`.
2. Compare each screenshot against sections 10 and 11. Look specifically for off-grid spacing, inconsistent heights, truncated text, low contrast, and any default JavaFX control.
3. Fix what is wrong and capture again. Only then report the feature done.

### Verification and honesty

- Never claim a test passes, a screen matches the spec or a feature works without having run it in this session.
- When something cannot be done as specified, say so, record it in `docs/open-questions.md` and continue with the next task that is unblocked.
- Follow the course's policy on AI-assisted work, and disclose AI assistance in the README if the course requires it.

## 13. Git and GitHub workflow

The remote is `https://github.com/djaysharma007/Cascade`. The agent pushes to it every time a feature is finished or a major change lands, so the history on GitHub always matches the working app.

1. **Branch per feature.** Create `feat/<short-name>` from `main` (`fix/`, `refactor/`, `docs/` and `test/` for other work). Commit in small steps on the branch.
2. **Verify before every push.** Run `mvn verify`. Never push a failing build.
3. **Push the branch** after each completed feature or major change: `git push -u origin <branch>`.
4. **Merge to `main`** once the feature meets its definition of done (section 17), using a pull request if the GitHub CLI is available, otherwise a local `--no-ff` merge. Then push `main`.
5. **Tag each finished phase** (`v0.1.0` after phase 1, and so on, `v1.0.0` at release) and push the tags.

**Commit messages** use Conventional Commits in the imperative: `feat(engine): add retry decorator with jittered backoff`. The subject is at most 72 characters; the body says why, not what. One logical change per commit.

**Never**

- Force-push, rewrite pushed history, or commit directly to `main` after phase 0.
- Commit secrets, tokens, passwords, personal paths or the contents of `~/.cascade/`.
- Commit `target/`, IDE folders, OS files or logs. `.gitignore` is created in phase 0.
- Put a credential in a remote URL or a file; the agent relies on the credentials already configured on the machine.

If a push fails (authentication, network or a rejected update), the agent stops, reports the exact error and waits instead of working around it.

## 14. Testing, CI and documentation

### Testing

| Layer | Approach |
| --- | --- |
| Core model | JUnit 5 and AssertJ; invariant-breaker tests; equals and hashCode tests; compile-fail tests for `docs/illegal-states.md` |
| Plugins | Each plugin extends the shared contract suite for its kind; the saboteur must be rejected |
| Engine | State machine transition table tested cell by cell; enable/disable race test with many threads; retry, timeout and idempotency tests with a controllable clock; the chaos test from section 8 |
| Persistence | Round-trip tests, a stored v1 file migration test, atomic-write test, corrupt-file test |
| Architecture | ArchUnit rules: `core` depends on nothing but the JDK; plugins depend only on `core`; `engine` never imports `ui` or a storage implementation; no `System.out`; every plugin is annotated and registered; every non-final field has `@ThreadSafety` |
| UI | View-model unit tests; a small set of TestFX tests tagged `ui` and run locally; screenshots for the visual check |

### Continuous integration

A GitHub Actions workflow at `.github/workflows/ci.yml` runs on every push and pull request: JDK 25 (Temurin), `mvn -B verify`, JaCoCo report as an artifact. Tests tagged `ui` are excluded in CI. The README shows the build status badge.

### Documentation deliverables

- **README:** what Cascade is, a screenshot, how to run it, architecture overview and a short guide to writing a plugin.
- **`docs/architecture.md`:** layers, dependency rule and the main flow from trigger to run record.
- **UML in `docs/uml/` (PlantUML source and exported PNG):** a class diagram of the core contracts and plugins, a sequence diagram of one run from trigger to run record, and a state diagram of the workflow lifecycle.
- **ADRs in `docs/adr/`:** each with context, decision, the rejected alternative and its cost.

| ADR | Decision | Rejected alternative |
| --- | --- | --- |
| 0001 | Desktop JavaFX interface | React with a Spring Boot API |
| 0002 | JSON files behind a repository interface | SQLite from the start |
| 0003 | Interfaces for plugin roles, sealed types for results and failures | Abstract base classes for plugins |
| 0004 | Virtual-thread executor with a semaphore cap | Fixed-size thread pool |
| 0005 | `@Plugin` annotation, explicit registry and a build-time check | Classpath scanning or `ServiceLoader` |

## 15. Course traceability

Each selected syllabus topic maps to a requirement and to the evidence that proves it. Topics not listed (money types, ORM, isolation levels, sharding, capacity estimation) are deliberately skipped.

| Module | Topic | Where it appears | Evidence |
| --- | --- | --- | --- |
| M1 (CO1) | Invariants as sentences, immutability, defensive copying | Section 6 types, FR-01 | Invariant-breaker tests |
| M1 (CO1) | Typed ids, sealed hierarchies, illegal states | `WorkflowId`, `PluginId`, `ExecutionResult`, `Failure` | `docs/illegal-states.md` and compile-fail tests |
| M1 (CO1) | Equality and hash contract, generics, collections by workload | Ids, `PluginRegistry<T>`, run history buffer | equals/hashCode tests, ADR notes |
| M2 (CO2) | Design by contract, Liskov, interface segregation | `Trigger`, `Condition`, `Action` contracts (section 6) | Shared contract suites and the saboteur |
| M3 (CO3) | Dependency inversion, composition root, ports and adapters | `WorkflowRepository`, `app` package | ArchUnit rules, ADR 0002 |
| M3 (CO3) | Factory, Strategy, Command, Observer, Builder | Registry, plugins, actions, engine events, `WorkflowBuilder` | Class diagram, README section |
| M3 (CO3) | Decorator, Composite, state machine | `RetryingAction` and friends, `AllOf`/`AnyOf`/`Not`, lifecycle table | Transition-table tests, state diagram |
| M3 (CO3) | Annotations that enforce design rules | `@Plugin`, `@ThreadSafety` | Build-failing architecture tests, ADR 0005 |
| M4 (CO4) | Failure taxonomy, timeouts as unknown outcomes | `Failure` sealed type, FR-13 | Engine tests |
| M4 (CO4) | Retries with backoff and jitter, idempotency | `RetryingAction`, idempotency key (FR-12, FR-14) | Retry tests, chaos test |
| M5 (CO5) | Three safe states, check-then-act, executors, safe publication | Lifecycle compare-and-set, executor design (section 8) | Race test, `@ThreadSafety` scan |
| M5 (CO5) | Serialization as a contract | `schemaVersion`, migrators (section 9) | Migration test |
| M6 (CO6) | UML as a graded deliverable | `docs/uml/` | Class, sequence and state diagrams |
| M6 (CO6) | ADRs, architecture tests, CI, observability | Sections 13 and 14, FR-17 | Five ADRs, ArchUnit, green Actions run |

## 16. Build phases

One developer working with an agent builds in this order. A phase is finished only when its gate passes, its code is pushed (section 13) and the phase tag exists. Size is a relative guide (S, M, L), not a schedule.

1. **Phase 0, scaffold (S).** Maven project with the Maven wrapper on Java 25, JavaFX, Jackson, JUnit, ArchUnit and JaCoCo; `Launcher` and `CascadeApplication`; `.gitignore`; CI workflow; `AGENTS.md` from sections 12 and 13; `cascade.css` with all tokens and bundled fonts; an empty themed window. *Gate:* CI is green and the window opens fully themed.
2. **Phase 1, core model and contracts (M).** Ids, records, sealed result and failure types, `WorkflowBuilder`, the three contract interfaces with Javadoc, contract test suites and the saboteur. *Gate:* invariant, equality, compile-fail and contract tests pass.
3. **Phase 2, plugins and registry (M).** `@Plugin`, `PluginRegistry`, three triggers, three conditions plus composites, three actions, template placeholders, payload-key validation. *Gate:* every plugin passes its contract suite; the annotation and registration test passes.
4. **Phase 3, engine (L).** Lifecycle state machine, executor and semaphore, run pipeline, decorators (logging, retry, timeout), idempotency store, event bus. *Gate:* transition-table, race and chaos tests pass.
5. **Phase 4, persistence (M).** JSON repository, atomic writes, schema version and migrator, run history, corrupt-file handling, settings. *Gate:* round-trip, migration and corruption tests pass.
6. **Phase 5, interface (L).** Shell and engine control, Workflows list, Editor with generated forms, Activity with step timeline, Settings. Build one screen at a time, each with the visual check from section 12. *Gate:* the demo in section 1 runs end to end and every screen matches sections 10 and 11 in all states.
7. **Phase 6, hardening and documentation (M).** Architecture rules complete, three UML diagrams, five ADRs, README with screenshots, accessibility and keyboard pass, performance check against the NFRs. *Gate:* the release checklist in section 17 is fully ticked; tag `v1.0.0`.

## 17. Definition of done and future scope

### Feature done

- [ ] Behaviour matches the requirement ID it implements
- [ ] Tests for the behaviour are written and `mvn verify` passes
- [ ] Architecture rules still pass
- [ ] For UI work, every touched screen was captured in all states and checked against sections 10 and 11
- [ ] No `TODO`, debug output, commented-out code or placeholder text
- [ ] Committed with Conventional Commits and pushed to GitHub

### Release (v1.0.0)

- [ ] Every Must requirement in section 4 is implemented and tested
- [ ] The three-minute demo runs from a clean clone with `mvn javafx:run`
- [ ] GitHub Actions is green on `main`
- [ ] README has screenshots, run instructions and the plugin-authoring guide
- [ ] Three UML diagrams and five ADRs are in `docs/`
- [ ] Core and engine coverage is at least 80%
- [ ] Contrast test and keyboard pass are complete

### Future scope

Not built in v1, but the design leaves room for each: circuit breaker and saga-style compensation, a SQLite repository, plugin JARs loaded at runtime, headless mode with start at boot, branching and nested condition groups in the editor, a REST layer with a web interface, a message queue between triggers and workers, and multi-user deployment with observability metrics.
