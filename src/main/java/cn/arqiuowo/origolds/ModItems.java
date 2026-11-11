package cn.arqiuowo.origolds;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** OriGolds 物品注册表。 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OriGolds.MOD_ID);

    public static final DeferredItem<Item> ORIGOLD_MOLD = ITEMS.register(
            "origold_mold", () -> new ReusableMoldItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON)));
    public static final DeferredItem<Item> COMPOUND_ORIGOLD_MOLD = ITEMS.register(
            "compound_origold_mold", () -> new ReusableMoldItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> POLYFUSIVE_ORIGOLD_MOLD = ITEMS.register(
            "polyfusive_origold_mold", () -> new ReusableMoldItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MAGIGOLD_CORE = ITEMS.register(
            "magigold_core", () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ORIGOLD_COATBOARD = ITEMS.register(
            "origold_coatboard", () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON)));
    public static final DeferredItem<Item> COMPOUND_ORIGOLD_COATBOARD = ITEMS.register(
            "compound_origold_coatboard", () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> POLYFUSIVE_ORIGOLD_COATBOARD = ITEMS.register(
            "polyfusive_origold_coatboard", () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> ORIGOLD_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "origold_coating_smithing_template", () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }
}
