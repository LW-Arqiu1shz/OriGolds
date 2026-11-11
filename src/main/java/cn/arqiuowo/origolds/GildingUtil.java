package cn.arqiuowo.origolds;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** 镀金效果的持久化数据、计算和资格判断。 */
public final class GildingUtil {
    public static final String LEVEL_KEY = "origolds_gilding_level";
    public static final String BASE_MAX_DAMAGE_KEY = "origolds_base_max_damage";
    public static final String BASE_ATTACK_DAMAGE_KEY = "origolds_base_attack_damage";
    /**
     * 满蓄力弓在提示框中显示的射弹伤害（纯展示值）。
     *
     * <p>本模组<b>不改动</b>箭矢的基础伤害结算——原版弓/弩的伤害浮动与随机暴击原样保留
     * （唯一例外：发射瞬间玩家处于空中的 ×1.5 远程暴击，见 {@link RangedCriticalModule}）。
     * 这里只是远程武器「x 射弹伤害」提示行使用的参考数值：满蓄力弓一箭通常造成 6 点。</p>
     */
    public static final double BOW_FULL_CHARGE_PROJECTILE_DAMAGE = 6.0D;
    /**
     * 弩发射箭在提示框中显示的射弹伤害（纯展示值）。
     *
     * <p>同样是纯展示数值：弩一发通常造成 9 点，实际结算仍完全走原版。
     * 镀金倍率等加成照常作用于真实伤害，与本值无关。</p>
     */
    public static final double CROSSBOW_PROJECTILE_DAMAGE = 9.0D;
    private GildingUtil() {
    }

