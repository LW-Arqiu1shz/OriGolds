package cn.arqiuowo.origolds;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 金块与对应模具的无序镀板配方。
 * 模具只在本配方中作为剩余物返回，因此制造复合模具时会正常消耗初级模具。
 */
public final class CoatingRecipe extends CustomRecipe {
    private final Item mold;
    private final Item resultItem;
    private final boolean compound;
    private final boolean polyfusive;

    public CoatingRecipe(CraftingBookCategory category, boolean compound) {
        this(category, compound, false);
    }

    public CoatingRecipe(CraftingBookCategory category, boolean compound, boolean polyfusive) {
        super(category);
        this.compound = compound;
        this.polyfusive = polyfusive;
        this.mold = (polyfusive
                ? ModItems.POLYFUSIVE_ORIGOLD_MOLD
                : (compound ? ModItems.COMPOUND_ORIGOLD_MOLD : ModItems.ORIGOLD_MOLD)).get();
        this.resultItem = (polyfusive
                ? ModItems.POLYFUSIVE_ORIGOLD_COATBOARD
                : (compound ? ModItems.COMPOUND_ORIGOLD_COATBOARD : ModItems.ORIGOLD_COATBOARD)).get();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean foundMold = false;
        boolean foundGoldBlock = false;
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(mold) && !foundMold) {
                foundMold = true;
            } else if (stack.is(Items.GOLD_BLOCK) && !foundGoldBlock) {
                foundGoldBlock = true;
            } else {
                return false;
            }
        }
        return foundMold && foundGoldBlock;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(resultItem);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(resultItem);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (!stack.isEmpty() && stack.is(mold)) {
                remaining.set(index, stack.copyWithCount(1));
            }
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(Items.GOLD_BLOCK));
        ingredients.add(Ingredient.of(mold));
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        if (polyfusive) {
            return ModRecipes.POLYFUSIVE_ORIGOLD_COATING.get();
        }
        return compound ? ModRecipes.COMPOUND_ORIGOLD_COATING.get() : ModRecipes.ORIGOLD_COATING.get();
    }

    @Override
    public boolean showNotification() {
        return true;
    }
}
