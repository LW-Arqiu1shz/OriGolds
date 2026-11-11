package cn.arqiuowo.origolds.api;

/**
 * 金源工艺文本播报的逻辑信息栏。
 *
 * <p>两个信息栏分别维护自己的播报队列；一个信息栏中的内容不会参与另一个信息栏的合并。
 * 合并规则完全相同：同一信息栏里所有正在显示的信息都按开始顺序用 {@code " | "} 拼接。</p>
 */
public enum TextBroadcastChannel {
    /** 玩家身边物品、技能等使用的 action bar 信息栏。 */
    PLAYER_INFO,

    /**
     * BOSS 战斗 / BOSS 事件使用的信息栏。
     *
     * <p>位置在玩家信息栏（action bar）正上方一行，即附属模组 origolds_ga 原本播报
     * 「源脉幽魂暴露出了对近战攻击的弱点」等信息的那一行。附属模组只管调用
     * {@code TextBroadcastService.show/stop}，渲染由本模组负责。</p>
     */
    BOSS_INFO
}