    public static int getLevel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty() || !data.contains(LEVEL_KEY)) {
            return 0;
        }
        // 只读访问：不复制整段 NBT（渲染时每帧都会调用，物品带大量自定义数据时复制开销很大）。
        @SuppressWarnings("deprecation")
        int level = data.getUnsafe().getInt(LEVEL_KEY);
        return Math.max(0, Math.min(255, level));
    }

    /** 将镀金等级转换为提示中使用的等级编号。 */
    public static String formatLevel(int level) {
        int displayLevel = Math.max(1, Math.min(255, level));
        if (displayLevel > 10) {
            return Integer.toString(displayLevel);
        }
        return switch (displayLevel) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> "I";
        };
    }

    /**
     * 可镀金物品必须是可损坏、可附魔的武器/工具、远程武器或盔甲。
     * 使用 Item.isEnchantable(ItemStack) 而不是 ItemStack.isEnchantable()，
     * 因此已经拥有普通附魔的武器/工具/远程武器/盔甲仍然可以镀金。
     */
    public static boolean isEligible(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageableItem()
                && stack.getItem().isEnchantable(stack)
                && (stack.getItem() instanceof ArmorItem
                        || stack.getItem() instanceof ProjectileWeaponItem
                        || getBaseAttackDamage(stack) > 0.0D);
    }

    public static double getBaseAttackDamage(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(BASE_ATTACK_DAMAGE_KEY)) {
            @SuppressWarnings("deprecation")
            double value = data.getUnsafe().getDouble(BASE_ATTACK_DAMAGE_KEY);
            return value;
        }
        return getRawAttackDamage(stack);
    }

    public static int getBaseMaxDamage(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(BASE_MAX_DAMAGE_KEY)) {
            @SuppressWarnings("deprecation")
            int value = data.getUnsafe().getInt(BASE_MAX_DAMAGE_KEY);
            return Math.max(0, value);
        }
        return stack.getMaxDamage();
    }

    public static ItemStack applyGilding(ItemStack base, int targetLevel) {
        ItemStack result = base.copy();
        int baseMaxDamage = getBaseMaxDamage(base);
        double baseAttackDamage = getBaseAttackDamage(base);

        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putInt(LEVEL_KEY, targetLevel);
            tag.putInt(BASE_MAX_DAMAGE_KEY, baseMaxDamage);
            tag.putDouble(BASE_ATTACK_DAMAGE_KEY, baseAttackDamage);
        });
        // 盔甲镀金不提高耐久上限；耐久加成仅适用于工具和武器。
        setMaxDamageSafely(result,
                base.getItem() instanceof ArmorItem ? baseMaxDamage : scaledMaxDamage(baseMaxDamage, targetLevel));
        return result;
    }

    /**
     * 写入耐久上限。{@code minecraft:max_damage} 只接受正整数，写入 0 会让这件物品在存档
     * （玩家背包、箱子、掉落物）序列化时编码失败；基础耐久异常时保持物品原有上限不动。
     */
    private static void setMaxDamageSafely(ItemStack stack, int maxDamage) {
        if (maxDamage > 0) {
            stack.set(DataComponents.MAX_DAMAGE, maxDamage);
            // 降低镀金（炼药锅清洗、/orimodify reduce）会让耐久上限变小；若已损耗的耐久超过
            // 新上限，下一次使用就会直接损坏。这里保证至少保留 1 点耐久，不把装备「洗爆」。
            Integer damage = stack.get(DataComponents.DAMAGE);
            if (damage != null && damage >= maxDamage) {
                stack.set(DataComponents.DAMAGE, maxDamage - 1);
            }
        }
    }

    /** 设置镀金等级；等级为 0 时移除镀金数据并恢复基础耐久上限。 */
    public static ItemStack setGildingLevel(ItemStack base, int targetLevel) {
        int level = Math.max(0, Math.min(255, targetLevel));
        if (level > 0) {
            return applyGilding(base, level);
        }

        ItemStack result = base.copy();
        int baseMaxDamage = getBaseMaxDamage(base);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.remove(LEVEL_KEY);
            tag.remove(BASE_MAX_DAMAGE_KEY);
            tag.remove(BASE_ATTACK_DAMAGE_KEY);
        });
        setMaxDamageSafely(result, baseMaxDamage);
        return result;
    }

    /**
     * 远程武器提示框显示的射弹伤害参考值：弓（满蓄力）为
     * {@value #BOW_FULL_CHARGE_PROJECTILE_DAMAGE} 点，弩为 {@value #CROSSBOW_PROJECTILE_DAMAGE} 点。
     *
     * <p><b>纯展示</b>：本模组不改动箭矢的基础伤害结算，原版伤害浮动与随机暴击保持原样
     * （空中 ×1.5 远程暴击见 {@link RangedCriticalModule}，属情境性加成，不计入展示值）。
     * 该值只用于提示框的「x 射弹伤害」行（见
     * {@code OrigoldsClientEvents#appendProjectileDamageTooltip}），镀金武器会按镀金倍率
     * 折算后显示。无法确定参考伤害的其它 {@link ProjectileWeaponItem} 子类返回 0，
     * 表示「未知，不显示」。</p>
     */
    public static double fullChargeProjectileDamage(ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            return BOW_FULL_CHARGE_PROJECTILE_DAMAGE;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return CROSSBOW_PROJECTILE_DAMAGE;
        }
        return 0.0D;
    }

    public static double scaledAttackDamage(ItemStack stack) {
        int level = getLevel(stack);
        return scaledAttackDamage(getBaseAttackDamage(stack), level);
    }

    /**
     * 返回该镀金等级相对于基础属性的总加成比例。
     *
     * <p>1～3 级沿用原有规则；3 级之后按所在等级区间逐级累加：4～39 级每级
     * 增加 100%，40～75 级每级增加 150%，依此类推。</p>
     */
    public static double bonusMultiplier(int level) {
        int clampedLevel = Math.max(0, Math.min(255, level));
        if (clampedLevel <= 0) {
            return 0.0D;
        }
        if (clampedLevel <= 2) {
            return 0.25D * clampedLevel;
        }
        if (clampedLevel == 3) {
            return 1.0D;
        }

        long bonusPercent = 100L; // 4～39 级
        int remainingLevels = clampedLevel - 3;
        int firstTierLevels = Math.min(remainingLevels, 36);
        long totalPercent = 100L + (long) firstTierLevels * bonusPercent;
        remainingLevels -= firstTierLevels;

        bonusPercent = 150L; // 40～75 级
        int tierLevels = Math.min(remainingLevels, 36);
        totalPercent += (long) tierLevels * bonusPercent;
        remainingLevels -= tierLevels;

        bonusPercent = 250L; // 76～111 级
        tierLevels = Math.min(remainingLevels, 36);
        totalPercent += (long) tierLevels * bonusPercent;
        remainingLevels -= tierLevels;

        bonusPercent = 500L; // 112～147 级
        tierLevels = Math.min(remainingLevels, 36);
        totalPercent += (long) tierLevels * bonusPercent;
        remainingLevels -= tierLevels;

        bonusPercent = 700L; // 148～183 级
        tierLevels = Math.min(remainingLevels, 36);
        totalPercent += (long) tierLevels * bonusPercent;
        remainingLevels -= tierLevels;

        bonusPercent = 950L; // 184～219 级
        tierLevels = Math.min(remainingLevels, 36);
        totalPercent += (long) tierLevels * bonusPercent;
        remainingLevels -= tierLevels;

        totalPercent += (long) remainingLevels * 1250L; // 220～255 级
        return totalPercent / 100.0D;
    }

    public static double scaledAttackDamage(double baseAttackDamage, int level) {
        if (level <= 0) {
            return baseAttackDamage;
        }
        double scaled = baseAttackDamage * (1.0D + bonusMultiplier(level));
        return Math.max(level >= 3 ? 2.0D : 1.0D, ceilToQuarter(scaled));
    }

    /**
     * 返回镀金武器对<b>整段</b>原始攻击伤害的倍率。
     *
     * <p>镀金不再把倍率预乘到武器的攻击力属性上；而是把这个倍率作用于攻击时
     * 结算的原始伤害。因为力量、虚弱、锋利、亡灵杀手、节肢杀手、穿刺、致密、
     * 重锤下落增伤等一切直接加减攻击数值的效果，最终都会汇入这一整段原始伤害，
     * 所以统一乘上该倍率即可让镀金同步放大或缩小所有这些加成。</p>
     */
    public static double gildingDamageMultiplier(int level) {
        return level <= 0 ? 1.0D : 1.0D + bonusMultiplier(level);
    }

    public static int scaledMaxDamage(int baseMaxDamage, int level) {
        long scaled = (long) Math.ceil(baseMaxDamage * (1.0D + bonusMultiplier(level)));
        scaled = Math.max(level >= 3 ? 500L : 250L, scaled);
        return (int) Math.min(Integer.MAX_VALUE, scaled);
    }

    /** 返回物品自身的攻击力贡献加上玩家默认的 1 点攻击力。 */
    public static double getRawAttackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null || modifiers.modifiers().isEmpty()) {
            modifiers = stack.getItem().getDefaultAttributeModifiers();
        }

        boolean found = false;
        double total = 1.0D;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.slot().test(EquipmentSlot.MAINHAND)
                    || !entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                continue;
            }
            found = true;
            AttributeModifier modifier = entry.modifier();
            switch (modifier.operation()) {
                case ADD_VALUE -> total += modifier.amount();
                case ADD_MULTIPLIED_BASE -> total += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> total += modifier.amount() * total;
            }
        }
        return found ? Math.max(0.0D, total) : 0.0D;
    }

    private static double ceilToQuarter(double value) {
        return Math.ceil((value * 4.0D) - 1.0E-9D) / 4.0D;
    }
}
