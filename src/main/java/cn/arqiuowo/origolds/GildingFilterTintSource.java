package cn.arqiuowo.origolds;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 把镀金滤镜叠到另一个 tint 之上的包装层。
 *
 * <p>物品模型可以自带 tints（皮革的染色、盔甲的纹饰、数据包的自定义着色等）。直接往列表里
 * 追加一项是不行的：quad 上的 {@code tintindex} 就是列表下标，追加会让原有下标错位。所以
 * 这里改为<b>逐个包装</b>原有的 tint：先让它按原样算出底色，再乘上镀金滤镜。</p>
 *
 * <p>未镀金时滤镜是纯白，乘法结果与底色完全一致，因此对原版外观零影响。</p>
 */
public record GildingFilterTintSource(ItemTintSource base) implements ItemTintSource {

    @Override
    public int calculate(ItemStack itemStack, ClientLevel level, LivingEntity owner) {
        return GildedTintSource.modulate(this.base.calculate(itemStack, level, owner),
                GildedTintSource.filterColor(itemStack));
    }

    /**
     * 这些实例只在模型烘焙时于内存中构造，不参与序列化，因此直接复用
     * {@code origolds:gilding} 的类型编码器即可。
     */
    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return GildedTintSource.MAP_CODEC;
    }
}
