package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AbyssalMonarch {

    public static class Abyssal_Monarch_Mark extends MobEffect {
        public Abyssal_Monarch_Mark() {
            super(MobEffectCategory.HARMFUL, 0x191970);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return false;
        }
    }

    public static void abyssalMonarchAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel){
        if (SoulCore.getAspect(player).equals("Shadow Slave"))return;;
        if (SoulCore.getSoulEssence(player)<500)return;
        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-500);

        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-30);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-20);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-10);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,0);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,10);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,20);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,30);

    }
    private static void abyssalMonarchAspectAbilityOneBeam(Player player, ServerLevel serverLevel, Level level, float offset){
        Vec3 startPosition = player.getEyePosition().add(0,-1,0);
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.Abyssal_Monarch_MARK.getHolder().get();

        double yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double pitch = (float)Math.toDegrees(Math.asin(-direction.y));

        yaw += offset;

        float fYaw = (float) yaw;
        float fPitch = (float) pitch;

        direction = Vec3.directionFromRotation(fPitch, fYaw);
        Vec3 current = startPosition;
        for (int i = 0; i <= 6; i++){
            current = current.add(direction.scale(1));

            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, current.x, current.y, current.z, 1, 0, 0, 0, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(current, current).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        if (!entity.isInvulnerable()){
                            int amp = entity.hasEffect(effectHolder) ?
                                    Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                            entity.addEffect(new MobEffectInstance(effectHolder, 100, amp));
                            entity.hurt(level.damageSources().playerAttack(player), 6.0f + (amp * 2.5f));
                            entity.invulnerableTime = 5;
                        }
                    });
        }


    }
}



