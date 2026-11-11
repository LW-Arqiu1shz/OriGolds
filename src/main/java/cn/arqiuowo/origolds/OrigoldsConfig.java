package cn.arqiuowo.origolds;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/** OriGolds 的 NeoForge 配置。 */
public final class OrigoldsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_GILDING_EFFECT;
    public static final ModConfigSpec.BooleanValue ENABLE_GILDING_GRADIENT;
    public static final ModConfigSpec.ConfigValue<String> GRADIENT_COLOR;
    public static final ModConfigSpec.DoubleValue GRADIENT_OPACITY;

    public static final String FILE_NAME = "origolds-client.toml";

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "作出更改后，保存文件即可应用更改，无需重启游戏。",
                "Saving the file after your modification will update the settings. No need for a game restart.");
        // 段名保持不变（改名会让玩家已有的配置文件失效）；只加翻译键，供配置界面显示。
        builder.translation("config.origolds.rendering").push("渲染选项/Rendering options");

        ENABLE_GILDING_EFFECT = builder
                .comment(
                        "控制是否启用镀金特效/Controls whether gilded items glint",
                        "默认值/Default: true")
                .translation("config.origolds.enable_gilding_effect")
                .define("enableGildingEffect", true);
        ENABLE_GILDING_GRADIENT = builder
                .comment(
                        "控制是否启用镀金颜色渐变，关闭后使用固定颜色滤镜/Controls whether gilding glint has a \"breathing\" effect. False means static glint.",
                        "默认值/Default: true")
                .translation("config.origolds.enable_gilding_gradient")
                .define("enableGildingGradient", true);
        GRADIENT_COLOR = builder
                .comment(
                        "渐变滤镜颜色/Color of gilding glint",
                        "值格式(不区分大小写)/Format(Capitals or not doen't matter):",
                        "#RRGGBB",
                        "默认值/Default: #FFFF00")
                .translation("config.origolds.gradient_color")
                .define(List.of("gradientColor"), () -> "#ffff00",
                        value -> value instanceof String string && string.matches("#[0-9a-fA-F]{6}"),
                        String.class);
        GRADIENT_OPACITY = builder
                .comment(
                        "渐变滤镜透明度/Opacity of gilding glint",
                        "接受值域/Range: 0.0 ~ 1.0",
                        "默认值/Default: 0.6")
                .translation("config.origolds.gradient_opacity")
                .defineInRange("gradientOpacity", 0.6D, 0.0D, 1.0D);

        builder.pop();
        SPEC = builder.build();
    }

    private OrigoldsConfig() {
    }

    /** 返回配置的目标颜色，格式为 RGB。 */
    public static int getConfiguredTargetColor() {
        return parseRgb(GRADIENT_COLOR.get());
    }

    /** 返回滤镜混合强度，范围为 0.0 到 1.0。 */
    public static float getConfiguredOpacity() {
        return (float) GRADIENT_OPACITY.get().doubleValue();
    }

    private static int parseRgb(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}")) {
            return 0xFFFF00;
        }
        try {
            return Integer.parseInt(value.substring(1), 16);
        } catch (NumberFormatException ignored) {
            return 0xFFFF00;
        }
    }
}
