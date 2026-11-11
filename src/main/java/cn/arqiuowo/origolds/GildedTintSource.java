package cn.arqiuowo.origolds;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 镀金物品的模型图层染色。
 *
 * <p>本版本（26.3）的物品颜色由模型自身的 tints 列表驱动。这里提供两件事：</p>
 *
 * <ul>
 *   <li>{@link #filterColor(ItemStack)}：算出该物品此刻应叠加的金色滤镜；未镀金、未启用
 *       或不合格时返回纯白（乘法单位元，等价于不染色）。</li>
 *   <li>作为 {@code origolds:gilding} 这一 tint 类型本身（{@link #INSTANCE}），供模型 json
 *       或数据包显式引用。</li>
 * </ul>
 *
 * <p>真正让「任何物品」都能镀金的是 {@link GildedItemModel}：它覆写了 {@code minecraft:model}
 * 这一物品模型类型，在烘焙时把镀金滤镜挂到每个模型的 tints 上，因此不需要为每种物品各写
 * 一份模型 json，其它模组的装备同样适用。</p>
 */
public final class GildedTintSource implements ItemTintSource {
    /** 物品本来的颜色：纯白表示不做任何染色。 */
    private static final int ORIGINAL_ITEM_COLOR = 0xFFFFFFFF;
    private static final long GILDING_CYCLE_MILLIS = 6_000L;
    private static final long GILDING_FADE_MILLIS = 2_000L;

    public static final GildedTintSource INSTANCE = new GildedTintSource();
    public static final MapCodec<GildedTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private GildedTintSource() {
    }

    @Override
    public int calculate(ItemStack itemStack, ClientLevel level, LivingEntity owner) {
        return filterColor(itemStack);
    }

    /**
     * 返回该物品此刻应该叠加的金色滤镜色。
     *
     * <p>未镀金、配置关闭或物品不合格时返回纯白 {@value #ORIGINAL_ITEM_COLOR}——把它乘进
     * 任何颜色都等于没乘，所以调用方可以无条件叠加，不必额外判断。</p>
     */
    public static int filterColor(ItemStack itemStack) {
        int gildingLevel = GildingUtil.getLevel(itemStack);
        if (!OrigoldsConfig.ENABLE_GILDING_EFFECT.get()
                || gildingLevel <= 0
                || !GildingUtil.isEligible(itemStack)) {
            return ORIGINAL_ITEM_COLOR;
        }

        int filteredTargetColor = blendColor(ORIGINAL_ITEM_COLOR, OrigoldsConfig.getConfiguredTargetColor(),
                OrigoldsConfig.getConfiguredOpacity());
        if (!OrigoldsConfig.ENABLE_GILDING_GRADIENT.get()) {
            return filteredTargetColor;
        }
        return animatedGildingColor(ORIGINAL_ITEM_COLOR, filteredTargetColor, System.currentTimeMillis());
    }

    /**
     * 把一层滤镜乘到已有颜色上（逐通道相乘，保留底色自身的透明度）。
     *
     * <p>滤镜为纯白时结果与底色完全相同，因此未镀金物品的观感不会有任何变化；底色为纯白
     * （模型原本没有 tint）时结果就是滤镜本身。</p>
     */
    public static int modulate(int baseColor, int filterColor) {
        int red = modulateChannel(baseColor >> 16, filterColor >> 16);
        int green = modulateChannel(baseColor >> 8, filterColor >> 8);
        int blue = modulateChannel(baseColor, filterColor);
        return (baseColor & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }

    private static int modulateChannel(int baseChannel, int filterChannel) {
        return ((baseChannel & 0xFF) * (filterChannel & 0xFF) + 127) / 255;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
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
}
