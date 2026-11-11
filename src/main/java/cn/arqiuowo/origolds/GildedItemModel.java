package cn.arqiuowo.origolds;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 给可镀金物品的所有模型面启用动态颜色滤镜。
 *
 * <p>染色索引的重新编排：原本没有染色的面（tintindex = -1）改为 {@link #GOLD_TINT_INDEX}（0），
 * 只叠镀金滤镜；原本就有染色的面（例如皮革盔甲的染色层 tintindex = 0）改为
 * {@code 原索引 + }{@link #ORIGINAL_TINT_OFFSET}，颜色处理器先按原索引取物品原来的颜色
 * （染料色等），再乘上镀金滤镜，因此皮革盔甲等自带染色的物品不会丢色。</p>
 */
public final class GildedItemModel extends BakedModelWrapper<BakedModel> {
    /** 原本没有染色的面使用的染色索引：只叠镀金滤镜。 */
    public static final int GOLD_TINT_INDEX = 0;
    /** 原本已有染色的面：新索引 = 原索引 + 该偏移量。 */
    public static final int ORIGINAL_TINT_OFFSET = 1;

    /** 每个朝向（含 null，下标 6）缓存一次重新编排后的面，避免每帧重建列表与 BakedQuad。 */
    private final CachedQuads[] cache = new CachedQuads[7];

    public GildedItemModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return tintQuadsCached(side, originalModel.getQuads(state, side, random));
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
            ModelData modelData, RenderType renderType) {
        // Vanilla item models provide their quads through the standard overload.
        // Calling the extension overload can return an empty list for those models.
        return tintQuadsCached(side, originalModel.getQuads(state, side, random));
    }

    private List<BakedQuad> tintQuadsCached(Direction side, List<BakedQuad> source) {
        if (source.isEmpty()) {
            return source;
        }
        int slot = side == null ? 6 : side.ordinal();
        CachedQuads cached = cache[slot];
        // 原模型返回同一个列表实例（普通物品模型都是如此）时直接复用上次的结果；
        // 动态模型每次给出新列表时照常重新编排。
        if (cached != null && cached.source() == source) {
            return cached.tinted();
        }
        List<BakedQuad> tinted = tintQuads(source);
        cache[slot] = new CachedQuads(source, tinted);
        return tinted;
    }

    private static List<BakedQuad> tintQuads(List<BakedQuad> quads) {
        List<BakedQuad> tinted = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int tintIndex = quad.isTinted()
                    ? quad.getTintIndex() + ORIGINAL_TINT_OFFSET
                    : GOLD_TINT_INDEX;
            tinted.add(new BakedQuad(
                    quad.getVertices(),
                    tintIndex,
                    quad.getDirection(),
                    quad.getSprite(),
                    quad.isShade(),
                    quad.hasAmbientOcclusion()));
        }
        return List.copyOf(tinted);
    }

    /** 不可变的缓存项：原列表与重新编排后的列表成对替换，多线程读取也不会拿到错配的结果。 */
    private record CachedQuads(List<BakedQuad> source, List<BakedQuad> tinted) {
    }
}
