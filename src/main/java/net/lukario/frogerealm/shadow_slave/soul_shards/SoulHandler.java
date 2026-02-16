package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulHandler {

    @SubscribeEvent
    public static void onPlayerGainXp(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return; // server only

        int xpGained = event.getAmount();
        if (xpGained <= 0) return;

        int soulShardsToAdd = xpGained * 2;

        SoulCore.addSoulShards(player, soulShardsToAdd);
        SoulShardPower.applySoulShardPowers(player);
        player.sendSystemMessage(Component.literal("You have: "+ SoulCore.getSoulShards(player)));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;

        if (player.tickCount % 5 == 0) {

            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + SoulCore.getAspectTier(player) * SoulCore.getAspectTier(player) * SoulCore.getAscensionStage(player));

            player.displayClientMessage(
                    Component.literal("Soul Essence: " + SoulCore.getSoulEssence(player)),  // text
                    true
            );
        }
    }
}
