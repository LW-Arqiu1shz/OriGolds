package cn.arqiuowo.origolds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/** 锻造台中的镀金 I/II/III 动态配方。 */
public final class GildingRecipe implements SmithingRecipe {
    private final int targetLevel;

    public GildingRecipe(int targetLevel) {
        if (targetLevel < 1 || targetLevel > 3) {
            throw new IllegalArgumentException("OriGolds gilding level must be between 1 and 3");
        }
        this.targetLevel = targetLevel;
    }

    public int level() {
        return targetLevel;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isTemplateIngredient(input.template())
                && isBaseIngredient(input.base())
                && isAdditionIngredient(input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return GildingUtil.applyGilding(input.base(), targetLevel);
    }

    /** 配方书需要一个非空的代表性输出，实际输出仍由 assemble 保留基材数据生成。 */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(Items.GOLDEN_SWORD);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ModItems.ORIGOLD_COATING_SMITHING_TEMPLATE.get()));
        ingredients.add(Ingredient.of(ItemTags.DURABILITY_ENCHANTABLE));
        ingredients.add(Ingredient.of(targetLevel == 1
                ? ModItems.ORIGOLD_COATBOARD.get()
                : targetLevel == 2
                        ? ModItems.COMPOUND_ORIGOLD_COATBOARD.get()
                        : ModItems.POLYFUSIVE_ORIGOLD_COATBOARD.get()));
        return ingredients;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(ModItems.ORIGOLD_COATING_SMITHING_TEMPLATE.get());
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return GildingUtil.isEligible(stack) && GildingUtil.getLevel(stack) == targetLevel - 1;
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(targetLevel == 1
                ? ModItems.ORIGOLD_COATBOARD.get()
                : targetLevel == 2
                        ? ModItems.COMPOUND_ORIGOLD_COATBOARD.get()
                        : ModItems.POLYFUSIVE_ORIGOLD_COATBOARD.get());
    }

    @Override
    public RecipeSerializer<GildingRecipe> getSerializer() {
        return ModRecipes.GILDING.get();
    }

    /** 固定配方参数的网络/数据包序列化器。 */
    public static final class Serializer implements RecipeSerializer<GildingRecipe> {
        private static final MapCodec<GildingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Codec.INT.fieldOf("level").forGetter(GildingRecipe::level))
                .apply(instance, GildingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GildingRecipe> STREAM_CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, GildingRecipe recipe) -> buffer.writeVarInt(recipe.level()),
                buffer -> new GildingRecipe(buffer.readVarInt()));

        @Override
        public MapCodec<GildingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GildingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
