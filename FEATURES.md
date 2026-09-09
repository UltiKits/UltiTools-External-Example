# UltiTools External Plugin Example — Feature Inventory

This document catalogues every operator- or player-visible function, command, content item and
configuration key in this repository, as read directly from source. It is an internal reference
for UAT execution and issue reconciliation — the public description of these features lives on
<https://doc.ultikits.com/>. Update this file in the same pull request as any feature change.

This repository is not a plugin module in the usual sense — it is a worked, buildable example
demonstrating how a **plain Bukkit `JavaPlugin`** (one that does NOT extend `UltiToolsPlugin`)
integrates with the framework through the External Plugin API (`UltiToolsAPI.connect(this)`,
`UltiToolsAPI.getDataOperator(...)`, `UltiToolsAPI.getEventBus()`,
`UltiToolsAPI.disconnect(this)`). Its five `@CmdMapping` commands and one `@EventListener` class
exist specifically to exercise that API surface end to end, including the one place in the
eighteen repositories this phase covers where the External Plugin API's own `DataOperator` data
path is exercised on a real server.

## Conventions

- **ID grammar:** `<repo-slug>.<area>.<action>`, dot-separated, every segment lowercase ASCII
  drawn from `[a-z0-9-]`. `<repo-slug>` is the repository name lowercased with no separators; per
  this phase's fixed exception, `UltiTools-External-Example`'s repo-slug is `ultitools-example`
  (not the mechanically-derived `ultitoolsexternalexample`). `<area>` is the feature section's
  slug. `<action>` is the verb. An ID changes only when the feature's identity changes, never on
  rewording. IDs are unique within a repository. This repository has **no** `@ConfigEntity`
  class, so it has no config-shaped ID exception to apply — the `config` row shape described
  elsewhere in this convention (used by every other repository in this fan-out) has zero
  instances here, by design (see `## Configuration` below).
- **Kind**, exactly these eight values: `command`, `config`, `event`, `gui`, `scheduled`,
  `placeholder`, `persistence`, `gate`. Each maps one-to-one onto a reconciliation-table line.
  This repository has no `config` rows (zero `@ConfigEntity` classes — this example ships no
  configuration of its own, by design: it demonstrates the External Plugin API from a plain
  Bukkit plugin, not a configuration surface), no `gui` rows (no GUI page class), no `scheduled`
  rows (no `@Scheduled` method — this example does not demonstrate `TaskManager`), no
  `placeholder` rows (no PlaceholderAPI expansion), and no `gate` rows (no `@ConditionalOnConfig`
  site — there is no config to condition on) — five of the eight Kinds stay in the vocabulary for
  cross-repository consistency even though they appear zero times below.
