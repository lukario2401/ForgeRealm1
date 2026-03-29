package net.lukario.frogerealm.network;

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

import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch.abyssalMonarchAbilitySix;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AetherWarden.aetherWardenAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AstralArbiter.cosmicOverdraw;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.BloodBoundAscetic.bloodBoundAsceticBloodFrenzy;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ChronoReaver.delayedExecution;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.EclipsePhantasm.phaseLock;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.FleshDevourer.bloodFrenzy;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.InfernalDuelist.cataclysmForm;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant.cataclysmState;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilityFourUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect.shadowSlaveAspectAbilitySixUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.SigilReaper.apocalypseScript;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormConduit.thunderCollapse;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.StormHerald.stormHeraldAbilitySevenUsed;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidAscendant.voidAscendantAbilitySix;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.VoidWalker.voidWalkerAbilitySixUsed;

public class SKeyPressAbilitySixUsed {

    public SKeyPressAbilitySixUsed() {}
    public SKeyPressAbilitySixUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        shadowSlaveAspectAbilitySixUsed(player,level,serverLevel);
        abyssalMonarchAbilitySix(player,serverLevel);
        stormHeraldAbilitySevenUsed(player,serverLevel);
        voidWalkerAbilitySixUsed(player,serverLevel);
        aetherWardenAbilitySixUsed(player,serverLevel);
        cataclysmState(player,serverLevel);
        apocalypseScript(player,serverLevel);
        cosmicOverdraw(player,serverLevel);
        cataclysmForm(player,serverLevel);
        phaseLock(player,level,serverLevel);
        delayedExecution(player,level,serverLevel);
        thunderCollapse(player,level,serverLevel);
        voidAscendantAbilitySix(player,level,serverLevel);
        bloodFrenzy(player,serverLevel);
        bloodBoundAsceticBloodFrenzy(player,serverLevel);

    }
}
