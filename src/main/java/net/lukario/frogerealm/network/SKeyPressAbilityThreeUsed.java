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

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityThreeUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.starfallStrike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.hemorrhageBurst;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.rewindBurst;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.harvest;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.echoCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.meatBurst;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.gravityCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.ignitionBurst;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.wraithUppercut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.accelerateDecay;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.ruinDetonation;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.sigilSurge;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.overloadStrike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.intersection;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityThree;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.shatterPoint;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityThreeUsed;

public class SKeyPressAbilityThreeUsed {

    public SKeyPressAbilityThreeUsed() {}
    public SKeyPressAbilityThreeUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        AbyssalMonarch.abyssalMonarchAspectAbilityThree(player,level,serverLevel);
        chronoDuelistAbilityFourUsed(player);
        stormHeraldAbilityFourUsed(player,serverLevel);
        voidWalkerAbilityThreeUsed(player,serverLevel);
        aetherWardenAbilityThreeUsed(player,serverLevel);
        ruinDetonation(player,level,serverLevel);
        sigilSurge(player,serverLevel);
        starfallStrike(player,level,serverLevel);
        ignitionBurst(player,level,serverLevel);
        echoCut(player,level,serverLevel);
        rewindBurst(player,level,serverLevel);
        overloadStrike(player,level,serverLevel);
        voidAscendantAbilityThree(player,level,serverLevel);
        meatBurst(player,level,serverLevel);
        hemorrhageBurst(player,level,serverLevel);
        intersection(player,level,serverLevel);
        shatterPoint(player,level,serverLevel);
        accelerateDecay(player,level,serverLevel);
        gravityCollapse(player,level,serverLevel);
        harvest(player,level,serverLevel);
        wraithUppercut(player,level,serverLevel);


    }
}
