package cn.arqiuowo.origolds.api;

import java.util.Objects;
import java.util.UUID;

import net.minecraft.network.chat.Component;

/**
 * 一条播报来源的句柄。
 *
 * <p>附属模组应保存句柄并在每次内容变化时调用 {@link #show(Component)}。同一个句柄更新文字
 * 不会改变其在合并结果中的顺序；调用 {@link #stop()} 后再次显示会作为新的播报重新排队。</p>
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

    /** 更新文字并作为持续显示信息参与合并。 */
    public TextBroadcast show(Component message) {
        TextBroadcastService.activate(this, Objects.requireNonNull(message, "message"), true);
        return this;
    }

    /**
     * 更新文字并作为暂时滞留信息显示。
     *
     * <p>滞留信息与持续信息参与同一套合并规则：只要同一信息栏里有两个或更多信息在显示，
     * 就按开始顺序用 {@code " | "} 拼接，不再被持续信息直接覆盖。不需要时调用 {@link #stop()}
     * 清除，合并结果里对应的 {@code " | "} 也会一并移除。</p>
     */
    public TextBroadcast showRetained(Component message) {
        TextBroadcastService.activate(this, Objects.requireNonNull(message, "message"), false);
        return this;
    }

    /** 停止这条播报；之后再次调用 {@link #show(Component)} 会重新取得开始顺序。 */
    public void stop() {
        TextBroadcastService.deactivate(this);
    }

    /** 返回这条句柄所属的信息栏。 */
    public TextBroadcastChannel channel() {
        return channel;
    }

    UUID playerId() {
        return playerId;
    }

    /** 返回附属模组为此来源指定的稳定标识。 */
    public String ownerId() {
        return ownerId;
    }

    long sequence() {
        return sequence;
    }

    Component message() {
        return message;
    }

    boolean isActive() {
        return active && message != null;
    }

    void activate(Component message, boolean persistent, long sequence) {
        this.message = message;
        this.persistent = persistent;
        this.sequence = sequence;
        this.active = true;
    }

    boolean hasSameMode(boolean persistent) {
        return active && this.persistent == persistent;
    }

    void deactivate() {
        this.active = false;
        this.message = null;
        this.persistent = false;
        this.sequence = Long.MIN_VALUE;
    }
}
