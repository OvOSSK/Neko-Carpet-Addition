# Neko Carpet Addition — Fabric 1.20.1

由 **OvOSSK** 维护的 [Neko-Carpet-Addition](https://github.com/YoshinoShizuka/Neko-Carpet-Addition) 社区移植版。原作者 Akirono 的署名、版权及 MIT 许可证保持不变。本分支只构建 1.20.1。

## 安装

从本仓库 Releases 下载不含 sources 的 JAR，安装到 Fabric 1.20.1 的 mods 目录。需要 Fabric Loader 0.16.14+、1.20.1 Fabric API 和 Carpet 1.4.112。

[搜索附加模组](https://github.com/OvOSSK/neko-voidwarehouse-search)和 [Hana 打印机](https://github.com/OvOSSK/litematica-printer)分别发布，仅安装于客户端。搜索修复需要主模组和搜索模组配套更新。

用 `/carpet voidWarehouse true` 启用仓库，把工作台命名为 `Void Warehouse` 后右键打开。打印联动另需 `/carpet voidWarehouseRestock true`；直接放置需 `/carpet voidWarehouseDirectPlace true`。客户端也需启用相应功能。

普通物品可拿在鼠标上，左键点击仓库物品区直接存入；保留 Shift 快捷存入。仓库终端本身禁止存入。

## 构建与验证

Java 17，运行 `./gradlew build`（Windows：`gradlew.bat build`），内置 Gradle 8.5 wrapper。

9 项单元测试、独立服务端启动及单人存取、搜索、Hana 材料统计、补货打印、余料返还和直接放置已验证。详见 [验证记录](VALIDATION-2026-11-13.txt)。复杂方块、大型投影及远程多人场景尚未全面验证。

修改包括 Java 17、1.20.1 注册表/配方/NBT API、Mixin 和旧版 Fabric 网络适配，以及标题、无匹配搜索、快速重开搜索与存入同步修复。

## 许可与来源

原项目和移植修改按 [MIT](LICENSE) 分发。OvOSSK 署名仅表示移植维护贡献，不替代原作者，不主张上游名称、图标等资源所有权。本版本不是上游官方发布。见 [移植说明](PORT-NOTICE.txt)。
