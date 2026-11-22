# Carpet Neko Addition [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE) ![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-2ea043) ![Side](https://img.shields.io/badge/side-%E7%BA%AF%E6%9C%8D%E5%8A%A1%E7%AB%AF-informational)

本分支由 **OvOSSK** 移植到 **Minecraft 1.20.1 Fabric**，同步上游 1.0.18。原作者 Akirono 的署名和 MIT 许可保留。

纯服务端 Carpet 扩展，面向技术生存与生电。全部规则默认关闭，客户端可为原版。

## 规则

| 规则 | 默认 | 说明 |
| --- | --- | --- |
| `autoMending` | `false` | 每秒把一件带经验修补的物品修满 |
| `voidWarehouse` | `false` | 铁砧改名的工作台成为全服共享仓库，一种物品一格、数量无上限 |
| `voidWarehouseRestock` | `false` | 允许 litematica-printer 缺料时从仓库领一组（依赖 `voidWarehouse`） |
| `voidWarehouseDirectPlace` | `false` | 打印机缺料时从仓库预扣一件并直接放置（依赖 `voidWarehouse` + `voidWarehouseRestock`） |
| `voidWarehouseExchange` | `false` | 成品缺货时用 N 合 1 配方的原料顶替（依赖 `voidWarehouse`） |
| `voidWarehouseCraft` | `false` | 成品缺货时按学过的配方扣原料（依赖 `voidWarehouse`） |
| `voidWarehouseBlockDrops` | `false` | 玩家挖掉的方块掉落物直接进仓库（依赖 `voidWarehouse`） |
| `concretePowderToGlass` | `false` | 普通熔炉把 16 种混凝土粉末烧成染色玻璃 |

用法 `/carpet <规则> true`，`/carpet list` 查看全部；分类 `neko` / `survival`。

### 虚空仓库速览

铁砧把**工作台**改名为 `虚空仓库`，手持右键开界面；潜行右键方块可摆成终端。一种物品一格、数量无上限，全服共享、跨维度、重启不丢。左键取 1 个、右键取一组；拿起物品后左键点击内容区可直接存入，也支持 Shift 快捷存入。仓库终端本身不能放进仓库。第 45 格切「合成页」，按已学过的配方扣原料现做。漏斗朝终端=入库、终端压漏斗=出料，双向自动化。

装 [neko-voidwarehouse-search](https://github.com/OvOSSK/neko-voidwarehouse-search) 可叠加搜索框、快捷键开仓、`Ctrl+Alt` 填满背包。

## 依赖

| 名称 | 类型 | 说明 |
| --- | --- | --- |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 必需 | Fabric 运行所需 API |
| [Carpet](https://modrinth.com/mod/carpet) | 必需 | 规则框架，使用 1.20.1 的 1.4.112 版 |
| [litematica-printer](https://github.com/OvOSSK/litematica-printer) | 可选 | 虚空仓库补货 / 直接放置的客户端 |
| [neko-voidwarehouse-search](https://github.com/OvOSSK/neko-voidwarehouse-search) | 可选 | 搜索框、快捷键开仓、填满背包手势 |

## 版本支持

仅支持 **Minecraft 1.20.1 Fabric**。需要 Java 17、Fabric Loader 0.16.14 或更新版本，以及 1.20.1 对应的 Fabric API。

Carpet 使用 1.4.112。Neko Client 和 Hana 打印机分别发布，本模组不包含它们。

## 安装

1. 服务端装 **Fabric Loader ≥ 0.16.14**，`mods/` 放 **Fabric API** + **Carpet** + 本模组（`carpet-Neko-Addition-1.0.18-mc1.20.1.jar`）。
2. 启动后用 `/carpet <规则> true` 逐条开启。
3. 客户端可原版直连；要用补货 / 直放另装 `litematica-printer`，要搜索框另装 `neko-voidwarehouse-search`。

从 [Releases](https://github.com/OvOSSK/Neko-Carpet-Addition/releases/latest) 下载产物。

## 构建

使用 **JDK 17、Gradle 8.5**（已提供 wrapper），在仓库根目录执行：

```bat
.\gradlew.bat build
```

Linux/macOS 使用 `./gradlew build`。产物位于 `build/libs/`，安装不带 `sources` 的 JAR。修改说明见 [PORT-NOTICE.txt](PORT-NOTICE.txt)，测试范围见 [验证记录](VALIDATION-2026-11-22.txt)。

## 文档

- [更新日志](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/866c72a5b91f44f45db3038d0a79d876de0410f3/CHANGELOG.md)

## 许可证

MIT，完整文本见 [LICENSE](LICENSE)。

## 反馈

请通过 [GitHub Issues](https://github.com/OvOSSK/Neko-Carpet-Addition/issues) 反馈。
