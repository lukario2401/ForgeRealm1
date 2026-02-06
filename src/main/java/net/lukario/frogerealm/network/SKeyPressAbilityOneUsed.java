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
import net.minecraft.world.entity.MobSpawnType;
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


        if (SoulCore.getAspect(player).equals("Shadow Slave")){
            SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-100);
            player.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 2, false, false));
        }
        if (SoulCore.getAspect(player).equals("Light Bringer")){
            SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-100);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20*20, 2, false, false));
        }
    }

}
