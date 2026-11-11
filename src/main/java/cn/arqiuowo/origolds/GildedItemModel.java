package cn.arqiuowo.origolds;

import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvableModel;
import org.joml.Matrix4fc;

/**
 * 让镀金滤镜作用于<b>所有</b>物品（含其它模组的装备）的全局挂载点。
 *
 * <p>本版本（26.3）里物品外观完全由 {@code assets/<命名空间>/items/<物品>.json} 描述，
 * 颜色则来自其中的 {@code minecraft:model} 类型上的 {@code tints} 列表。想要给某个物品染色，
 * 传统做法是覆盖那份 json、往 tints 里塞一项——但那只对原版物品有效，别的模组的装备我们
 * 没法替它写文件。</p>
 *
 * <p>这里改从<b>类型层</b>下手：{@code RegisterItemModelsEvent} 允许注册物品模型类型的编解码器，
 * 而 {@code ItemModels.bootstrap()} 是先注册原版类型再抛出事件的，因此我们可以<b>顶掉</b>
 * {@code minecraft:model} 这一个条目（{@code LateBoundIdMapper} 内部是 {@code HashBiMap}，
 * 同键重复 put 就是替换）。换上的编解码器解码出同样的字段（模型路径、变换、tints），
 * 但在烘焙前把 tints 逐个包成 {@link GildingFilterTintSource}。</p>
 *
 * <p>由于 {@code select}/{@code condition}/{@code range_dispatch}/{@code composite} 这些类型
 * 的子模型同样走 {@code minecraft:model}，覆盖面自然延伸到所有嵌套结构：弓的拉弓阶段、
 * 三叉戟的界面/掉落物形态、盔甲的纹饰分支等等，无需逐个列举。物品只要是可镀金的
 * （判定见 {@link GildingUtil#isEligible}），不论来自原版还是别的模组，都会自动镀上。</p>
 *
 * <p>滤镜本身对未镀金物品是纯白（乘法单位元），所以非镀金物品的渲染结果与原来逐像素一致。</p>
 */
public final class GildedItemModel {
    /**
     * 顶替 {@code minecraft:model} 的编解码器：字段与原版完全一致，只是烘焙时多套一层滤镜。
     */
    public static final MapCodec<Unbaked> MAP_CODEC =
            CuboidItemModelWrapper.Unbaked.MAP_CODEC.xmap(Unbaked::new, Unbaked::vanilla);

    private GildedItemModel() {
    }

    /**
     * 原版 {@code minecraft:model} 的包装：保留原模型与变换，只把 tints 列表整体套上镀金滤镜。
     */
    public record Unbaked(CuboidItemModelWrapper.Unbaked vanilla) implements ItemModel.Unbaked {

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.vanilla.resolveDependencies(resolver);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new CuboidItemModelWrapper.Unbaked(
                    this.vanilla.model(),
                    this.vanilla.transformation(),
                    wrapTints(this.vanilla.tints()))
                    .bake(context, transformation);
        }

        /**
         * 编码时 {@code ItemModels.CODEC} 会用返回的编解码器反查类型 id，所以必须返回
         * 我们自己这个（它对应 {@code minecraft:model}），而不是原版那个已被顶掉的实例。
         */
        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }

    /**
     * 给 tints 列表套上镀金滤镜。
     *
     * <p>原有 tint 逐个包装以保留 {@code tintindex} 与列表下标的对应关系；列表为空时（大多数
     * 物品都是如此）补上一项，让 {@code tintindex} 为 0 的基础图层被染色——这正是原版逐个
     * 覆盖 json 时写下的那种 {@code tints} 列表。</p>
     */
    private static List<ItemTintSource> wrapTints(List<ItemTintSource> tints) {
        if (tints.isEmpty()) {
            return List.of(GildedTintSource.INSTANCE);
        }
        List<ItemTintSource> wrapped = new ArrayList<>(tints.size());
        for (ItemTintSource tint : tints) {
            wrapped.add(new GildingFilterTintSource(tint));
        }
        return List.copyOf(wrapped);
    }
}
