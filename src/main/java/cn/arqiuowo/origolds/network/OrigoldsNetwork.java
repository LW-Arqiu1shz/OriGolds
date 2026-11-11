package cn.arqiuowo.origolds.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import cn.arqiuowo.origolds.OriGolds;

/** 自定义数据包的注册入口（mod 总线事件，由注解自动挂上）。 */
@EventBusSubscriber(modid = OriGolds.MOD_ID)
public final class OrigoldsNetwork {
    /** 协议版本，改动负载结构时要一并提升。 */
    private static final String VERSION = "1";

    private OrigoldsNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
                .playToClient(BossInfoPayload.TYPE, BossInfoPayload.STREAM_CODEC, BossInfoPayload::handle);
    }
}
