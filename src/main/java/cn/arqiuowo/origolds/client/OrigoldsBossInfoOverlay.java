package cn.arqiuowo.origolds.client;

import cn.arqiuowo.origolds.OriGolds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * BOSS 信息栏的客户端渲染层。
 *
 * <p>位置与附属模组 origolds_ga 原本的 boss_hint 渲染层完全一致：位于玩家信息栏
 * （action bar）正上方那一行。原版 action bar 的文本画在
 * {@code guiHeight - max(leftHeight, rightHeight, 68) - 4}，这里取无其它 HUD 占位时的
 * 基准 68，再抬高一个行高，得到 {@code guiHeight - 68 - 4 - lineHeight}，即紧贴玩家信息栏上方。</p>
 *
 * <p>内容由服务端 {@link cn.arqiuowo.origolds.api.TextBroadcastService} 合并后经
 * {@link cn.arqiuowo.origolds.network.BossInfoPayload} 推送，本层只负责画。</p>
 */
@EventBusSubscriber(modid = OriGolds.MOD_ID, value = Dist.CLIENT)
public final class OrigoldsBossInfoOverlay {
    /** 原版 action bar 在无其它 HUD 占位时的 y 偏移。 */
    private static final int PLAYER_INFO_Y_OFFSET = 68;
    /** 原版 action bar 文本相对其基准点再上移的距离。 */
    private static final int PLAYER_INFO_LINE_LIFT = 4;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(OriGolds.MOD_ID, "boss_info");

    private static volatile Component message;

    private OrigoldsBossInfoOverlay() {
    }

    /** 更新 BOSS 信息栏文本；null 表示该栏当前为空。 */
    public static void setMessage(Component value) {
        message = value;
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, OrigoldsBossInfoOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Component current = message;
        if (current == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        int y = graphics.guiHeight() - PLAYER_INFO_Y_OFFSET - PLAYER_INFO_LINE_LIFT
                - minecraft.font.lineHeight;
        graphics.drawCenteredString(minecraft.font, current, graphics.guiWidth() / 2, y, DEFAULT_COLOR);
    }
}
