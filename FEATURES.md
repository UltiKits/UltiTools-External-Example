# UltiTools External Plugin Example — Feature Inventory

This standalone `JavaPlugin` declares `depend: [UltiTools]` and uses `UltiToolsAPI.connect(this)` / `disconnect(this)`. It is not an UltiTools module. The external connection scans the sample's `@Service`, `@CmdExecutor` and `@EventListener` beans. The commands use Paper's Brigadier lifecycle; the join handler receives ordinary Bukkit events, not UltiTools EventBus events.

## Conventions

- **ID:** stable `<repo-slug>.<area>.<action>`; this repository uses `ultitools-example`. Rows are sorted by ID in each section.
- **Kind:** `command`, `config`, `event`, `gui`, `scheduled`, `placeholder`, `persistence` or `gate`. `help` is a framework-supplied mapping invoking the sample's help handler, not an additional `@CmdMapping`.
- **Permission:** all command paths share `ultiext.greet` and target players and console (`@CmdTarget(BOTH)`). The sample does not declare a default for the node; an ungranted player without operator status is denied on an otherwise unmodified server.
- **Tier:** `player` for the greeting, info, help and join demonstrations; `internal` for sample storage exercises. **Manual** is `none` for this developer-facing sample.
- **Source:** `ClassName#member` names the sample implementation. Sample replies are literal English strings. Framework-generated diagnostics (such as missing-argument usage) can be localized separately.

### Reconciliation

| Annotation | Sites | Source |
|---|---:|---|
| `@CmdExecutor` | 1 | `GreetCommand`, with labels `ultiext` and `uext` |
| `@CmdMapping` | 5 | `GreetCommand#hello`, `#info`, `#visit`, `#visitors`, `#delVisitor` |
| `@EventListener` | 1 | `JoinListener`, with the Bukkit `@EventHandler` method `onJoin` |
| `@Table` | 1 | `VisitorRecord`, table `visitor_records` |
| `@Scheduled` | 0 | No scheduled task |
| `@ConfigEntity` | 0 | No sample configuration class |
| `@ConfigEntry` | 0 | No sample configuration key |
| `@ConditionalOnConfig` | 0 | No configuration-gated registration |

There are no GUI or placeholder components. All five declared command mappings have one corresponding row below, in addition to the inherited help path, join handler and restart persistence.

## Greeting

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultitools-example.greet.hello | Greet the player by name or address a non-player sender as `Console`, using the injected `GreetService` | command | `/ultiext hello` (alias `/uext hello`) | ultiext.greet | both | player | none | GreetCommand#hello, GreetService#greet |
| ultitools-example.greet.help | List the five sample subcommands | command | `/ultiext help` | ultiext.greet | both | player | none | GreetCommand#handleHelp |
| ultitools-example.greet.info | Show the example's version and confirm service injection | command | `/ultiext info` | ultiext.greet | both | player | none | GreetCommand#info, GreetService#info |

## Data Storage (External Plugin API)

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultitools-example.data.delvisitor | Delete by name and print the same confirmation whether or not a record existed | command | `/ultiext delvisitor <name>` | ultiext.greet | both | internal | none | GreetCommand#delVisitor |
| ultitools-example.data.persistence | Store named visits through the external plugin's scoped data operator; a clean restart reconnects and reads the stored records | persistence | `ultiext visit <name>`, clean stop and start, `ultiext visitors` | n/a | n/a | internal | none | UltiToolsExtExample#onEnable, GreetCommand#visit, GreetCommand#visitors, VisitorRecord#VisitorRecord |
| ultitools-example.data.visit | Create a record with count 1, or increment its count and update its last-visit timestamp | command | `/ultiext visit <name>` | ultiext.greet | both | internal | none | GreetCommand#visit, VisitorRecord#incrementVisit |
| ultitools-example.data.visitors | List all records by name and visit count, or report the empty store | command | `/ultiext visitors` | ultiext.greet | both | internal | none | GreetCommand#visitors |

## Player Join

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultitools-example.join.on-join | Greet the joining player and create or increment that player's visit record without running a command | event | Join the server | n/a | n/a | player | none | JoinListener#onJoin, GreetService#greet, VisitorRecord#incrementVisit |

## Configuration

No sample configuration is shipped. `plugin.yml` is the server plugin descriptor, not a user settings file. The framework selects the data backend; the sample obtains its `VisitorRecord` operator with `UltiToolsAPI.getDataOperator(this, VisitorRecord.class)`.
