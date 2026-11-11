package cn.arqiuowo.origolds.client;

import cn.arqiuowo.origolds.OriGolds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 屏幕下方的 Boss 信息文本行（1.1.1 起随附属模组 action bar 接口引入）。
 *
 * <p>内容来自服务端 {@code TextBroadcastChannel.BOSS_INFO} 通道，由 {@code BossInfoPayload} 同步。
 * 原版动作栏只有一个槽位，多个附属模组一起播报时会互相顶掉；这一行独立绘制在动作栏正上方，
 * 让「源金渴望」「古金极锻」这类附属模组能同时显示自己的状态。</p>
 *
 * <p>26.x 里 HUD 图层的绘制参数从 {@code GuiGraphics} 换成了 {@code GuiGraphicsExtractor}，
 * 居中绘制文本从 {@code drawCenteredString} 换成了 {@code centeredText}，坐标算法保持原样：
 * 原版动作栏画在 {@code guiHeight - max(68, 左右侧高度 + 9) - 4}，本行再往上抬 9px。</p>
 */
@EventBusSubscriber(modid = OriGolds.MOD_ID, value = Dist.CLIENT)
public final class OrigoldsBossInfoOverlay {
    /** 与原版动作栏共用的底部基准偏移。 */
    private static final int PLAYER_INFO_Y_OFFSET = 68;
    /** 原版动作栏文本的抬升量。 */
    private static final int PLAYER_INFO_LINE_LIFT = 4;
    /** 本行相对动作栏再往上抬的距离。 */
    private static final int BOSS_INFO_LINE_GAP = 9;
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(OriGolds.MOD_ID, "boss_info");

    private static volatile Component message;

    private OrigoldsBossInfoOverlay() {
    }

    /** 设置要显示的文本，传 {@code null} 表示清空。由网络线程调用，故字段是 volatile。 */
    public static void setMessage(Component value) {
        message = value;
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, OrigoldsBossInfoOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Component current = message;
        if (current == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // 26.3：F1 隐藏界面的状态从 Options.hideGui 挪到了 Hud（ Gui.hud ），由 isHidden() 读取。
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.hud.isHidden()) {
            return;
        }
        int y = graphics.guiHeight() - PLAYER_INFO_Y_OFFSET - PLAYER_INFO_LINE_LIFT - BOSS_INFO_LINE_GAP;
        graphics.centeredText(minecraft.font, current, graphics.guiWidth() / 2, y, DEFAULT_COLOR);
    }
}
