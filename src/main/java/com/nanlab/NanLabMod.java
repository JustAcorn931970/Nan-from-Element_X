package com.nanlab;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.Locale;

public final class NanLabMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nan")
            .requires(source -> source.hasPermission(2))
            .executes(context -> help(context.getSource()))
            .then(Commands.literal("help").executes(context -> help(context.getSource())))
            .then(Commands.literal("list").executes(context -> list(context.getSource())))
            .then(Commands.literal("set")
                // /nan set <field-or-group> <value> applies to the command source player.
                .then(Commands.argument("field", StringArgumentType.word())
                    .then(Commands.argument("value", StringArgumentType.word())
                        .executes(NanLabMod::setSelf)))
                // /nan set <player> <field-or-group> <value> applies to another player.
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("field", StringArgumentType.word())
                        .then(Commands.argument("value", StringArgumentType.word())
                            .executes(NanLabMod::setTarget)))))
            .then(Commands.literal("reset")
                .executes(NanLabMod::resetSelf)
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(NanLabMod::resetTarget))));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("NaN Lab commands (requires cheats / permission level 2):"), false);
        source.sendSuccess(() -> Component.literal("/nan set <field> <nan|inf|-inf|number>"), false);
        source.sendSuccess(() -> Component.literal("/nan set <player> <field> <value>"), false);
        source.sendSuccess(() -> Component.literal("Groups: position, motion, rotation, speeds, attributes, vitals, hunger, all"), false);
        source.sendSuccess(() -> Component.literal("Fields: x y z motion_x motion_y motion_z yaw pitch fall_distance walk_speed fly_speed movement_speed armor attack_damage luck health absorption saturation exhaustion xp_progress"), false);
        source.sendSuccess(() -> Component.literal("/nan reset [player] - restore safe values and move the player to the dimension spawn"), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("NaN-capable fields: position (x/y/z), motion (x/y/z), rotation (yaw/pitch), fall_distance, walk_speed, fly_speed, movement_speed, armor, attack_damage, luck, health, absorption, saturation, exhaustion, xp_progress."), false);
        source.sendSuccess(() -> Component.literal("Use /nan set all nan to apply the experiment to every supported field."), false);
        return 1;
    }

    private static int setSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return apply(player, StringArgumentType.getString(context, "field"), StringArgumentType.getString(context, "value"), context.getSource());
    }

    private static int setTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "target");
        return apply(player, StringArgumentType.getString(context, "field"), StringArgumentType.getString(context, "value"), context.getSource());
    }

    private static int apply(ServerPlayer player, String field, String rawValue, CommandSourceStack source) {
        final double value;
        try {
            value = parseFloatingPoint(rawValue);
            applyField(player, field.toLowerCase(Locale.ROOT), value);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("NaN Lab: " + exception.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("NaN Lab: set " + player.getGameProfile().getName() + "'s " + field + " to " + rawValue + "."), true);
        return 1;
    }

    private static double parseFloatingPoint(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "nan", "+nan", "-nan" -> Double.NaN;
            case "inf", "+inf", "infinity", "+infinity" -> Double.POSITIVE_INFINITY;
            case "-inf", "-infinity" -> Double.NEGATIVE_INFINITY;
            default -> {
                try {
                    yield Double.parseDouble(input);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("'" + input + "' is not a valid floating-point value. Try nan, inf, -inf, or a decimal number.");
                }
            }
        };
    }

    private static void applyField(ServerPlayer player, String field, double value) {
        switch (field) {
            case "all" -> {
                // Do the other mutations first; corrupting position is deliberately last.
                applyField(player, "motion", value);
                applyField(player, "rotation", value);
                applyField(player, "fall_distance", value);
                applyField(player, "speeds", value);
                applyField(player, "attributes", value);
                applyField(player, "vitals", value);
                applyField(player, "hunger", value);
                applyField(player, "xp_progress", value);
                applyField(player, "position", value);
            }
            case "position", "pos" -> {
                applyField(player, "x", value);
                applyField(player, "y", value);
                applyField(player, "z", value);
            }
            case "motion", "velocity" -> {
                applyField(player, "motion_x", value);
                applyField(player, "motion_y", value);
                applyField(player, "motion_z", value);
            }
            case "rotation", "rot" -> {
                applyField(player, "yaw", value);
                applyField(player, "pitch", value);
            }
            case "speeds" -> {
                applyField(player, "walk_speed", value);
                applyField(player, "fly_speed", value);
                applyField(player, "movement_speed", value);
            }
            case "attributes", "attribute" -> {
                applyField(player, "movement_speed", value);
                applyField(player, "armor", value);
                applyField(player, "attack_damage", value);
                applyField(player, "luck", value);
            }
            case "vitals", "survival" -> {
                applyField(player, "health", value);
                applyField(player, "absorption", value);
            }
            case "hunger", "food" -> {
                applyField(player, "saturation", value);
                applyField(player, "exhaustion", value);
            }
            case "x" -> player.setPos(value, player.getY(), player.getZ());
            case "y" -> player.setPos(player.getX(), value, player.getZ());
            case "z" -> player.setPos(player.getX(), player.getY(), value);
            case "motion_x" -> setMotion(player, value, player.getDeltaMovement().y, player.getDeltaMovement().z);
            case "motion_y" -> setMotion(player, player.getDeltaMovement().x, value, player.getDeltaMovement().z);
            case "motion_z" -> setMotion(player, player.getDeltaMovement().x, player.getDeltaMovement().y, value);
            case "yaw" -> player.setYRot((float) value);
            case "pitch" -> player.setXRot((float) value);
            case "fall_distance", "falldistance" -> requireFieldWrite(player, new String[]{"fallDistance"}, value, "fall distance");
            case "walk_speed", "walking_speed" -> requireFieldWrite(player.getAbilities(), new String[]{"walkingSpeed", "walkSpeed"}, value, "walking speed");
            case "fly_speed", "flying_speed" -> requireFieldWrite(player.getAbilities(), new String[]{"flyingSpeed", "flySpeed"}, value, "flying speed");
            case "movement_speed" -> setAttribute(player, Attributes.MOVEMENT_SPEED, value, "movement speed");
            case "armor" -> setAttribute(player, Attributes.ARMOR, value, "armor");
            case "attack_damage" -> setAttribute(player, Attributes.ATTACK_DAMAGE, value, "attack damage");
            case "luck" -> setAttribute(player, Attributes.LUCK, value, "luck");
            case "health" -> player.setHealth((float) value);
            case "absorption", "absorption_amount" -> player.setAbsorptionAmount((float) value);
            case "saturation" -> player.getFoodData().setSaturation((float) value);
            case "exhaustion" -> player.getFoodData().setExhaustion((float) value);
            case "xp", "xp_progress", "experience_progress" -> requireFieldWrite(player, new String[]{"experienceProgress"}, value, "XP progress");
            default -> throw new IllegalArgumentException("Unknown field '" + field + "'. Use /nan list or /nan help.");
        }
    }

    private static void setMotion(ServerPlayer player, double x, double y, double z) {
        player.setDeltaMovement(new Vec3(x, y, z));
    }

    private static void setAttribute(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value, String label) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            throw new IllegalArgumentException("This player does not have the " + label + " attribute.");
        }
        instance.setBaseValue(value);
    }

    private static void requireFieldWrite(Object target, String[] possibleNames, double value, String label) {
        if (!writeNumericField(target, possibleNames, value)) {
            throw new IllegalArgumentException("Could not access the " + label + " field in this Minecraft build. This field may have changed in 26.3.");
        }
    }

    private static boolean writeNumericField(Object target, String[] possibleNames, double value) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (String name : possibleNames) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    if (field.getType() == float.class) {
                        field.setFloat(target, (float) value);
                        return true;
                    }
                    if (field.getType() == double.class) {
                        field.setDouble(target, value);
                        return true;
                    }
                } catch (NoSuchFieldException ignored) {
                    // Try the next candidate name or superclass.
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    return false;
                }
            }
        }
        return false;
    }

    private static int resetSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        reset(player);
        context.getSource().sendSuccess(() -> Component.literal("NaN Lab: restored safe defaults for " + player.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int resetTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "target");
        reset(player);
        context.getSource().sendSuccess(() -> Component.literal("NaN Lab: restored safe defaults for " + player.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static void reset(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        player.setPos(spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5);
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        writeNumericField(player, new String[]{"fallDistance"}, 0.0);
        writeNumericField(player, new String[]{"experienceProgress"}, 0.0);
        writeNumericField(player.getAbilities(), new String[]{"walkingSpeed", "walkSpeed"}, 0.1);
        writeNumericField(player.getAbilities(), new String[]{"flyingSpeed", "flySpeed"}, 0.05);
        setAttribute(player, Attributes.MOVEMENT_SPEED, 0.1, "movement speed");
        setAttribute(player, Attributes.ARMOR, 0.0, "armor");
        setAttribute(player, Attributes.ATTACK_DAMAGE, 1.0, "attack damage");
        setAttribute(player, Attributes.LUCK, 0.0, "luck");
        player.setHealth((float) player.getMaxHealth());
        player.setAbsorptionAmount(0.0F);
        player.getFoodData().setSaturation(5.0F);
        player.getFoodData().setExhaustion(0.0F);
    }
}
