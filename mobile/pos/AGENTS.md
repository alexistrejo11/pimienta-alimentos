# Mobile POS (Android)

Early **Kotlin + Jetpack Compose** point-of-sale for school cafeterias. Single Gradle module `:app`, package `io.github.alexistrejo.pimienta.pos`.

- Native Android only (not KMP).
- Do not add DI, Room, or networking unless the task asks for them.
- Product intent (Spanish) lives in [docs/product/](docs/product/README.md). Do not invent MVP rules that contradict those docs.

## Comments (beginner-friendly)

When writing or substantially editing Kotlin, add **short English comments** on each meaningful block (class, function, composable, Gradle/config section) that explain **what it does**.

Skip noise: no essay comments, no restating a one-line function name. Prefer a sentence above a class or a non-obvious block.

## Skills

- [pimienta-pos-ui](.agents/skills/pimienta-pos-ui/SKILL.md) — brand tokens, fonts, dark surfaces, operational (non-card) layout. Tokens must stay aligned with [web pimienta-frontend-ui](../../web/.agents/skills/pimienta-frontend-ui/SKILL.md).
