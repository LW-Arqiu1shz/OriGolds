package cn.arqiuowo.origolds;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** OriGolds 自定义进度触发器注册表。 */
public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, OriGolds.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, GildingStateTrigger> GILDING_STATE =
            TRIGGERS.register("gilding_state", GildingStateTrigger::new);

    private ModTriggers() {
    }
}
