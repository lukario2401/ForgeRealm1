package net.lukario.frogerealm.network;

import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.astralPulse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilityFive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.voidDrift;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.flareDash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.overload;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityTwoUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.spreadMark;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilityFiveUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityFourUsed;

public class SKeyPressAbilityFourUsed {

    public SKeyPressAbilityFourUsed() {}
    public SKeyPressAbilityFourUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        shadowSlaveAspectAbilityFourUsed(player,level,serverLevel);
        AbyssalMonarch.abyssalMonarchAbilityFour(player,serverLevel);
        chronoDuelistAbilityFive(player,serverLevel);
        stormHeraldAbilityFiveUsed(player,serverLevel);
        voidWalkerAbilityFourUsed(player,serverLevel);
        aetherWardenAbilityFourUsed(player,serverLevel);
        overload(player);
        spreadMark(player,level,serverLevel);
        astralPulse(player,level,serverLevel);
        flareDash(player,level,serverLevel);
        voidDrift(player,level,serverLevel);

    }

}
