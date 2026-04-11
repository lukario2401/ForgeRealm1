package net.lukario.frogerealm.network;

import net.lukario.frogerealm.particles.ModParticles;
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

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch.abyssalMonarchAspectAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalPenitent.abyssalPenitentProfaneCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.astralBolt;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.bloodSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoDuelist.chronoDuelistAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.temporalSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.DeathDescendant.shadowCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.phaseShift;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.fleshDevourerRend;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.GravityArchitect.placeGravityWell;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.HangedAscetic.hangedAsceticShadowCurse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.flameCut;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.LightBringerAspect.lightBringerAspectAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PhantomSequence.phantomSequencePhantomStrike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.PlagueSovereign.plagueTouch;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.ruinSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RunicSequencer.runicSequencerSigilMark;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.sigilCarve;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.arcSpike;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilityOneUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VectorArbiter.vectorSlash;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilityOne;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidResonator.voidPulse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilityOneUsed;

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

        shadowSlaveAspectAbilityOneUsed(player,level,serverLevel);
        lightBringerAspectAbilityOneUsed(player,level,serverLevel);
        abyssalMonarchAspectAbilityOneUsed(player,level,serverLevel);
        chronoDuelistAbilityOneUsed(player,level,serverLevel);
        stormHeraldAbilityOneUsed(player,serverLevel);
        voidWalkerAbilityOneUsed(player,serverLevel);
        aetherWardenAbilityOneUsed(player,serverLevel);
        ruinSlash(player,level,serverLevel);
        sigilCarve(player,level,serverLevel);
        astralBolt(player,level,serverLevel);
        flameCut(player,level,serverLevel);
        phaseShift(player,serverLevel);
        temporalSlash(player,level,serverLevel);
        arcSpike(player,level,serverLevel);
        voidAscendantAbilityOne(player,level,serverLevel);
        fleshDevourerRend(player,level,serverLevel);
        bloodSlash(player,level,serverLevel);
        vectorSlash(player,level,serverLevel);
        voidPulse(player,level,serverLevel);
        plagueTouch(player,level,serverLevel);
        placeGravityWell(player,level,serverLevel);
        shadowCut(player,level,serverLevel);
        phantomSequencePhantomStrike(player,level,serverLevel);
        runicSequencerSigilMark(player,level,serverLevel);
        abyssalPenitentProfaneCut(player,level,serverLevel);
        hangedAsceticShadowCurse(player,level,serverLevel,false);
    }
}
