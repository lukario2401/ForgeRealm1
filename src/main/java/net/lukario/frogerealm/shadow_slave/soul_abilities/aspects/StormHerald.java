package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StormHerald {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        @SubscribeEvent
        public static void onStaticTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            CompoundTag nbt = entity.getPersistentData();

            if (nbt.contains("StaticCharge")) {
                int ticks = nbt.getInt("StaticCharge");
                if (ticks > 0) {
                    nbt.putInt("StaticCharge", ticks - 1);

                    // Visual: Occasional sparks while charged
                    if (ticks % 10 == 0 && entity.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + 1, entity.getZ(), 1, 0.3, 0.3, 0.3, 0);
                    }
                } else {
                    nbt.remove("StaticCharge");
                }
            }
        }
    }

    public static void stormHeraldAbilityOneUsed(Player player, ServerLevel level) {

        if (SoulCore.getSoulEssence(player) < 600) return;
        if (!SoulCore.getAspect(player).equals("Storm Herald"))return;
//        if (SoulCore.getAscensionStage(player)<1)return;

        // 1. Cooldown Check (Using a dummy item or custom system)

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int maxTargets = SoulCore.getAscensionStage(player) >= 6 ? 3 : 2;

        Set<LivingEntity> hitEntities = new HashSet<>();

        // 2. "March" the spark forward 10 blocks
        for (double i = 1; i < 10; i += 0.5) { // Moves in 0.5 block increments for accuracy
            Vec3 checkPos = eyePos.add(look.scale(i));

            // Visual: The "Projectile" trail
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, checkPos.x, checkPos.y, checkPos.z, 2, 0.1, 0.1, 0.1, 0.02);
            level.sendParticles(ParticleTypes.INSTANT_EFFECT, checkPos.x, checkPos.y, checkPos.z, 1, 0, 0, 0, 0);

            // 3. Check for enemies at this specific point in the air
            AABB checkArea = new AABB(checkPos.x - 0.5, checkPos.y - 0.5, checkPos.z - 0.5,
                    checkPos.x + 0.5, checkPos.y + 0.5, checkPos.z + 0.5);

            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, checkArea,
                    e -> e != player && e.isAlive() && !hitEntities.contains(e));

            for (LivingEntity target : targets) {
                if (hitEntities.size() < maxTargets) {
                    applySparkEffect(target, player);
                    hitEntities.add(target);

                    // Visual: Impact burst
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                }
            }

            // Optional: Stop the bolt if it hits a solid wall
            if (!level.getBlockState(BlockPos.containing(checkPos)).isAir()) {
                break;
            }
        }

        // 4. Finalize
        player.getCooldowns().addCooldown(Items.IRON_SWORD, 100);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 2.0f);
    }

    private static void applySparkEffect(LivingEntity target, Player source) {
        target.hurt(target.level().damageSources().playerAttack(source), 4.0f);

        // Apply Static Charge NBT
        CompoundTag nbt = target.getPersistentData();
        nbt.putInt("StaticCharge", 100); // 5 seconds of charge
    }

}
