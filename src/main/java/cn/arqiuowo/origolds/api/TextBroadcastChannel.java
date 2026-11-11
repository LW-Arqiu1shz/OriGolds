package cn.arqiuowo.origolds.api;

/**
 * 文本广播通道。附属模组通过通道选择文本要落在屏幕的哪一块。
 *
 * <p>该枚举自 1.1.1 起对外提供，是 {@link TextBroadcastService} 接口的一部分，
 * 常量名与顺序都不可随意改动（附属模组按名字调用）。</p>
 */
public enum TextBroadcastChannel {
    /** 动作栏（action bar）：由服务端每帧重发，多条会按提交顺序用 {@code " | "} 拼接。 */
    PLAYER_INFO,
    /** Boss 栏上方的独立一行：由服务端同步到客户端，自行绘制，不受动作栏互相覆盖的影响。 */
    BOSS_INFO
}
