package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CShowHudOverlayPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttendantOfMysteries {
    private static final String DAMAGE_DURATION = "attendant_of_mysteries_damage_duration";
    private static final String DAMAGE_TARGET_UUID = "attendant_of_mysteries_target_uuid";

    // Ability 5
    private static final String COUNTDOWN_TICKS = "attendant_of_mysteries_countdown_ticks";
    private static final String COUNTDOWN_SNEAKING = "attendant_of_mysteries_countdown_sneaking";
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COUNTDOWN_COLOR = 0xE8C872;      // pale gold
    private static final int COUNTDOWN_LAST_COLOR = 0xD83A4A; // the final "1"
    private static final String SNEAK_IMAGE = "attendant_of_mysteries_sneak"; // textures/gui/attendant_of_mysteries_sneak.png

    // Stacks: every ability used adds one picture next to the hotbar, the 5th one explodes
    private static final String STACKS = "attendant_of_mysteries_stacks";
    private static final String STACK_IMAGE_KEY = "attendant_of_mysteries_stack_image_"; // + slot number
    private static final String STACK_IMAGE_ID = "aom_stack_";                           // screen image ids
    private static final int MAX_STACKS = 5;
    private static final float STACK_BURST_DAMAGE = 40f;
    private static final double STACK_BURST_RADIUS = 8.0;
    // Pictures for the stacks, from assets/forgerealmmod/textures/gui/ (without .png). One is picked at random.
    private static final List<String> STACK_IMAGES = List.of(
            "attendant_stack_orb"
    );

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AttendantOfMysteriesEvents {

        @SubscribeEvent
        public static void onAttendantOfMysteriesTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;

            // Handle the 5-second Damage Transfer Timer
            if (player.getPersistentData().contains(DAMAGE_DURATION)) {
                int duration = player.getPersistentData().getInt(DAMAGE_DURATION);
                if (duration > 0) {
                    player.getPersistentData().putInt(DAMAGE_DURATION, duration - 1);
                }
            }

            // Ability 5 countdown (5, 4, 3, 2, 1)
            tickAbilityFiveCountdown(player, sl);

            if (!SoulCore.getAspect(player).equals("Attendant Of Mysteries")) return;

            int ascensionStage = SoulCore.getAscensionStage(player);

            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 1));
            if (player.isOnFire()) {
                player.clearFire();
            }
        }

        @SubscribeEvent
        public static void onAttendantOfMysteriesMobTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide()) return;

            // Left this intact in case you are using it for other mobs,
            // but converted it to Int for cleaner tick math.
            if (entity.getPersistentData().contains(DAMAGE_DURATION)) {
                int duration = entity.getPersistentData().getInt(DAMAGE_DURATION);
                if (duration > 0) {
                    entity.getPersistentData().putInt(DAMAGE_DURATION, duration - 1);
                }
            }
        }

        @SubscribeEvent
        public static void onAttendantOfMysteriesPlayerTakeDamage(LivingDamageEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (player.level().isClientSide()) return;

            // Check if the Damage Transfer ability is active
            if (player.getPersistentData().getInt(DAMAGE_DURATION) > 0) {
                if (player.getPersistentData().hasUUID(DAMAGE_TARGET_UUID)) {
                    UUID targetUuid = player.getPersistentData().getUUID(DAMAGE_TARGET_UUID);

                    if (player.level() instanceof ServerLevel serverLevel) {
                        Entity targetEntity = serverLevel.getEntity(targetUuid);

                        // Ensure the target is still alive
                        if (targetEntity instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                            float originalDamage = event.getAmount();
                            float redirectedDamage = originalDamage * 0.5f; // Redirects 50%
                            float remainingDamage = originalDamage - redirectedDamage;

                            // Reduce damage taken by the player
                            event.setAmount(remainingDamage);

                            // Deal redirected damage to the target
                            livingTarget.hurt(player.damageSources().thorns(player), redirectedDamage);
                        }
                    }
                }
            }
        }

        // pictures on screen don't survive relogging/respawning, so redraw them from the saved stack count
        @SubscribeEvent
        public static void onAttendantOfMysteriesLogin(PlayerEvent.PlayerLoggedInEvent event) {
            refreshStackImages(event.getEntity());
        }

        @SubscribeEvent
        public static void onAttendantOfMysteriesRespawn(PlayerEvent.PlayerRespawnEvent event) {
            refreshStackImages(event.getEntity());
        }
    }

    //Ability 1
    public static void attendantOfMysteriesPaperDagger(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);

        if (player.isShiftKeyDown()){
            shootProjectiles(player,sl,90,7,12 ,12);
        }else{
            shootProjectiles(player,sl,30,3, 32, 32);
        }

        addStack(player, sl);
    }

    //Ability 2
    public static void attendantOfMysteriesFlamingJump(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 1250) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-1250);

        if (player.isShiftKeyDown()){
            Level world = player.level();

            BlockPos blockBelow = player.blockPosition().below();
            if (world.isEmptyBlock(blockBelow.above()) && world.getBlockState(blockBelow).isSolidRender(world, blockBelow)) {
                world.setBlockAndUpdate(blockBelow.above(), Blocks.FIRE.defaultBlockState());
            }
        }else{
            BlockPos pos = findNearestFire(player,32);

            if (pos != null) {
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                player.teleportTo(x,y,z);
            }
        }

        addStack(player, sl);
    }

    //Ability 3
    public static void attendantOfMysteriesDamageTransfer(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 1250) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        LivingEntity target = getTarget(player, sl, level, 16);
        if (target == null) return; // Fail ability if no target is looked at

        // Consume essence only upon successful cast
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1250);

        if (player.isShiftKeyDown()){
            // Transfer Debuffs
            List<MobEffectInstance> debuffsToMove = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (!effect.getEffect().value().isBeneficial()) {
                    debuffsToMove.add(effect);
                }
            }

            for (MobEffectInstance debuff : debuffsToMove) {
                target.addEffect(new MobEffectInstance(debuff.getEffect(), debuff.getDuration(), debuff.getAmplifier()));
                player.removeEffect(debuff.getEffect());
            }
        } else {
            player.getPersistentData().putInt(DAMAGE_DURATION, 100);
            player.getPersistentData().putUUID(DAMAGE_TARGET_UUID, target.getUUID());
        }

        addStack(player, sl);
    }

    //Ability 4
    public static void attendantOfMysteriesAirCannon(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 2450) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2450);

        if (player.isShiftKeyDown()){
            player.setDeltaMovement(0, 80, 0);
            player.hurtMarked = true;
        }else{
            shootAirCannon(player,sl,24,64);

            Vec3 lookDirection = player.getLookAngle().normalize();

            double strength = 1.8;

            double recoilX = -lookDirection.x * strength;
            double recoilY = (-lookDirection.y * strength * 0.5) + 0.2;
            double recoilZ = -lookDirection.z * strength;

            player.setDeltaMovement(recoilX, recoilY, recoilZ);
            player.hurtMarked = true;
        }

        addStack(player, sl);
    }

    //Ability 5
    public static void attendantOfMysteriesCountdown(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassKeyOfStars(player, bypassClassCheck)) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (SoulCore.getSoulEssence(player) < 3000) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (player.getPersistentData().getInt(COUNTDOWN_TICKS) > 0) return; // already running

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);

        boolean sneaking = player.isShiftKeyDown();
        player.getPersistentData().putInt(COUNTDOWN_TICKS, COUNTDOWN_SECONDS * 20);
        player.getPersistentData().putBoolean(COUNTDOWN_SNEAKING, sneaking);

        if (sneaking){
            ScreenImages.show(player, "aom_sneak", SNEAK_IMAGE, 128, COUNTDOWN_SECONDS * 20);
        }else{
            showCountdownNumber(player, COUNTDOWN_SECONDS);
        }

        addStack(player, sl);
    }

    // =========================
    // Stacks
    // =========================

    private static void addStack(Player player, ServerLevel sl) {
        int slot = player.getPersistentData().getInt(STACKS); // 0..4
        String image = STACK_IMAGES.get(player.getRandom().nextInt(STACK_IMAGES.size()));
        player.getPersistentData().putString(STACK_IMAGE_KEY + slot, image);
        showStackImage(player, slot, image);

        int stacks = slot + 1;
        if (stacks >= MAX_STACKS) {
            unleashStacks(player, sl);
            ScreenImages.hide(player, STACK_IMAGE_ID + "*", 10); // let the 5th show for a moment, then all fade
            stacks = 0;
        }
        player.getPersistentData().putInt(STACKS, stacks);
    }

    // Runs when the 5th stack is added. Big hit on everything around the player.
    private static void unleashStacks(Player player, ServerLevel sl) {
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(STACK_BURST_RADIUS),
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.invulnerableTime = 0; // so it still hits right after the ability that triggered it
            target.hurt(player.damageSources().playerAttack(player), STACK_BURST_DAMAGE);
        }

        double x = player.getX(), y = player.getY() + 1, z = player.getZ();
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            sl.sendParticles(ParticleTypes.END_ROD,
                    x + Math.cos(angle) * STACK_BURST_RADIUS, y, z + Math.sin(angle) * STACK_BURST_RADIUS,
                    1, 0, 0, 0, 0);
        }
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
    }

    // Stacks sit to the right of the hotbar, 18px apart like hotbar slots
    private static void showStackImage(Player player, int slot, String image) {
        ScreenImages.show(player, STACK_IMAGE_ID + slot, image,
                ScreenAnchor.BOTTOM, 103 + slot * 18, -3, 16, ScreenImages.FOREVER);
    }

    private static void refreshStackImages(Player player) {
        int stacks = player.getPersistentData().getInt(STACKS);
        for (int slot = 0; slot < MAX_STACKS; slot++) {
            if (slot < stacks) {
                String image = player.getPersistentData().getString(STACK_IMAGE_KEY + slot);
                showStackImage(player, slot, image.isEmpty() ? STACK_IMAGES.get(0) : image);
            } else {
                ScreenImages.hide(player, STACK_IMAGE_ID + slot);
            }
        }
    }

    // Runs when the 5,4,3,2,1 countdown hits 0. Put what the ability actually does here.
    private static void onAbilityFiveCountdownFinished(Player player, ServerLevel sl) {

    }

    // Runs when the sneak version's texture has finished showing (same 5 seconds).
    private static void onAbilityFiveSneakFinished(Player player, ServerLevel sl) {

    }

    private static void tickAbilityFiveCountdown(Player player, ServerLevel sl) {
        int ticks = player.getPersistentData().getInt(COUNTDOWN_TICKS);
        if (ticks <= 0) return;

        ticks--;
        player.getPersistentData().putInt(COUNTDOWN_TICKS, ticks);
        boolean sneaking = player.getPersistentData().getBoolean(COUNTDOWN_SNEAKING);

        if (ticks == 0) {
            if (sneaking) {
                onAbilityFiveSneakFinished(player, sl);
            } else {
                onAbilityFiveCountdownFinished(player, sl);
            }
        } else if (!sneaking && ticks % 20 == 0) {
            showCountdownNumber(player, ticks / 20); // 80 -> 4, 60 -> 3, 40 -> 2, 20 -> 1
        }
    }

    private static void showCountdownNumber(Player player, int number) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int color = number == 1 ? COUNTDOWN_LAST_COLOR : COUNTDOWN_COLOR;
        PacketHandler.sendToPlayer(CShowHudOverlayPacket.number(number, color, 20), serverPlayer);
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



    private static BlockPos findNearestFire(Player player, int radius) {
        Level world = player.level();
        BlockPos playerPos = player.blockPosition();

        BlockPos nearestFirePos = null;
        double shortestDistanceSqr = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (world.getBlockState(checkPos).is(Blocks.FIRE)) {
                        double distSqr = checkPos.distSqr(playerPos);
                        if (distSqr < shortestDistanceSqr) {
                            shortestDistanceSqr = distSqr;
                            nearestFirePos = checkPos;
                        }
                    }
                }
            }
        }
        return nearestFirePos;
    }

    private static void shootAirCannon(Player player, ServerLevel sl, int distance, int damage) {
        Vec3 location = player.getEyePosition();
        Vec3 baseDirection = player.getLookAngle().normalize();

        for (float j = 0; j <= distance; j += 0.5f) {
            Vec3 c = location.add(baseDirection.scale(j));

            List<LivingEntity> hits = sl.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(c, c).inflate(0.25),
                    e -> e != player && e.isAlive()
            );

            if (!hits.isEmpty()) {
                for (LivingEntity livingEntity : hits) {
                    livingEntity.hurt(
                            player.level().damageSources().playerAttack(player),
                            damage
                    );
                }
                break;
            }
        }

    }

    private static void shootProjectiles(Player player, ServerLevel sl, float angle, int spreadAmount, int distance, int damage) {
        Vec3 location = player.getEyePosition();
        Vec3 baseDirection = player.getLookAngle().normalize();

        double baseYaw = Math.toDegrees(Math.atan2(-baseDirection.x, baseDirection.z));
        double basePitch = Math.toDegrees(Math.asin(-baseDirection.y));

        for (float i = -angle / 2; i < angle / 2; i += angle / spreadAmount) {
            float yaw = (float) (baseYaw + i);
            float pitch = (float) basePitch;

            Vec3 direction = Vec3.directionFromRotation(pitch, yaw);

            for (float j = 0; j <= distance; j += 0.5f) {
                Vec3 c = location.add(direction.scale(j));

                sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 1, 0, 0, 0, 0);

                List<LivingEntity> hits = sl.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(c, c).inflate(0.25),
                        e -> e != player && e.isAlive()
                );

                if (!hits.isEmpty()) {
                    for (LivingEntity livingEntity : hits) {
                        livingEntity.hurt(
                                player.level().damageSources().playerAttack(player),
                                damage
                        );
                    }
                    break;
                }
            }
        }
    }

    private static boolean canUseClassKeyOfStars(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Attendant Of Mysteries");
    }
}