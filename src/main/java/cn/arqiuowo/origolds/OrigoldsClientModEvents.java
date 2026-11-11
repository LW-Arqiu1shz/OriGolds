package cn.arqiuowo.origolds;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** 客户端模型和物品颜色注册。 */
@EventBusSubscriber(modid = OriGolds.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OrigoldsClientModEvents {
    // ItemRenderer 按 ARGB 读取颜色；配置透明度作为滤镜混合强度，而不是物品 alpha。
    private static final int ORIGINAL_ITEM_COLOR = 0xFFFFFFFF;
    private static final long GILDING_CYCLE_MILLIS = 6_000L;
    private static final long GILDING_FADE_MILLIS = 2_000L;
    private static final Logger LOGGER = LogUtils.getLogger();

    private OrigoldsClientModEvents() {
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // 先记下这些物品原有的颜色处理器（原版皮革盔甲的染料色、其它模组的染色等）。
        // 同一物品重复注册会顶掉旧处理器，所以镀金处理器必须自己转调原处理器，否则皮革盔甲会丢色。
        Map<Item, ItemColor> previous = existingItemColors(event.getItemColors());
        Map<ItemColor, List<Item>> byPrevious = new IdentityHashMap<>();
        List<Item> withoutPrevious = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!GildingUtil.isEligible(new ItemStack(item))) {
                continue;
            }
            ItemColor original = previous.get(item);
            if (original == null) {
                withoutPrevious.add(item);
            } else {
                byPrevious.computeIfAbsent(original, ignored -> new ArrayList<>()).add(item);
            }
        }
        if (!withoutPrevious.isEmpty()) {
            event.register(gildingColor(null), withoutPrevious.toArray(Item[]::new));
        }
        byPrevious.forEach((original, items) -> event.register(gildingColor(original), items.toArray(Item[]::new)));
    }

    /**
     * 镀金颜色处理器。染色索引约定见 {@link GildedItemModel}：
     * {@link GildedItemModel#GOLD_TINT_INDEX} 只返回镀金滤镜；更大的索引先向原处理器取
     * {@code 索引 - ORIGINAL_TINT_OFFSET} 的原色，再乘上镀金滤镜。
     */
    private static ItemColor gildingColor(ItemColor original) {
        return (stack, tintIndex) -> {
            int filter = filterColor(stack);
            if (tintIndex < GildedItemModel.ORIGINAL_TINT_OFFSET) {
                return filter;
            }
            int baseColor = original == null
                    ? ORIGINAL_ITEM_COLOR
                    : original.getColor(stack, tintIndex - GildedItemModel.ORIGINAL_TINT_OFFSET);
            return modulate(baseColor, filter);
        };
    }

    /** 该物品此刻应叠加的镀金滤镜；未镀金或特效关闭时为纯白（不改变颜色）。 */
    private static int filterColor(ItemStack stack) {
        if (!OrigoldsConfig.ENABLE_GILDING_EFFECT.get() || GildingUtil.getLevel(stack) <= 0) {
            return ORIGINAL_ITEM_COLOR;
        }
        int filteredTargetColor = blendColor(ORIGINAL_ITEM_COLOR, OrigoldsConfig.getConfiguredTargetColor(),
                OrigoldsConfig.getConfiguredOpacity());
        if (!OrigoldsConfig.ENABLE_GILDING_GRADIENT.get()) {
            return filteredTargetColor;
        }
        return animatedGildingColor(ORIGINAL_ITEM_COLOR, filteredTargetColor, System.currentTimeMillis());
    }

    /** 逐通道相乘，保留底色自身的透明度；滤镜为纯白时结果与底色完全相同。 */
    private static int modulate(int baseColor, int filterColor) {
        int red = ((baseColor >> 16 & 0xFF) * (filterColor >> 16 & 0xFF) + 127) / 255;
        int green = ((baseColor >> 8 & 0xFF) * (filterColor >> 8 & 0xFF) + 127) / 255;
        int blue = ((baseColor & 0xFF) * (filterColor & 0xFF) + 127) / 255;
        return (baseColor & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }

    /** 读取 ItemColors 里已注册的处理器快照；读取失败时返回空表（退化为只叠镀金滤镜）。 */
    private static Map<Item, ItemColor> existingItemColors(ItemColors itemColors) {
        try {
            Map<Item, ItemColor> map = ObfuscationReflectionHelper.getPrivateValue(
                    ItemColors.class, itemColors, "itemColors");
            return map == null ? Map.of() : new IdentityHashMap<>(map);
        } catch (RuntimeException exception) {
            LOGGER.warn("[origolds] 无法读取已有的物品颜色处理器，镀金物品的原有染色可能无法保留", exception);
            return Map.of();
        }
    }

    private static int animatedGildingColor(int baseColor, int targetColor, long currentTimeMillis) {
        long cyclePosition = Math.floorMod(currentTimeMillis, GILDING_CYCLE_MILLIS);
        if (cyclePosition < GILDING_FADE_MILLIS) {
            return interpolateColor(baseColor, targetColor,
                    (float) cyclePosition / GILDING_FADE_MILLIS);
        }
        if (cyclePosition < GILDING_FADE_MILLIS * 2L) {
            return interpolateColor(targetColor, baseColor,
                    (float) (cyclePosition - GILDING_FADE_MILLIS) / GILDING_FADE_MILLIS);
        }
        return baseColor;
    }

    private static int blendColor(int from, int to, float amount) {
        int red = interpolateChannel(from >> 16, to >> 16, amount);
        int green = interpolateChannel(from >> 8, to >> 8, amount);
        int blue = interpolateChannel(from, to, amount);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int interpolateColor(int from, int to, float progress) {
        return blendColor(from, to, progress);
    }

    private static int interpolateChannel(int from, int to, float progress) {
        return Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * progress);
    }

    @SubscribeEvent
    public static void tintGildableItemModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            ModelResourceLocation modelLocation = entry.getKey();
            if (!ModelResourceLocation.INVENTORY_VARIANT.equals(modelLocation.variant())) {
                continue;
            }

            ResourceLocation itemId = modelLocation.id();
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (GildingUtil.isEligible(new ItemStack(item))) {
                entry.setValue(new GildedItemModel(entry.getValue()));
            }
        }
    }
}