- **Tier**, exactly three: `player`, `admin`, `internal`. Judged from what the feature is for.
  `hello` and `info` are harmless demonstrations any player could run. `visit`, `visitors`, and
  `delvisitor` are explicitly labeled "Data Storage Tests" in the source's own code comment — they
  exist to exercise `DataOperator` CRUD by hand, not to serve a real gameplay purpose, so they are
  `internal`. All five share the same class-level permission node (`ultiext.greet`, undeclared in
  `plugin.yml`, so OP-only by Bukkit's own default) — an ordinary non-OP player cannot currently
  reach the `internal`-tier commands, so no D-11 issue is filed for them.
- **Manual**, exactly three: `detailed`, `brief`, `none`. This repository's own features are
  developer-facing example code, not something doc.ultikits.com describes for end users — every
  row below is `none`, since there is nothing here for the public manual to expand.
- **Target**, exactly four: `player`, `console`, `both`, or `n/a`. This repository's one
  `@CmdExecutor` class carries `@CmdTarget(BOTH)` at the class level with no method-level
  override, so all five command rows below carry Target `both`.
- **Permission:** the literal node string, `none`, or `n/a`. This repository's one `@CmdExecutor`
  class does not set `requireOp = true`, so no row below carries that suffix; all five commands
  rely on the single literal node `ultiext.greet` alone. No permission node is declared in
  `plugin.yml` (no `permissions:` section exists), so Bukkit's own undeclared-permission default
  applies: OP-only.
- **Source:** `ClassName#member` — the class and member that actually implements the feature.
- **Row order:** by section, then by ID ascending within the section.
- **No manual prose:** no troubleshooting column, no explanatory paragraphs, no draft page text.

### Reconciliation command family

The canonical form for counting an annotation site across this repository's real sources:

```bash
find <repo-root> -path '*/src/main/java/*' -name '*.java' -not -path '*/target/*' \
  -not -path '*/.worktrees/*' -print0 | xargs -0 grep -nE '^[[:space:]]*@AnnotationName\b' | wc -l
```

This repository is a single-root Maven project (`src/main/java` only, 5 source files total),
carries no git worktree directory, and has no javadoc or string-literal mention of any of its own
annotation names — the naive (unanchored) and line-start counts are identical for every kind
measured below, but the anchored `find`/`grep` form is used regardless, so the same command is
trustworthy unmodified against every repository in the fan-out.

**Positive controls**, each confirmed by reading the cited line directly, not by trusting the
count alone:

| Annotation | Sites | Positive control |
|---|---|---|
| `@CmdExecutor` | 1 | `GreetCommand.java:21`, class-level, `alias = {"ultiext", "uext"}` |
| `@CmdMapping` | 5 | `GreetCommand.java:31` (`hello`), `:37` (`info`), `:44` (`visit <name>`), `:74` (`visitors`), `:93` (`delvisitor <name>`) |
| `@EventListener` | 1 | `JoinListener.java:16`, class-level, on `PlayerJoinEvent`'s handler class |
| `@Scheduled` | 0 | no background task exists anywhere in this repository's 5 source files — confirmed by reading all 5 in full |
| `@ConfigEntity` | 0 | this repository ships no configuration of its own, by design — it demonstrates the External Plugin API from a plain Bukkit plugin, not a configuration surface. Zero written, not omitted: this line is the phase's own live test of that rule |
| `@ConditionalOnConfig` | 0 | there is no configuration key to condition registration on — direct consequence of the `@ConfigEntity` line above |
| `@ConfigEntry` | 0 | same reason as `@ConfigEntity` — zero keys, because zero config classes |
| `@Table` | 1 | `VisitorRecord.java:13`, `@Table("visitor_records")` |

This document's command-row count (5) matches the `@CmdMapping` site count exactly (5 against 5).
The three `@ConfigEntity`/`@ConditionalOnConfig`/`@ConfigEntry` lines above all read 0 against 0
with a stated reason — this repository is the phase's own live instance of the rule that a zero
count is written, never silently omitted from the reconciliation table.

## Greeting

`GreetCommand` — class-level `@CmdExecutor(permission = "ultiext.greet", alias = {"ultiext",
"uext"})`, `@CmdTarget(BOTH)`. Backed by `GreetService`, an `@Service` bean injected via
`@Autowired`, demonstrating that this framework's IoC container auto-scans and wires beans for a
plain `JavaPlugin` connected through `UltiToolsAPI.connect(this)`, exactly as it does for a real
`UltiToolsPlugin` module.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultitools-example.greet.hello | Send a fixed greeting naming the sender (or `Console` for a non-player sender), proving `@Service`/`@Autowired` injection works for an externally-connected plugin | command | `/ultiext hello` (alias `/uext hello`) | ultiext.greet | both | player | none | GreetCommand#hello, GreetService#greet |
| ultitools-example.greet.info | Show a fixed plugin-info line naming this example and confirming `@Service` injection succeeded | command | `/ultiext info` (alias `/uext info`) | ultiext.greet | both | player | none | GreetCommand#info, GreetService#info |

## Data Storage (External Plugin API)

`GreetCommand`'s three data-manipulation sub-commands, explicitly labeled "Data Storage Tests" in
this class's own code comment — they exist to exercise `UltiToolsAPI.getDataOperator(this,
VisitorRecord.class)`'s CRUD surface by hand, not to serve real gameplay. `VisitorRecord`
(`@Table("visitor_records")`, extends `BaseDataEntity<String>`, current-generation API, not the
deprecated `AbstractDataEntity`) is this repository's one persisted entity, tracking a player
name, a visit count, and a last-visit timestamp.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultitools-example.data.delvisitor | Delete a visitor record by player name; reports success unconditionally regardless of whether a matching record actually existed — `DataOperator#del` is not checked for a prior existence, so deleting a name with no record produces the identical "[DATA] Deleted..." message as deleting one that did exist | command | `/ultiext delvisitor <name>` | ultiext.greet | both | internal | none | GreetCommand#delVisitor |
| ultitools-example.data.visit | Record a visit for the named player: creates a new `VisitorRecord` (visit count 1) if none exists for that name, or increments the existing record's visit count and updates its `last_visit` timestamp otherwise | command | `/ultiext visit <name>` | ultiext.greet | both | internal | none | GreetCommand#visit |
| ultitools-example.data.visitors | List every currently stored visitor record with its player name and visit count, or a "no records" message if the table is empty | command | `/ultiext visitors` | ultiext.greet | both | internal | none | GreetCommand#visitors |
| ultitools-example.data.persistence | Every `VisitorRecord` created or updated through `/ultiext visit` or an automatic on-join visit (see `## Player Join` below) survives a full server restart — `UltiToolsAPI.getDataOperator` resolves a `DataOperator` backed by the framework's own configured storage backend (JSON/SQLite/MySQL per the framework's `config.yml` `datasource.type`), scoped to this plugin's own data folder (or a `DataScope` if the connecting adapter supplies one), the same persistence guarantee any `UltiToolsPlugin` module's own `@Table` entities get — this is the only place in this phase's eighteen repositories where the External Plugin API's own data path, rather than a module's built-in one, is exercised end to end on a real server | persistence | run `/ultiext visit <name>`, then restart the server, then run `/ultiext visitors` | n/a | n/a | internal | none | VisitorRecord#VisitorRecord, UltiToolsExtExample#onEnable, ExternalPluginAdapter#getDataFolder |

## Player Join

`JoinListener` — `@EventListener` on a plain Bukkit `Listener` implementation, demonstrating that
the framework auto-registers a `@EventListener`-annotated class's Bukkit `@EventHandler` methods
for a plain `JavaPlugin` connected via `UltiToolsAPI.connect(this)`. Bundles two independently
observable behaviours in one handler: a chat greeting, and an automatic visit-record
creation/increment identical in effect to running `/ultiext visit <own-name>` by hand.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultitools-example.join.on-join | Send the joining player the same fixed greeting `/ultiext hello` produces, and automatically create or increment that player's `VisitorRecord` exactly as `/ultiext visit <own-name>` would, without the player running any command | event | join the server as any player | n/a | n/a | player | none | JoinListener#onJoin |

## Configuration

This repository has **zero** `@ConfigEntity` classes, **zero** `@ConfigEntry` keys, and **zero**
`@ConditionalOnConfig` sites — by design, not by omission. It demonstrates the External Plugin
API from a plain Bukkit `JavaPlugin`, and ships no configuration surface of its own; its
`plugin.yml` carries only the fixed fields Bukkit itself requires (`name`, `version`, `main`,
`api-version`, `description`, `authors`, `depend`), none of which is operator-configurable content
in the sense this document otherwise catalogues. This section intentionally carries no rows — see
the reconciliation table above for the explicit 0-against-0 lines this absence produces.
