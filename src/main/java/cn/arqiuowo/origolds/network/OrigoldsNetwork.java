package cn.arqiuowo.origolds.network;

import cn.arqiuowo.origolds.OriGolds;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 本模组的自定义网络负载注册。 */
@EventBusSubscriber(modid = OriGolds.MOD_ID)
public final class OrigoldsNetwork {
    private static final String VERSION = "1";

    private OrigoldsNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
                .playToClient(BossInfoPayload.TYPE, BossInfoPayload.STREAM_CODEC, BossInfoPayload::handle);
    }
}
