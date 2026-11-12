# Carpet Neko Addition

> **1.20.1 Fabric 移植版** · 移植维护：OvOSSK · 原作者：Akirono
> [原项目](https://github.com/YoshinoShizuka/Neko-Carpet-Addition) · [移植说明](PORT-NOTICE.txt)

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-2ea043)
![Side](https://img.shields.io/badge/side-%E7%BA%AF%E6%9C%8D%E5%8A%A1%E7%AB%AF-informational)

纯服务端 Carpet 扩展，面向技术生存与生电场景。共八条规则：**自动经验修补**、**虚空仓库**（全服共享、跨维度的分页仓库，配漏斗双向自动化）、**虚空仓库补货**、**虚空仓库直接放置**、**虚空仓库 9 合 1 兑换**、**虚空仓库配方扣料**、**方块掉落物直接进虚空仓库**、**混凝土粉末烧制染色玻璃**。全部默认关闭，用 `/carpet` 逐条开启。客户端可以是原版。

另有配套**纯客户端模组** [neko-voidwarehouse-search](https://github.com/OvOSSK/neko-voidwarehouse-search)：在虚空仓库界面上叠加搜索框，支持中文 / 拼音 / 注册名筛选物品，详见下文「[虚空仓库搜索框](#虚空仓库搜索框配套客户端-mod)」。

## 规则

| 规则 | 默认 | 一句话 |
| --- | --- | --- |
| `autoMending` | `false` | 每秒把一件带经验修补的物品修满，先花经验条、再动等级 |
| `voidWarehouse` | `false` | 铁砧改名的工作台成为全服共享仓库，一种物品一格、数量无上限 |
| `voidWarehouseRestock` | `false` | 允许 litematica-printer 缺料时直接从仓库领一组（**依赖** `voidWarehouse`） |
| `voidWarehouseDirectPlace` | `false` | 打印机缺料时从仓库预扣一件、直接放到目标位置，不经过背包（**依赖** `voidWarehouse` + `voidWarehouseRestock`） |
| `voidWarehouseExchange` | `false` | 成品缺货时改用 N 合 1 配方的原料顶替，也可拆上级找零（**依赖** `voidWarehouse`） |
| `voidWarehouseCraft` | `false` | 成品缺货时按玩家亲手合成过的配方扣原料，直接把成品取出来（**依赖** `voidWarehouse`） |
| `voidWarehouseBlockDrops` | `false` | 玩家挖掉的方块，掉落物不进世界、直接进仓库（**依赖** `voidWarehouse`） |
| `concretePowderToGlass` | `false` | 普通熔炉把 16 种混凝土粉末烧成对应颜色的染色玻璃 |

分类为 `neko` / `survival`，用法 `/carpet <规则> true`，`/carpet list` 查看全部规则。

### 虚空仓库

- 铁砧把**工作台**重命名为 `虚空仓库`（精确匹配、区分大小写），**手持右键**任意目标即可开界面
- 要把它当终端摆到世界里，**潜行 + 右键方块**；之后右键那个方块就能开仓库，不用再拿在手上
- **一种物品 = 一格，数量无上限**（内部 `BigInteger`，真任意精度）；页数随物品种类增长，也没有上限
- 全服唯一一份、跨维度共享（存在主世界 `SavedData`），重启不丢；排列按创造模式物品栏顺序，与存取历史无关
- 左键取 1 个 / 右键取一组，**都直接进背包**；格子里只画图标，真实数量看鼠标提示
- **直接存入（1.20.1 移植版）**：拿起普通物品后，左键点击仓库物品区即可存入，仍支持 Shift 快捷存入；仓库终端本身不能存入。
- 导航行：`47` 潜影盒模式（**图标关 = 红盒、开 = 绿盒**；取出给装满的盒、存入把盒拆开）、`48` / `50` 翻页、`49` 页码、`51` 数量排序
- **漏斗双向自动化**：朝终端摆放的漏斗 = 入库（无速率上限，玩家手动放过的物品成为白名单，分类物每格留 1 个）；**终端压在漏斗正上方** = 出料（按漏斗 5 格里的分类物发货，分类物永不外流）
- **入库筛选**：只要非空就收。带 NBT 数据的物品（受损工具、附魔、改名、药水、成书）**不拒收**，各占一格；要不要放行某个物品由玩家在漏斗里自己过滤（分类物白名单）
- **不可堆叠物**（水桶 / 矿车 / 床 / 空潜影盒…）：**出料已支持** —— 标本留在格子里当分类物，货走「仓库 → 下游」直连；**入库还不能拿它当分类物**，因为格子只装得下 1 个、标本占住就腾不出位置（不带分类物的漏斗照常收这类物品）。三条候选方案见 `docs/入库漏斗支持不可堆叠物品-路线对照.md`

### 虚空仓库 9 合 1 兑换

- 只在**成品本身没货**时才兑换：仓库里有铁锭就老实扣铁锭，一个原料都不动
- 扣减优先级：目标物品 → 族内更小的单位（下级合成）→ 族内更大的单位（拆整块，需要时多拆一块并找零退库）→ 单向边的上游
- **绝不刷物**：每次成功兑换都满足 `Σ(数量 × 价值) == 需要数 × 目标价值`，自检逐笔校验（含随机 fuzz）
- 比例是每条边自己的属性，引擎里没有写死 9：上游配置共 47 条候选边，1.20.1 按该版本实际存在的物品和配方复核（含 4:1 的 2×2 配方与 3:1 的骨粉 ← 骨头）
- 只走**原版真实存在的配方**：启动时用 `RecipeManager` 反查复核，两个物品缺一即丢边、单边缺失则降级；`/reload` 重算

### 虚空仓库配方扣料

- **先学会，才能用**：在合成台（含背包 2×2）里成功合成一次，这次用到的原料与产物就被记进**配方本**
- 之后取这件物品时若**成品没货**，直接按记下的配方扣仓库里的原料，把成品给你 —— 等价于替你做掉这一步合成
- **原料不够会往下钻**（最多 4 层）：取活塞要 3 块木板、木板没货但仓库里有原木，就扣原木做木板 —— 只在**已学过的配方**之间钻，不会凭空出现你没做过的东西
- 配方本**全服共用一份**（和仓库一样），配方随存档持久化，重启不丢
- 和 9 合 1 的分工：9 合 1 只会原版那几十条 9:1 / 4:1 的换算；本规则能覆盖**任意**配方（活塞、漏斗、侦测器……）
- 优先级：真实库存 → 9 合 1 可兑 → 配方可合成。同一件物品**不会同时出现两格**
- 界面里标「（可合成）」，鼠标提示写清「可合成 N 个，用 圆石 64 个 ＋ …」；一次动作能拿多少与真账同源，不会出现「说 64 实给 63」
- 特殊配方（染色、烟火、地图缩放、成书复制等产物带 NBT 数据的）**不记录** —— 学会了也只会给一件白板物品

### 方块掉落物直接进虚空仓库

- 玩家**挖掉**的方块，掉落物不进世界，直接进仓库 —— 不占背包格子，也不受背包满不满影响
- 掉落表、附魔、时运、精准采集**全部照原版算**，算出几个就收几个（拦的是掉落物的唯一出口 `ServerLevel#addFreshEntity`，不是某个方块）
- 入库口径与漏斗完全一致：空潜影盒整盒占一格、带 NBT 数据的按物品 + NBT归并
- **聊天栏会说一声**：`[虚空仓库] 挖到 3 个 泥土（仓库共 128 个）、12 个 圆石（仓库共 64 个）` —— 与补货提示同一套配色（灰正文 + 亮绿数字）。除了挖到几个，还报**收完之后仓库里一共有多少**（现读，口径 = 物品 + NBT）；非空潜影盒只标「已拆开」不报库存、折成「N 种共 M 个」时也不报。同一种物品合并报数；一条最多列 4 种；同一玩家**每秒最多一条**，连挖不会刷屏
- **只认玩家挖的**：TNT、苦力怕、活塞拆的、怪物掉落的、玩家丢的都照原样进世界；经验球不是物品，也不收
- **绝不吞物品**：仓库没开、存档读不出、入库失败、任何异常 —— 一律照原样掉在地上
- 关着的时候连上下文都不立，`addFreshEntity` 的额外代价只剩一次 `null` 读

### 虚空仓库补货

给 [litematica-printer](https://github.com/OvOSSK/litematica-printer) 用的通道（`carpet_neko_addition:printer_query`）：建造缺料时直接从共享仓库领一组。

**默认关是有意的** —— 开着就等于「任何装了对应打印机的玩家都能动用全服共享库存」，这个授权该由服主主动打开。

### 虚空仓库直接放置

补货的下一步：打印机缺料时**不再把一整组塞进背包**，而是从仓库预扣**一件**、直接放到目标位置。手上不会出现物品，打印完背包里也不会剩一堆零头。

- **零净变化的预扣**：流程是「从仓库预扣 1 件 → 临时塞进当前快捷栏格 → 走原版 `BlockItem#place` → 没放成的原样还回仓库」（`Withdrawal#returnUnplaced`）。结束之后快捷栏那一格与背包跟放置前完全一致
- **走的是原版放置原语**，不是自己往世界里写方块 —— 朝向、半砖上下、楼梯拐角、栅栏连接、含水状态、红石灯亮灭、铁轨的 `shape` / `powered` / `waterlogged` 全部按原版算
- **半砖合并**：对着底/顶台阶点缺失的那一面，一件预扣物直接给出双台阶（与手动放完全同理）
- **结构化状态自己算，不依赖 Servux**：方块属性由本模组按语义属性表推出，装了 Servux 就借它的解码器交叉验证。**没装 Servux 也能用** —— 之前缺了它会整条回退成「补货到背包、手上放」，一条直放都发不出去
- **不接的方块**：床 / 门 / 双格植物 / 潜影盒（一件预扣物放不出目标状态，或会额外消耗掉玩家自己的东西）；以及任何不是「裸 `BlockItem`」的物品（水桶、船、矿车……原版放置会改容器或产出多件）
- **和手动放一样受规矩约束**：非生存模式、`mayBuild` 为假、界面没关、手臂够不着、目标位置已被别人改过（`expectedStateId` 不符）—— 任一条不满足就整笔撤回，**仓库零消耗**
- **扣料会报一声**：`[虚空仓库] 放置消耗 64 个 石头（仓库剩余 998213 个）`，与取货提示同一套配色（灰正文 + 亮绿数字）。同一玩家**每秒最多一条**、同一种物品合并累加、一条最多列 4 种（再多折成「N 种共 M 个」）—— 上游直放实测峰值 **34 次/秒**（非本移植版性能结论），一次一条会把聊天栏刷穿
- 默认 `false`，**依赖** `voidWarehouse` + `voidWarehouseRestock`。客户端要装 [litematica-printer](https://github.com/OvOSSK/litematica-printer)，并在其配置里打开 `voidWarehouseDirectPlace`（与 `voidWarehouseRestock` 配套使用）

### 混凝土粉末烧制染色玻璃

16 种混凝土粉末在**普通熔炉**里烧成对应颜色的染色玻璃，数值与原版「沙子 → 玻璃」一致（0.1 经验 / 200 tick）。高炉与烟熏炉不在覆盖范围内。

规则名**故意**与 Carpet DDS Addition 的 `smeltConcretePowderToGlass` 不同：Carpet 的 `SettingsManager#addCarpetRule` 遇重名会直接抛异常，沿用原名意味着「两个模组同装 = 服务端起不来」。

### 虚空仓库搜索框（配套客户端 mod）

[neko-voidwarehouse-search](https://github.com/OvOSSK/neko-voidwarehouse-search) 是独立的**纯客户端** Fabric mod：在虚空仓库界面（一个 6 行原版大箱子）上方叠加一个搜索框，输入**中文名 / 拼音全拼 / 拼音首字母 / 注册名**都能筛选物品——`铁锭`、`tieding`、`td`、`iron_ingot` 均可命中，清空输入框即恢复完整列表。

- **服务端界面零改动**：GUI 布局、导航行、点击语义与原版完全一致，只是「第 n 页装谁」按过滤结果变了；不装它时一切照旧，纯原版玩家零感知
- **匹配在客户端做**（只有客户端有中文语言文件），服务端只按物品身份集合求交；过滤是会话级状态，关掉界面即消失，不写存档
- 物品种类超过一页时最有用：不用翻页找，一个 `td` 直达全部铁质物品
- 本移植版支持 MC 1.20.1，按客户端版本从其 [Releases](https://github.com/OvOSSK/neko-voidwarehouse-search/releases/latest) 下载；旧服务端上搜索框自动隐藏，纯原版客户端零影响

## 文档

- [更新日志](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/CHANGELOG.md)

以下链接指向原项目文档，其中的版本号和构建方法可能与本分支不同：
- [多版本架构说明](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/多版本架构说明.md)
- [虚空仓库 9 合 1 兑换](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E8%99%9A%E7%A9%BA%E4%BB%93%E5%BA%93%209%20%E5%90%88%201%20%E5%85%91%E6%8D%A2.md)
- [虚空仓库配方扣料](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E8%99%9A%E7%A9%BA%E4%BB%93%E5%BA%93%E9%85%8D%E6%96%B9%E6%89%A3%E6%96%99.md)
- [方块掉落物直接进虚空仓库](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E6%96%B9%E5%9D%97%E6%8E%89%E8%90%BD%E7%89%A9%E7%9B%B4%E6%8E%A5%E8%BF%9B%E8%99%9A%E7%A9%BA%E4%BB%93%E5%BA%93.md)
- [混凝土粉末烧制染色玻璃规则移植](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E6%B7%B7%E5%87%9D%E5%9C%9F%E7%B2%89%E6%9C%AB%E7%83%A7%E5%88%B6%E6%9F%93%E8%89%B2%E7%8E%BB%E7%92%83%E8%A7%84%E5%88%99%E7%A7%BB%E6%A4%8D.md)
- [投影材料列表显示虚空仓库库存](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E6%8A%95%E5%BD%B1%E6%9D%90%E6%96%99%E5%88%97%E8%A1%A8%E6%98%BE%E7%A4%BA%E8%99%9A%E7%A9%BA%E4%BB%93%E5%BA%93%E5%BA%93%E5%AD%98.md)
- [功能审计与纯服务端可行性评估](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E5%8A%9F%E8%83%BD%E5%AE%A1%E8%AE%A1%E4%B8%8E%E7%BA%AF%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%8F%AF%E8%A1%8C%E6%80%A7%E8%AF%84%E4%BC%B0.md)
- [出料漏斗支持不可堆叠物品](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E5%87%BA%E6%96%99%E6%BC%8F%E6%96%97%E6%94%AF%E6%8C%81%E4%B8%8D%E5%8F%AF%E5%A0%86%E5%8F%A0%E7%89%A9%E5%93%81.md)
- [入库漏斗支持不可堆叠物品 · 路线对照](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E5%85%A5%E5%BA%93%E6%BC%8F%E6%96%97%E6%94%AF%E6%8C%81%E4%B8%8D%E5%8F%AF%E5%A0%86%E5%8F%A0%E7%89%A9%E5%93%81-%E8%B7%AF%E7%BA%BF%E5%AF%B9%E7%85%A7.md)
- [仓库终端不能存进仓库](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E4%BB%93%E5%BA%93%E7%BB%88%E7%AB%AF%E4%B8%8D%E8%83%BD%E5%AD%98%E8%BF%9B%E4%BB%93%E5%BA%93.md)
- [中键取物被当成打印余料返还](https://github.com/YoshinoShizuka/Neko-Carpet-Addition/blob/11224f0b49c3a23731a76e2fa20e711331f78979/docs/%E4%B8%AD%E9%94%AE%E5%8F%96%E7%89%A9%E8%A2%AB%E5%BD%93%E6%88%90%E6%89%93%E5%8D%B0%E4%BD%99%E6%96%99%E8%BF%94%E8%BF%98.md)

## 安装

1. 服务端装好 **Fabric Loader ≥ 0.16.14**（Minecraft 1.20.1）
2. 把下面三个 jar 放进 `mods/`：
   - **Fabric API**（1.20.1 版，必需）
   - **Carpet 1.4.112**（Minecraft 1.20.1）
   - **本模组**，选择 `carpet-Neko-Addition-1.0.17-mc1.20.1.jar`（从 [Releases](https://github.com/OvOSSK/Neko-Carpet-Addition/releases/latest) 下载）
3. 启动后用 `/carpet <规则> true` 逐条开启
4. 客户端可以原版直连；要用打印机补货 / 直接放置，客户端另装 `litematica-printer` 并在它的配置里打开 `voidWarehouseRestock` / `voidWarehouseDirectPlace`，服务端开对应的那几条规则
5. 想要搜索框，客户端另装 [neko-voidwarehouse-search](https://github.com/OvOSSK/neko-voidwarehouse-search)（按客户端 MC 版本选 jar）；不装也不影响任何服务端功能

> 旧版 `carpet-automending`（包 `carpetaddition`）的仓库数据不兼容，升级相当于换一个新仓库。

## 支持版本

本分支只支持 **Minecraft 1.20.1 Fabric**。

| MC 版本 | 工程 | 工具链 |
| --- | --- | --- |
| 1.20.1 | 仓库根目录 | Fabric Loom 1.5.8 + Mojang 官方映射，Java 17 |

搜索附加模组与 Hana 打印机在各自 fork 独立发布，不包含在本模组 JAR 中。

## 构建

- **JDK 17**
- **Gradle 8.5**（wrapper 已固定，无需另装）

```bat
.\gradlew.bat build
```

Linux/macOS 使用 `./gradlew build`。产物位于 `build/libs/`，安装不带 `sources` 的 JAR。开发时可运行 `./gradlew runServer`。

已测过仓库存取、搜索和打印机联动。大型投影、复杂方块和多人服还没测全，详见 [测试记录](VALIDATION-2026-11-13.txt)。

## 许可证

MIT，完整文本见 [LICENSE](LICENSE)。

## 反馈

请通过 [GitHub Issues](https://github.com/OvOSSK/Neko-Carpet-Addition/issues) 反馈。
