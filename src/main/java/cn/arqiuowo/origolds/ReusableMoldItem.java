package cn.arqiuowo.origolds;

import net.minecraft.world.item.Item;

/** 仅用于标识可作为镀板模具的物品；模具复用由 CoatingRecipe 精确控制。 */
public final class ReusableMoldItem extends Item {
    public ReusableMoldItem(Properties properties) {
        super(properties);
    }
}
