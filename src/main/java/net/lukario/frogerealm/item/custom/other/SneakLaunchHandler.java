package net.lukario.frogerealm.item.custom.other;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

// 1. Remove Dist.CLIENT to allow this to run on both sides (or just the Server)
@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SneakLaunchHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        // 2. Ensure we only run this once per tick (PlayerTick fires twice: START and END)
        if (event.phase != PlayerTickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();

        if (!player.onGround() && player.isShiftKeyDown()) {
            launchPlayer(player, level);
        }
    }

    private static void launchPlayer(Player player, Level level) {
        Vec3 look = player.getLookAngle().normalize();
        double forwardStrength = 1.6;
        double upwardStrength = 0.4; // Increased slightly for feel

        // 3. Remove the !level.isClientSide check
        // We want the client to push the player immediately for responsiveness
        player.push(look.x * forwardStrength, upwardStrength, look.z * forwardStrength);

        // This tells the server the player's position has changed
        player.hurtMarked = true;

        Vec3 pos = player.position();
        level.playSound(player, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS,
                1.0f, 1.0f);
    }
}
