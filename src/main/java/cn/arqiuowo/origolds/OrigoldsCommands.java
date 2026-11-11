package cn.arqiuowo.origolds;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /orimodify 指令。 */
public final class OrigoldsCommands {
    private static final int MAX_GILDING_LEVEL = 255;

    private OrigoldsCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("orimodify")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("object", EntityArgument.entities())
                        .then(Commands.literal("gild")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("n", IntegerArgumentType.integer(0, MAX_GILDING_LEVEL))
                                                .executes(context -> set(context,
                                                        IntegerArgumentType.getInteger(context, "n")))))
                                .then(Commands.literal("add")
                                        .executes(context -> add(context, 1))
                                        .then(Commands.argument("n", IntegerArgumentType.integer(1, MAX_GILDING_LEVEL))
                                                .executes(context -> add(context,
                                                        IntegerArgumentType.getInteger(context, "n")))))
                                .then(Commands.literal("reduce")
                                        .executes(context -> reduce(context, 1))
                                        .then(Commands.argument("n", IntegerArgumentType.integer(1, MAX_GILDING_LEVEL))
                                                .executes(context -> reduce(context,
                                                        IntegerArgumentType.getInteger(context, "n"))))))));
    }

    private static int set(CommandContext<CommandSourceStack> context, int level) throws CommandSyntaxException {
        return modify(context, Operation.SET, level);
    }

    private static int add(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        return modify(context, Operation.ADD, amount);
    }

    private static int reduce(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        return modify(context, Operation.REDUCE, amount);
    }

    private static int modify(CommandContext<CommandSourceStack> context, Operation operation, int amount)
            throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "object");
        int modified = 0;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity livingEntity)) {
                context.getSource().sendFailure(Component.translatable("command.origolds.gild.not_living", target.getDisplayName()));
                continue;
            }

            ItemStack heldItem = livingEntity.getMainHandItem();
            if (heldItem.isEmpty() || !GildingUtil.isEligible(heldItem)) {
                context.getSource().sendFailure(Component.translatable(
                        "command.origolds.gild.invalid_item", target.getDisplayName()));
                continue;
            }

            int currentLevel = GildingUtil.getLevel(heldItem);
            int targetLevel = switch (operation) {
                case SET -> amount;
                case ADD -> Math.min(MAX_GILDING_LEVEL, currentLevel + amount);
                case REDUCE -> Math.max(0, currentLevel - amount);
            };
            Component itemName = heldItem.getHoverName();
            ItemStack updatedItem = GildingUtil.setGildingLevel(heldItem, targetLevel);
            livingEntity.setItemInHand(InteractionHand.MAIN_HAND, updatedItem);
            if (livingEntity instanceof Player player) {
                player.sendSystemMessage(createSuccessMessage(operation, amount, itemName, targetLevel));
            }
            modified++;
        }
        return modified;
    }

    private static Component createSuccessMessage(Operation operation, int amount, Component itemName, int targetLevel) {
        return switch (operation) {
            case SET -> Component.translatable("command.origolds.gild.set", itemName, targetLevel);
            case ADD -> Component.translatable(amount == 1
                    ? "command.origolds.gild.add.one"
                    : "command.origolds.gild.add.many", itemName, amount);
            case REDUCE -> Component.translatable(amount == 1
                    ? "command.origolds.gild.reduce.one"
                    : "command.origolds.gild.reduce.many", itemName, amount);
        };
    }

    private enum Operation {
        SET,
        ADD,
        REDUCE
    }
}
