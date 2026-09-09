# UltiTools External Plugin Example — UAT Checklist

This document is the executable companion to `FEATURES.md`: one row per feature stating the
steps to exercise it and the observable truth that proves it works. It is an internal reference
for real-machine verification, not user-facing documentation.

> Batches are dispatched at 60 rows or fewer, and a batch never spans two repositories. There are
> exactly two legitimate exits to `human-uat-pending`: a row needing the pixel layer while the
> real-client harness is not ready, and a row needing personal credentials. Every other row must
> reach `pass`, `fail`, or `blocked`.

## Conventions

- **Columns:** `ID`, `Preconditions`, `Steps`, `Expected`, `Layer`, `Covers`.
- **ID:** cites its `FEATURES.md` ID verbatim. A negative case suffixes the checklist ID only,
  as `.neg-<slug>` — a negative case still tests the same feature, so the base ID is unchanged.
- **Layer**, copied verbatim from Laojun's own `ultitools-real-client-uat` skill so no
  translation step exists at dispatch time: `protocol`, `java-client`, `os-input`, `pixel`,
  `server`, `human`.
- **Human-authenticated-session rows (D-27b):** a row whose Steps can only be exercised through
  the maintainer's own authenticated UltiCloud panel session carries the fixed Preconditions
  phrase `maintainer-authenticated UltiCloud panel session (personal credentials)` and Layer
  `human`, ending at `human-uat-pending` by design. This repository has no panel-capability rows
  of its own (it is a plain external Bukkit plugin with no UltiCloud/panel integration at all —
  that surface belongs to `UltiToolsPlugin` modules, not to a plugin connected only through the
  External Plugin API), so no row below is currently affected; the convention is stated here for
  template consistency with the framework's checklist.
- **Expected** must name an observable truth — an exact chat line, a log line, a database row,
  an inventory slot — and never the words "it works".
