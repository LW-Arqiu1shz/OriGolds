# 金源工艺 · OriGolds

> 本分支 / This branch: **26.1.2NeoForge**

[中文](#中文) · [English](#english)

> Author / 作者：**Arqiu-owo** · Code: `LGPL-3.0-only` · Assets: `CC-BY-NC-SA-4.0` — see [LICENSE.md](LICENSE.md)

---

## 中文

让金制装备不再垃圾，让镀金大法重出江湖，让附魔苹果成功复活。让金子再次伟大！

### 功能

- **金装备重做**：金质剑、镐、斧、锹、锄的耐久提升到 520，攻击力上调；金镐的挖掘等级与铁镐相同。
  金盔甲耐久翻三倍，四件护甲值合计 18 点。
- **镀金系统**：
  - 用金源铸模 + 金块合成金源镀板，铸模可以重复使用；铸模逐级升级为复合金源铸模（钻石）、聚融金源铸模（下界合金碎片），对应产出更高级的镀板。
  - 魔金之核（金块 + 青金石块）可以拆成 4 个金镀锻造模板。
  - 在锻造台里用「金镀锻造模板 + 装备 + 镀板」依次镀上 **镀金 I / II / III**。武器、工具、弓弩、盔甲都能镀金，已有附魔的装备也可以。
- **镀金效果**：
  - 武器与工具：耐久上限、攻击力、挖掘速度提升（I/II/III 级分别 +25% / +50% / +100%）。
  - 弓与弩：耐久上限和箭矢伤害按同样比例提升。
  - 盔甲：镀金等级越高，耐久消耗越少（III 级只消耗原来的四分之一）；四件盔甲的镀金等级合计越高，受到的伤害越低。
  - 力量、锋利等一切加攻击的效果都会被镀金倍率一起放大。
  - 镀金物品带有可配置的金色呼吸光效。
- **洗去镀金**：手持镀金物品右键装水的炼药锅，消耗一格水降低一级镀金。
- **神果重生**：金苹果 + 魔金之核 = 附魔金苹果。
- **战斗调整**：
  - 近战暴击现在也会放大附魔伤害。
  - 在空中射出的箭伤害 ×1.5。
  - 猪灵蛮兵攻击力降低 1 点。
- **进度**：一整套金源工艺进度，从第一块金锭到镀金极意。
- **指令**：`/orimodify <目标> gild set|add|reduce <n>`，可以把手持物品的镀金等级直接改到 0～255（需要管理员权限）。

客户端配置文件位于 `config/origolds-client.toml`（镀金光效开关、渐变、颜色、透明度），也可以在 NeoForge 的模组列表里直接打开配置界面。
本模组添加了新物品，**客户端和服务端都需要安装**。

### 附属模组

金源工艺是以下附属模组的前置：《金源工艺：古金极锻》（OriGolds: Ancient Reforge）、《金源工艺：工业时代》（OriGolds: Industrialized Age）、《金源工艺：源金渴望》（OriGolds: Gilding Avarice）。
附属模组可以通过 [文本播报接口](TEXT_BROADCAST_API.md) 在快捷栏上方显示信息，多个来源会自动合并、互不覆盖。

### 支持的版本

| 分支 | Minecraft | 加载器 |
|---|---|---|
| [`1.21.1NeoForge`](../../tree/1.21.1NeoForge) | 1.21.1 | NeoForge 21.1.244+ |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.103+ |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.51-beta+ |

每个版本是仓库里的一个**分支**（`1.21.1NeoForge` 是默认分支，也是功能基准版本），分支根目录就是 Gradle 工程。
[CHANGELOG.md](CHANGELOG.md) 里有更新记录。

### 自己编译

需要 **Gradle 9.7+**（仓库里没有附带 Gradle Wrapper），以及 **JDK 21**（1.21.1）或 **JDK 25**（26.1.2 / 26.3）。切换到想要的版本分支，在根目录运行：

```
gradle build
```

jar 会生成在 `build/libs/` 里。

---

## English

Make golden gear worth using again, bring back the art of gilding, and revive the Enchanted Golden Apple. Make Gold Great Again!

### Features

- **Golden gear rework**: golden swords, pickaxes, axes, shovels and hoes get 520 durability and more attack damage;
  the golden pickaxe mines like an iron one. Golden armor has three times the durability and 18 armor points as a full set.
- **Gilding system**:
  - Craft an Origold Coatboard from an Origold Mold and a gold block; the mold is kept and can be reused.
    Upgrade the mold to a Compound Origold Mold (diamonds) and a Polyfusive Origold Mold (netherite scrap) for higher-tier coatboards.
  - A Magigold Core (gold blocks + a lapis block) breaks down into 4 Origold Coating Smithing Templates.
  - In a smithing table, combine template + gear + coatboard to apply **Gilding I / II / III**. Weapons, tools, bows, crossbows
    and armor can all be gilded, even if they are already enchanted.
- **Gilding effects**:
  - Weapons and tools: more max durability, attack damage and mining speed (+25% / +50% / +100% for I/II/III).
  - Bows and crossbows: max durability and arrow damage scale the same way.
  - Armor: the higher the gilding, the less durability it loses (a quarter of normal at III); the higher the combined
    gilding of your four armor pieces, the less damage you take.
  - Strength, Sharpness and every other attack bonus are scaled by the gilding multiplier too.
  - Gilded items get a configurable golden "breathing" glow.
- **Washing off gilding**: right-click a water cauldron with a gilded item to use one level of water and remove one gilding level.
- **Revival of the God Apple**: Golden Apple + Magigold Core = Enchanted Golden Apple.
- **Combat tweaks**:
  - Melee critical hits now also multiply enchantment damage.
  - Arrows shot while airborne deal ×1.5 damage.
  - Piglin Brutes deal 1 less damage.
- **Advancements**: a full OriGolds advancement tree, from your first gold ingot to Gilding Mastery.
- **Command**: `/orimodify <targets> gild set|add|reduce <n>` sets the held item's gilding level anywhere from 0 to 255 (operators only).

Client config: `config/origolds-client.toml` (glow on/off, gradient, color, opacity); a config screen is also available from the NeoForge mod list.
This mod adds new items, so it **must be installed on both the client and the server**.

### Add-ons

OriGolds is required by its add-ons: OriGolds: Ancient Reforge, OriGolds: Industrialized Age and OriGolds: Gilding Avarice.
Add-ons can show messages above the hotbar through the [text broadcast API](TEXT_BROADCAST_API.md) (in Chinese);
messages from several sources are merged instead of overwriting each other.

### Supported versions

| Branch | Minecraft | Loader |
|---|---|---|
| [`1.21.1NeoForge`](../../tree/1.21.1NeoForge) | 1.21.1 | NeoForge 21.1.244+ |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.103+ |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.51-beta+ |

Each version lives on its own **branch** (`1.21.1NeoForge` is the default branch and the reference version for features);
the branch root is the Gradle project. See [CHANGELOG.md](CHANGELOG.md) for the changelog.

### Building

Requires **Gradle 9.7+** (no Gradle Wrapper is included) and **JDK 21** (1.21.1) or **JDK 25** (26.1.2 / 26.3).
Check out the branch for the version you want and run, in the repository root:

```
gradle build
```

The jar is written to `build/libs/`.

---

## 贡献者 / Contributors

- **Arqiu-owo** — 设计、美术、文案、调试 / Design, art, writing and testing
- **Claude**（Anthropic）— 程序、项目管理 / Programming and project management
- **DeepSeek** — 程序、项目管理 / Programming and project management

## License / 许可证

- Code / 代码：[GNU LGPL v3.0](COPYING.LESSER)（with [GPL v3.0](COPYING)）
- Assets (images) / 素材（图片）：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)

Details / 详情：[LICENSE.md](LICENSE.md)
