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
import java.util.UUID;

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

    private static final String NBT_ORBITAL_TARGET = "key_of_stars_orbital_target_uuid";
    private static final String NBT_ORBITAL_STAR_COUNT = "key_of_stars_orbital_star_count";
    private static final String NBT_ORBITAL_TICK = "key_of_stars_orbital_tick";
    private static final String NBT_ORBITAL_ACTIVE = "key_of_stars_orbital_active";
    private static final String NBT_ORBITAL_COLLAPSE_CD = "key_of_stars_orbital_collapse_cd";
    private static final String NBT_ORBITAL_BLOCK_X = "key_of_stars_orbital_block_x";
    private static final String NBT_ORBITAL_BLOCK_Y = "key_of_stars_orbital_block_y";
    private static final String NBT_ORBITAL_BLOCK_Z = "key_of_stars_orbital_block_z";
    private static final String NBT_ORBITAL_IS_BLOCK = "key_of_stars_orbital_is_block";

    private static final String NBT_COSMIC_PLAGUE_DURATION = "key_of_stars_cosmic_plague_duration";

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KeyOfStarsEvents {
        @SubscribeEvent
        public static void onKeyOfStarsTick(TickEvent.PlayerTickEvent event) {
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
            int orbitalCollapseCD = player.getPersistentData().getInt(NBT_ORBITAL_COLLAPSE_CD);
            if (orbitalCollapseCD > 0) {
                player.getPersistentData().putInt(NBT_ORBITAL_COLLAPSE_CD, orbitalCollapseCD - 1);
            }

            if (player.getPersistentData().getBoolean(NBT_ORBITAL_ACTIVE)) {
                tickOrbitalStars(player, sl);
            }
        }
        @SubscribeEvent
        public static void onMobTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide()) return;
            if (entity.getPersistentData().contains(NBT_COSMIC_PLAGUE_DURATION)) {

                float duration = entity.getPersistentData().getFloat(NBT_COSMIC_PLAGUE_DURATION);

                if (duration > 0) {
                    entity.getPersistentData().putFloat(NBT_COSMIC_PLAGUE_DURATION, duration - 1);

                    if (entity.level() instanceof ServerLevel sl && duration % 5 == 0) {
                        sl.sendParticles(ParticleTypes.GLOW,
                                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                                4, 0.3, 0.5, 0.3, 0.02);
                    }
                    if (entity.level() instanceof ServerLevel sl && duration % 20 == 0) {

                        List<LivingEntity> hits = entity.level().getEntitiesOfClass(
                                LivingEntity.class, new AABB(entity.position(), entity.position()).inflate(1.5),
                                e -> e != entity && e.isAlive());

                        for (LivingEntity targets : hits){

                            targets.getPersistentData().putFloat(NBT_COSMIC_PLAGUE_DURATION,targets.getPersistentData().getFloat(NBT_COSMIC_PLAGUE_DURATION)+400);

                        }
                    }
                    if (entity.level() instanceof ServerLevel sl && duration % 60 == 0) {
                        List<LivingEntity> hits = entity.level().getEntitiesOfClass(
                                LivingEntity.class, new AABB(entity.position(), entity.position()).inflate(8),
                                e -> e != entity && e.isAlive());

                        for (LivingEntity targets : hits) {
                            // Only process actual players
                            if (!(targets instanceof Player player)) continue;
                            if (!SoulCore.getAspect(player).equals("Key Of Stars")) continue;

                            int current = player.getPersistentData().getInt(NBT_KEY_OF_STARS_STAR_COUNT);
                            int max = SoulCore.getAscensionStage(player); // stars cap at ascension stage
                            if (current < max) {
                                player.getPersistentData().putInt(NBT_KEY_OF_STARS_STAR_COUNT, current + 1);
                                player.sendSystemMessage(Component.literal("§bPlague feeds you a star §e[" + (current + 1) + "/" + max + "]"));
                            }
                        }
                    }
                }
            }
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

    // Ability 5
    public static void keyOfStarsTransport(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CURE,SoundSource.PLAYERS,1,1);

        if (player.isShiftKeyDown()) {
            LivingEntity target = getTarget(player, sl, level, 32);
            target.teleportTo(player.getX(),player.getY(),player.getZ());
        } else {
            LivingEntity target = getTarget(player, sl, level, 32);
            player.teleportTo(target.getX(),target.getY(),target.getZ());
        }
    }

    // Ability 6 — Orbital Stars: throw stars that orbit target, shift to collapse and explode
    public static void keyOfStarsOrbital(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (SoulCore.getSoulEssence(player) < 500) return;

        net.minecraft.nbt.CompoundTag data = player.getPersistentData();

        // Shift + ability = collapse if stars are currently orbiting
        if (player.isShiftKeyDown()) {
            if (!data.getBoolean(NBT_ORBITAL_ACTIVE)) {
                player.sendSystemMessage(Component.literal("§8No stars currently orbiting."));
                return;
            }
            collapseOrbitalStars(player, sl);
            return;
        }

        // Can't launch new orbital while one is active
        if (data.getBoolean(NBT_ORBITAL_ACTIVE)) {
            player.sendSystemMessage(Component.literal("§8Stars already orbiting — shift to collapse first."));
            return;
        }

        int collapseCD = data.getInt(NBT_ORBITAL_COLLAPSE_CD);
        if (collapseCD > 0) {
            player.sendSystemMessage(Component.literal("§8Orbital on cooldown: §7" + (collapseCD / 20) + "s"));
            return;
        }

        int currentStars = data.getInt(NBT_KEY_OF_STARS_STAR_COUNT);
        if (currentStars <= 0) {
            player.sendSystemMessage(Component.literal("§8No stars to throw."));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        // Raycast forward to find hit target or block
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 current = start;
        boolean hitSomething = false;

        for (float i = 0; i < 24; i += 0.5f) {
            current = current.add(direction.scale(0.5));

            // Particle trail as stars travel
            sl.sendParticles(ParticleTypes.END_ROD,
                    current.x, current.y, current.z, 1, 0.05, 0.05, 0.05, 0.01);

            // Check entity hit
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(current, current).inflate(0.6),
                    e -> e != player && e.isAlive());

            if (!hits.isEmpty()) {
                LivingEntity hitTarget = hits.get(0);
                data.putUUID(NBT_ORBITAL_TARGET, hitTarget.getUUID());
                data.putBoolean(NBT_ORBITAL_IS_BLOCK, false);
                data.putBoolean(NBT_ORBITAL_ACTIVE, true);
                data.putInt(NBT_ORBITAL_STAR_COUNT, currentStars);
                data.putInt(NBT_ORBITAL_TICK, 0);
                data.putInt(NBT_KEY_OF_STARS_STAR_COUNT, 0); // consume all stars

                player.sendSystemMessage(Component.literal("§bStars orbiting: §e" + currentStars + " §bstars locked on §7" + hitTarget.getName().getString()));
                player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
                hitSomething = true;
                break;
            }

            // Check block hit
            BlockPos blockPos = BlockPos.containing(current);
            if (level.getBlockState(blockPos).isSolid()) {
                data.putInt(NBT_ORBITAL_BLOCK_X, blockPos.getX());
                data.putInt(NBT_ORBITAL_BLOCK_Y, blockPos.getY());
                data.putInt(NBT_ORBITAL_BLOCK_Z, blockPos.getZ());
                data.putBoolean(NBT_ORBITAL_IS_BLOCK, true);
                data.putBoolean(NBT_ORBITAL_ACTIVE, true);
                data.putInt(NBT_ORBITAL_STAR_COUNT, currentStars);
                data.putInt(NBT_ORBITAL_TICK, 0);
                data.putInt(NBT_KEY_OF_STARS_STAR_COUNT, 0);

                player.sendSystemMessage(Component.literal("§bStars orbiting block — §e" + currentStars + " §bstars locked."));
                player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
                hitSomething = true;
                break;
            }
        }

        if (!hitSomething) {
            player.sendSystemMessage(Component.literal("§8No target found."));
            // Refund essence
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + 500);
        }
    }

    private static void tickOrbitalStars(Player player, ServerLevel sl) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        int starCount = data.getInt(NBT_ORBITAL_STAR_COUNT);
        if (starCount <= 0) {
            data.putBoolean(NBT_ORBITAL_ACTIVE, false);
            return;
        }

        int tick = data.getInt(NBT_ORBITAL_TICK) + 1;
        data.putInt(NBT_ORBITAL_TICK, tick);

        boolean isBlock = data.getBoolean(NBT_ORBITAL_IS_BLOCK);
        double cx, cy, cz;

        if (isBlock) {
            cx = data.getInt(NBT_ORBITAL_BLOCK_X) + 0.5;
            cy = data.getInt(NBT_ORBITAL_BLOCK_Y) + 0.5;
            cz = data.getInt(NBT_ORBITAL_BLOCK_Z) + 0.5;
        } else {
            // Track living entity
            if (!data.contains(NBT_ORBITAL_TARGET)) {
                data.putBoolean(NBT_ORBITAL_ACTIVE, false);
                return;
            }
            UUID targetUUID = data.getUUID(NBT_ORBITAL_TARGET);
            LivingEntity target = (LivingEntity) sl.getEntity(targetUUID);
            if (target == null || target.isDeadOrDying()) {
                data.putBoolean(NBT_ORBITAL_ACTIVE, false);
                player.sendSystemMessage(Component.literal("§8Orbital target lost."));
                return;
            }
            cx = target.getX();
            cy = target.getY() + 1.0;
            cz = target.getZ();

            // Tick damage to entity every 20 ticks per orbiting star
            if (tick % 20 == 0) {
                float orbitDmg = 0.75f * starCount;
                target.hurt(sl.damageSources().magic(), orbitDmg);
            }
        }

        // Spawn particles for each orbiting star
        for (int s = 0; s < starCount; s++) {
            double angleOffset = (2 * Math.PI / starCount) * s;
            double angle = Math.toRadians(tick * 6) + angleOffset; // 6 degrees per tick
            double radius = 2.5;

            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(tick * 0.05 + angleOffset) * 0.4; // gentle vertical bob
            double pz = cz + Math.sin(angle) * radius;

            sl.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private static void collapseOrbitalStars(Player player, ServerLevel sl) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        int starCount = data.getInt(NBT_ORBITAL_STAR_COUNT);
        boolean isBlock = data.getBoolean(NBT_ORBITAL_IS_BLOCK);

        double cx, cy, cz;
        LivingEntity target = null;

        if (isBlock) {
            cx = data.getInt(NBT_ORBITAL_BLOCK_X) + 0.5;
            cy = data.getInt(NBT_ORBITAL_BLOCK_Y) + 0.5;
            cz = data.getInt(NBT_ORBITAL_BLOCK_Z) + 0.5;
        } else {
            if (!data.contains(NBT_ORBITAL_TARGET)) {
                data.putBoolean(NBT_ORBITAL_ACTIVE, false);
                return;
            }
            UUID targetUUID = data.getUUID(NBT_ORBITAL_TARGET);
            target = (LivingEntity) sl.getEntity(targetUUID);
            if (target == null || target.isDeadOrDying()) {
                data.putBoolean(NBT_ORBITAL_ACTIVE, false);
                return;
            }
            cx = target.getX();
            cy = target.getY() + 1.0;
            cz = target.getZ();
        }

        // Explosion damage — scales with star count
        float explosionDamage = 8.0f + (starCount * 5.0f);
        float explosionRadius = 2.5f + (starCount * 0.4f);

        AABB blastArea = new AABB(
                cx - explosionRadius, cy - explosionRadius, cz - explosionRadius,
                cx + explosionRadius, cy + explosionRadius, cz + explosionRadius);

        List<LivingEntity> blastTargets = sl.getEntitiesOfClass(LivingEntity.class, blastArea, e ->
                e != player && !e.isDeadOrDying());

        for (LivingEntity blastTarget : blastTargets) {
            double dist = Math.sqrt(blastTarget.distanceToSqr(cx, cy, cz));
            float falloff = 1.0f - (float)(dist / explosionRadius);
            float actualDmg = Math.max(explosionDamage * falloff, 4.0f);
            blastTarget.hurt(sl.damageSources().magic(), actualDmg);

            // Knockback away from center
            Vec3 knockDir = blastTarget.position().subtract(cx, cy, cz).normalize();
            blastTarget.setDeltaMovement(blastTarget.getDeltaMovement().add(
                    knockDir.x * 1.2, 0.5, knockDir.z * 1.2));
            blastTarget.hurtMarked = true;
        }

        // Implosion particle spiral inward then burst
        for (int ring = 0; ring < 3; ring++) {
            double r = explosionRadius - ring * 0.5;
            for (int p = 0; p < 16; p++) {
                double angle = (2 * Math.PI / 16) * p;
                sl.sendParticles(ParticleTypes.END_ROD,
                        cx + Math.cos(angle) * r,
                        cy,
                        cz + Math.sin(angle) * r,
                        1, 0, 0, 0, 0.15); // velocity toward center via speed
            }
        }
        // Central detonation burst
        sl.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 40, explosionRadius * 0.3, explosionRadius * 0.3, explosionRadius * 0.3, 0.2);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 60, explosionRadius * 0.2, explosionRadius * 0.2, explosionRadius * 0.2, 0.15);

        player.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.5f);
        player.sendSystemMessage(Component.literal("§bOrbital Stars collapsed — §e" + starCount + " §bstars detonated."));

        // Clean up
        data.putBoolean(NBT_ORBITAL_ACTIVE, false);
        data.putInt(NBT_ORBITAL_STAR_COUNT, 0);
        data.putInt(NBT_ORBITAL_TICK, 0);
        data.remove(NBT_ORBITAL_TARGET);
        data.putBoolean(NBT_ORBITAL_IS_BLOCK, false);
        player.getPersistentData().putInt(NBT_ORBITAL_COLLAPSE_CD, 40); // 10s before can orbit again
    }

    // ability 7
    public static void keyOfStarsCosmicPlague(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 6) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);

        if (player.isShiftKeyDown()){



        }else{
            int radius = 4;

            List<LivingEntity> hits = sl.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.position(), player.position()).inflate(radius, 1, radius),
                    e -> e != player && e.isAlive()
            );

            Vec3 center = player.position();
            for (int i = 0; i < 16; i++) {
                double angle = (i * 2 * Math.PI) / 16;
                double offsetX = Math.cos(angle) * (radius+1);
                double offsetZ = Math.sin(angle) * (radius+1);

                sl.sendParticles(ParticleTypes.END_ROD,
                        center.x + offsetX, center.y + 0.1, center.z + offsetZ,
                        1, 0, 0, 0, 0);
            }

            for (LivingEntity target : hits){
                effectAddCosmicPlague(target);
            }
        }
    }

    private static void effectAddCosmicPlague(LivingEntity target){
        target.getPersistentData().putFloat(NBT_COSMIC_PLAGUE_DURATION,target.getPersistentData().getFloat(NBT_COSMIC_PLAGUE_DURATION)+400);
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
