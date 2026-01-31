package net.lukario.frogerealm.shadow_slave.soul_shards;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulShardCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("soul")
                .requires(source -> source.hasPermission(2)) // OP only

                // =========================
                // /soul get <player>
                // =========================
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    Player player = EntityArgument.getPlayer(ctx, "player");

                                    int shards = SoulShards.getSoulShards(player);
                                    int tier = SoulShards.getAspectTier(player);

                                    ctx.getSource().sendSuccess(() ->
                                                    Component.literal("Player: " + player.getName().getString()
                                                            + " | SoulShards=" + shards
                                                            + " | Tier=" + tier),
                                            false
                                    );
                                    return 1;
                                })
                        )
                )

                // =========================
                // /soul setshards <player> <amount>
                // =========================
                .then(Commands.literal("setshards")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                            SoulShards.setSoulShards(player, amount);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Set SoulShards to " + SoulShards.getSoulShards(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )

                // =========================
                // /soul addshards <player> <amount>
                // =========================
                .then(Commands.literal("addshards")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                            SoulShards.addSoulShards(player, amount);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Added shards. New value = " + SoulShards.getSoulShards(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )

                // =========================
                // /soul settier <player> <tier>
                // =========================
                .then(Commands.literal("settier")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            Player player = EntityArgument.getPlayer(ctx, "player");
                                            int tier = IntegerArgumentType.getInteger(ctx, "tier");

                                            SoulShards.setAspectTier(player, tier);

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("Set Aspect Tier to " + SoulShards.getAspectTier(player)),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}
