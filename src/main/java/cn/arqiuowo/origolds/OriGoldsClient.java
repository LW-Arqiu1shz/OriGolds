package cn.arqiuowo.origolds;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端入口：在 NeoForge 的模组列表里为本模组注册「配置」按钮。
 *
 * <p>使用 NeoForge 自带的 {@link ConfigurationScreen}，直接展示 {@link OrigoldsConfig} 里的客户端配置；
 * 改动保存后立即生效（与手动编辑 origolds-client.toml 等价）。该类只在客户端加载，专用服务端不会触碰界面代码。</p>
 */
@Mod(value = OriGolds.MOD_ID, dist = Dist.CLIENT)
public final class OriGoldsClient {
    public OriGoldsClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}