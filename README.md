# UltiTools External Plugin Example

A minimal example showing how any standard Bukkit/Spigot/Paper plugin can integrate with [UltiTools-API](https://github.com/UltiKits/UltiTools-Reborn) using the External Plugin API introduced in **v6.2.2**.

## What This Demonstrates

| Feature | File | What it proves |
|---------|------|----------------|
| `UltiToolsAPI.connect(this)` | `UltiToolsExtExample.java` | One-line framework integration |
| `@Service` + `@Autowired` | `GreetService.java` | IoC container auto-scan & injection |
| `@CmdExecutor` | `GreetCommand.java` | Auto-registered commands with tab completion |
| `@EventListener` | `JoinListener.java` | Auto-registered Bukkit event listeners |
| `DataOperator` CRUD | `GreetCommand.java` + `VisitorRecord.java` | Data storage scoped to plugin's own folder |

## Quick Start

### 1. Add UltiTools-API dependency

```xml
<dependency>
    <groupId>com.ultikits</groupId>
    <artifactId>UltiTools-API</artifactId>
    <version>6.2.2</version>
    <scope>provided</scope>
</dependency>
```

### 2. Declare UltiTools dependency in plugin.yml

```yaml
name: MyPlugin
depend: [UltiTools]
```

### 3. Connect in onEnable

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        UltiToolsAPI.connect(this);  // That's it!
    }
}
```

After `connect()`, UltiTools automatically scans your plugin's package for:
- `@Service` — registered as beans in a child IoC container
- `@CmdExecutor` — commands auto-registered with Bukkit
- `@EventListener` — Bukkit listeners auto-registered
- `@Autowired` — dependency injection across all scanned classes

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
| `/ultiext hello` | Greet message (tests @Service injection) |
| `/ultiext info` | Plugin info |
| `/ultiext visit <name>` | Record a visit (tests insert + update) |
| `/ultiext visitors` | List all visitors (tests getAll) |
| `/ultiext delvisitor <name>` | Delete a visitor (tests delete) |

## Building

```bash
mvn clean package
```

Copy the JAR to your server's `plugins/` folder. Requires UltiTools-API 6.2.2+ on the server.

## Documentation

- [External Plugin API Guide (EN)](https://dev.ultikits.com/guide/advanced/external-plugin-api.html)
- [外部插件 API 指南 (中文)](https://dev.ultikits.com/zh/guide/advanced/external-plugin-api.html)
- [UltiTools-API Framework](https://github.com/UltiKits/UltiTools-Reborn)

## License

MIT
