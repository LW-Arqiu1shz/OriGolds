package cn.arqiuowo.origolds.api;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.network.chat.Component;

/**
 * 一条「某个玩家 × 某个通道 × 某个所有者」的文本广播句柄。
 *
 * <p>由 {@link TextBroadcastService#get(net.minecraft.world.entity.player.Player, TextBroadcastChannel, String)}
 * 取得并缓存，附属模组用它把自己的文本挂上去：</p>
 *
 * <ul>
 *   <li>{@link #show(Component)}：显示一段会自然到期的文本（动作栏默认 10 秒）；</li>
 *   <li>{@link #showRetained(Component)}：显示常驻文本，直到调用 {@link #stop()} 或被覆盖；</li>
 *   <li>{@link #stop()}：撤下自己这一段，不影响同通道上别的附属模组。</li>
 * </ul>
 *
 * <p>同一通道上多个所有者互不覆盖：服务端每 tick 把所有生效中的文本按首次提交的先后顺序
 * 用 {@code " | "} 连成一行，所以两个附属模组同时播报时都能看到。</p>
 */
public final class TextBroadcast {
    private final UUID playerId;
    private final TextBroadcastChannel channel;
    private final String ownerId;
    private Component message;
    private long sequence = Long.MIN_VALUE;
    private boolean persistent;
    private boolean active;

    TextBroadcast(UUID playerId, TextBroadcastChannel channel, String ownerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    }

    /** 播报一段会自行到期的文本。 */
    public TextBroadcast show(Component message) {
        TextBroadcastService.activate(this, Objects.requireNonNull(message, "message"), true);
        return this;
    }

    /** 播报一段常驻文本，直到 {@link #stop()} 或被新的文本覆盖。 */
    public TextBroadcast showRetained(Component message) {
        TextBroadcastService.activate(this, Objects.requireNonNull(message, "message"), false);
        return this;
    }

    /** 撤下自己这一段文本。 */
    public void stop() {
        TextBroadcastService.deactivate(this);
    }

    public TextBroadcastChannel channel() {
        return this.channel;
    }

    UUID playerId() {
        return this.playerId;
    }

    public String ownerId() {
        return this.ownerId;
    }

    long sequence() {
        return this.sequence;
    }

    Component message() {
        return this.message;
    }

    boolean isActive() {
        return this.active && this.message != null;
    }

    void activate(Component message, boolean persistent, long sequence) {
        this.message = message;
        this.persistent = persistent;
        this.sequence = sequence;
        this.active = true;
    }

    /** 显示模式没变时沿用原来的排序号，避免每帧刷新把这段文本顶到行尾。 */
    boolean hasSameMode(boolean persistent) {
        return this.active && this.persistent == persistent;
    }

    void deactivate() {
        this.active = false;
        this.message = null;
        this.persistent = false;
        this.sequence = Long.MIN_VALUE;
    }
}
