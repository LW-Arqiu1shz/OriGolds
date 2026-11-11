package cn.arqiuowo.origolds;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** OriGolds 的事件逻辑。 */
public final class OrigoldsEvents {
    private static final ResourceLocation PIGLIN_BRUTE_ATTACK_REDUCTION_ID =
            ResourceLocation.fromNamespaceAndPath(OriGolds.MOD_ID, "piglin_brute_attack_reduction");

    /**
     * 同一线程上最近一次玩家近战攻击的精确暴击倍率，附带被击目标 id。
     *
     * <p>{@code CriticalHitEvent} 与随后的 {@code LivingDamageEvent.Pre} 在同一个
     * 服务端线程上同步触发，因此用 {@link ThreadLocal} 暂存即可拿到本次攻击真实的
     * 暴击倍率（含其它模组对 {@code CriticalHitEvent} 的修改），不必再只靠伤害反推。
     * 记录目标 id 是为了让横扫等「一次暴击判定、多次伤害结算」的场景只对真正的
     * 主目标使用该倍率。客户端线程即使写入也不会污染服务端结算。</p>
     */
    private static final ThreadLocal<CriticalBonus> CRITICAL_MULTIPLIER = new ThreadLocal<>();

    /** 一次暴击判定的结果：被击目标 id 与最终暴击倍率。 */
    private record CriticalBonus(int targetId, float multiplier) {
    }

    private OrigoldsEvents() {
    }

