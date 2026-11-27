# UltiTools External Plugin Example

A minimal standalone `JavaPlugin` using the [UltiTools External Plugin API](https://github.com/UltiKits/UltiTools-Reborn). This example targets **UltiTools-API 7.0**; it is not an UltiTools module.

## What This Demonstrates

| Feature | File | What it proves |
|---------|------|----------------|
| `UltiToolsAPI.connect(this)` | `UltiToolsExtExample.java` | One-line framework integration |
| `@Service` + `@Autowired` | `GreetService.java` | IoC container auto-scan & injection |
| `Messages` | `src/main/resources/messages/`, `GreetService.java` | Plugin-owned replies in English and Chinese |
| `@CmdExecutor` | `GreetCommand.java` | Auto-registered commands with tab completion |
| `@EventListener` | `JoinListener.java` | Auto-registered Bukkit event listeners |
| `DataOperator` CRUD | `GreetCommand.java` + `VisitorRecord.java` | Data storage scoped to plugin's own folder |

## Quick Start

### 1. Add UltiTools-API dependency

```xml
<dependency>
    <groupId>com.ultikits</groupId>
    <artifactId>UltiTools-API</artifactId>
    <version>7.0.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Until the framework release is available, enable Maven Central snapshots in your project:

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```

When UltiTools-API `7.0.0` is released, change the dependency to `7.0.0` and remove the snapshots repository if nothing else uses it.

### 2. Declare UltiTools dependency in plugin.yml

```yaml
name: MyPlugin
depend: [UltiTools]
```

### 3. Connect during enable and disconnect during disable

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        UltiToolsAPI.connect(this);
    }

    @Override
    public void onDisable() {
        UltiToolsAPI.disconnect(this);
    }
}
```

After `connect()`, UltiTools automatically scans your plugin's package for:
- `@Service` — registered as beans in a child IoC container
- `@CmdExecutor` — compiled into Brigadier command trees and registered through Paper’s commands lifecycle event
- `@EventListener` — Bukkit listeners auto-registered
- `@Autowired` — dependency injection for scanned beans; the external plugin instance itself can also be injected

An external plugin connected during normal startup participates in the initial commands event. A new command shape connected after startup waits for the next `/minecraft:reload` (data-pack reload) or restart. `/ul reload` reloads framework/module configuration without reconnecting this plugin; it does not replace JARs. Restart to install or update the plugin rather than using Bukkit `/reload`.

### 4. Use DataOperator for persistent storage

```java
// Get a DataOperator scoped to your plugin's data folder
DataOperator<MyEntity> data = UltiToolsAPI.getDataOperator(this, MyEntity.class);

// CRUD operations
data.insert(entity);
data.getAll();
data.update(entity);
data.del(WhereCondition.builder().column("name").value("test").build());
```

## Messages and overrides

Bundle `messages/en.yml` and `messages/zh.yml` directly under `messages/` at the root of your JAR (in this Maven project, use `src/main/resources/messages/`). The example declares `source-language: en` in `plugin.yml`. Once connected, scanned beans receive a `Messages` instance bound to this plugin. Use relative keys like `messages.key("greet.hello")` and pass values with `Arg.string` or `Arg.number`; let `Messages.send` select the language. With `language: auto` in UltiTools, each player sees their client language if available, and the console uses the server fallback language. Startup and shutdown logs remain in English.

To customize a reply, copy only the keys you want to change to `plugins/UltiToolsExtExample/messages/en.yml` or `zh.yml`. After connection, the bundled files are copied to `plugins/UltiToolsExtExample/messages/reference/` for reference; editing those copies does not change replies. Run `/ultiext reload` to apply overrides without restarting. It calls `UltiToolsAPI.reloadMessages(plugin)`; `/ul reload` does not reread external plugin overrides. Malformed override files are ignored with a console warning, and the bundled messages remain available as fallback.

## Project Structure

```
src/main/java/com/example/ultitoolsext/
├── UltiToolsExtExample.java          # Main plugin class — connect/disconnect
├── commands/
│   └── GreetCommand.java             # @CmdExecutor with @Autowired + DataOperator CRUD
├── data/
│   └── VisitorRecord.java            # @Table data entity (BaseDataEntity)
├── listeners/
│   └── JoinListener.java             # @EventListener with @Autowired
└── services/
    └── GreetService.java             # @Service bean
```

## Commands

| Command | Description |
|---------|-------------|
| `/ultiext help` | List sub-commands |
| `/ultiext hello` | Greet message (tests @Service injection) |
| `/ultiext info` | Plugin info |
| `/ultiext visit <name>` | Record a visit (tests insert + update) |
| `/ultiext visitors` | List all visitors (tests getAll) |
| `/ultiext delvisitor <name>` | Delete a visitor (tests delete) |
| `/ultiext reload` | Reload this plugin’s message overrides |

Use `/uext` as an alias. The console or a player with `ultiext.greet` can run these commands. Players receive the greeting and have their own visit recorded when they join, regardless of command permission.

## Building

Use JDK 25 and Maven:

```bash
mvn -U clean verify
```

Requires UltiTools-API `7.0.0` or later on the server.

With the server stopped, install `target/UltiTools-External-Example-1.0.0.jar` in `plugins/` alongside UltiTools (not in the UltiTools modules directory), then start the server.

## Documentation

- [External Plugin API Guide (EN)](https://dev.ultikits.com/guide/advanced/external-plugin-api.html)
- [外部插件 API 指南 (中文)](https://dev.ultikits.com/zh/guide/advanced/external-plugin-api.html)
- [UltiTools-API Framework](https://github.com/UltiKits/UltiTools-Reborn)

## License

MIT
