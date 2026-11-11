package cn.arqiuowo.origolds;

import cn.arqiuowo.origolds.api.TextBroadcastService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 金·源（OriGolds）的模组入口。
 */
@Mod(OriGolds.MOD_ID)
public final class OriGolds {
    public static final String MOD_ID = "origolds";

    public OriGolds(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, OrigoldsConfig.SPEC, OrigoldsConfig.FILE_NAME);

        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModTriggers.TRIGGERS.register(modBus);

        modBus.addListener(OrigoldsEvents::modifyVanillaComponents);

        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onArmorHurt);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onLivingDamagePre);
        // 远程空中暴击模块（RangedCriticalModule）：玩家发射瞬间处于空中时，
        // 该箭命中伤害 ×1.5（乘法结算，与附魔/镀金/附属词条叠加）。
        NeoForge.EVENT_BUS.addListener(RangedCriticalModule::onArrowSpawned);
        NeoForge.EVENT_BUS.addListener(RangedCriticalModule::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onCriticalHit);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::checkGildingAdvancements);
        NeoForge.EVENT_BUS.addListener(OrigoldsEvents::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(TextBroadcastService::onServerTick);
        NeoForge.EVENT_BUS.addListener(TextBroadcastService::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(TextBroadcastService::onServerStopped);
        NeoForge.EVENT_BUS.addListener(OrigoldsCommands::register);
    }
}
