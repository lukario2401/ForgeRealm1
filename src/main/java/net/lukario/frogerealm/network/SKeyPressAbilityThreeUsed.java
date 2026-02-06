package net.lukario.frogerealm.network;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SKeyPressAbilityThreeUsed {

    public SKeyPressAbilityThreeUsed() {}
    public SKeyPressAbilityThreeUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        ServerLevel level = player.serverLevel();
        player.sendSystemMessage(Component.literal("worked 3"));
        player.playNotifySound(
                SoundEvents.DRAGON_FIREBALL_EXPLODE,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
        );
        level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER,true,player.getX(),player.getY(),player.getZ(),1,0,0,0,0);
        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-300);
    }

}
