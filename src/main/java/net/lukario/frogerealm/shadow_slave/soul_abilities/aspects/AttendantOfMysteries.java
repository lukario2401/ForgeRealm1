package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public class AttendantOfMysteries {

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KeyOfStarsEvents {
        @SubscribeEvent
        public static void onKeyOfStarsTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Attendant Of Mysteries")) return;

            int ascensionStage = SoulCore.getAscensionStage(player);

        }
    }

    //Ability 1
    public static void attendantOfMysteriesPaperDagger(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);

        if (player.isShiftKeyDown()){
            player.sendSystemMessage(Component.literal("ws"));
        }else{
            player.sendSystemMessage(Component.literal("ws"));

        }
    }




    private static boolean canUseClassKeyOfStars(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Attendant Of Mysteries");
    }
}
