# 文本播报接口

包名：`cn.arqiuowo.origolds.api`

## 用法

附属模组为每个玩家和每个播报来源保存一个 `TextBroadcast`，不要每 tick 重复创建来源标识：

```java
private static final String SOUL_CHAIN = "origolds_ga:soul_chain";

TextBroadcastService.show(
        player,
        TextBroadcastChannel.PLAYER_INFO,
        SOUL_CHAIN,
        Component.literal("魂锁剩余 0:10"));
```

持续播报每次更新文字时调用 `show` 即可。首次开始显示的来源排在前面，同一信息栏里所有正在显示的信息自动使用 ` | ` 拼接：

```text
aaaaa | bbbaa | ccd
```

停止来源：

```java
TextBroadcastService.stop(player, TextBroadcastChannel.PLAYER_INFO, SOUL_CHAIN);
```

## 合并规则

- `show(...)` 是持续播报，`showRetained(...)` 是暂时滞留播报（例如蓄力层数衰退提示）。
- 两者参与**同一套**合并规则：只要同一信息栏里有两个或更多信息在显示，就按开始顺序
  用 ` | ` 拼接，不存在“滞留信息被覆盖”的行为。
- 某条信息停止后，它和对应的 ` | ` 会一起从合并结果里移除；不需要时调用 `stop` 清除。
- 同一来源连续更新不会改变顺序；停止后再次 `show` 会重新排到队尾。
- `PLAYER_INFO` 和 `BOSS_INFO` 是两个独立信息栏，附属模组应按原本用途选择对应枚举值。

## 两个信息栏的渲染位置

| 信息栏 | 位置 | 渲染方 |
| --- | --- | --- |
| `PLAYER_INFO` | 原版 action bar（快捷栏上方那行） | `origolds` 服务端直接写入 |
| `BOSS_INFO` | 玩家信息栏正上方一行（origolds_ga 原本播报「源脉幽魂暴露出了对近战攻击的弱点」的位置） | `origolds` 客户端渲染层 |

附属模组只需要按上面的方式 `show` / `stop`，**不需要自己渲染 BOSS 信息栏**：
`origolds` 会合并 `BOSS_INFO` 队列并同步到客户端，画在玩家信息栏上方那一行。
两个信息栏共用同一套合并规则，但来源与顺序互相独立。
