package cn.arqiuowo.origolds;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** OriGolds 物品注册表。 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OriGolds.MOD_ID);

    // 26.x 的 Item.Properties 必须带 ResourceKey（Item 构造器会调用 effectiveDescriptionId()，
    // 缺失即抛 "Item id not set"）。因此不能用 register(name, Supplier) 这个旧重载，
    // 必须走 registerItem / registerSimpleItem —— 它们内部会执行 properties.setId(key)。
    public static final DeferredItem<Item> ORIGOLD_MOLD = ITEMS.registerItem(
            "origold_mold", ReusableMoldItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> COMPOUND_ORIGOLD_MOLD = ITEMS.registerItem(
            "compound_origold_mold", ReusableMoldItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> POLYFUSIVE_ORIGOLD_MOLD = ITEMS.registerItem(
            "polyfusive_origold_mold", ReusableMoldItem::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> MAGIGOLD_CORE = ITEMS.registerSimpleItem(
            "magigold_core", () -> new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ORIGOLD_COATBOARD = ITEMS.registerSimpleItem(
            "origold_coatboard", () -> new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> COMPOUND_ORIGOLD_COATBOARD = ITEMS.registerSimpleItem(
            "compound_origold_coatboard", () -> new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> POLYFUSIVE_ORIGOLD_COATBOARD = ITEMS.registerSimpleItem(
            "polyfusive_origold_coatboard", () -> new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> ORIGOLD_COATING_SMITHING_TEMPLATE = ITEMS.registerSimpleItem(
            "origold_coating_smithing_template", () -> new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));

    private ModItems() {
    }
}
