# 移植注意事项 / Porting notes

1.21.1、26.1.2、26.3 三个版本分别在 `1.21.1NeoForge`、`26.1.2NeoForge`、`26.3NeoForge` 分支上，各自是独立的 Gradle 工程，功能以 1.21.1 为准。在版本之间同步改动时，下面这些位置需要按目标版本改写（以 26.1.2 → 26.3 为主，也适用于从 1.21.1 移植）。

> The three versions live on separate branches, each a standalone Gradle project; 1.21.1 is the feature reference. The notes below (in Chinese) list the spots that must be rewritten when syncing a change between versions.

几个版本不是简单的复制关系，移植时已知需要改动的位置：

1. **进度触发器包名**：`net.minecraft.advancements.criterion.*` 在 26.3 拆成
   `advancements.triggers.*` 与 `advancements.predicates.*`
   （见 26.3 分支的 `ModTriggers.java`、`GildingStateTrigger.java`）。
2. **`ContextAwarePredicate` 已删除**：26.3 里触发条件的 `player` 字段类型是
   `Optional<Holder<LootItemCondition>>`。
3. **F1 隐藏 HUD 的开关搬家**：`Options#hideGui` 取消，改为
   `minecraft.gui.hud.isHidden()`（见 `client/OrigoldsBossInfoOverlay.java`）。
4. **`pack.mcmeta` 的包格式号要跟着版本走**：26.1.2 是 `84 / 101`，
   26.3 是 `97 / 121`（分别对应该版本的资源包主版本与数据包主版本）。
5. **进度 JSON 是致命级校验**：26.3 把进度改成了数据包动态注册表，单个条目解码失败会
   让注册表加载整体崩溃（表现为新建世界卡在"准备创建世界"、进已有存档触发安全模式；
   26.1.2 里同样的错误只记日志跳过）。`minecraft:recipe_crafted` 的触发条件从 26.1.2 的
   `"recipe_id": "<id>"` 改为 26.3 必填的 `"recipes": "<id>"`（HolderSet 写法，见
   26.3 分支的 `advancement/origolds/revival_of_god_apple.json`）。移植时所有进度/配方
   JSON 都要对照目标版本原版数据文件核一遍字段名。
6. **属性译名 key 去掉了 `generic.` 前缀**：26.x 里攻击伤害属性的 description id 是
   `attribute.name.attack_damage`，1.21.x 及更早是 `attribute.name.generic.attack_damage`。
   本模组按这个 key 在提示框里定位「攻击伤害」行并改写成镀金后的数值，写死任何一版都会在
   另一版上静默失效（表现为镀金武器攻击力仍显示原始值，且不报错）。现在运行期取
   `Attributes.ATTACK_DAMAGE` 真实的 description id，并保留两个已知字面量兜底：注册表未
   bootstrap 时触碰 `Attributes` 抛的是 `ExceptionInInitializerError`（属 `Error` 不是
   `RuntimeException`），必须 `catch (Throwable)`，否则整条提示框都渲染不出来。
   另外 26.x 的属性提示改由 `ItemAttributeModifiers.Display`（`Default` / `Hidden` /
   `OverrideText`）渲染，基础攻击力行形如
   `CommonComponents.space().append(translatable("attribute.modifier.equals.N", 数值, 属性名))`
   ——真正的可译组件是行根的**兄弟节点**，所以匹配必须递归下探。