- **Covers** back-references a Phase 9 GUI-excluded class name; left blank when no such class
  applies. This repository is not one of the nine modules in Phase 9's GUI-exclusion register
  (confirmed by reading
  `.planning/phases/09-module-ecosystem-readiness-and-test-coverage/gui-exclusions/` — no file
  named for this repository exists there), so every row below leaves `Covers` blank. This
  repository also has no GUI page class of its own (`FEATURES.md`'s own `gui`-Kind count is 0),
  so the question does not arise a second way either.
- A row whose Preconditions name a prior row must appear after that row in file order — asserted
  mechanically: for every row, every checklist ID cited in its Preconditions cell must have a
  strictly smaller line number in this file than the row citing it (sweep class 8, D-27a).
- **Config-per-file rule (D-06):** one checklist row per `@ConfigEntity`-annotated class or per
  shipped yml file, never one row per key. This repository ships zero `@ConfigEntity` classes and
  zero configuration yml files of its own (`plugin.yml` carries only Bukkit's own required
  plugin-descriptor fields, not operator-configurable content) — zero config-per-file rows exist
  below, and this is correct, not a missing section: see `FEATURES.md`'s `## Configuration`
  section for the same rule stated on the catalogue side.
- This repository's `plugin.yml` declares no `permissions:` section, and none of its five
  `@CmdMapping` sub-commands has an in-game i18n catalogue to check against (this example has no
  `lang/*.json` file at all — every player-facing string is a hardcoded English literal in
  `GreetCommand.java`/`GreetService.java`/`JoinListener.java`), so no `language: en` precondition
  is needed on any row below, unlike a module that ships its own `lang/zh.json`/`lang/en.json`
  pair with a non-English shipped default.
- **The same absent `permissions:` section also means every one of the five sub-commands is
  OP-only by Bukkit's own undeclared-node default**, since all five share the single class-level
  `@CmdExecutor(permission = "ultiext.greet")` node. Every row below whose Steps dispatch through
  a non-console player therefore states that player must be OP or hold `ultiext.greet`, stated in
  each row's own Preconditions cell rather than only here — an ordinary non-OP player dispatching
  any of these commands is rejected before the command body ever runs, which a Precondition that
  merely says "none" would let an executor discover only as a false failure. The one row that
  dispatches from the server console (`ultitools-example.greet.hello.neg-console`) is unaffected:
  Bukkit's console `CommandSender` always passes every permission check.

## Greeting

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultitools-example.greet.hello | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only | Run `/ultiext hello` as a player named `Tester` | Chat line reads exactly `Hello Tester! This message is from an external plugin using UltiTools-API.` | server | |
| ultitools-example.greet.hello.neg-console | none | Run `/ultiext hello` from the server console | Console line reads exactly `Hello Console! This message is from an external plugin using UltiTools-API.` — `GreetCommand#hello` substitutes the literal string `Console` for any non-`Player` sender, proving `@CmdTarget(BOTH)` genuinely admits console execution rather than merely declaring it | server | |
| ultitools-example.greet.info | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only | Run `/ultiext info` as a player | Chat/console line reads exactly `UltiTools External Example v1.0.0 - verifying @Service injection works!` | server | |

## Data Storage (External Plugin API)

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultitools-example.data.delvisitor | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; a visitor record named `probe` exists — run `/ultiext visit probe` first if none does | Run `/ultiext delvisitor probe` as a player, then run `/ultiext visitors` | Chat/console line reads `[DATA] Deleted visitor record for probe`; the subsequent `/ultiext visitors` no longer lists `probe` | server | |
| ultitools-example.data.delvisitor.neg-not-found | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; no visitor record named `ghost` exists (delete it first via `ultitools-example.data.delvisitor` if a prior dispatch created one, or use a name never visited in this session) | Run `/ultiext delvisitor ghost` as a player | Chat/console line STILL reads `[DATA] Deleted visitor record for ghost` — `GreetCommand#delVisitor` calls `DataOperator#del` unconditionally and reports success regardless of whether a matching row ever existed; this is the row's actual assertion, not a bug workaround | server | |
| ultitools-example.data.visit | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; no visitor record named `probe` currently exists (run `ultitools-example.data.delvisitor` first if one does) | Run `/ultiext visit probe` as a player | Chat/console line reads `[DATA] Created record for probe — first visit!` | server | |
| ultitools-example.data.visit.neg-repeat | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; a visitor record named `probe` exists with visit count 1 (run `ultitools-example.data.visit` immediately before this row, in the same dispatch) | Run `/ultiext visit probe` as a player a second time, then run `/ultiext visitors` | The first command's chat/console line reads `[DATA] Updated probe — visit #2`; the subsequent `/ultiext visitors` lists `probe` EXACTLY ONCE with `2 visits` — proving the SAME record's count incremented rather than a second, duplicate `probe` row being inserted alongside the first | server | |
| ultitools-example.data.visitors | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; at least one visitor record exists (run `ultitools-example.data.visit` first) | Run `/ultiext visitors` as a player | Chat/console starts with `[DATA] === Visitor Records (<n>) ===` where `<n>` matches the actual stored count, followed by one `  <name> — <count> visits` line per record | server | |
| ultitools-example.data.visitors.neg-empty | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; zero visitor records exist (delete every record created earlier in this dispatch via `ultitools-example.data.delvisitor`) | Run `/ultiext visitors` as a player | Chat/console shows exactly `[DATA] No visitor records.` — no header line, no record lines | server | |
| ultitools-example.data.persistence | the executing player is OP, or has been granted `ultiext.greet` — this plugin declares no `permissions:` section in `plugin.yml`, so Bukkit's own undeclared-node default is OP-only; a visitor record named `restarttest` exists with visit count 1 (run `ultitools-example.data.visit` with `restarttest` first); the player who runs the post-restart query below is named something OTHER than `restarttest` (or the query is run from the server console) — `JoinListener#onJoin` auto-increments a visitor record matching the JOINING player's own name, so a player named `restarttest` rejoining after the restart would increment the very record this row checks BEFORE `/ultiext visitors` ever runs, producing a false persistence failure | Stop the server completely (a full clean shutdown, not `/ul reload`), start it again, then run `/ultiext visitors` as a player NOT named `restarttest` (or from the console) | `restarttest` is still listed with visit count 1, unchanged by the restart or by any other player's own join — `UltiToolsAPI.getDataOperator`'s backing store persisted the record to disk (JSON/SQLite/MySQL per the framework's own `datasource.type`), independent of this plugin's own in-memory state, which is rebuilt from scratch on every `onEnable` | server | |

## Player Join

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultitools-example.join.on-join | no visitor record exists yet for the joining player's name | The player joins the server for the first time in this session | The player receives the chat line `Hello <name>! This message is from an external plugin using UltiTools-API.` (identical to `/ultiext hello`'s own line, substituting the real player name), and a subsequent `/ultiext visitors` (run by any sender holding `ultiext.greet`) lists that player's name with visit count 1, without the player having run any command themselves | server | |
| ultitools-example.join.on-join.neg-rejoin | a visitor record already exists for the joining player's name, with visit count `N` (the player joined once already earlier in this same dispatch, per `ultitools-example.join.on-join`) | The same player quits and rejoins | The player again receives the join greeting; a subsequent `/ultiext visitors` shows that player's visit count as `N + 1`, NOT reset to 1 and NOT a second entry for the same name — `JoinListener#onJoin` looks up the existing record by name and increments it, mirroring `/ultiext visit`'s own repeat-visit behaviour | server | |
