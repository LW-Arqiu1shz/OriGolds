package cn.arqiuowo.origolds.network;

import java.util.Optional;

import cn.arqiuowo.origolds.OriGolds;
import cn.arqiuowo.origolds.client.OrigoldsBossInfoOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端到客户端的 Boss 信息文本同步包（1.1.1 起随附属模组 action bar 接口引入）。
 *
 * <p>负载为空表示清空客户端上显示的那一行。真正的绘制在
 * {@link OrigoldsBossInfoOverlay}。</p>
 */
public record BossInfoPayload(Optional<Component> message) implements CustomPacketPayload {
    public static final Type<BossInfoPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(OriGolds.MOD_ID, "boss_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossInfoPayload> STREAM_CODEC =
            StreamCodec.composite(ComponentSerialization.OPTIONAL_STREAM_CODEC,
                    BossInfoPayload::message, BossInfoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 只在客户端被调用，因此这里直接摸客户端类是安全的。 */
    public static void handle(BossInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OrigoldsBossInfoOverlay.setMessage(payload.message().orElse(null)));
    }
}
