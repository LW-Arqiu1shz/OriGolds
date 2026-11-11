package cn.arqiuowo.origolds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** OriGolds 的创造模式物品栏。 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, OriGolds.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORIGOLDS_TAB =
            CREATIVE_MODE_TABS.register("origolds", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.origolds"))
                    .icon(() -> new ItemStack(ModItems.MAGIGOLD_CORE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ORIGOLD_MOLD.get());
                        output.accept(ModItems.COMPOUND_ORIGOLD_MOLD.get());
                        output.accept(ModItems.POLYFUSIVE_ORIGOLD_MOLD.get());
                        output.accept(ModItems.MAGIGOLD_CORE.get());
                        output.accept(ModItems.ORIGOLD_COATBOARD.get());
                        output.accept(ModItems.COMPOUND_ORIGOLD_COATBOARD.get());
                        output.accept(ModItems.POLYFUSIVE_ORIGOLD_COATBOARD.get());
                        output.accept(ModItems.ORIGOLD_COATING_SMITHING_TEMPLATE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
