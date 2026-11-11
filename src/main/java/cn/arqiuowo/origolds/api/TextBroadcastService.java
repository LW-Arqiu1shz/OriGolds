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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 通用文本播报接口。
 *
 * <p>附属模组为每个玩家、信息栏和来源保存一个句柄。同一信息栏里所有正在显示的信息
 * （无论持续显示还是暂时滞留）都按首次开始显示的顺序用 {@code " | "} 合并，不再互相覆盖；
 * 某条信息停止后，它和对应的分隔符会一起从合并结果里移除。每个信息栏独立维护自己的队列。</p>
 *
 * <p>{@link TextBroadcastChannel#PLAYER_INFO} 由服务端直接写进原版 action bar；
 * {@link TextBroadcastChannel#BOSS_INFO} 合并后经 {@code BossInfoPayload} 同步给客户端，
 * 由客户端渲染层画在玩家信息栏正上方一行，两个信息栏各自独立合并、互不干扰。</p>
 */
public final class TextBroadcastService {
    private static final String SEPARATOR = " | ";
    private static final Map<UUID, PlayerState> PLAYERS = new LinkedHashMap<>();
    private static long nextSequence;

    private TextBroadcastService() {
    }

    /** 获取或创建一个指定玩家、信息栏和来源的播报句柄。 */
    public static synchronized TextBroadcast get(Player player, TextBroadcastChannel channel, String ownerId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(ownerId, "ownerId");
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        PlayerState state = PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        Map<String, TextBroadcast> entries = state.broadcasts.computeIfAbsent(channel, ignored -> new LinkedHashMap<>());
        return entries.computeIfAbsent(ownerId, ignored -> new TextBroadcast(player.getUUID(), channel, ownerId));
    }

    /** 以玩家为目标开始或更新一条持续播报。 */
    public static TextBroadcast show(Player player, TextBroadcastChannel channel, String ownerId, Component message) {
        return get(player, channel, ownerId).show(message);
    }

    /** 以玩家为目标显示一条滞留播报；与持续播报一样参与 {@code " | "} 合并。 */
    public static TextBroadcast showRetained(Player player, TextBroadcastChannel channel, String ownerId,
            Component message) {
        return get(player, channel, ownerId).showRetained(message);
    }

    /** 停止指定来源在指定信息栏中的播报。 */
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

    static synchronized void activate(TextBroadcast broadcast, Component message, boolean persistent) {
        long sequence = broadcast.hasSameMode(persistent) ? broadcast.sequence() : nextSequence++;
        broadcast.activate(message, persistent, sequence);
    }

    static synchronized void deactivate(TextBroadcast broadcast) {
        broadcast.deactivate();
    }

    /** 在服务端 tick 中刷新玩家信息栏；由模组入口注册。 */
    public static synchronized void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Map.Entry<UUID, PlayerState>> iterator = PLAYERS.entrySet().iterator(); iterator.hasNext();) {
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

    /** 玩家退出时清理其播报句柄，避免 UUID 状态泄漏到下一次登录。 */
    public static synchronized void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYERS.remove(event.getEntity().getUUID());
    }

    /**
     * 服务端关闭时清空全部播报状态。单人游戏退出世界后 JVM 不会重启，静态表里残留的句柄
     * 会带进下一个存档（同一个玩家 UUID），这里统一清理。
     */
    public static synchronized void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        PLAYERS.clear();
    }
    /** 返回指定玩家信息栏的最终文本；没有持续或滞留信息时返回空。 */
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

    private static void refresh(ServerPlayer player, PlayerState state) {
        Optional<Component> message = currentMessage(
                state.broadcasts.getOrDefault(TextBroadcastChannel.PLAYER_INFO, Map.of()));
        message.ifPresent(value -> player.displayClientMessage(value, true));

        // BOSS 信息栏没有对应的原版数据包，合并结果变化时推给客户端由渲染层画在
        // 玩家信息栏上方一行；首次刷新必定推送，用于清掉上一个服务器残留的文本。
        Component bossInfo = currentMessage(
                state.broadcasts.getOrDefault(TextBroadcastChannel.BOSS_INFO, Map.of())).orElse(null);
        if (!state.bossInfoSynced || !Objects.equals(bossInfo, state.lastBossInfo)) {
            state.bossInfoSynced = true;
            state.lastBossInfo = bossInfo;
            PacketDistributor.sendToPlayer(player, new BossInfoPayload(Optional.ofNullable(bossInfo)));
        }
    }

    private static Optional<Component> currentMessage(Map<String, TextBroadcast> entries) {
        List<TextBroadcast> active = new ArrayList<>();
        for (TextBroadcast broadcast : entries.values()) {
            // 持续播报和滞留播报一律参与合并：只要同一信息栏里有两个或更多信息在显示，
            // 就用 " | " 拼接，不再让持续信息直接覆盖滞留信息。
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
        for (int i = 1; i < broadcasts.size(); i++) {
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
        private final EnumMap<TextBroadcastChannel, Map<String, TextBroadcast>> broadcasts =
                new EnumMap<>(TextBroadcastChannel.class);
        private boolean bossInfoSynced;
        private Component lastBossInfo;
    }
}
