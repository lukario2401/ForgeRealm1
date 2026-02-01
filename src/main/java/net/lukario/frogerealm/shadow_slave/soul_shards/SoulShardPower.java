package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulShardPower {

    // ================= CONFIG =================

    public static final int MIN_SHARDS = 0;
    public static final int MAX_SHARDS = 7000;

    // ================= TICK UPDATE =================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;

        // Use the world time to trigger the update every 40 ticks (2 seconds)
        // This is much cleaner than a static counter!
        if (player.level().getGameTime() % 40 == 0) {
            applySoulShardPowers(player);
        }
    }

    // ================= MAIN LOGIC =================

    public static void applySoulShardPowers(Player player) {

        int shards =  SoulCore.getSoulShards(player);

        double percent = shards / (double) MAX_SHARDS;

        // remove old effects & attributes
        clearEffects(player);
        // ===== POTION EFFECTS (scale with shards) =====

        if (shards >= 500) {
            int amplifier = (int) (percent * 4); // up to level 4
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, amplifier, false, false));
        }

        if (shards >= 1500) {
            int amplifier = (int) (percent * 3);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, amplifier, false, false));
        }

        if (shards >= 3000) {
            int amplifier = (int) (percent * 2);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, amplifier, false, false));
        }

        if (shards >= 5000) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
        }

//        player.sendSystemMessage(Component.literal("worked"+shards));
    }

    private static void clearEffects(Player player) {
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.REGENERATION);
    }
}
