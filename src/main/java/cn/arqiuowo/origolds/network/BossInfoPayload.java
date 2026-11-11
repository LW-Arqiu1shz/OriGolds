package cn.arqiuowo.origolds.network;

import java.util.Optional;

import cn.arqiuowo.origolds.OriGolds;
import cn.arqiuowo.origolds.client.OrigoldsBossInfoOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 把服务端合并后的 BOSS 信息栏文本同步到客户端渲染层。
 *
 * <p>服务端 {@link cn.arqiuowo.origolds.api.TextBroadcastService} 每 tick 合并 BOSS 信息栏内容，
 * 内容变化时用本负载推送；{@link Optional#empty()} 表示该栏当前没有任何内容，客户端随即清除。</p>
 */
public record BossInfoPayload(Optional<Component> message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BossInfoPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(OriGolds.MOD_ID, "boss_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossInfoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ComponentSerialization.OPTIONAL_STREAM_CODEC, BossInfoPayload::message,
                    BossInfoPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BossInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OrigoldsBossInfoOverlay.setMessage(payload.message().orElse(null)));
    }
}
