package cn.arqiuowo.origolds;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/** 锻造台中的镀金 I/II/III 动态配方。 */
public final class GildingRecipe implements SmithingRecipe {
    public static final MapCodec<GildingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.INT.fieldOf("level").forGetter(GildingRecipe::level))
            .apply(instance, GildingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GildingRecipe> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, GildingRecipe recipe) -> buffer.writeVarInt(recipe.level()),
            buffer -> new GildingRecipe(buffer.readVarInt()));

    public static final RecipeSerializer<GildingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final int targetLevel;
    /** 延迟计算并缓存的摆料信息，见 {@link #placementInfo()}。 */
    private volatile PlacementInfo placementInfo;

    public GildingRecipe(int targetLevel) {
        if (targetLevel < 1 || targetLevel > 3) {
            throw new IllegalArgumentException("OriGolds gilding level must be between 1 and 3");
        }
        this.targetLevel = targetLevel;
    }

    public int level() {
        return targetLevel;
    }

    private static ItemStack coatboard(int targetLevel) {
        return new ItemStack(targetLevel == 2
                ? ModItems.COMPOUND_ORIGOLD_COATBOARD.get()
                : targetLevel == 3
                        ? ModItems.POLYFUSIVE_ORIGOLD_COATBOARD.get()
                        : ModItems.ORIGOLD_COATBOARD.get());
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isTemplateIngredient(input.template())
                && isBaseIngredient(input.base())
                && isAdditionIngredient(input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return GildingUtil.applyGilding(input.base(), targetLevel);
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(Ingredient.of(ModItems.ORIGOLD_COATING_SMITHING_TEMPLATE.get()));
    }

    @Override
    public Ingredient baseIngredient() {
        // 必须用已冻结的运行时注册表取标签：acquireBootstrapRegistrationLookup 只在注册表
        // bootstrap 阶段有效，而 baseIngredient() 会在世界创建时加载/同步配方期间被调用，
        // 那里再碰 bootstrap 查询会直接把世界加载卡死。
        return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE));
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(Ingredient.of(coatboard(targetLevel).getItem()));
    }

    @Override
    public PlacementInfo placementInfo() {
        // 与原版 smithing 配方一致：按「模板 / 基底 / 附加」三个槽位声明材料，
        // 这样配方书里点一下就能自动摆料。写成 NOT_PLACEABLE 会让原版在配方收尾阶段
        // 判定「无法摆放」并把配方忽略（日志里就是那句 can't be placed due to empty ingredients）。
        // 这里延迟到首次调用再算：那时标签已绑定完成，构造期（数据包解析线程）取标签会拿到空集合。
        PlacementInfo info = this.placementInfo;
        if (info == null) {
            info = PlacementInfo.createFromOptionals(
                    List.of(templateIngredient(), Optional.of(baseIngredient()), additionIngredient()));
            this.placementInfo = info;
        }
        return info;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(ModItems.ORIGOLD_COATING_SMITHING_TEMPLATE.get());
    }

    public boolean isBaseIngredient(ItemStack stack) {
        return GildingUtil.isEligible(stack) && GildingUtil.getLevel(stack) == targetLevel - 1;
    }

    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(coatboard(targetLevel).getItem());
    }

    @Override
    public RecipeSerializer<GildingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
