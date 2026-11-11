package cn.arqiuowo.origolds.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import cn.arqiuowo.origolds.network.BossInfoPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 面向附属模组的文本播报服务（1.1.1 起提供）。
 *
 * <p>服务端每 tick 汇总一次 {@link TextBroadcastChannel} 上生效中的文本：</p>
 *
 * <ul>
 *   <li>{@link TextBroadcastChannel#PLAYER_INFO} 直接以动作栏下发
 *       （26.3 里仍是 {@code Player#sendOverlayMessage}，等价于 1.21.1 的
 *       {@code displayClientMessage(message, true)}）；</li>
 *   <li>{@link TextBroadcastChannel#BOSS_INFO} 通过 {@link BossInfoPayload} 同步到客户端，
 *       由 {@code OrigoldsBossInfoOverlay} 画在动作栏正上方，避免和动作栏抢同一行。</li>
 * </ul>
 *
 * <p>典型用法（附属模组侧）：</p>
 *
 * <pre>{@code
 * TextBroadcast line = TextBroadcastService.get(player, TextBroadcastChannel.PLAYER_INFO, "origolds_ia")
 *         .showRetained(Component.literal("源金产能 12/s"));
 * // 不再需要时
 * line.stop();
 * }</pre>
 *
 * <p>全部入口都在服务端调用（玩家所在的服务端实例），文本会由本服务负责持续下发，
 * 附属模组不需要自己每帧发包。</p>
 */
public final class TextBroadcastService {
    /** 同一通道上多段文本之间的连接符。 */
    private static final String SEPARATOR = " | ";
    private static final Map<UUID, PlayerState> PLAYERS = new LinkedHashMap<>();
    private static long nextSequence;

    private TextBroadcastService() {
    }

    /**
     * 取（或创建）这个玩家、这个通道、这个所有者对应的广播句柄。
     *
     * @param player  服务端玩家（也接受 {@link Player}，附属模组从事件里拿到的对象可直接传入）
     * @param channel 显示通道
     * @param ownerId 所有者标识，建议用附属模组自己的 modid；同一个玩家同一通道下不同所有者互不覆盖
     */
    public static synchronized TextBroadcast get(Player player, TextBroadcastChannel channel, String ownerId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(ownerId, "ownerId");
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        Map<String, TextBroadcast> entries = state.broadcasts
                .computeIfAbsent(channel, ignored -> new LinkedHashMap<>());
        return entries.computeIfAbsent(ownerId, ignored -> new TextBroadcast(player.getUUID(), channel, ownerId));
    }

    /** 一步播报一段会自行到期的文本。 */
    public static TextBroadcast show(Player player, TextBroadcastChannel channel, String ownerId, Component message) {
        return get(player, channel, ownerId).show(message);
    }

    /** 一步播报一段常驻文本。 */
    public static TextBroadcast showRetained(Player player, TextBroadcastChannel channel, String ownerId, Component message) {
        return get(player, channel, ownerId).showRetained(message);
    }

    /** 撤下某个所有者的文本；参数为空时静默返回，方便附属模组在清理阶段直接调用。 */
    public static synchronized void stop(Player player, TextBroadcastChannel channel, String ownerId) {
        if (player == null || channel == null || ownerId == null) {
            return;
        }
        PlayerState state = PLAYERS.get(player.getUUID());
        if (state == null) {
            return;
        }
        TextBroadcast broadcast = state.broadcasts.getOrDefault(channel, Map.of()).get(ownerId);
        if (broadcast != null) {
            broadcast.stop();
        }
        prune(state);
    }

    /**
     * 服务端关闭时清空全部播报状态。单人游戏退出世界后 JVM 不会重启，静态表里残留的句柄
     * 会带进下一个存档（同一个玩家 UUID），这里统一清理。
     */
    public static synchronized void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        PLAYERS.clear();
    }
    /** 当前拼好的整行文本，供附属模组读取（例如写进日志或自己的界面）。 */
    public static synchronized Optional<Component> combined(Player player, TextBroadcastChannel channel) {
        if (player == null || channel == null) {
            return Optional.empty();
        }
        PlayerState state = PLAYERS.get(player.getUUID());
        if (state == null) {
            return Optional.empty();
        }
        return currentMessage(state.broadcasts.getOrDefault(channel, Map.of()));
    }

    static synchronized void activate(TextBroadcast broadcast, Component message, boolean persistent) {
        long sequence;
        if (broadcast.hasSameMode(persistent)) {
            // 同一模式的重复提交不重新排队，保持原来的显示顺序。
            sequence = broadcast.sequence();
        } else {
            sequence = nextSequence;
            nextSequence = sequence + 1L;
        }
        broadcast.activate(message, persistent, sequence);
    }

    static synchronized void deactivate(TextBroadcast broadcast) {
        broadcast.deactivate();
    }

    /** 由 {@code OriGolds} 注册到游戏总线：每 tick 把汇总结果下发给对应玩家。 */
    public static synchronized void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, PlayerState>> iterator = PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerState> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                iterator.remove();
                continue;
            }
            refresh(player, entry.getValue());
            prune(entry.getValue());
        }
    }

    /** 由 {@code OriGolds} 注册到游戏总线：玩家退出后丢掉状态，防止跨存档串文本。 */
    public static synchronized void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYERS.remove(event.getEntity().getUUID());
    }

    private static void refresh(ServerPlayer player, PlayerState state) {
        // 动作栏：每 tick 重发，文本因此能常驻而不被原版 10 秒淡出。
        Optional<Component> message = currentMessage(state.broadcasts.getOrDefault(TextBroadcastChannel.PLAYER_INFO, Map.of()));
        message.ifPresent(value -> player.sendOverlayMessage(value));

        // Boss 信息：只在内容真的变了的时候同步，避免每帧发包。
        Component bossInfo = currentMessage(state.broadcasts.getOrDefault(TextBroadcastChannel.BOSS_INFO, Map.of()))
                .orElse(null);
        if (!state.bossInfoSynced || !Objects.equals(bossInfo, state.lastBossInfo)) {
            state.bossInfoSynced = true;
            state.lastBossInfo = bossInfo;
            PacketDistributor.sendToPlayer(player, new BossInfoPayload(Optional.ofNullable(bossInfo)));
        }
    }

    private static Optional<Component> currentMessage(Map<String, TextBroadcast> entries) {
        List<TextBroadcast> active = new ArrayList<>();
        for (TextBroadcast broadcast : entries.values()) {
            if (broadcast.isActive()) {
                active.add(broadcast);
            }
        }
        if (active.isEmpty()) {
            return Optional.empty();
        }
        active.sort(Comparator.comparingLong(TextBroadcast::sequence));
        return Optional.of(join(active));
    }

    private static Component join(List<TextBroadcast> broadcasts) {
        MutableComponent result = broadcasts.get(0).message().copy();
        for (int i = 1; i < broadcasts.size(); ++i) {
            result.append(Component.literal(SEPARATOR));
            result.append(broadcasts.get(i).message());
        }
        return result;
    }

    private static void prune(PlayerState state) {
        for (Map<String, TextBroadcast> entries : state.broadcasts.values()) {
            entries.values().removeIf(broadcast -> !broadcast.isActive());
        }
        state.broadcasts.values().removeIf(Map::isEmpty);
    }

    private static final class PlayerState {
        private final Map<TextBroadcastChannel, Map<String, TextBroadcast>> broadcasts =
                new EnumMap<>(TextBroadcastChannel.class);
        private boolean bossInfoSynced;
        private Component lastBossInfo;
    }
}
