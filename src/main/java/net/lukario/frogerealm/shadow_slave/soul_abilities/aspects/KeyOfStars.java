package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public class KeyOfStars {

    private static final String NBT_KEY_OF_STARS_STAR_COUNT = "key_of_stars_star_count";
    private static final String NBT_KEY_OF_STARS_NEW_STAR_CD = "key_of_stars_new_star_cooldown";
    private static final String NBT_KEY_OF_STARS_IS_DASHING = "key_of_stars_is_dashing";
    private static final String NBT_KEY_OF_STARS_IS_SEALED_OFFENSE = "key_of_stars_is_sealed_offense";
    private static final String NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DURATION = "key_of_stars_is_sealed_offense_duration";
    private static final String NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DAMAGE_STORED = "key_of_stars_is_sealed_offense_damage_stored";
    private static final String NBT_KEY_OF_STARS_IS_SEALED_DEFENSE = "key_of_stars_is_sealed_defense";
    private static final String NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DURATION = "key_of_stars_is_sealed_defense_duration";
    private static final String NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DAMAGE_STORED = "key_of_stars_is_sealed_defense_damage_stored";

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KeyOfStarsEvents {
        @SubscribeEvent
        public static void onShepardTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Key Of Stars")) return;

            int ascensionStage = SoulCore.getAscensionStage(player);
            int starCount = player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT);

            if (starCount<ascensionStage){
                if (player.getPersistentData().getInt(NBT_KEY_OF_STARS_NEW_STAR_CD)==0){

                    player.getPersistentData().putInt(NBT_KEY_OF_STARS_STAR_COUNT,starCount+1);
                    player.getPersistentData().putInt(NBT_KEY_OF_STARS_NEW_STAR_CD,20);

                    player.sendSystemMessage(Component.literal("New Star Added [" + (starCount+1) +"/7]"));

                }else{
                    player.getPersistentData().putInt(NBT_KEY_OF_STARS_NEW_STAR_CD,player.getPersistentData().getInt(NBT_KEY_OF_STARS_NEW_STAR_CD)-1);
                }
            }
            if (player.getPersistentData().getBoolean(NBT_KEY_OF_STARS_IS_DASHING)){

                double dx = player.getX() - player.xo;
                double dy = player.getY() - player.yo;
                double dz = player.getZ() - player.zo;
                double blocksPerTick = Math.sqrt(dx * dx + dy * dy + dz * dz);

                double blocksPerSecond = blocksPerTick * 20.0;
                if (blocksPerSecond < 7.5) {
                    player.getPersistentData().putBoolean(NBT_KEY_OF_STARS_IS_DASHING, false);
                }

                List<LivingEntity> hits = sl.getEntitiesOfClass(
                        LivingEntity.class, new AABB(player.position(), player.position()).inflate(1),
                        e -> e != player && e.isAlive());

                for (LivingEntity livingEntity : hits){
                    livingEntity.hurt(player.level().damageSources().playerAttack(player),24);
                }
            }
        }
        @SubscribeEvent
        public static void onMobTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide()) return;
            if (entity.getPersistentData().contains(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DURATION)) {

                float duration = entity.getPersistentData().getFloat(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DURATION);

                if (duration > 0) {
                    entity.getPersistentData().putFloat(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DURATION, duration - 1);

                    if (entity.level() instanceof ServerLevel sl && duration % 5 == 0) {
                        sl.sendParticles(ParticleTypes.END_ROD,
                                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                                4, 0.3, 0.5, 0.3, 0.02);
                    }
                } else if (entity.getPersistentData().getBoolean(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE)) {
                    entity.getPersistentData().putBoolean(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE, false);
                    float storedDamage = entity.getPersistentData().getFloat(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DAMAGE_STORED);
                    entity.hurt(entity.level().damageSources().magic(), (float) Math.floor(storedDamage*1.5));
                }
            }
            if (entity.getPersistentData().contains(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DURATION)) {

                float duration = entity.getPersistentData().getFloat(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DURATION);

                if (duration > 0) {
                    entity.getPersistentData().putFloat(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DURATION, duration - 1);

                    if (entity.level() instanceof ServerLevel sl && duration % 5 == 0) {
                        sl.sendParticles(ParticleTypes.END_ROD,
                                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                                4, 0.3, 0.5, 0.3, 0.02);
                    }
                } else if (entity.getPersistentData().getBoolean(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE)) {
                    entity.getPersistentData().putBoolean(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE, false);
                    float storedDamage = entity.getPersistentData().getFloat(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DAMAGE_STORED);
                    entity.hurt(entity.level().damageSources().magic(), (float) Math.floor(storedDamage*1.5));
                }
            }
        }
        @SubscribeEvent
        public static void onMobTakeDamage(LivingDamageEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) return;
            if (victim.getPersistentData().getBoolean(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE)) {
                float currentDamage = event.getAmount();
                event.setAmount(0);
                event.setCanceled(true);
                victim.getPersistentData().putFloat(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DAMAGE_STORED,victim.getPersistentData().getFloat(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DAMAGE_STORED)+currentDamage);
            }
        }

        @SubscribeEvent
        public static void onMobDealDamage(LivingDamageEvent event) {
            // 1. Check if the source of the damage came from a Living Entity (Mob or Player)
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {

                // Only run on server-side
                if (attacker.level().isClientSide()) return;

                // 2. Check if the attacker is currently under the "Sealed Offense" effect
                if (attacker.getPersistentData().getBoolean(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE)) {
                    float dealtDamage = event.getAmount();

                    // 3. Read how much damage we've already stored, and add this new damage to it
                    float currentlyStored = attacker.getPersistentData().getFloat(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DAMAGE_STORED);
                    attacker.getPersistentData().putFloat(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DAMAGE_STORED, currentlyStored + dealtDamage);

                    // 4. NULLIFY THE DAMAGE: Stop the mob from actually hurting its target!
                    event.setAmount(0.0f);
                    event.setCanceled(true);

                    // 5. Visual flare: Spawn a "negated attack" particle effect at the attacker's weapon level
                    if (attacker.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.WITCH,
                                attacker.getX(), attacker.getY() + 1.2, attacker.getZ(),
                                8, 0.2, 0.2, 0.2, 0.02);

                        // Play a muffled magic sound indicating the damage was eaten by the seal
                        attacker.level().playSound(null, attacker.blockPosition(),
                                SoundEvents.VEX_CHARGE, SoundSource.PLAYERS, 0.6f, 0.5f);
                    }
                }
            }
        }
    }

    // Ability 1
    public static void keyOfStarsStarBurst(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);

        if (player.isShiftKeyDown()){
            if (player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT)>=1){
                int starCount = player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT);

                player.getPersistentData().putInt(NBT_KEY_OF_STARS_STAR_COUNT,0);

                player.sendSystemMessage(Component.literal("Star Used. Stars left [0/7]"));

                player.playNotifySound(SoundEvents.BEACON_POWER_SELECT,SoundSource.MASTER,2,1);

                Vec3 start = player.getEyePosition();
                Vec3 direction = player.getLookAngle().normalize();
                Vec3 current = start;

                for (int i = 0; i < 16; i++){
                    current = current.add(direction);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            current.x, current.y, current.z, 4, 0.2, 0.2, 0.2, 0.03);

                    List<LivingEntity> hits = level.getEntitiesOfClass(
                            LivingEntity.class, new AABB(current, current).inflate(0.5),
                            e -> e != player && e.isAlive());

                    for (LivingEntity livingEntity : hits){
                        livingEntity.hurt(player.level().damageSources().playerAttack(player),21+(starCount*3));
                    }
                }
            }
        }else{
            if (player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT)>=1){
                player.getPersistentData().putInt(NBT_KEY_OF_STARS_STAR_COUNT,player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT)-1);

                player.sendSystemMessage(Component.literal("Star Used. Stars left [" + (player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT) +"/7]")));
                player.playNotifySound(SoundEvents.BEACON_ACTIVATE,SoundSource.MASTER,1,1);

                Vec3 start = player.getEyePosition();
                Vec3 direction = player.getLookAngle().normalize();
                Vec3 current = start;

                for (int i = 0; i < 16; i++){
                    current = current.add(direction);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            current.x, current.y, current.z, 1, 0, 0, 0, 0);


                    List<LivingEntity> hits = level.getEntitiesOfClass(
                            LivingEntity.class, new AABB(current, current).inflate(0.5),
                            e -> e != player && e.isAlive());

                    for (LivingEntity livingEntity : hits){
                        livingEntity.hurt(player.level().damageSources().playerAttack(player),21);
                        return;
                    }
                }
            }
        }
    }

    // Ability 2
    public static void keyOfStarsDash(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);

        if (player.isShiftKeyDown()){
            teleport(player,sl,16);
            player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);

        }else{
            player.playNotifySound(SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);

            Vec3 lookDirection = player.getLookAngle().normalize();
            Vec3 dashVelocity = lookDirection.scale(2.5);
            player.setDeltaMovement(dashVelocity);
            player.hurtMarked = true;

            player.getPersistentData().putBoolean(NBT_KEY_OF_STARS_IS_DASHING,true);
        }
    }

    // Ability 3
    public static void keyOfStarsBuff(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CURE,SoundSource.PLAYERS,1,1);

        if (player.isShiftKeyDown()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 400, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
        }
    }

    // Ability 4
    public static void keyOfStarsSeal(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CURE,SoundSource.PLAYERS,1,1);

        if (player.isShiftKeyDown()) {
            LivingEntity target = getTarget(player, sl, level, 16);
            target.getPersistentData().putBoolean(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE,true);
            target.getPersistentData().putFloat(NBT_KEY_OF_STARS_IS_SEALED_DEFENSE_DURATION,100);
        } else {
            LivingEntity target = getTarget(player, sl, level, 16);
            target.getPersistentData().putBoolean(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE,true);
            target.getPersistentData().putFloat(NBT_KEY_OF_STARS_IS_SEALED_OFFENSE_DURATION,100);
        }
    }

    private static LivingEntity getTarget(Player player, ServerLevel sl, Level level, float distance){
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 current = start;
        LivingEntity livingEntity = null;

        for (float i = 0; i < distance; i+=0.5f){
            current = current.add(direction);
            sl.sendParticles(ParticleTypes.END_ROD,
                    current.x, current.y, current.z, 1, 0, 0, 0, 0);


            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());
            if(!hits.isEmpty()){
                livingEntity = hits.get(0);
                return livingEntity;
            }
        }
        return livingEntity;
    }

    private static void teleport(Player player, Level level, float distance){
        Vec3 location = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        for (float i = distance; i > 0; i -=0.5f){
            Vec3 current = location.add(direction.scale(i));

            BlockState state = level.getBlockState(BlockPos.containing(current));
            if (!state.isSolid()){
                player.teleportTo(current.x,current.y,current.z);
                return;
            }
        }
    }

    private static boolean canUseClassKeyOfStars(Player player, Boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Key Of Stars");
    }
}
