package cn.arqiuowo.origolds;

import cn.arqiuowo.origolds.client.OrigoldsBossInfoOverlay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;

import java.util.ArrayList;
import java.util.List;

/** 客户端物品提示显示逻辑。 */
@EventBusSubscriber(modid = OriGolds.MOD_ID, value = Dist.CLIENT)
public final class OrigoldsClientEvents {
    private OrigoldsClientEvents() {
    }

    /** 退出服务器时清掉 BOSS 信息栏，避免文本残留到下一次进入的世界。 */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        OrigoldsBossInfoOverlay.setMessage(null);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // 物品实用提示与废话：无论是否镀金都追加在提示框最底部。
        appendUsefulHint(stack, event.getToolTip());

        // 消耗箭矢的远程武器：显示「x 射弹伤害」，与近战「攻击伤害」属性行同位置。
        appendProjectileDamageTooltip(stack, event.getToolTip());

        int level = GildingUtil.getLevel(stack);
        if (level <= 0) {
            return;
        }

        // 把提示框里原有「攻击伤害」属性行的数值，改写为镀金后的实际攻击力，
        // 让战利品数值直接显示在原来的位置上，而不再额外追加一行「镀金攻击力」。
        overrideGildedAttackTooltip(stack, level, event.getToolTip());

        event.getToolTip().add(Component.translatable(
                        "tooltip.origolds.gilding",
                        GildingUtil.formatLevel(level))
                .withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("tooltip.origolds.gilding_hint")
                .withStyle(ChatFormatting.GRAY));
        if (Screen.hasControlDown()) {
            if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
                event.getToolTip().add(Component.translatable(
                                "tooltip.origolds.gilding_armor_durability",
                                level + 1)
                        .withStyle(ChatFormatting.YELLOW));
                event.getToolTip().add(Component.translatable(
                                "tooltip.origolds.gilding_armor_shield",
                                GildingUtil.formatLevel(level))
                        .withStyle(ChatFormatting.YELLOW));
            } else if (stack.getItem() instanceof ProjectileWeaponItem) {
                event.getToolTip().add(Component.translatable("tooltip.origolds.gilding_ranged_bonus",
                                (int) Math.round(GildingUtil.bonusMultiplier(level) * 100.0D))
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.origolds.gilding_bonus",
                                (int) Math.round(GildingUtil.bonusMultiplier(level) * 100.0D))
                        .withStyle(ChatFormatting.YELLOW));
                // 「镀金攻击力」不再单独追加一行：实际攻击力已改写进
                // 原有的「攻击伤害」属性行（见 overrideGildedAttackTooltip）。
            }
        }
    }

    /**
     * 为消耗箭矢的远程武器追加「x 射弹伤害」提示行。
     *
     * <p>数值取 {@link GildingUtil#fullChargeProjectileDamage} 给出的<b>展示用</b>参考伤害：
     * 满蓄力弓 6 点，弩 9 点。基础结算不做任何改动——原版弓/弩的伤害浮动与随机暴击保持
     * 原样（空中 ×1.5 远程暴击属情境性加成，见 {@link RangedCriticalModule}，不计入展示值），
     * 这里只提供与近战「攻击伤害」属性行对等的可读数值。武器镀金时按镀金倍率
     * 折算成实际射弹伤害显示，与近战行的处理方式一致。</p>
     *
     * <p>排版完全复刻原版属性行：先插一行空行，再插灰色分组头「手持时：」
     * （原版键 {@code item.modifiers.hand}，由 {@link EquipmentSlotGroup#HAND} 拼出——
     * 弓弩主副手皆可持用，故不用「在主手时：」），最后是深绿色、前置一个
     * 空格缩进的数值行（原版键 {@code attribute.modifier.equals.0} + 属性名
     * {@code attribute.origolds.projectile_damage}），与近战攻击伤害那一行同字体
     * 颜色、同缩进、同分组头。无法确定参考伤害的其它 {@code ProjectileWeaponItem}
     * 子类不显示。</p>
     */
    private static void appendProjectileDamageTooltip(ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof ProjectileWeaponItem) || tooltip.isEmpty()) {
            return;
        }
        double base = GildingUtil.fullChargeProjectileDamage(stack);
        if (base <= 0.0D) {
            return;
        }
        int level = GildingUtil.getLevel(stack);
        double shown = (level > 0 && GildingUtil.isEligible(stack))
                ? GildingUtil.scaledAttackDamage(base, level)
                : base;
        // 与原版 addAttributeTooltips 相同的三段式排版：空行 + 灰色分组头 + 深绿缩进数值行。
        // 分组头键与原版一样由 EquipmentSlotGroup 的序列化名拼出；这里用 HAND 组，
        // 复用原版译名「手持时：」，表示主手、副手皆生效。
        tooltip.add(1, CommonComponents.space()
                .append(Component.translatable("attribute.modifier.equals.0",
                        formatDamage(shown),
                        Component.translatable("attribute.origolds.projectile_damage")))
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(1, Component.translatable(
                        "item.modifiers." + EquipmentSlotGroup.HAND.getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(1, CommonComponents.EMPTY);
    }

    /**
     * 将提示框里原有的「攻击伤害」属性行改写为镀金后的实际攻击力。
     *
     * <p>原版属性行是按属性修饰器给出的组件，key 形如
     * {@code attribute.modifier.equals.N} 且参数 {@code args[0]} 是数值文本、
     * {@code args[1]} 是属性名组件（攻击伤害对应
     * {@code attribute.name.generic.attack_damage}）。普通武器的攻击伤害行由
     * {@code 空格 + 上述可译组件} 组成——真正的 {@code attribute.modifier.*}
     * 可译组件是该行的<b>兄弟节点</b>，而不是行根节点的内容，因此必须递归地
     * 在兄弟组件里定位并替换数值，而不是只看行根节点。</p>
     *
     * <p>仅在可镀金的近战武器上生效；盔甲与远程武器没有攻击伤害属性行，直接跳过。
     * 注意这只影响<b>提示框显示</b>，伤害结算仍由 {@link OrigoldsEvents} 在
     * {@code LivingDamageEvent.Pre} 里按镀金倍率处理，不在此处重复计算。</p>
     */
    private static void overrideGildedAttackTooltip(ItemStack stack, int level, List<Component> tooltip) {
        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof ProjectileWeaponItem) {
            return;
        }
        double base = GildingUtil.getBaseAttackDamage(stack);
        if (base <= 0.0D) {
            return;
        }
        String gildedText = formatDamage(GildingUtil.scaledAttackDamage(base, level));

        for (int i = 0; i < tooltip.size(); i++) {
            Component original = tooltip.get(i);
            Component replaced = replaceAttackDamageInComponent(original, gildedText);
            if (replaced != original) {
                tooltip.set(i, replaced);
                break;
            }
        }
    }

    /**
     * 递归替换组件树里的「攻击伤害」属性行数值。
     *
     * <p>原样返回未发生替换的组件；一旦在任意一层（内容或其兄弟组件）发现攻击伤害
     * 属性行，就用新数值重建该组件，并保留其原有样式与其余兄弟组件。</p>
     */
    private static Component replaceAttackDamageInComponent(Component component, String newText) {
        if (!(component instanceof MutableComponent mutable)) {
            return component;
        }

        ComponentContents contents = mutable.getContents();
        ComponentContents newContents = replaceAttackDamageContents(contents, newText);
        boolean changed = newContents != contents;

        List<Component> siblings = mutable.getSiblings();
        List<Component> newSiblings = null;
        if (!siblings.isEmpty()) {
            newSiblings = new ArrayList<>(siblings.size());
            for (int i = 0; i < siblings.size(); i++) {
                Component s = siblings.get(i);
                Component ns = replaceAttackDamageInComponent(s, newText);
                newSiblings.add(ns);
                if (ns != s) {
                    changed = true;
                }
            }
        }

        if (!changed) {
            return component;
        }

        MutableComponent rebuilt = MutableComponent.create(newContents);
        rebuilt.setStyle(mutable.getStyle());
        if (newSiblings != null) {
            for (Component s : newSiblings) {
                rebuilt.append(s);
            }
        }
        return rebuilt;
    }

    /**
     * 若 {@code contents} 恰好是「攻击伤害」属性行，则替换其数值并返回新的内容；
     * 否则原样返回原内容。
     *
     * <p>通过参数中属性名组件（{@code attribute.name.generic.attack_damage}）
     * 精确识别攻击伤害行，避免误伤攻击速度、盔甲等其它属性行。</p>
     */
    private static ComponentContents replaceAttackDamageContents(ComponentContents contents, String newText) {
        if (contents instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            Object[] args = translatable.getArgs();
            if (key != null
                    && key.startsWith("attribute.modifier.")
                    && args.length >= 2
                    && args[1] instanceof Component attrName
                    && attrName.getContents() instanceof TranslatableContents attrContents
                    && "attribute.name.generic.attack_damage".equals(attrContents.getKey())) {
                Object[] newArgs = new Object[args.length];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                newArgs[0] = newText;
                return Component.translatable(key, newArgs).getContents();
            }
        }
        return contents;
    }

    /**
     * 将伤害数值按<b>原版属性行同款</b>格式化成文本。
     *
     * <p>直接复用 {@link ItemAttributeModifiers#ATTRIBUTE_MODIFIER_FORMAT}（模式
     * {@code #.##}），因此射弹伤害行与近战「攻击伤害」行、镀金改写后的攻击伤害行
     * 的数字排版规则完全一致（整数不带小数点，最多两位小数）。</p>
     */
    private static String formatDamage(double damage) {
        return ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(damage);
    }

    /** 为金源系列物品追加“实用提示”与“废话”，实用提示浅蓝色，废话浅灰色斜体。 */
    private static void appendUsefulHint(ItemStack stack, List<Component> tooltip) {
        Item item = stack.getItem();
        String key = null;
        if (item == ModItems.ORIGOLD_MOLD.get()) {
            key = "origold_mold";
        } else if (item == ModItems.COMPOUND_ORIGOLD_MOLD.get()) {
            key = "compound_origold_mold";
        } else if (item == ModItems.POLYFUSIVE_ORIGOLD_MOLD.get()) {
            key = "polyfusive_origold_mold";
        } else if (item == ModItems.ORIGOLD_COATBOARD.get()) {
            key = "origold_coatboard";
        } else if (item == ModItems.COMPOUND_ORIGOLD_COATBOARD.get()) {
            key = "compound_origold_coatboard";
        } else if (item == ModItems.POLYFUSIVE_ORIGOLD_COATBOARD.get()) {
            key = "polyfusive_origold_coatboard";
        } else if (item == ModItems.MAGIGOLD_CORE.get()) {
            key = "magigold_core";
        } else if (item == ModItems.ORIGOLD_COATING_SMITHING_TEMPLATE.get()) {
            key = "origold_coating_smithing_template";
        }
        if (key == null) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.origolds.useful." + key)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.origolds.bs." + key)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
