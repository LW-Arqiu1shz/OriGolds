package cn.arqiuowo.origolds;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 进度触发器：根据玩家当前背包/装备中的镀金等级状态判断是否满足条件。
 *
 * <p>由于原版物品谓词对 {@code minecraft:custom_data} 只能精确匹配整段 tag，
 * 无法表达「镀金等级 ≥ N」，因此由本触发器直接读取 {@link GildingUtil} 中的
 * 镀金等级数据，并对「任意镀金物品」「指定工具」「全套盔甲」三类条件做实时判定。
 * 本触发器由 {@link OrigoldsEvents#checkGildingAdvancements} 周期触发。</p>
 */
public class GildingStateTrigger extends SimpleCriterionTrigger<GildingStateTrigger.TriggerInstance> {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** 对指定玩家触发一次判定，满足条件的进度监听器将被授予完成。 */
    public void trigger(ServerPlayer player) {
        super.trigger(player, instance -> instance.matches(player));
    }

    public static final class TriggerInstance implements SimpleInstance {
        private final Optional<Holder<LootItemCondition>> player;
        private final int minLevel;
        private final boolean any;
        private final List<String> tools;
        private final boolean armor;

        public TriggerInstance(Optional<Holder<LootItemCondition>> player, int minLevel,
                boolean any, List<String> tools, boolean armor) {
            this.player = player;
            this.minLevel = Math.max(1, Math.min(255, minLevel));
            this.any = any;
            this.tools = tools == null ? List.of() : List.copyOf(tools);
            this.armor = armor;
        }

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.INT.optionalFieldOf("min_level", 1).forGetter(TriggerInstance::minLevel),
                        Codec.BOOL.optionalFieldOf("any", false).forGetter(TriggerInstance::any),
                        Codec.STRING.listOf().optionalFieldOf("tools", List.of()).forGetter(TriggerInstance::tools),
                        Codec.BOOL.optionalFieldOf("armor", false).forGetter(TriggerInstance::armor))
                .apply(instance, TriggerInstance::new));

        @Override
        public Optional<Holder<LootItemCondition>> player() {
            return player;
        }

        public int minLevel() {
            return minLevel;
        }

        public boolean any() {
            return any;
        }

        public List<String> tools() {
            return tools;
        }

        public boolean armor() {
            return armor;
        }

        /** 背包/装备任意镀金等级大于等于 minLevel 的物品。 */
        public boolean matches(ServerPlayer player) {
            if (any && !hasAnyGilded(player)) {
                return false;
            }
            for (String tool : tools) {
                if (!hasToolAtLevel(player, tool)) {
                    return false;
                }
            }
            if (armor && !hasFullGildedArmor(player)) {
                return false;
            }
            return true;
        }

        private boolean hasAnyGilded(ServerPlayer player) {
            if (containsGilded(player.getInventory().getNonEquipmentItems())) {
                return true;
            }
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                if (GildingUtil.getLevel(player.getItemBySlot(slot)) >= minLevel) {
                    return true;
                }
            }
            return GildingUtil.getLevel(player.getItemBySlot(EquipmentSlot.OFFHAND)) >= minLevel;
        }

        private boolean containsGilded(List<ItemStack> stacks) {
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty() && GildingUtil.getLevel(stack) >= minLevel) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasToolAtLevel(ServerPlayer player, String tool) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.isEmpty() || GildingUtil.getLevel(stack) < minLevel) {
                    continue;
                }
                if (matchesTool(stack, tool)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matchesTool(ItemStack stack, String tool) {
            return switch (tool) {
                case "sword" -> stack.is(ItemTags.SWORDS);
                case "pickaxe" -> stack.is(ItemTags.PICKAXES);
                case "axe" -> stack.is(ItemTags.AXES);
                default -> false;
            };
        }

        private boolean hasFullGildedArmor(ServerPlayer player) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()
                        || !GildingUtil.isArmor(stack)
                        || GildingUtil.getLevel(stack) < minLevel) {
                    return false;
                }
            }
            return true;
        }
    }
}
