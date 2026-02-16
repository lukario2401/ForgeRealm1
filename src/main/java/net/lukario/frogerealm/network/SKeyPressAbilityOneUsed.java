package net.lukario.frogerealm.network;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SKeyPressAbilityOneUsed {

    public SKeyPressAbilityOneUsed() {}
    public SKeyPressAbilityOneUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

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
                level.playSound(null,player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.MASTER,8 ,1);

                player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 4f,1f);
            }

        }
        if (SoulCore.getAspect(player).equals("Light Bringer")){
            if (SoulCore.getSoulEssence(player)<150)return;
            SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-150);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20*20, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20*20, 0, false, false));

            player.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 2, false, false));
        }
    }
}
