package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ChronoDuelist {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onAttackStoreEcho(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
                if (SoulCore.getAscensionStage(player)<2)return;

                // Get current echoes from NBT
                CompoundTag data = player.getPersistentData();
                int currentEchoes = data.getInt("ChronoEchoes");

                // Max echoes: 3
                if (currentEchoes < SoulCore.getAscensionStage(player)) {
                    data.putInt("ChronoEchoes", currentEchoes + 1);

                    // Visual feedback: Blue sparks on the player
                    if (player.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onDamageConsumeEcho(LivingHurtEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;

                CompoundTag data = player.getPersistentData();
                int currentEchoes = data.getInt("ChronoEchoes");

                if (currentEchoes > 0) {
                    // 1. Consume the echo
                    data.putInt("ChronoEchoes", currentEchoes - 1);

                    // 2. Reduce damage by 30%
                    float originalDamage = event.getAmount();
                    event.setAmount(originalDamage * 0.7f);

                    // 3. Teleport slightly backward
                    Vec3 look = player.getLookAngle();
                    // Move opposite to where they are looking (-1.5 blocks)
                    player.teleportTo(player.getX() - (look.x * 1.5), player.getY(), player.getZ() - (look.z * 1.5));

                    // 4. Late Upgrade: Restore Soul Essence (Stage 5+)
                    if (SoulCore.getAscensionStage(player) >= 5) {
                        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + 150);
                    }

                    // 5. Effects
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 2.0f);
                }
            }
        }


    }

    public static void chronoDuelistAbilityOneUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 300 || SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();


        for (int i = SoulCore.getAscensionStage(player) + 4; i > 0; i--) {
            Vec3 c = start.add(direction.scale(i));

            BlockPos blockPos = new BlockPos(Mth.floor(c.x), Mth.floor(c.y), Mth.floor(c.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.getBlock().defaultBlockState().isSolid()) {
                player.teleportTo(c.x, c.y, c.z);
                return;
            }
        }
    }

    public static void chronoDuelistAbilityThreeUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 1200 || SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);


        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200,
                (player.hasEffect(MobEffects.DIG_SPEED) ? player.getEffect(MobEffects.DIG_SPEED).getAmplifier() + 3 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200,
                (player.hasEffect(MobEffects.MOVEMENT_SPEED) ? player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 2 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1200,
                (player.hasEffect(MobEffects.JUMP) ? player.getEffect(MobEffects.JUMP).getAmplifier() + 1 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200,
                (player.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200,
                (player.hasEffect(MobEffects.DAMAGE_BOOST) ? player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1 : 0)));

    }

}