    /** 调整原版金质装备与工具的默认组件。 */
    public static void modifyVanillaComponents(ModifyDefaultComponentsEvent event) {
        event.modify(Items.GOLDEN_SWORD, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 520);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    SwordItem.createAttributes(Tiers.GOLD, 5.5F, -2.4F));
        });

        event.modify(Items.GOLDEN_PICKAXE, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 520);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    DiggerItem.createAttributes(Tiers.GOLD, 3.5F, -2.8F));
            builder.set(DataComponents.TOOL, goldPickaxeTool());
        });

        event.modify(Items.GOLDEN_AXE, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 520);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    DiggerItem.createAttributes(Tiers.GOLD, 7.0F, -3.0F));
        });

        event.modify(Items.GOLDEN_SHOVEL, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 520);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    DiggerItem.createAttributes(Tiers.GOLD, 4.0F, -3.0F));
        });

        event.modify(Items.GOLDEN_HOE, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 520);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    DiggerItem.createAttributes(Tiers.GOLD, 0.0F, -1.0F));
        });

        // 金盔甲材料耐久基数从 7 调整为 21，各部位沿用原版倍率。
        event.modify(Items.GOLDEN_HELMET, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 231);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, goldArmorAttributes("helmet", 3, EquipmentSlotGroup.HEAD));
        });
        event.modify(Items.GOLDEN_CHESTPLATE, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 336);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS,
                    goldArmorAttributes("chestplate", 7, EquipmentSlotGroup.CHEST));
        });
        event.modify(Items.GOLDEN_LEGGINGS, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 315);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, goldArmorAttributes("leggings", 5, EquipmentSlotGroup.LEGS));
        });
        event.modify(Items.GOLDEN_BOOTS, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, 273);
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, goldArmorAttributes("boots", 2, EquipmentSlotGroup.FEET));
        });
    }

    /** 创建指定部位的金盔甲防御属性，四件合计为 18 点。 */
    private static ItemAttributeModifiers goldArmorAttributes(String slotName, int defense,
            EquipmentSlotGroup slotGroup) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(OriGolds.MOD_ID, "gold_armor_" + slotName),
                                defense,
                                AttributeModifier.Operation.ADD_VALUE),
                        slotGroup)
                .build();
    }

    private static Tool goldPickaxeTool() {
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_IRON_TOOL),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, Tiers.GOLD.getSpeed())),
                1.0F,
                1);
    }

    /**
     * 在原版耐久附魔处理前缩小镀金盔甲的耐久损耗，并向上取整。
     * 最终耐久损耗 = 本应扣除的耐久 / ((镀金等级 + 1)^2 / 4)。
     */
    public static void onArmorHurt(ArmorHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }

            ItemStack armor = event.getArmorItemStack(slot);
            if (!(armor.getItem() instanceof ArmorItem)
                    || !GildingUtil.isEligible(armor)) {
                continue;
            }

            int gildingLevel = GildingUtil.getLevel(armor);
            Float newDamage = event.getNewDamage(slot);
            if (gildingLevel <= 0 || newDamage == null || newDamage <= 0.0F) {
                continue;
            }

            double divisor = ((double) gildingLevel + 1.0D) * (gildingLevel + 1.0D) / 4.0D;
            float reducedDamage = (float) Math.ceil(newDamage / divisor);
            event.setNewDamage(slot, reducedDamage);
        }
    }

    /**
     * 在原版护甲、护甲韧性等减伤完成后，按镀金盔甲总等级追加减伤，
     * 并对攻击者镀金武器的整段攻击伤害套用镀金倍率。
     *
     * <p>这里保留两个明确的中间变量名称，便于调试和后续数据驱动化：
     * aurum_shielding_level 是四个盔甲栏位镀金等级之和除以 4 后向下取整，
     * aurum_shielded_damage 是最终替换回伤害容器的伤害值。</p>
     *
     * <p>镀金不再预乘武器攻击力属性，而是用一个统一倍率作用于这段原始伤害。
     * 力量、虚弱、锋利、亡灵杀手、节肢杀手、穿刺、致密及重锤下落增伤等一切
     * 直接加减攻击数值的效果都会汇入这段原始伤害，因此都会被镀金同步放大或
     * 缩小，而非在镀金之后平直叠加。</p>
     */
    public static void onLivingDamagePre(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) {
            return;
        }

        int totalGildingLevel = 0;
        for (ItemStack armor : living.getArmorSlots()) {
            if (armor.getItem() instanceof ArmorItem && GildingUtil.isEligible(armor)) {
                totalGildingLevel += GildingUtil.getLevel(armor);
            }
        }
        int aurum_shielding_level = Math.min(255, totalGildingLevel / 4);

        // 攻击者武器/弓（或投掷物）的镀金倍率。
        double gildingBoost = 1.0D;
        ItemStack attackingWeapon = resolveAttackingWeapon(event.getSource().getDirectEntity(), living);
        // 药水、燃烧、摔落等非近战伤害来源没有武器，getWeaponItem() 会返回 null；
        // 此时只跳过镀金武器倍率这一段（gildingBoost 保持 1.0），
        // 受害者自身的镀金盔甲减伤仍照常结算，不能从本方法提前返回。
        if (attackingWeapon != null && !attackingWeapon.isEmpty()
                && GildingUtil.isEligible(attackingWeapon)) {
            gildingBoost = GildingUtil.gildingDamageMultiplier(GildingUtil.getLevel(attackingWeapon));
        }

        double aurum_shielded_damage = event.getContainer().getNewDamage();
        // 先修正暴击对附魔伤害的处理，再乘镀金倍率，使镀金倍率同样放大这段修正。
        aurum_shielded_damage = applyCritEnchantmentCorrection(event, attackingWeapon, living, aurum_shielded_damage);
        aurum_shielded_damage *= gildingBoost;
        aurum_shielded_damage *= aurumShieldDamageMultiplier(aurum_shielding_level);

        event.getContainer().setNewDamage((float) aurum_shielded_damage);
    }

    /**
     * 记录玩家本次近战攻击的精确暴击倍率。
     *
     * <p>其它模组（例如「金源工艺：古金极锻」的暴伤词条）会通过
     * {@link CriticalHitEvent#setDamageMultiplier(float)} 改写倍率，这里记下最终值，
     * 供随后的伤害结算使用，比事后从伤害反推更可靠。</p>
     */
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player == null || !event.isCriticalHit()) {
            CRITICAL_MULTIPLIER.remove();
            return;
        }
        CRITICAL_MULTIPLIER.set(new CriticalBonus(event.getTarget().getId(), event.getDamageMultiplier()));
    }

    // 远程箭矢的基础伤害结算不做改动：原版弓/弩的伤害浮动与随机暴击保持原样，
    // 提示框的「射弹伤害」行仅为展示（见 OrigoldsClientEvents）。
    // 唯一的例外是「发射瞬间玩家处于空中」的 ×1.5 远程暴击，见 RangedCriticalModule。

    /**
     * 修正原版暴击对「暴击前基数」的漏算。
     *
     * <p>原版在 {@code Player#attack} 里先算出一段<b>暴击前基数</b>——它由
     * 「武器基础攻击力 × 攻击强度」加上物品攻击加成（重锤的下落增伤，
     * 含「致密」的每格下落加成，见 {@code Item#getAttackDamageBonus}）组成；
     * 随后给这段基数乘上暴击倍率。附魔加成（锋利、亡灵杀手、节肢杀手、穿刺等，
     * 来自 {@link EnchantmentHelper#modifyDamage}）却是<b>在暴击之后</b>才追加的，
     * 因此永远不享受 ×1.5 的暴击倍率。</p>
     *
     * <p>重锤下落增伤虽然在原版里已经随基数一起暴击，但本模组不能假设这一点：
     * 这里显式把 {@code Item#getAttackDamageBonus} 并入暴击前基数，并以本次攻击
     * 真实的暴击倍率 {@code c} 计算目标总伤害
     * {@code (暴击前基数 + 附魔加成) × c}，再把与原始伤害的差值补进当前伤害。
     * 对原版而言，重锤增伤已在原始伤害中暴击，差值恰好只有附魔部分
     * {@code enchScaled × (c - 1)}，不会被重复放大；若某些环境下重锤增伤同样漏了
     * 暴击，这个差值会把它一并补上。</p>
     *
     * <p>修正量按当前伤害与原始伤害的比值缩放，因此护甲、保护附魔等减伤也会正确地
     * 作用到这段修正上。只对<b>玩家近战攻击</b>生效（伤害源的直击实体是玩家本人），
     * 避免误改投射物、药水等非近战伤害。非暴击时 {@code c = 1}，修正自然为 0。</p>
     *
     * @return 修正后的伤害值；不满足修正条件时原样返回。
     */
    private static double applyCritEnchantmentCorrection(LivingDamageEvent.Pre event,
            ItemStack weapon, LivingEntity victim, double currentDamage) {
        if (weapon == null || weapon.isEmpty() || victim.level().isClientSide()) {
            return currentDamage;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof Player attacker)
                || !(victim.level() instanceof ServerLevel serverLevel)) {
            return currentDamage;
        }

        // 本次攻击真实的暴击倍率（可能被其它模组改写过），读取后立即清除避免串到下一次攻击；
        // 只有被击目标与暴击判定目标一致时才采用，横扫的其余目标仍走下面的反推。
        CriticalBonus capturedCrit = CRITICAL_MULTIPLIER.get();
        CRITICAL_MULTIPLIER.remove();

        float attr = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (attr <= 0.0F) {
            return currentDamage;
        }
        // 复刻原版攻击强度缩放：基础部分按 (0.2 + 0.8 * f6^2) 缩放，
        // 附魔部分按 f6 缩放（f6 = getAttackStrengthScale(0.5F)）。
        float f6 = attacker.getAttackStrengthScale(0.5F);
        float attackStrength = 0.2F + 0.8F * f6 * f6;
        DamageSource source = event.getSource();

        // 暴击前基数 = 缩放后的武器攻击力 + 物品攻击加成（重锤下落增伤，含致密）。
        float weaponScaled = attr * attackStrength;
        float itemBonus = weapon.getItem().getAttackDamageBonus(victim, weaponScaled, source);
        float preCritBase = weaponScaled + itemBonus;

        float enchanted = EnchantmentHelper.modifyDamage(serverLevel, weapon, victim, source, attr);
        float enchScaled = (enchanted - attr) * f6;
        if (preCritBase <= 0.0F || enchScaled <= 0.0F) {
            return currentDamage;
        }
        float orig = event.getOriginalDamage();
        if (orig <= 0.0F) {
            return currentDamage;
        }

        // 优先用 CriticalHitEvent 记录的精确倍率，缺失时按 orig = 暴击前基数 * c + 附魔加成 反推。
        float c = capturedCrit != null && capturedCrit.targetId() == victim.getId()
                && capturedCrit.multiplier() > 1.0F
                        ? capturedCrit.multiplier()
                        : (orig - enchScaled) / preCritBase;
        if (c <= 1.0F + 1.0E-3F) {
            return currentDamage; // 非暴击，无需修正
        }

        // 目标：让「暴击前基数（含重锤下落增伤）」与附魔加成都享受同一暴击倍率。
        double desired = (preCritBase + enchScaled) * c;
        double correction = (desired - orig) * (currentDamage / orig);
        return currentDamage + correction;
    }

    /**
     * 解析造成本次伤害的攻击者所持武器：近战为主手，箭矢/投掷物取发射原武器。
     * 伤害源为空或指向受害者自身（如摔落、自伤等非攻击伤害）时返回空，避免
     * 把镀金倍率误加到非攻击伤害上。
     */
    private static ItemStack resolveAttackingWeapon(Entity direct, LivingEntity victim) {
        if (direct == null || direct == victim) {
            return ItemStack.EMPTY;
        }
        // 投掷药水等非武器弹射物的 getWeaponItem() 返回 null，统一归一成 EMPTY，
        // 保证任何调用方拿到的都是非 null 的 ItemStack。
        ItemStack weapon = direct.getWeaponItem();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    /** 返回指定镀金等级对应的剩余伤害比例。 */
    public static double aurumShieldDamageMultiplier(int level) {
        int clampedLevel = Math.max(0, Math.min(255, level));
        if (clampedLevel <= 0) {
            return 1.0D;
        }
        if (clampedLevel == 1) {
            return 0.875D; // 减伤 12.5%
        }
        if (clampedLevel == 2) {
            return 0.75D; // 减伤 25%
        }
        if (clampedLevel == 3) {
            return 0.5D; // 减伤 50%
        }

        // 4 级起每 36 级将剩余伤害减半，且区间内按相同指数比例递进。
        double perLevel = Math.pow(0.5D, 1.0D / 36.0D);
        int completedTiers = (clampedLevel - 3) / 36;
        int tierStart = 3 + completedTiers * 36;
        double tierStartMultiplier = Math.pow(0.5D, completedTiers + 1.0D);
        return tierStartMultiplier * Math.pow(perLevel, clampedLevel - tierStart);
    }

    /** 将猪灵蛮兵在当前难度下的最终攻击力降低 1 点。 */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof PiglinBrute piglinBrute) || event.getLevel().isClientSide()) {
            return;
        }

        AttributeInstance attackDamage = piglinBrute.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null && !attackDamage.hasModifier(PIGLIN_BRUTE_ATTACK_REDUCTION_ID)) {
            attackDamage.addTransientModifier(new AttributeModifier(
                    PIGLIN_BRUTE_ATTACK_REDUCTION_ID,
                    -1.0D,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** 为带有镀金数据的物品增加挖掘速度。 */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        int level = GildingUtil.getLevel(stack);
        if (level > 0 && !(stack.getItem() instanceof ArmorItem) && GildingUtil.isEligible(stack)) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + (float) GildingUtil.bonusMultiplier(level)));
        }
    }

    /**
     * 允许镀金物品与含水炼药锅交互，消耗一格水并降低一层镀金。
     * origolds_ar 已提供该功能时完全跳过，避免两个模组同时处理一次交互。
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ModList.get().isLoaded("origolds_ar")
                || event.getHand() == null
                || event.getLevel().isClientSide()) {
            return;
        }

        BlockState cauldronState = event.getLevel().getBlockState(event.getPos());
        if (!cauldronState.is(Blocks.WATER_CAULDRON)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        int gildingLevel = GildingUtil.getLevel(stack);
        if (gildingLevel <= 0 || !GildingUtil.isEligible(stack)
                || !event.getLevel().mayInteract(event.getEntity(), event.getPos())
                || !event.getEntity().mayUseItemAt(event.getPos(), event.getFace(), stack)) {
            return;
        }

        event.getEntity().setItemInHand(event.getHand(),
                GildingUtil.setGildingLevel(stack, gildingLevel - 1));
        LayeredCauldronBlock.lowerFillLevel(cauldronState, event.getLevel(), event.getPos());
        event.setCancellationResult(InteractionResult.sidedSuccess(false));
        event.setCanceled(true);
    }

    /** 周期性检查玩家的镀金状态进度（每 20 tick 一次，约 1 秒）。 */
    private static int gildingAdvancementTick = 0;

    public static void checkGildingAdvancements(ServerTickEvent.Post event) {
        if (++gildingAdvancementTick % 20 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        GildingStateTrigger trigger = ModTriggers.GILDING_STATE.get();
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            trigger.trigger(player);
        }
    }

    /** 玩家完成任意合成（含锻造台镀金）后立即检查一次，让进度在合成瞬间就能点亮。 */
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModTriggers.GILDING_STATE.get().trigger(serverPlayer);
        }
    }

}
