package cn.arqiuowo.origolds;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

/** 客户端物品染色来源与物品模型覆写。 */
@EventBusSubscriber(modid = OriGolds.MOD_ID, value = Dist.CLIENT)
public final class OrigoldsClientModEvents {
    private OrigoldsClientModEvents() {
    }

    /**
     * 注册 {@code origolds:gilding} 这一 tint 类型。
     *
     * <p>本版本（26.1.2）不再有 {@code ItemColor} 与 {@code RegisterColorHandlersEvent.Item}：
     * 物品颜色由模型 json 的 {@code tints} 列表声明。实际颜色计算见 {@link GildedTintSource}。</p>
     */
    @SubscribeEvent
    public static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(OriGolds.MOD_ID, "gilding"),
                GildedTintSource.MAP_CODEC);
    }

    /**
     * 顶替 {@code minecraft:model} 这一物品模型类型，让镀金滤镜作用到所有物品上。
     *
     * <p>这样就不必再为每个可镀金物品覆盖一份 {@code assets/minecraft/items/*.json}，
     * 原版之外的模组装备也能一并镀金。详见 {@link GildedItemModel}。</p>
     */
    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(Identifier.withDefaultNamespace("model"), GildedItemModel.MAP_CODEC);
    }
}
