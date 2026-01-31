package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulShardXpHandler {

    @SubscribeEvent
    public static void onPlayerGainXp(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return; // server only

        int xpGained = event.getAmount();
        if (xpGained <= 0) return;

        int soulShardsToAdd = xpGained * 2;

        SoulShards.addSoulShards(player, soulShardsToAdd);
        SoulShardPower.applySoulShardPowers(player);
        player.sendSystemMessage(Component.literal("You have: "+SoulShards.getSoulShards(player)));
    }
}
