package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.particles.ModParticles;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ShadowSlaveAspect {

    public static void shadowSlaveAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel){

        if (SoulCore.getAspect(player).equals("Shadow Slave")){
            if (SoulCore.getSoulEssence(player)<250)return;

            SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player) - 250);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.position(), player.position()).inflate(2.5),
                    e -> e != player
            );

            for (LivingEntity livingEntity : entities){

                livingEntity.hurt(level.damageSources().playerAttack(player),12.0f);
                livingEntity.invulnerableTime = 0;

                serverLevel.sendParticles(
                        ParticleTypes.SOUL,
                        livingEntity.getX(),
                        livingEntity.getY() + 1,
                        livingEntity.getZ(),
                        15,
                        0.3, 0.3, 0.3,
                        0.01
                );
                serverLevel.sendParticles(
                        ModParticles.VOID_RIFT.get(),
                        livingEntity.getX(),
                        livingEntity.getY() + 1,
                        livingEntity.getZ(),
                        10,
                        0.3, 0.3, 0.3,
                        0.02
                );
                level.playSound(null,player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.MASTER,8 ,1);

                player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 4f,1f);
            }

        }

    }

}
