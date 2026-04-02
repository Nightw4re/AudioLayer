# AI Rules

This repository is a Minecraft mod project named `Audiolayer`.

## Core goals

- Keep the architecture clean and testable.
- Prefer pure Java domain logic outside Minecraft-specific integration.
- Use immutable data models where practical.
- Avoid giant classes and hidden global state.
- Keep namespaces, mappings, and APIs deterministic.

## Scope rules

- Do not patch third-party mods.
- Do not build universal sound hacks.
- Do not rewrite or overwrite other mods' assets.
- Keep the current MVP focused on:
  - scanning external audio files
  - mapping sound IDs
  - cache/index handling
  - reload orchestration
  - commands and wiring

## Code rules

- Prefer constructor injection or explicit wiring.
- Keep public APIs documented with JavaDoc.
- Use English only for code comments, documentation, and commit messages.
- Use clear package boundaries:
  - `api`
  - `audio`
  - `config`
  - `cache`
  - `conversion`
  - `registry`
  - `commands`
  - `client`
  - `network`
  - `resource`
  - `testsupport`
- Keep Minecraft/NeoForge code thin and delegate logic to core services.

## Testing rules

- Add tests for all non-trivial logic.
- Keep tests deterministic.
- Prefer unit tests first, then integration-style tests with temp directories.
- Verify changes by running `npm test`.
- Do not stop with unverified build changes if tests can reasonably be run.

## Tooling rules

- Use `npm` as the primary local entrypoint for build/test scripts.
- Do not require Gradle for basic local test execution.

## Commit format

Every git commit must follow:

```text
<type>: <short description>

<explanation of what changed and why>
```

Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`

- Keep both parts in English.
- Keep the description concise and specific.
- Do not add `Co-Authored-By` trailers to commits.
- Do not push to any remote repository.

## When extending the project

- Start with the smallest useful implementation.
- Preserve existing naming and folder structure unless there is a strong reason to change it.
- Add tests before or alongside behavior changes.
- If a change affects the public API, update docs and sample layout notes.
