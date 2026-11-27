# FieldJournal

An UltiTools **module** for recording exploration sites and keeping a short note per player. It is independent of the standalone Paper plugin example at the repository root.

## Build and install

Run `mvn -B -ntp clean verify` in this directory with JDK 25. The `pom.xml` uses UltiTools-API `7.0.0-SNAPSHOT` from Maven Central snapshots and Paper API as `provided` dependencies. The JAR contains the module's descriptor and catalogues, not the server or framework.

On a stopped server running UltiTools 7.0, put `target/FieldJournal-1.0.0.jar` in `plugins/UltiTools/plugins/`, not directly in `plugins/`, then start the server. The root `plugin.yml` declares the module API level as the integer `700`.

## Use the journal

All commands are in-game only and require `fieldjournal.use`. On a fresh server with no permissions plugin, use an operator to try them.

| Command | What it does |
| --- | --- |
| `/fieldjournal record <site>` | Records `forest`, `river`, or `mountain` (Tab suggests them). Increments your count and replaces the latest site without changing your note. |
| `/fieldjournal status` | Reads your count, latest site, and note from module storage. |
| `/fieldjournal note` | Opens a native dialog. Submit a short note after recording a site; closing the dialog does not save. |
| `/fieldjournal book` | Opens a chest screen. The centre book shows your saved journal in chat and closes the screen. |

The module uses the framework's selected data backend; no separate database credentials are needed for the default SQLite setup. With the framework's `language: auto`, replies and UI labels follow the player's client language (English or Chinese). Site tokens in the command remain English.

## Configure and reload

Edit `plugins/UltiTools/pluginConfig/FieldJournal/config/journal.yml`, then run `/ul reload`:

- `journal-name` defaults to `Field Journal`. It must not be blank or longer than 40 characters. A valid change appears the next time you open a journal screen; a refused change keeps the previous running value.
- `reminder-seconds` defaults to `60`. Use a positive integer (up to `107374182`). Both the initial delay and the interval use this setting. A successful reload updates the running schedule; an out-of-range setting keeps the previous timing. The reminder appears in the action bar for online players with `fieldjournal.use`.

The early hook contributes `fieldjournal:trail_mark` (`Trail Mark`), a level-one enchantment for a diamond shovel. The demo enchantment has no additional gameplay effect. The contribution is made on server start, not on `/ul reload`.

See [UAT-CHECKLIST.md](UAT-CHECKLIST.md) for steps to verify the module on Paper. Unit tests cover module behavior and public API arguments; they do not start a Paper server.

## 中文速览

用 JDK 25 运行 `mvn -B -ntp clean verify`，在服务器停止时将 `target/FieldJournal-1.0.0.jar` 放入 `plugins/UltiTools/plugins/`，再启动服务器。需要 UltiTools 7.0。

持有 `fieldjournal.use` 权限的玩家可用 `/fieldjournal record <site>` 记录地点、`status` 查询、`note` 打开表单保存笔记、`book` 打开箱子界面查看记录。框架设为 `language: auto` 时，中文客户端会收到中文消息。修改 `config/journal.yml` 后运行 `/ul reload` 可更新界面名称和提醒间隔；引导期注册的附魔只在启动时贡献。

## License

MIT
