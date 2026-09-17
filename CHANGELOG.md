# Changelog

All notable changes to this project are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

本文件记录本项目的所有重要更改，格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)。

## [Unreleased]

### Fixed

- The `/ultiext` (alias `/uext`) sub-commands `hello`, `info`, `visit`, `visitors` and `delvisitor`
  register again on UltiTools-API 6.3.0. `GreetCommand` now extends `BaseCommandExecutor`;
  it previously extended `AbstractCommandExecutor`, which 6.3.0 removed, so the plugin loaded and
  its join listener worked, but every `/ultiext` sub-command got Bukkit's
  `Unknown or incomplete command` reply (UltiKits/UltiTools-External-Example#4).
- `/ultiext`（别名 `/uext`）的 `hello`、`info`、`visit`、`visitors`、`delvisitor` 子命令在
  UltiTools-API 6.3.0 上重新注册。`GreetCommand` 现继承 `BaseCommandExecutor`；此前它继承的
  `AbstractCommandExecutor` 已在 6.3.0 中移除，因此插件虽能加载、进服监听器也正常，但所有 `/ultiext`
  子命令都只返回 Bukkit 的 `Unknown or incomplete command` 提示（UltiKits/UltiTools-External-Example#4）。

### Changed

- Migrated to UltiTools-API 6.3.0 and Paper: the example now builds against
  `UltiTools-API 6.3.0-SNAPSHOT` and `paper-api 1.21.11-R0.1-SNAPSHOT` instead of UltiTools-API 6.2.2
  and `spigot-api 1.20.4-R0.1-SNAPSHOT` (UltiKits/UltiTools-External-Example#4).
- 迁移到 UltiTools-API 6.3.0 与 Paper：示例改为基于 `UltiTools-API 6.3.0-SNAPSHOT` 与
  `paper-api 1.21.11-R0.1-SNAPSHOT` 构建，不再使用 UltiTools-API 6.2.2 与 `spigot-api 1.20.4-R0.1-SNAPSHOT`
  （UltiKits/UltiTools-External-Example#4）。
