package net.lukario.frogerealm.item.custom.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 1. Remove Dist.CLIENT to allow this to run on both sides (or just the Server)
@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SneakLaunchHandler {

    private static final String DOUBLE_JUMP = "fr_double_jump";
    private static final String CAN_USE = "fr_can_use_double_jump";
    private static final String COOLDOWN = "fr_sneak_double_jump_cooldown";


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        var data = player.getPersistentData();

        if (!mc.options.keyJump.isDown() && !player.onGround()) {
            data.putBoolean(DOUBLE_JUMP, false);
        }
        if (!mc.options.keyJump.isDown() && player.onGround()) {
            data.putBoolean(DOUBLE_JUMP, true);
            data.putBoolean(CAN_USE, true);
        }

        if (mc.options.keyJump.consumeClick()) {
            boolean used = data.getBoolean(DOUBLE_JUMP);
            boolean can_use = data.getBoolean(CAN_USE);

            if (!used && !player.onGround() && can_use) {
                launchPlayer(player,0.4,1.8);
                data.putBoolean(DOUBLE_JUMP, true);
                data.putBoolean(CAN_USE, false);
            }
        }
        if (mc.options.keyJump.isDown()) {
            data.putBoolean(DOUBLE_JUMP, true);
        }

        if (!player.isShiftKeyDown()){
            data.putBoolean(COOLDOWN, false);
        }

        if (!data.getBoolean(CAN_USE) && player.isShiftKeyDown() && !data.getBoolean(COOLDOWN)){
            launchPlayer(player, 1.6, 0.4);
            data.putBoolean(COOLDOWN, true);
        }

    }



    private static void launchPlayer(Player player, double forwardStrength, double upwardStrength) {

        Vec3 look = player.getLookAngle().normalize();
        player.push(look.x * forwardStrength, upwardStrength, look.z * forwardStrength);

        player.hurtMarked = true;

        Vec3 pos = player.position();
        player.level().playSound(player, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS,
                1.0f, 1.0f);

        spawnParticleCloud(player.level(), pos);
    }


    private static void spawnParticleCloud(Level level, Vec3 pos) {
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = level.random.nextDouble() * 0.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            double speedX = (level.random.nextDouble() - 0.5) * 0.05;
            double speedY = level.random.nextDouble() * 0.05;
            double speedZ = (level.random.nextDouble() - 0.5) * 0.05;

            level.addParticle(
                    ParticleTypes.FIREWORK,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    speedX, speedY, speedZ
            );
        }
    }

}
