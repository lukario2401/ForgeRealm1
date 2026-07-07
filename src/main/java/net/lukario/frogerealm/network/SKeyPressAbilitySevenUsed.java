package net.lukario.frogerealm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentDescentIntoTheAbyss;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilitySevenUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.astralConvergence;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.martyrsAscension;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.chronoOverload;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.abyssalHarvest;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.eclipseState;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.apexAbomination;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.gravitationalSingularity;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticDescentIntoDepravity;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsCosmicPlague;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.infinitePhantom;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.sovereignsPlague;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerPerfectRitual;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.Shepard.shepardShadowChrysalis;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.singularityStorm;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.perfectAlignment;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.cataclysmEngine;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilitySevenUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners.Verso.crescendoBladeEndBringer;

public class SKeyPressAbilitySevenUsed {

    public SKeyPressAbilitySevenUsed() {}
    public SKeyPressAbilitySevenUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        voidWalkerAbilitySevenUsed(player,serverLevel);
        aetherWardenAbilitySevenUsed(player,serverLevel);
        astralConvergence(player,serverLevel,false);
        eclipseState(player,serverLevel);
        chronoOverload(player,serverLevel);
        singularityStorm(player,serverLevel);
        apexAbomination(player,serverLevel);
        martyrsAscension(player,serverLevel);
        perfectAlignment(player,serverLevel);
        cataclysmEngine(player,serverLevel);
        sovereignsPlague(player,level,serverLevel);
        gravitationalSingularity(player,serverLevel);
        abyssalHarvest(player,level,serverLevel);
        infinitePhantom(player,level,serverLevel);
        runicSequencerPerfectRitual(player,level,serverLevel);
        abyssalPenitentDescentIntoTheAbyss(player,serverLevel);
        hangedAsceticDescentIntoDepravity(player,level,serverLevel,false);
        shepardShadowChrysalis(player,level,serverLevel,false);
        keyOfStarsCosmicPlague(player,level,serverLevel,false);
        crescendoBladeEndBringer(player,level,serverLevel);


    }

}
