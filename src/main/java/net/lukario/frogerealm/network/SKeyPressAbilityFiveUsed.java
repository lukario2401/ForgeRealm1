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

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityFiveUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.orbSplit;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilityFive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilitySix;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.timeCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.realityFracture;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.fleshHook;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.overheat;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.executionDrive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.delayedCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.stormChannel;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityFive;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityFiveUsed;

public class SKeyPressAbilityFiveUsed {

    public SKeyPressAbilityFiveUsed() {}
    public SKeyPressAbilityFiveUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        chronoDuelistAbilitySix(player,serverLevel);
        stormHeraldAbilitySixUsed(player,serverLevel);
        voidWalkerAbilityFiveUsed(player,serverLevel);
        aetherWardenAbilityFiveUsed(player,serverLevel);
        executionDrive(player,level,serverLevel);
        delayedCollapse(player,level,serverLevel);
        orbSplit(player,serverLevel);
        overheat(player,serverLevel);
        realityFracture(player,level,serverLevel);
        timeCollapse(player,serverLevel);
        stormChannel(player,serverLevel);
        voidAscendantAbilityFive(player,level,serverLevel);
        fleshHook(player,level,serverLevel);


    }
}
