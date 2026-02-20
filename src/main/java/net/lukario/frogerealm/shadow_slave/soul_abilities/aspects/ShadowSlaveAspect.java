package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.particles.ModParticles;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

public class ShadowSlaveAspect {

    /**
     * 1. THE LIFESTEAL EVENT
     * This handles the "Heal more based on Shadow Mark" logic.
     * Being a static inner class with @Mod.EventBusSubscriber makes it work automatically.
     */
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        @SubscribeEvent
        public static void onDamage(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                if (SoulCore.getAspect(player).equals("Shadow Slave")) {
                    if (SoulCore.getSoulEssence(player) <= 100 || SoulCore.getAscensionStage(player) < 3) return;
                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 100);

                    LivingEntity victim = event.getEntity();
                    var effectHolder = ModEffects.SHADOW_MARK.getHolder().get();

                    if (victim.hasEffect(effectHolder)) {
                        int amp = victim.getEffect(effectHolder).getAmplifier();

                        // Lifesteal calculation: 5% base + 2% per stack
                        float lifestealMult = 0.05f + (amp * 0.02f);
                        float healAmount = event.getAmount() * lifestealMult;

                        player.heal(healAmount);

                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    player.getX(), player.getY() + 1.5, player.getZ(),
                                    2, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                }
            }
        }
    }

    /**
     * 2. THE CUSTOM EFFECT
     * Put this inside your Aspect class so you don't need a separate file for the Effect.
     */
    public static class ShadowMarkEffect extends MobEffect {
        public ShadowMarkEffect() {
            super(MobEffectCategory.HARMFUL, 0x3e0066);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return false;
        }
    }

    /**
     * 3. ABILITY ONE: DASH & AOE
     */
    public static void shadowSlaveAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel){

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
                serverLevel.sendParticles(
                        ModParticles.VOID_RIFT.get(),
                        livingEntity.getX(),
                        livingEntity.getY() + 1,
                        livingEntity.getZ(),
                        10,
                        0.3, 0.3, 0.3,
                        0.02
                );
                level.playSound(null,player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.MASTER,8 ,1);

                player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 4f,1f);
            }

        }

    }

    /**
     * 4. ABILITY TWO: SHADOW BEAM
     */
    public static void shadowSlaveAspectAbilityTwoUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Shadow Slave")) return;
        if (SoulCore.getSoulEssence(player) < 300 || SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.SHADOW_MARK.getHolder().get();

        for (double i = 0; i < 8; i += 0.5) {
            Vec3 pos = start.add(direction.scale(i));
            serverLevel.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        int amp = entity.hasEffect(effectHolder) ?
                                Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                        entity.addEffect(new MobEffectInstance(effectHolder, 100, amp));
                        entity.hurt(level.damageSources().playerAttack(player), 6.0f + (amp * 2.5f));
                        entity.invulnerableTime = 0;
                    });
        }
    }

    public static void shadowSlaveAspectAbilityFourUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Shadow Slave")) return;
        if (SoulCore.getSoulEssence(player) < 1200 || SoulCore.getAscensionStage(player) < 4) return;

        Vec3 pos = player.position();

        // Find the closest entity instead of looping through all
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12), e -> e != player)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target != null) {
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);

            // Teleport slightly behind or next to them instead of inside them
            Vec3 targetPos = target.position().add(target.getLookAngle().scale(-1.5));
            player.teleportTo(targetPos.x, target.getY(), targetPos.z);

            // Effects
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }
}