package cn.arqiuowo.origolds;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 远程空中暴击模块：玩家<b>发射瞬间</b>若处于空中，该箭命中时造成 150% 伤害。
 *
 * <p>判定在箭矢进入世界的服务端 tick 完成（玩家拉弓到 {@code addFreshEntity}
 * 同 tick 同步），此刻玩家的 {@code onGround()} 即发射瞬间的状态。「处于空中」的
 * 语义与原版近战暴击的跳跃攻击对齐（但更宽松）：离地、非骑乘、不在水中、不在攀爬。
 * 跳起、下落、鞘翼滑翔与创造飞行均算空中；骑马射箭、游泳、爬梯子不算。</p>
 *
 * <p>标记写入箭实体的持久化数据，随存档保存——蓄力中途落地不丢暴击，
 * 射出的箭跨区块加载后命中依然结算。</p>
 *
 * <p>倍率以<b>乘法</b>作用于 {@link LivingIncomingDamageEvent} 结算链末端：
 * 原版伤害浮动与随机暴击、力量等附魔、本模组镀金倍率、附属模组（origolds_ar）
 * 的攻击力/暴击伤害词条全部照常生效，与本暴击相互叠加、互不覆盖。本模块
 * <b>不改动</b>弓/弩的基础伤害结算（伤害浮动移除功能已按需求删除）。</p>
 *
 * <p>所有结算只作用于<b>玩家射出</b>的箭矢；骷髅、掠夺者、发射器等非玩家来源
 * 仍走原版。</p>
 */
public final class RangedCriticalModule {
    /** 远程空中暴击的伤害倍率：发射瞬间玩家处于空中时命中伤害 ×1.5。 */
    public static final double AIR_CRIT_MULTIPLIER = 1.5D;

    /** 箭实体持久化数据：发射瞬间玩家是否处于空中（本模组远程暴击标记）。 */
    private static final String AIR_CRIT_KEY = "origolds_air_critical";
    /** 低于该速度视为世界加载还原的旧箭矢，而非本 tick 刚射出的新箭。 */
    private static final double MIN_LAUNCH_SPEED = 1.0E-4D;

    private RangedCriticalModule() {
    }

    /**
     * 箭矢进入世界的瞬间：若发射玩家处于空中，打上远程暴击标记。
     *
     * <p>初速大于阈值用来区分「刚射出」与「区块加载还原的旧箭」——还原实体
     * 不带运动数据，速度为 0，不重新判定，沿用存档里已有的标记。</p>
     */
    public static void onArrowSpawned(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof Player player)) {
            return;
        }
        if (arrow.getDeltaMovement().length() <= MIN_LAUNCH_SPEED) {
            return;
        }
        if (isAirborne(player)) {
            arrow.getPersistentData().putBoolean(AIR_CRIT_KEY, true);
        }
    }

    /**
     * 「处于空中」的判定：离地、非骑乘、不在水中、不在攀爬。
     */
    private static boolean isAirborne(Player player) {
        return !player.onGround()
                && !player.isPassenger()
                && !player.isInWater()
                && !player.onClimbable();
    }

    /**
     * 玩家箭矢命中结算：发射瞬间玩家处于空中 → 命中伤害 ×1.5。
     *
     * <p>乘法直接作用在 {@code event.getAmount()} 上：附属 origolds_ar 在同一事件里
     * 按「弓暴击箭 / 弩箭」条件追加的暴击伤害词条倍率与注册武器的攻击力加成都会
     * 原样保留，并与本暴击相互叠加。</p>
     */
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        // 只对玩家射出的箭启用；骷髅满蓄力弓、掠夺者弩等怪物箭矢保持原版结算。
        if (!(arrow.getOwner() instanceof Player)) {
            return;
        }
        if (arrow.getPersistentData().getBoolean(AIR_CRIT_KEY)) {
            event.setAmount(event.getAmount() * (float) AIR_CRIT_MULTIPLIER);
        }
    }
}
