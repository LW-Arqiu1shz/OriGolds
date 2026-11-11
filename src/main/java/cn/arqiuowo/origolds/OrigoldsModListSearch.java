package cn.arqiuowo.origolds;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * NeoForge 26.3 模组列表的搜索只拿 {@code displayName()} 做包含匹配；本模组名称被翻译后，
 * 中文界面下搜英文原名就搜不到了。ModListSearchMixin 在搜索过滤处调用这里，
 * 把匹配用的文本换成"翻译名 + 原名"，显示的名称不变（与「金源工艺：工业时代」同一套做法）。
 *
 * <p>此类必须位于 Mixin 配置包之外，供 Mixin 安全引用。</p>
 */
public final class OrigoldsModListSearch {
    private OrigoldsModListSearch() {
    }

    public static Component searchName(String modId, Component shown) {
        if (!OriGolds.MOD_ID.equals(modId)) {
            return shown;
        }
        String original = ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse("");
        String translated = shown.getString();
        return original.isEmpty() || translated.equals(original)
                ? shown : Component.literal(translated + " " + original);
    }
}