package cn.arqiuowo.origolds;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.modlist.DefaultModDisplayInfo;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;

/**
 * 客户端入口（专用服务端不会加载此类）。
 *
 * <ul>
 *   <li>在 NeoForge 的模组列表里为本模组注册「配置」按钮，使用 NeoForge 自带的 {@link ConfigurationScreen}
 *       展示 {@link OrigoldsConfig} 里的客户端配置，保存后立即生效。</li>
 *   <li>模组列表里的模组名称按当前语言显示（{@value #NAME_KEY}），简介沿用 NeoForge 默认的
 *       {@code neoforge.screen.mods.info.description.<modid>} 翻译键。与「金源工艺：工业时代」同一套做法。</li>
 * </ul>
 */
@Mod(value = OriGolds.MOD_ID, dist = Dist.CLIENT)
public final class OriGoldsClient {
    private static final String NAME_KEY = "neoforge.screen.mods.info.displayname." + OriGolds.MOD_ID;

    public OriGoldsClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modContainer.registerExtensionPoint(ModDisplayInfo.class, new DefaultModDisplayInfo(modContainer) {
            @Override
            public Component displayName() {
                return Component.translatableWithFallback(NAME_KEY, modContainer.getModInfo().getDisplayName());
            }
        });
    }
}