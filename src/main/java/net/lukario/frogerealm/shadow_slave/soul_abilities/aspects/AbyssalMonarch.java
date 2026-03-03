package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbyssalMonarch {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onDamageTaken(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                if (SoulCore.getAspect(player).equals("Abyssal Monarch")) {
                    if (SoulCore.getSoulEssence(player) > 250 && SoulCore.getAscensionStage(player) >= 2) {
                        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);

                        LivingEntity victim = event.getEntity();
                        victim.addEffect(new MobEffectInstance(ModEffects.Abyssal_Monarch_MARK.getHolder().get(), 200, 0));
                    }
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ShadeEvents {

        @SubscribeEvent
        public static void onShadeTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) return;

            // 1. Loop through EVERY ArmorStand in the world (more reliable than searching around players)
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof ArmorStand shade)) continue;

                // 2. Filter for your Shades
                if (!shade.getPersistentData().contains("ShadeOwner")) continue;

                UUID ownerUUID = shade.getPersistentData().getUUID("ShadeOwner");
                int life = shade.getPersistentData().getInt("ShadeLife");

                // 3. Handle Lifetime
                if (life > 0) {
                    shade.getPersistentData().putInt("ShadeLife", life - 1);
                } else {
                    shade.discard();
                    continue;
                }

                ServerPlayer owner = (ServerPlayer) serverLevel.getPlayerByUUID(ownerUUID);
                if (owner == null) continue;

                AABB attackBox = shade.getBoundingBox().inflate(12.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, attackBox, target -> {
                    // 1. Basic safety: Must be alive and NOT the shade itself
                    if (!target.isAlive() || target.getUUID().equals(shade.getUUID())) {
                        return false;
                    }
                    // 2. Protect the owner
                    if (target.getUUID().equals(ownerUUID)) {
                        return false;
                    }
                    // 3. Handle other Shades
                    if (target.getPersistentData().contains("ShadeOwner")) {
                        UUID otherOwner = target.getPersistentData().getUUID("ShadeOwner");
                        // Only ignore if it belongs to the SAME owner (allies)
                        // If you want shades to fight OTHER players' shades, keep this as is.
                        return !otherOwner.equals(ownerUUID);
                    }
                    // 4. If it's not a shade and not the owner, it's a valid enemy (Zombie, Skeleton, etc.)
                    return true;
                });


                if (targets.isEmpty()){
                    shade.teleportTo(owner.getX()+1,owner.getY()+1,owner.getZ()+1);
                }else{
                    LivingEntity target = targets.getFirst();
                    float distance = shade.distanceTo(target);
                    owner.sendSystemMessage(Component.literal("Distance: "+distance));

                    shade.teleportTo(target.getX(),target.getY(),target.getZ());
                    if (distance<=1.5) {
                        target.hurt(serverLevel.damageSources().playerAttack(owner), 0.5f);
                        target.invulnerableTime = 0;
                    }
                }

                owner.sendSystemMessage(Component.literal("Targets: "+targets));
            }
        }
    }

    public static class Abyssal_Monarch_Mark extends MobEffect {
        public Abyssal_Monarch_Mark() {
            super(MobEffectCategory.HARMFUL, 0x191970);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return false;
        }
    }

    public static void abyssalMonarchAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel){
        if (SoulCore.getAspect(player).equals("Shadow Slave"))return;
        if (SoulCore.getSoulEssence(player)<500)return;
        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-500);

        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-30);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-20);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-10);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,0);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,10);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,20);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,30);

    }
    private static void abyssalMonarchAspectAbilityOneBeam(Player player, ServerLevel serverLevel, Level level, float offset){
        Vec3 startPosition = player.getEyePosition().add(0,-1,0);
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.Abyssal_Monarch_MARK.getHolder().get();

        double yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));

        yaw += offset;

        float fYaw = (float) yaw;

        direction = Vec3.directionFromRotation(0, fYaw);
        Vec3 current = startPosition;
        for (int i = 0; i <= (5+SoulCore.getAscensionStage(player)); i++){
            current = current.add(direction.scale(1));

            serverLevel.sendParticles(ParticleTypes.TRIAL_OMEN, current.x, current.y+Math.sin(0.5*i)*0.5, current.z, 10, 0.1, 0.1, 0.1, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(current, current).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        if (!entity.isInvulnerable()){
                            int amp = entity.hasEffect(effectHolder) ?
                                    Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                            entity.addEffect(new MobEffectInstance(effectHolder, 100, amp));
                            entity.hurt(level.damageSources().playerAttack(player), 6.0f + (amp * 2.5f));
                            entity.invulnerableTime = 5;
                        }
                    });
        }
    }

    public static void abyssalMonarchAspectAbilityThree(Player player, Level level, ServerLevel serverLevel){
        if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
        if (SoulCore.getSoulEssence(player)<1500)return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-1500);

        AtomicBoolean canRun = new AtomicBoolean(true);

        Vec3 startPosition = player.getEyePosition().add(0,0,0);
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.Abyssal_Monarch_MARK.getHolder().get();

        Vec3 current = startPosition;
        for (int i = 0; i <= (12+SoulCore.getAscensionStage(player)); i++){
            current = current.add(direction.scale(1));

            serverLevel.sendParticles(ParticleTypes.TRIAL_OMEN, current.x, current.y, current.z, 3, 0.1, 0.1, 0.1, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(current, current).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        int amp = entity.hasEffect(effectHolder) ?
                        Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                        entity.addEffect(new MobEffectInstance(effectHolder, 200, amp));
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 200, false, false));
                        Vec3 delta = player.getDeltaMovement();
                        player.setDeltaMovement(delta.x, 0, delta.z);
                        entity.setSprinting(false);
                        canRun.set(false);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(), 3, 0.1, 0.1, 0.1, 0);
                        player.playNotifySound(SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);                    });
            if (!canRun.get()){
                return;
            }
        }
    }

    public static void abyssalMonarchAbilityFour(Player player, ServerLevel level) {
        if (SoulCore.getSoulEssence(player) < 3000) return;
        if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
        int stage = SoulCore.getAscensionStage(player);
        if (SoulCore.getAscensionStage(player)<4)return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);
//        int count = (stage >= 5) ? 2 : 1;
        int count = 1;

        for (int i = 0; i < count; i++) {
            ArmorStand shade = EntityType.ARMOR_STAND.create(level);
            if (shade != null) {
                // Setup "Shade" Appearance
                shade.isMarker();
                shade.setNoGravity(true);
                shade.setNoBasePlate(true);
                shade.setCustomName(Component.literal("Shade of " + player.getName().getString()));


                // Position near player
                shade.moveTo(player.getX() + (i * 1.2), player.getY() + 0.5, player.getZ(), player.getYRot(), 0);

                // Store Metadata (Owner and Duration)
                // Using Forge/Vanilla Tags to identify it later
                shade.getPersistentData().putUUID("ShadeOwner", player.getUUID());
                shade.getPersistentData().putInt("ShadeLife", 600); // 30 seconds
                shade.getPersistentData().putInt("ShadeStage", stage);

                level.addFreshEntity(shade);

                // Sound/Particles
                level.playSound(null, shade.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.5f);
            }
        }
    }
}



