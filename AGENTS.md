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

### Interface quality (v1.1 upgrade specification)

- Use only the tokens in section 10 and 18. No raw hex outside CSS, no default JavaFX (Modena) styling visible, no default focus ring, no stock blue.
- Spacing comes from the 4 px scale only. Align everything to the grid; equal gaps for equal relationships. Misaligned edges are defects.
- Visual depth: soft shadows on cards, dialogs and popups; faint accent glow only on live or active elements; subtle gradient allowed on primary buttons and page headers. No neon, no glassmorphism, no emoji.
- One icon set, one weight, using crisp vector shapes. No emoji anywhere: UI, logs, comments, commit messages, README.
- Show friendly display names everywhere ("File created", "Move file") and never raw ids like file-created.
- Smooth animation using the JavaFX transition and Timeline API, 120 to 250 ms with ease-out: page transitions (fade plus a short slide), hover and press feedback on every button, a toggle switch whose thumb slides, list rows that fade in with a short stagger, toasts that slide in, smooth expand and collapse, a pulsing live-run indicator, and animated counters on the dashboard. Add a "Reduce motion" setting that turns animation off.
- Every list, table and form has designed empty, loading, error and populated states.
- Controls look and behave consistently: same height, radius, hover, pressed, focus and disabled states everywhere. Build each component once in `ui/components` and reuse it.
- Every interactive element is keyboard reachable with a visible focus state and a meaningful accessible name.


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
