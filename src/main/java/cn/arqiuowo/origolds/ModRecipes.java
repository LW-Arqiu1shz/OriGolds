package cn.arqiuowo.origolds;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** OriGolds 配方序列化器注册表。 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, OriGolds.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CoatingRecipe>> ORIGOLD_COATING =
            RECIPE_SERIALIZERS.register("origold_coating", () -> CoatingRecipe.serializer(false, false));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CoatingRecipe>> COMPOUND_ORIGOLD_COATING =
            RECIPE_SERIALIZERS.register("compound_origold_coating", () -> CoatingRecipe.serializer(true, false));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CoatingRecipe>> POLYFUSIVE_ORIGOLD_COATING =
            RECIPE_SERIALIZERS.register("polyfusive_origold_coating", () -> CoatingRecipe.serializer(false, true));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GildingRecipe>> GILDING =
            RECIPE_SERIALIZERS.register("gilding", () -> GildingRecipe.SERIALIZER);

    private ModRecipes() {
    }
}
