package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.combat.AnimatedShot;
import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.SpellFx;
import net.lukario.frogerealm.combat.Spells;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.lukario.frogerealm.status.Concealment;
import net.lukario.frogerealm.status.FallGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class PaleEmperor {

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // CHAINS
    // =====================================================================================

    private static final SlashFx CHAIN_SLASH = SlashFx.line("slash/smooth")
            .color(0xC0F5C211)
            .core(0xC0AADD20)
            .width(0.12f)
            .taper(SlashFx.Taper.UNIFORM);

    /*
     * One chain lasts 120 ticks = 6 seconds.
     */
    public static final int CHAIN_TICKS = 120;

    /**
     * Chain used by the Pale Emperor root ability.
     *
     * 0-5    : appears
     * 5-108  : stays fully extended
     * 108-118: retracts
     * 120    : disappears
     */
    public static final ModelFx CHAIN = ModelFx.of("pale_emperor/underworld_chain")
            .pivot(8, -14, 8)
            .glow()
            .lifetime(CHAIN_TICKS)
            .fade(0, 4)
            .key(0, pose().scale(0.04f))
            .key(5, pose().scale(1f), ModelFx.Ease.OUT)
            .during(108, 118, pose().scale(0.04f), ModelFx.Ease.IN);

    public static final ModelFx CHAIN_STRONG = ModelFx.of("pale_emperor/underworld_chain")
            .pivot(8, -14, 8)
            .glow()
            .lifetime(100)
            .fade(0, 4)
            .key(0, pose().scale(0.04f))
            .key(5, pose().scale(1f), ModelFx.Ease.OUT)
            .during(58, 68, pose().scale(0.04f), ModelFx.Ease.IN);

    /**
     * Creates a chain between two points.
     */
    public static void chain(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 reach = to.subtract(from);

        float length = (float) reach.length() / 2.875f;

        float thick = Math.max(
                1.1f,
                Math.min(1.8f, length)
        );

        ParticleShapes.modelAlong(
                sl,
                CHAIN.scale(thick, length, thick),
                from,
                reach
        );
    }

    // =====================================================================================
    // SEAL
    // =====================================================================================

    private static final ModelFx SEAL_APPEARS = ModelFx.of("pale_emperor/death_sigil_ring")
            .glow()
            .unshaded()
            .seeThrough()
            .fade(0, 14)
            .key(0, pose().scale(0.2f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT);

    public static final ModelFx SEAL_RING = SEAL_APPEARS.spin(1.2f);

    public static final ModelFx SEAL_CORE = ModelFx.of("pale_emperor/death_sigil_core")
            .glow()
            .unshaded()
            .seeThrough()
            .fade(0, 14)
            .key(0, pose().scale(0.2f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT)
            .spin(-2.4f);

    /**
     * Places the Pale Emperor seal on the ground.
     */
    public static void seal(
            ServerLevel sl,
            Vec3 ground,
            float yaw,
            float blocksWide,
            int ticks
    ) {
        float scale = blocksWide / 3f;

        Vec3 floor = ground.add(0, 0.06, 0);

        ParticleShapes.model(
                sl,
                SEAL_RING.scale(scale).lifetime(ticks),
                floor,
                yaw,
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                SEAL_CORE.scale(scale).lifetime(ticks),
                floor.add(0, 0.02, 0),
                yaw,
                0f,
                0f
        );
    }

    // =====================================================================================
    // GATE MODELS
    // =====================================================================================

    private static final ModelFx GATE = ModelFx.of("pale_emperor/underworld_gate")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(500)
            .fade(5, 10);

    private static final ModelFx GATE_VOID = ModelFx.of("pale_emperor/underworld_void")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(500)
            .fade(5, 10);

    private static final ModelFx GATE_DOOR_LEFT = ModelFx.of("pale_emperor/underworld_gate_door_left")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(500)
            .fade(5, 10);

    private static final ModelFx GATE_DOOR_RIGHT = ModelFx.of("pale_emperor/underworld_gate_door_right")
            .scale(2f)
            .pivot(8, 0, 8)
            .glow()
            .aura(0xFF0000, 0.1f, 3)
            .lifetime(500)
            .fade(5, 10);

    // =====================================================================================
    // PALE EMPEROR ROOT
    // =====================================================================================

    public static void paleEmperorRoot(
            Player player,
            ServerLevel sl,
            boolean bypassClassCheck
    ) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;

        if (SoulCore.getSoulEssence(player) < 1250) return;

        if (SoulCore.getAscensionStage(player) < 0) return;

        LivingEntity enemy = Spells.aimEnemy(
                player,
                sl,
                32
        );

        if (enemy == null) return;

        int duration = 100;

        SoulCore.setSoulEssence(
                player,
                SoulCore.getSoulEssence(player) - 1250
        );

        Root.apply(
                enemy,
                duration,
                RootRestriction.EVERYTHING
        );

        tether(
                sl,
                CHAIN_SLASH,
                player,
                enemy,
                duration
        );

        Vec3 ground = enemy.position();

        float yaw = player.getYRot();

        // =====================================================================
        // NORMAL CAST
        // =====================================================================

        if (!player.isShiftKeyDown()) {

            Vec3 position = ground.add(
                    0,
                    -1.5,
                    0
            );

            ModelFx emperorHand = ModelFx.of(
                            "pale_emperor/emperor_hand"
                    )
                    .frames(5)
                    .smooth()
                    .scale(1.5f)
                    .pivot(7, -6, 7)
                    .glow()
                    .lifetime(duration)
                    .fade(3, 8)
                    .key(
                            0,
                            pose().frame(3)
                    )
                    .during(
                            4,
                            14,
                            pose().frame(0),
                            ModelFx.Ease.OUT
                    )
                    .during(
                            26,
                            32,
                            pose().frame(4),
                            ModelFx.Ease.IN
                    );

            ParticleShapes.model(
                    sl,
                    emperorHand,
                    position,
                    yaw,
                    0f,
                    0f
            );

            // =====================================================================
            // SHIFT CAST — SEAL + 4 CHAINS
            // =====================================================================

        } else {

            seal(
                    sl,
                    ground,
                    yaw,
                    6f,
                    CHAIN_TICKS
            );

            Vec3 heart = ground.add(
                    0,
                    1.5,
                    0
            );

            for (int i = 0; i < 4; i++) {

                double angle = Math.toRadians(
                        yaw + 45 + i * 90
                );

                Vec3 from = ground.add(
                        -Math.sin(angle) * 2.6,
                        0,
                        Math.cos(angle) * 2.6
                );

                Vec3 stop = heart.add(
                        from.subtract(heart)
                                .normalize()
                                .scale(0.45)
                );

                chain(
                        sl,
                        from,
                        stop
                );
            }
        }
    }

    // =====================================================================================
    // PALE EMPEROR DOOR
    // =====================================================================================

    public static void paleEmperorDoor(
            Player player,
            ServerLevel sl,
            boolean bypassClassCheck
    ) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;

        if (SoulCore.getSoulEssence(player) < 11250) return;

        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(
                player,
                SoulCore.getSoulEssence(player) - 11250
        );

        Vec3 position = player.position().add(
                0,
                2,
                0
        );

        ModelFx gate = GATE;

        ModelFx gateVoid = GATE_VOID;

        ModelFx gateDoorLeft = GATE_DOOR_LEFT.key(
                100,
                pose().up(-6),
                ModelFx.Ease.IN
        );

        ModelFx gateDoorRight = GATE_DOOR_RIGHT.key(
                100,
                pose().up(-6),
                ModelFx.Ease.IN
        );

        for (int i = 100; i <= 500; i += 10) {

            Later.run(
                    sl,
                    i,
                    () -> paleEmpGatePull(
                            sl,
                            player,
                            position
                    )
            );
        }

        ParticleShapes.model(
                sl,
                gate,
                position,
                player.getYRot(),
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                gateVoid,
                position,
                player.getYRot(),
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                gateDoorLeft,
                position,
                player.getYRot(),
                0f,
                0f
        );

        ParticleShapes.model(
                sl,
                gateDoorRight,
                position,
                player.getYRot(),
                0f,
                0f
        );
    }

    // =====================================================================================
    // GATE PULL
    // =====================================================================================

    private static void paleEmpGatePull(
            ServerLevel sl,
            Player player,
            Vec3 position
    ) {
        List<LivingEntity> entities = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position, position).inflate(12),
                entity -> entity != player
        );

        for (LivingEntity entity : entities) {
            Spells.pull(
                    entity,
                    position,
                    1,
                    0.1
            );
        }

        List<LivingEntity> enemies = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position, position).inflate(2),
                entity -> entity != player
        );

        for (LivingEntity enemy : enemies) {
            enemy.hurt(
                    sl.damageSources().playerAttack(player),
                    4
            );
        }
    }

    // =====================================================================================
    // CLASS CHECK
    // =====================================================================================

    private static boolean canUseCharacteristic(
            Player player,
            boolean bypassClassCheck
    ) {
        if (bypassClassCheck) return true;

        return "Pale Emperor".equals(
                SoulCore.getAspect(player)
        );
    }

    public static void tether(ServerLevel sl, SlashFx line, Supplier<Vec3> from, Supplier<Vec3> to, int ticks) {
        Vec3 a = from.get().add(0,-0.95,0);
        Vec3 b = to.get().add(0,-1,0);
        if (a == null || b == null) return;
        // each copy lives a little longer than the gap to the next one, so the line never blinks
        ParticleShapes.slashBetween(sl, line.lifetime(2 * 3).sweep(1), a, b);
        if (ticks > 2) {
            Later.run(sl, 2, () -> tether(sl, line, from, to, ticks - 2));
        }
    }

    public static void tether(ServerLevel sl, SlashFx line, Entity from, Entity to, int ticks) {
        tether(sl, line, () -> middleOf(from, sl), () -> middleOf(to, sl), ticks);
    }

    public static Vec3 middleOf(Entity entity, ServerLevel sl) {
        if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.level() != sl) return null;
        return entity.getBoundingBox().getCenter();
    }

    // =====================================================================================
    // FEATHERED SERPENT MODELS
    // =====================================================================================

    public static final int SERPENT_TICKS = 150;

    /** The tick the serpent's jaws are wide open: the screech leaves it, or the lines do. */
    public static final int SERPENT_ROARS = 40;

    public static final ModelFx SERPENT = ModelFx.of("pale_emperor/feathered_serpent").frames(3).smooth()
            .scale(2f).pivot(8, -16, 12).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-6.2f))                                    // under the ground
            .key(12, pose().up(-6.2f))                                   // waits for the coils and the seal
            .key(30, pose().up(0), ModelFx.Ease.OUT)                             // rears up
            .during(34, 40, pose().frame(2), ModelFx.Ease.OUT)                   // roars: jaws wide, collar flared
            .during(60, 68, pose().frame(0), ModelFx.Ease.IN_OUT)                // shuts its mouth
            .during(78, 84, pose().frame(2), ModelFx.Ease.OUT)                   // opens it again...
            .during(80, 86, pose().pitch(28), ModelFx.Ease.IN)                   // ...and strikes forward and down
            .during(86, 89, pose().frame(0), ModelFx.Ease.IN)                    // the bite
            .during(94, 108, pose().pitch(0), ModelFx.Ease.IN_OUT)               // draws back
            .during(128, 148, pose().up(-6.2f), ModelFx.Ease.IN);                // sinks away

    /**
     * The serpent of the sneak cast: it rears up, opens its jaws and keeps them open for as long as
     * its lines hold, then shuts them and sinks away. No bite.
     */
    public static final ModelFx SERPENT_BINDING = ModelFx.of("pale_emperor/feathered_serpent").frames(3).smooth()
            .scale(2f).pivot(8, -16, 12).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-6.2f))                                    // under the ground
            .key(12, pose().up(-6.2f))                                   // waits for the coils and the seal
            .key(30, pose().up(0), ModelFx.Ease.OUT)                             // rears up
            .during(34, 40, pose().frame(2), ModelFx.Ease.OUT)                   // jaws wide: the lines leave its mouth
            .during(100, 108, pose().frame(0), ModelFx.Ease.IN_OUT)              // lets go and shuts its mouth
            .during(128, 148, pose().up(-6.2f), ModelFx.Ease.IN);                // sinks away

    /** Its coiled body, 5 blocks across. */
    public static final ModelFx SERPENT_COIL = ModelFx.of("pale_emperor/feathered_serpent_coil")
            .scale(2f).pivot(8, -16, 8).glow().lifetime(SERPENT_TICKS).fade(0, 4)
            .key(0, pose().up(-2.3f))
            .key(12, pose().up(0), ModelFx.Ease.OUT)
            .during(132, 148, pose().up(-2.3f), ModelFx.Ease.IN);

    /** A seal opens, the coils rise out of it, the serpent rears up, roars, strikes once and sinks away. */
    public static void serpent(ServerLevel sl, Vec3 ground, float yaw) {
        serpent(sl, SERPENT, ground, yaw);
    }

    /** The same with another animation for the head, e.g. SERPENT_BINDING. */
    public static void serpent(ServerLevel sl, ModelFx head, Vec3 ground, float yaw) {
        seal(sl, ground, yaw, 7.5f, SERPENT_TICKS);
        ParticleShapes.model(sl, SERPENT_COIL, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, head, ground, yaw, 0f, 0f);
    }

    /** Where the open mouth of a serpent that rose at 'ground' is. */
    private static Vec3 serpentMouth(Vec3 ground, float yaw) {
        return ground.add(0, 2.4, 0).add(Vec3.directionFromRotation(0f, yaw).scale(1.7));
    }

    // =====================================================================================
    // SERPENT: SCREECH (normal cast)
    // =====================================================================================

    /** What the screech does to everyone its wave reaches. */
    public static final float SCREECH_DAMAGE = 45f;

    /** How far the wave travels from the serpent, in blocks. */
    public static final double SCREECH_RADIUS = 16;

    /** How far above and below the serpent's ground the wave still catches someone. */
    public static final double SCREECH_HEIGHT = 6;

    /** How long the wave takes to get that far: 16 blocks in 20 ticks = 16 blocks a second. */
    public static final int SCREECH_TICKS = 20;

    /**
     * The serpent screeches: a wave races outward from it in every direction, and each enemy is hurt
     * once, at the moment the wave gets to it.
     */
    private static void screech(Player player, ServerLevel sl, Vec3 ground, float yaw) {
        if (!Spells.casterStillHere(player, sl)) return;

        Vec3 mouth = serpentMouth(ground, yaw);

        Spells.sound(sl, mouth, SoundEvents.ENDER_DRAGON_GROWL, 1.6f, 1.7f);
        Spells.sound(sl, mouth, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.4f);

        // the first ring is the wave itself, the others follow it like an echo
        screechRing(sl, ground.add(0, 0.07, 0), 0xF09CFFD2, 0);
        screechRing(sl, ground.add(0, 0.09, 0), 0xB0F5C211, 3);
        screechRing(sl, ground.add(0, 0.11, 0), 0x809CFFD2, 6);
        screechRing(sl, new Vec3(ground.x, mouth.y, ground.z), 0x709CFFD2, 0);

        Set<UUID> alreadyHit = new HashSet<>();

        for (int tick = 1; tick <= SCREECH_TICKS; tick++) {

            double reach = SCREECH_RADIUS * tick / SCREECH_TICKS;

            Later.run(
                    sl,
                    tick,
                    () -> screechWaveHits(player, sl, ground, reach, alreadyHit)
            );
        }
    }

    /** A ring that grows at a steady speed until it is as wide as the screech reaches. */
    private static void screechRing(ServerLevel sl, Vec3 at, int color, int delay) {
        ModelFx ring = SpellFx.SHOCK_RING.color(color).delay(delay).lifetime(SCREECH_TICKS)
                .key(0, pose().scale(1f))
                .key(SCREECH_TICKS, pose().scale((float) (SCREECH_RADIUS * 2.0)))    // steady: the damage keeps pace with it
                .during(0, SCREECH_TICKS, pose().alpha(0f), ModelFx.Ease.IN);        // stays bright, fades at the end

        ParticleShapes.model(sl, ring, at, sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** Hurts every enemy the wave has reached by now and has not hurt before. */
    private static void screechWaveHits(
            Player player,
            ServerLevel sl,
            Vec3 ground,
            double reach,
            Set<UUID> alreadyHit
    ) {
        if (!Spells.casterStillHere(player, sl)) return;

        List<LivingEntity> near = Spells.enemiesAround(
                player,
                sl,
                ground,
                SCREECH_RADIUS + SCREECH_HEIGHT
        );

        for (LivingEntity enemy : near) {

            if (Math.abs(enemy.getY() - ground.y) > SCREECH_HEIGHT) continue;

            double dx = enemy.getX() - ground.x;
            double dz = enemy.getZ() - ground.z;

            // measured to the edge of its body, so the wave hits when the ring touches it
            double distance = Math.sqrt(dx * dx + dz * dz) - enemy.getBbWidth() / 2.0;

            if (distance > reach) continue;

            if (!alreadyHit.add(enemy.getUUID())) continue;

            Spells.strike(player, enemy, SCREECH_DAMAGE);
        }
    }

    // =====================================================================================
    // SERPENT: LINES (sneak cast)
    // =====================================================================================

    private static final SlashFx SERPENT_LINE = SlashFx.line("slash/smooth")
            .color(0xC09CFFD2)
            .core(0xE0F4FFE8)
            .width(0.1f)
            .taper(SlashFx.Taper.UNIFORM);

    /** How far in front of the serpent a mob can stand and still be caught, in blocks. */
    public static final double LINE_RANGE = 24;

    /** How far to either side of straight ahead still counts as "in front", in degrees. */
    public static final double LINE_HALF_ANGLE = 50;

    /** How long the lines hold. The serpent keeps its jaws open exactly this long. */
    public static final int LINE_TICKS = 60;

    /** A line hurts what it holds once every this many ticks: 6 times in all. */
    public static final int LINE_PULSE = 10;

    /** What each of those does. */
    public static final float LINE_DAMAGE = 8f;

    /**
     * The serpent draws a line from its mouth to every enemy standing in front of it. Each line hurts
     * what it holds again and again until its time is up, the enemy dies or it gets too far away.
     */
    private static void serpentLines(
            Player player,
            ServerLevel sl,
            Vec3 ground,
            float yaw,
            Vec3 castFrom
    ) {
        if (!Spells.casterStillHere(player, sl)) return;

        Vec3 mouth = serpentMouth(ground, yaw);

        List<LivingEntity> caught = enemiesInFront(player, sl, ground, yaw, mouth, castFrom);

        if (caught.isEmpty()) return;

        Spells.sound(sl, mouth, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.9f);
        Spells.sound(sl, mouth, SoundEvents.CHAIN_PLACE, 1.4f, 0.6f);

        double snapsAt = (LINE_RANGE + 8) * (LINE_RANGE + 8);

        for (LivingEntity enemy : caught) {

            boolean[] snapped = {false};

            // the far end of the line. Once it is null the line is gone for good, and so is its damage
            Supplier<Vec3> end = () -> {
                if (snapped[0]) return null;

                Vec3 middle = Spells.casterStillHere(player, sl) && Spells.isEnemy(player, enemy)
                        ? SpellFx.middleOf(enemy, sl)
                        : null;

                if (middle == null || middle.distanceToSqr(mouth) > snapsAt) {
                    snapped[0] = true;
                    return null;
                }

                return middle;
            };

            SpellFx.tether(
                    sl,
                    SERPENT_LINE,
                    () -> mouth,
                    end,
                    LINE_TICKS
            );

            for (int tick = 0; tick < LINE_TICKS; tick += LINE_PULSE) {
                Later.run(
                        sl,
                        tick,
                        () -> {
                            if (end.get() != null) Spells.strike(player, enemy, LINE_DAMAGE);
                        }
                );
            }
        }
    }

    /**
     * Every enemy standing in front of the serpent: within LINE_RANGE of it, no more than
     * LINE_HALF_ANGLE to either side of the way it faces, and not behind a wall.
     */
    private static List<LivingEntity> enemiesInFront(
            Player player,
            ServerLevel sl,
            Vec3 ground,
            float yaw,
            Vec3 mouth,
            Vec3 castFrom
    ) {
        Vec3 facing = Vec3.directionFromRotation(0f, yaw);

        double narrowest = Math.cos(Math.toRadians(LINE_HALF_ANGLE));

        List<LivingEntity> result = new ArrayList<>();

        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, LINE_RANGE)) {

            Vec3 body = enemy.getBoundingBox().getCenter();

            Vec3 toEnemy = new Vec3(
                    body.x - ground.x,
                    0,
                    body.z - ground.z
            );

            // something standing right on the serpent has no direction: it counts as in front
            if (toEnemy.lengthSqr() > 0.01 && toEnemy.normalize().dot(facing) < narrowest) continue;

            // seen from the serpent's mouth, or from where the caster stood (the head may be up in a ceiling)
            if (!Spells.clearLine(player, sl, mouth, body)
                    && !Spells.clearLine(player, sl, castFrom, body)) continue;

            result.add(enemy);
        }

        return result;
    }

    // =====================================================================================
    // PALE EMPEROR SERPENT
    // =====================================================================================

    public static void paleEmperorSerpent(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);

        Vec3 direction = player.getLookAngle().normalize();

        Vec3 ground = player.position();

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // =====================================================================
        // NORMAL CAST - THE SERPENT SCREECHES
        // =====================================================================

        if (!player.isShiftKeyDown()) {

            serpent(sl, ground, yaw);

            Later.run(
                    sl,
                    SERPENT_ROARS,
                    () -> screech(player, sl, ground, yaw)
            );

            // =====================================================================
            // SHIFT CAST - LINES TO THE MOBS IN FRONT OF IT
            // =====================================================================

        } else {

            Vec3 castFrom = player.getEyePosition();

            serpent(sl, SERPENT_BINDING, ground, yaw);

            Later.run(
                    sl,
                    SERPENT_ROARS,
                    () -> serpentLines(player, sl, ground, yaw, castFrom)
            );
        }
    }

    public static final int SKULL_TICKS = 40;
    public static final ModelFx SKULL = ModelFx.of("pale_emperor/death_skull").frames(3).smooth()
            .scale(0.5f).pivot(8, 8, 8).glow().lifetime(SKULL_TICKS).fade(0, 12)
            .key(0, pose().scale(0.15f).alpha(0f))
            .key(10, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)
            .during(10, 40, pose().forward(32f), ModelFx.Ease.IN_OUT);


    public static final int SPEAR_TICKS = 80;
    public static final ModelFx SPEAR = ModelFx.of("pale_emperor/bone_spear")
            .scale(1.2f).pivot(8, 8, 8).glow().aura(0x409CFFD2, 0.07f, 2).lifetime(40).fade(0, 4);
    private static final ModelFx SPEAR_HOVERS = SPEAR.lifetime(SPEAR_TICKS)
            .key(0, pose().scale(0.3f).alpha(0f))
            .key(8, pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)           // appears, point up
            .during(4, 25, pose().spin(360), ModelFx.Ease.IN_OUT)                // turns once
            .during(25, 30, pose().pitch(90), ModelFx.Ease.IN_OUT)               // tips over: the point looks forward
            .during(30, 32, pose().forward(-1f), ModelFx.Ease.OUT)               // draws back
            .during(32, 64, pose().forward(36f), ModelFx.Ease.IN);


    // =====================================================================================
    // SPEAR AND SKULL (ability 4)
    // =====================================================================================
    //
    // SPEAR_HOVERS and SKULL above are only the animations. The two AnimatedShots below follow them tick by
    // tick and hit what they reach, so the keys, the timing and the lifetime up there can be changed freely:
    // nothing down here names a tick. Both fly at what the crosshair is on, up and down too.

    /** How high above the caster's feet they appear. */
    private static final double SPEAR_HEIGHT = 2.2;
    private static final double SKULL_HEIGHT = 2.6;

    /** What the spear does to each enemy it goes through. */
    public static final float SPEAR_DAMAGE = 40f;

    /** What the skull's blast does to every enemy within SKULL_BLAST_RADIUS blocks of it. */
    public static final float SKULL_DAMAGE = 30f;
    public static final double SKULL_BLAST_RADIUS = 4.5;

    private static final AnimatedShot SPEAR_SHOT = AnimatedShot.of(SPEAR_HOVERS)
            .width(0.6)              // how close its path must pass to a body to hit it
            .tip(1.8)                // blocks from the middle of the shaft (its pivot) to its point, at scale 1.2
            .pierce();               // goes through every enemy on its way

    private static final AnimatedShot SKULL_SHOT = AnimatedShot.of(SKULL)
            .width(0.6);             // stops at the first enemy: that is where it bursts

    private static final ParticleFx PALE_SPARK = ParticleFx.of("fx/glow")
            .color(0xFFC8FFE4).endColor(0x0018A070)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(16, 10).gravity(-0.02f).friction(0.9f)
            .glow();

    public static void paleEmperorSpearSkull(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        if (!player.isShiftKeyDown()){

            // SPEAR: hurts every enemy it goes through and flies on. A block ends it.
            Vec3 from = Spells.clearStart(player, sl, player.position().add(0, SPEAR_HEIGHT, 0));

            SPEAR_SHOT
                    .onHit((target, at) -> {
                        Spells.strike(player, target, SPEAR_DAMAGE);
                        ParticleShapes.burst(sl, PALE_SPARK, at, 12, 0.05, 0.25);
                        Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.7f);
                    })
                    .onStop((at, hitBlock) -> {
                        ParticleShapes.burst(sl, PALE_SPARK, at, 16, 0.05, 0.3);
                        Spells.sound(sl, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7f, 0.6f);
                    })
                    .fire(player, sl, from);

        }else{

            // SKULL: bursts on the first enemy it reaches, or on a block. With nothing in its way it only fades.
            Vec3 from = Spells.clearStart(player, sl, player.position().add(0, SKULL_HEIGHT, 0));

            SKULL_SHOT
                    .onStop((at, hitBlock) -> skullBursts(player, sl, at))
                    .fire(player, sl, from);
        }
    }

    /** The skull is gone in a pale blast that hurts every enemy around it. */
    private static void skullBursts(Player player, ServerLevel sl, Vec3 at) {
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, SKULL_BLAST_RADIUS)) {
            Spells.strike(player, enemy, SKULL_DAMAGE);
        }

        ModelFx flash = SpellFx.FIRE_BLAST.color(0xFFB8FFD8).spin(25f).lifetime(8)
                .key(0, pose().scale(0.8f))
                .key(8, pose().scale((float) (SKULL_BLAST_RADIUS * 1.8)), ModelFx.Ease.OUT)   // ends about as wide as the blast
                .during(2, 8, pose().alpha(0f));

        ParticleShapes.model(sl, flash, at, sl.getRandom().nextFloat() * 360f, 0f, 0f);

        SpellFx.shockRing(sl, at.add(0, -0.3, 0), SKULL_BLAST_RADIUS * 1.1, 0xE09CFFD2, 8, 0);
        SpellFx.shockRing(sl, at.add(0, -0.3, 0), SKULL_BLAST_RADIUS * 0.7, 0xB0F5C211, 10, 2);

        ParticleShapes.burst(sl, PALE_SPARK, at, 40, 0.1, 0.5);
        ParticleShapes.burst(sl, SpellFx.FIRE_SMOKE, at, 12, 0.03, 0.12);

        Spells.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.3f);
        Spells.sound(sl, at, SoundEvents.WITHER_HURT, 0.8f, 0.6f);
    }

    // =====================================================================================
    // BONE SPIKES AND THE HANDS OF THE UNDERWORLD (ability 5)
    // =====================================================================================
    //
    // Normal cast: spikes break out under every enemy the caster can see. They hurt and blind it, and it
    //   remembers how often it was hit: every hit after the first hurts more.
    // Sneak cast: every enemy in sight pays for the hits it remembers, 15 damage for each. With 3 or more,
    //   hands come out of the ground around it, close on it and drag it under.

    private static final float SPIKES_COST = 2000;               // either cast
    private static final int SPIKES_STAGE = 3;

    /** Who it reaches: every enemy in sight this far away, this many degrees to either side of the crosshair. */
    public static final double SIGHT_RANGE = 24;
    public static final double SIGHT_ANGLE = 45;

    /** The first hit of the spikes, and how much more each hit it already remembers adds. */
    public static final float SPIKE_DAMAGE = 10f;
    public static final float SPIKE_DAMAGE_PER_HIT = 5f;

    /** How long the spikes blind, and how near a blinded mob must be to something to go after it. */
    public static final int BLIND_TICKS = 80;
    public static final double BLIND_MOB_SEES = 3.0;

    /** An enemy forgets its hits this long after the last one, and never remembers more than HITS_MAX. */
    public static final int HITS_FORGOTTEN_AFTER = 300;
    public static final int HITS_MAX = 10;

    /** Sneak cast: damage for each hit it remembers, and how many hits it takes for the hands to come. */
    public static final float HANDS_DAMAGE_PER_HIT = 15f;
    public static final int HANDS_NEED_HITS = 3;

    /** How long the hands keep it under the ground before it is given back. */
    public static final int BURIED_TICKS = 40;

    // saved with the enemy
    private static final String HITS = "pale_emperor_spike_hits";
    private static final String HITS_UNTIL = "pale_emperor_spike_hits_until";        // game time
    private static final String BLIND_UNTIL = "pale_emperor_blind_until";            // game time

    /** The feathers over an enemy's head, one for each hit it remembers. */
    private static final String HITS_TAG = "pale_emperor_hits";

    public static void paleEmperorSpikes(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < SPIKES_STAGE) return;

        List<LivingEntity> seen = Spells.enemiesInSight(player, sl, SIGHT_RANGE, SIGHT_ANGLE);

        // =====================================================================
        // NORMAL CAST - SPIKES UNDER EVERYONE IN SIGHT
        // =====================================================================

        if (!player.isShiftKeyDown()) {

            if (seen.isEmpty()) {
                player.sendSystemMessage(Component.literal("No one stands before you."));
                return;                                             // nothing spent
            }

            if (!Spells.payEssence(player, SPIKES_COST)) return;

            for (LivingEntity enemy : seen) {
                spikesUnder(player, sl, enemy);
            }

            // =====================================================================
            // SHIFT CAST - THEY PAY FOR THE HITS THEY REMEMBER
            // =====================================================================

        } else {

            List<LivingEntity> marked = new ArrayList<>();

            for (LivingEntity enemy : seen) {
                if (spikeHits(enemy) > 0) marked.add(enemy);
            }

            if (marked.isEmpty()) {
                player.sendSystemMessage(Component.literal("No one before you has felt your spikes."));
                return;                                             // nothing spent
            }

            if (!Spells.payEssence(player, SPIKES_COST)) return;

            for (LivingEntity enemy : marked) {
                handsTake(player, sl, enemy);
            }
        }
    }

    // =====================================================================================
    // THE HITS AN ENEMY REMEMBERS
    // =====================================================================================

    /** How many spike hits this enemy remembers right now (0 once it has forgotten them). */
    public static int spikeHits(LivingEntity enemy) {
        if (enemy.level().getGameTime() >= enemy.getPersistentData().getLong(HITS_UNTIL)) return 0;
        return enemy.getPersistentData().getInt(HITS);
    }

    private static void setSpikeHits(ServerLevel sl, LivingEntity enemy, int hits) {
        enemy.getPersistentData().putInt(HITS, hits);
        enemy.getPersistentData().putLong(HITS_UNTIL, sl.getGameTime() + HITS_FORGOTTEN_AFTER);
        showSpikeHits(sl, enemy, hits);
    }

    private static void forgetSpikeHits(ServerLevel sl, LivingEntity enemy) {
        enemy.getPersistentData().remove(HITS);
        enemy.getPersistentData().remove(HITS_UNTIL);
        ParticleShapes.clearModels(sl, enemy, HITS_TAG);
    }

    private static final ModelFx HIT_FEATHER = ModelFx.of("pale_emperor/pale_feather")
            .scale(0.3f).pivot(8, 8, 8).glow().spin(6f)
            .lifetime(HITS_FORGOTTEN_AFTER).fade(4, 10)                  // gone when the hits are forgotten
            .tag(HITS_TAG);

    /** A ring of small feathers over its head, one for each hit. They turn gold when the hands can come. */
    private static void showSpikeHits(ServerLevel sl, LivingEntity enemy, int hits) {
        ParticleShapes.clearModels(sl, enemy, HITS_TAG);             // the old ring

        ModelFx feather = hits >= HANDS_NEED_HITS ? HIT_FEATHER.color(0xFFFFC83C) : HIT_FEATHER;

        double radius = hits == 1 ? 0.0 : Math.max(0.3, enemy.getBbWidth() * 0.5);

        for (int i = 0; i < hits; i++) {

            double angle = Math.PI * 2 * i / hits;

            Vec3 offset = new Vec3(
                    Math.cos(angle) * radius,
                    enemy.getBbHeight() + 0.45,
                    Math.sin(angle) * radius
            );

            ParticleShapes.modelOn(sl, feather, enemy, offset, 0f, 0f, 0f);
        }
    }

    // =====================================================================================
    // SPIKES (normal cast)
    // =====================================================================================

    /** The tick the spikes are out (the hit lands), the tick they start to sink, and when they are gone. */
    private static final int SPIKES_OUT = 4;
    private static final int SPIKES_SINK = 30;
    private static final int SPIKES_TICKS = 44;

    /** The spikes at this size: they are 2.75 blocks tall at scale 1. */
    private static ModelFx boneSpikes(float scale) {
        float hidden = -2.9f * scale;                                // this far down they are under the ground

        return ModelFx.of("pale_emperor/bone_spikes")
                .scale(scale).pivot(8, -16, 8).glow().lifetime(SPIKES_TICKS).fade(0, 4)
                .key(0, pose().up(hidden))
                .key(SPIKES_OUT, pose().up(0), ModelFx.Ease.OUT_BACK)                 // burst out, a little too far, and settle
                .during(SPIKES_SINK, SPIKES_TICKS - 2, pose().up(hidden), ModelFx.Ease.IN);
    }

    private static void spikesUnder(Player player, ServerLevel sl, LivingEntity enemy) {
        // a little taller than what they hit
        float scale = (float) Math.max(0.5, Math.min(1.6, enemy.getBbHeight() / 2.75 * 1.2));

        ParticleShapes.model(
                sl,
                boneSpikes(scale),
                groundUnder(player, sl, enemy),
                sl.getRandom().nextFloat() * 360f,
                0f,
                0f
        );

        Later.run(
                sl,
                SPIKES_OUT,
                () -> spikesHit(player, sl, enemy)
        );
    }

    private static void spikesHit(Player player, ServerLevel sl, LivingEntity enemy) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (enemy.level() != sl || !Spells.isEnemy(player, enemy)) return;

        int hits = Math.min(HITS_MAX, spikeHits(enemy) + 1);

        Spells.strike(player, enemy, SPIKE_DAMAGE + SPIKE_DAMAGE_PER_HIT * (hits - 1));

        Vec3 middle = enemy.getBoundingBox().getCenter();

        ParticleShapes.burst(sl, PALE_SPARK, middle, 10, 0.05, 0.25);
        Spells.sound(sl, middle, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9f, 0.5f);

        if (!enemy.isAlive()) return;                                // that was the end of it

        setSpikeHits(sl, enemy, hits);
        blind(sl, enemy);
    }

    /**
     * Blindness. A player's screen goes dark by itself; a mob does not care about the effect, so it is also
     * made to lose what it was hunting, and until it can see again it only goes after what is right next to
     * it (PaleEmperorEvents below).
     */
    private static void blind(ServerLevel sl, LivingEntity enemy) {
        enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0));
        enemy.getPersistentData().putLong(BLIND_UNTIL, sl.getGameTime() + BLIND_TICKS);

        if (enemy instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    /** The ground an enemy stands on or hangs just above. One high in the air gets them at its feet. */
    private static Vec3 groundUnder(Player player, ServerLevel sl, LivingEntity enemy) {
        if (enemy.onGround()) return enemy.position();

        Vec3 ground = Spells.groundAt(player, sl, enemy.position());

        return enemy.getY() - ground.y <= 4.0 ? ground : enemy.position();
    }

    // =====================================================================================
    // HANDS (sneak cast)
    // =====================================================================================

    /**
     * The hands' timing. Their animation is built from these four numbers and so is everything that happens
     * to the enemy, so changing one moves both.
     */
    private static final int HANDS_OUT = 8;                          // they are out of the ground
    private static final int HANDS_GRASP = 16;                       // they have closed: the damage lands
    private static final int HANDS_PULL = 24;                        // they start to pull down
    private static final int HANDS_UNDER = 44;                       // they are gone, and so is what they held

    /** One arm at this size: it is 3 blocks tall at scale 1. */
    private static ModelFx draggingArm(float scale) {
        float hidden = -3.1f * scale;                                // this far down it is under the ground

        return ModelFx.of("pale_emperor/underworld_arm").frames(5).smooth()
                .scale(scale).pivot(8, -16, 8).glow().lifetime(HANDS_UNDER + 2).fade(0, 4)
                .key(0, pose().frame(3).up(hidden))                                   // half closed, under the ground
                .key(HANDS_OUT, pose().up(0), ModelFx.Ease.OUT)                       // rises
                .during(2, HANDS_OUT + 2, pose().frame(0), ModelFx.Ease.OUT)          // opens wide
                .during(HANDS_GRASP - 5, HANDS_GRASP, pose().frame(4), ModelFx.Ease.IN)       // closes on it
                .during(HANDS_PULL, HANDS_UNDER, pose().up(hidden), ModelFx.Ease.IN);         // and takes it down
    }

    /** One enemy pays for the hits it remembers. They are used up by this. */
    private static void handsTake(Player player, ServerLevel sl, LivingEntity enemy) {
        int hits = spikeHits(enemy);

        forgetSpikeHits(sl, enemy);

        float damage = HANDS_DAMAGE_PER_HIT * hits;

        // too few hits, or nothing under its feet for hands to come out of: only the damage
        if (hits < HANDS_NEED_HITS || !enemy.onGround()) {
            Vec3 middle = enemy.getBoundingBox().getCenter();

            Spells.strike(player, enemy, damage);

            ParticleShapes.burst(sl, PALE_SPARK, middle, 14, 0.05, 0.3);
            Spells.sound(sl, middle, SoundEvents.WITHER_HURT, 0.7f, 0.8f);
            return;
        }

        Vec3 ground = enemy.position();
        float yaw = player.getYRot();

        // arms about as tall as one and a half of what they take
        float scale = Math.max(0.8f, Math.min(2.0f, enemy.getBbHeight() * 0.55f));
        ModelFx arm = draggingArm(scale);

        // they lean in by 16 degrees: this far out, their hands close just outside its body
        double radius = enemy.getBbWidth() / 2.0 + 0.25 + 0.83 * scale;

        int arms = 5;

        for (int i = 0; i < arms; i++) {

            float around = yaw + i * 360f / arms;
            double angle = Math.toRadians(around);

            Vec3 at = ground.add(
                    -Math.sin(angle) * radius,
                    0,
                    Math.cos(angle) * radius
            );

            // around + 180 = its palm looks back at the middle; pitch 16 = it leans that way
            ParticleShapes.model(sl, i % 2 == 0 ? arm : arm.mirrored(), at, around + 180f, 16f, 0f);
        }

        seal(sl, ground, yaw, (float) (radius * 2.0 + 1.5), HANDS_UNDER);

        boolean buried = canBeBuried(enemy);

        // held for as long as the hands have it, and while it is under the ground
        Root.apply(enemy, buried ? HANDS_UNDER + BURIED_TICKS : HANDS_UNDER, RootRestriction.EVERYTHING);

        Spells.sound(sl, ground, SoundEvents.WITHER_AMBIENT, 0.9f, 0.5f);

        Later.run(
                sl,
                HANDS_GRASP,
                () -> handsClose(player, sl, enemy, damage)
        );

        if (!buried) return;

        Later.run(
                sl,
                HANDS_PULL,
                () -> {
                    // where it stands when the pulling starts is where it comes back up
                    if (Spells.casterStillHere(player, sl)) dragUnder(sl, enemy, arm, enemy.position(), HANDS_PULL);
                }
        );
    }

    private static void handsClose(Player player, ServerLevel sl, LivingEntity enemy, float damage) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (enemy.level() != sl || !enemy.isAlive()) return;

        Vec3 middle = enemy.getBoundingBox().getCenter();

        Spells.strike(player, enemy, damage);

        ParticleShapes.burst(sl, PALE_SPARK, middle, 24, 0.05, 0.35);
        Spells.sound(sl, middle, SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.5f);
    }

    /** Too big for the hands to pull under, or a player nothing can hurt: those only take the damage. */
    private static boolean canBeBuried(LivingEntity enemy) {
        if (enemy.getBbHeight() > 4.0f || enemy.getBbWidth() > 3.0f) return false;

        return !(enemy instanceof Player other && other.getAbilities().invulnerable);
    }

    /**
     * One tick of being dragged under. The enemy goes down exactly as far as the arms have gone (asked from
     * their own animation), stays under for BURIED_TICKS, where the ground chokes it like anything that is
     * buried, and is then put back where it stood. It calls itself again a tick later until that is done.
     */
    private static void dragUnder(ServerLevel sl, LivingEntity enemy, ModelFx arm, Vec3 surface, int tick) {
        if (enemy.level() != sl || !enemy.isAlive() || enemy.isRemoved()) return;

        if (tick >= HANDS_UNDER + BURIED_TICKS) {
            putAt(enemy, surface);                                   // the ground gives it back

            ParticleShapes.burst(sl, PALE_SPARK, surface.add(0, 0.2, 0), 16, 0.05, 0.3);
            Spells.sound(sl, surface, SoundEvents.WITHER_HURT, 0.6f, 0.5f);
            return;
        }

        double deepest = enemy.getBbHeight() + 0.2;                  // all of it under the ground
        double pulled = tick >= HANDS_UNDER ? deepest : -arm.poseAt(tick).up();

        putAt(enemy, surface.add(0, -Math.max(0.0, Math.min(deepest, pulled)), 0));

        Later.run(
                sl,
                1,
                () -> dragUnder(sl, enemy, arm, surface, tick + 1)
        );
    }

    private static void putAt(LivingEntity entity, Vec3 spot) {
        entity.teleportTo(spot.x, spot.y, spot.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
    }

    // =====================================================================================
    // PALE WINGS AND THE STEP (ability 6)
    // =====================================================================================
    //
    // Normal cast: wings grow from the caster's back and for FLIGHT_TICKS they can fly the way a creative
    //   player does. Cast again to fold them early. However the flight ends, the fall after it does no harm.
    // Sneak cast: the caster is at once where they look.

    private static final float WINGS_COST = 4000;
    private static final float STEP_COST = 1500;
    private static final int WINGS_STAGE = 4;

    /** How long the wings carry: 20 seconds. */
    public static final int FLIGHT_TICKS = 400;

    /** The caster is told this many ticks before the wings fold. */
    private static final int FLIGHT_WARNING = 60;

    /** How far the step reaches. */
    public static final double STEP_RANGE = 32;

    /** Ticks of flight left. Saved with the player, so a flight goes on after logging out and in. */
    private static final String FLIGHT_LEFT = "pale_emperor_flight";

    private static final String WINGS_TAG = "pale_emperor_wings";

    public static void paleEmperorWings(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < WINGS_STAGE) return;

        if (player.isShiftKeyDown()) {
            stepThere(player, sl);
            return;
        }

        if (hasWings(player)) {                                      // cast again: the wings fold, nothing spent
            endFlight(player, sl, true);
            return;
        }

        if (!Spells.payEssence(player, WINGS_COST)) return;

        startFlight(player, sl);
    }

    // =====================================================================================
    // WINGS (normal cast)
    // =====================================================================================

    /** True while the wings of this ability carry the player. */
    public static boolean hasWings(Player player) {
        return player.getPersistentData().getInt(FLIGHT_LEFT) > 0;
    }

    /** Creative and spectator players fly anyway: they get the wings, and their flying is left alone. */
    private static boolean fliesByWings(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    private static void startFlight(Player player, ServerLevel sl) {
        player.getPersistentData().putInt(FLIGHT_LEFT, FLIGHT_TICKS);

        if (fliesByWings(player)) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();

            // off the ground at once: a player who is told to fly while standing lands again right away
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.5), motion.z);
            player.hurtMarked = true;
        }

        showWings(sl, player, FLIGHT_TICKS);

        PaleEmperorFx.featherFall(sl, player.position(), 1.5, 10);
        Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.8f);
    }

    /** foldNow = it ends before its time (cast again): the wings have to be folded by hand. */
    private static void endFlight(Player player, ServerLevel sl, boolean foldNow) {
        player.getPersistentData().remove(FLIGHT_LEFT);

        if (fliesByWings(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();

            FallGuard.protect(player, 400);                          // the way down does no harm
        }

        if (foldNow) {
            Vec3 shoulders = shouldersOf(player);

            ParticleShapes.clearModels(sl, player, WINGS_TAG);
            ParticleShapes.modelOnTurning(sl, foldingWing(1), player, shoulders, 0f, 0f, 0f);
            ParticleShapes.modelOnTurning(sl, foldingWing(-1), player, shoulders, 0f, 0f, 0f);
        }

        Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 0.6f);
    }

    // ---------- the wings themselves ----------

    /** The first beat starts here, a beat takes this long, and folding away takes this long at the end. */
    private static final int WINGS_SPREAD = 20;
    private static final int WING_BEAT = 20;
    private static final int WINGS_FOLD = 24;

    private static Vec3 shouldersOf(Player player) {
        return new Vec3(0, player.getBbHeight() * 0.75, 0);
    }

    /**
     * The wing model, pivot at the shoulder. In the keys of the two below, side is 1 for the right wing and -1
     * for the left: right(0.16 * side).forward(-0.28) puts it on that side of the back.
     */
    private static ModelFx wingModel() {
        return ModelFx.of("pale_emperor/pale_wing").frames(5).smooth()
                .scale(1.4f).pivot(-10, 11.5f, 8).glow()
                .tag(WINGS_TAG);
    }

    /**
     * A wing for a flight of 'ticks' ticks: it unfolds, beats for as long as the flight lasts and folds away
     * in its last second. Played with modelOnTurning, so it stays on the back whichever way the player turns.
     */
    private static ModelFx flightWing(int side, int ticks) {
        int foldAt = ticks - WINGS_FOLD;

        // a model holds 64 keys at most: a very long flight gets slower beats instead of more of them
        int beat = Math.max(WING_BEAT, (foldAt - WINGS_SPREAD) / 28 + 1);

        ModelFx wing = wingModel().lifetime(ticks).fade(2, 6)
                .key(0, pose().right(0.16f * side).forward(-0.28f).scale(0.5f))
                .during(0, 8, pose().scale(1f), ModelFx.Ease.OUT_BACK)
                .during(3, 15, pose().frame(4), ModelFx.Ease.OUT);                           // unfolds

        for (int t = WINGS_SPREAD; t + beat <= foldAt; t += beat) {
            int down = t + Math.round(beat * 0.3f);
            int up = t + Math.round(beat * 0.9f);

            wing = wing
                    .during(t, down, pose().roll(24f * side).frame(3), ModelFx.Ease.IN_OUT)          // down
                    .during(down, up, pose().roll(-10f * side).frame(4), ModelFx.Ease.IN_OUT);       // and up
        }

        wing = wing
                .during(foldAt, foldAt + 8, pose().roll(0), ModelFx.Ease.IN_OUT)
                .during(foldAt + 4, foldAt + 18, pose().frame(0), ModelFx.Ease.IN);          // folds away

        return side > 0 ? wing : wing.mirrored();
    }

    /** A spread wing that folds away at once: for a flight that is ended early. */
    private static ModelFx foldingWing(int side) {
        ModelFx wing = wingModel().lifetime(20).fade(0, 6)
                .key(0, pose().right(0.16f * side).forward(-0.28f).frame(4))
                .during(0, 14, pose().frame(0), ModelFx.Ease.IN);

        return side > 0 ? wing : wing.mirrored();
    }

    /** Puts the pair of wings on the player for 'ticks' more ticks (and takes off any they still wear). */
    private static void showWings(ServerLevel sl, Player player, int ticks) {
        ParticleShapes.clearModels(sl, player, WINGS_TAG);

        if (ticks < WINGS_SPREAD + WING_BEAT + WINGS_FOLD) return;   // too little left to unfold them for

        Vec3 shoulders = shouldersOf(player);

        ParticleShapes.modelOnTurning(sl, flightWing(1, ticks), player, shoulders, 0f, 0f, 0f);
        ParticleShapes.modelOnTurning(sl, flightWing(-1, ticks), player, shoulders, 0f, 0f, 0f);
    }

    /** One tick of a flight: counts it down and ends it, and keeps the player able to fly until then. */
    private static void flightTick(Player player, ServerLevel sl) {
        int left = player.getPersistentData().getInt(FLIGHT_LEFT);

        if (left <= 0) return;

        left--;

        if (left <= 0) {
            endFlight(player, sl, false);                            // (the wings fold by themselves at the end of their time)
            return;
        }

        player.getPersistentData().putInt(FLIGHT_LEFT, left);

        // a change of game mode takes flying away: give it back for as long as the wings last
        if (fliesByWings(player) && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }

        if (left == FLIGHT_WARNING) {
            player.sendSystemMessage(Component.literal("Your wings grow heavy."));
        }

        if (player.getAbilities().flying && !player.isSpectator()) {
            if (left % WING_BEAT == 0) {
                Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.35f, 1.2f);
            }
            if (left % 10 == 0) {
                // one feather, starting about where the player is and drifting down behind them
                PaleEmperorFx.featherFall(sl, player.position().add(0, -3.5, 0), 0.5, 1);
            }
        }
    }

    // =====================================================================================
    // THE STEP (sneak cast)
    // =====================================================================================

    private static final SlashFx STEP_STREAK = SlashFx.line("slash/smooth")
            .color(0xA09CFFD2)
            .core(0xC0F4FFE8)
            .width(0.25f)
            .taper(SlashFx.Taper.COMET);

    private static void stepThere(Player player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("You are held in place."));
            return;
        }

        Vec3 there = stepSpot(player, sl);

        if (there == null) {
            player.sendSystemMessage(Component.literal("There is no room for you there."));
            return;                                                  // nothing spent
        }

        if (!Spells.payEssence(player, STEP_COST)) return;

        Vec3 left = player.position();

        if (player.isPassenger()) player.stopRiding();

        player.teleportTo(there.x, there.y, there.z);
        player.fallDistance = 0;

        FallGuard.protect(player, 200);                              // it may end in the air: the way down does no harm

        for (Vec3 end : new Vec3[]{left, there}) {
            ParticleShapes.burst(sl, PALE_SPARK, end.add(0, 1.0, 0), 18, 0.05, 0.3);
            PaleEmperorFx.featherFall(sl, end, 0.9, 6);
            Spells.sound(sl, end, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 0.6f);
        }

        ParticleShapes.slashBetween(sl, STEP_STREAK.lifetime(8).sweep(3), left.add(0, 1.0, 0), there.add(0, 1.0, 0));
    }

    /**
     * Where the player's feet go: at what the crosshair is on, as near to it as they fit. Looking at the
     * ground they stand on that spot, looking at a wall they stand in front of it (or hang in the air in
     * front of it, if it is a cliff), looking at the open sky they end up STEP_RANGE blocks out.
     * Null when there is no room anywhere near.
     */
    private static Vec3 stepSpot(Player player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 point = Spells.aimPoint(player, sl, STEP_RANGE);

        double far = eye.distanceTo(point);

        // a little back from what was hit, then further back until there is room
        for (double back : new double[]{0.4, 0.8, 1.2, 1.8, 2.6, 3.6}) {

            if (back >= far) break;

            Vec3 near = point.subtract(look.scale(back));
            Vec3 ground = Spells.groundAt(player, sl, near);

            // on the ground under it if that is close; else with the spot at the chest, the head or the feet
            if (near.y - ground.y <= 3.0 && Spells.fits(player, sl, ground)) return ground;

            for (double drop : new double[]{1.0, 1.85, 0.0}) {
                Vec3 feet = near.add(0, -drop, 0);
                if (Spells.fits(player, sl, feet)) return feet;
            }
        }

        return null;
    }

    // =====================================================================================
    // WRAITHS AND THE UNDERWORLD (ability 7)
    // =====================================================================================
    //
    // Normal cast: wraiths rise around the caster, three for every enemy within WRAITH_RADIUS, fly at the
    //   enemies and slam into them. Each slam hurts and lays a curse: the cursed wither and take more damage.
    // Sneak cast: the caster steps into the underworld. Nothing sees or hurts them there. They come back
    //   when they deal damage, cast it again, or run out of soul essence.

    private static final float WRAITHS_COST = 5000;
    private static final float UNDERWORLD_COST = 3000;
    private static final int WRAITHS_STAGE = 5;

    /** Enemies this near are counted, each of them gets this many wraiths. */
    public static final double WRAITH_RADIUS = 12;
    public static final int WRAITHS_PER_ENEMY = 3;

    /** Only the nearest this many enemies are counted, so a crowd does not bring a hundred wraiths. */
    public static final int WRAITH_MAX_ENEMIES = 10;

    /** What one wraith does when it slams into its enemy. */
    public static final float WRAITH_DAMAGE = 12f;

    /** The curse: how long it lasts, how many can lie on one enemy, and how much more damage each makes it take. */
    public static final int CURSE_TICKS = 200;
    public static final int CURSE_MAX = 6;
    public static final float CURSE_DAMAGE_PER_STACK = 0.10f;

    /** Soul essence the underworld takes each second. With too little left the caster is thrown back. 0 = it is free. */
    public static final float UNDERWORLD_DRAIN = 100f;

    // saved with the enemy / the player
    private static final String CURSES = "pale_emperor_curses";
    private static final String CURSES_UNTIL = "pale_emperor_curses_until";          // game time
    private static final String IN_UNDERWORLD = "pale_emperor_underworld";

    public static void paleEmperorWraiths(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < WRAITHS_STAGE) return;

        // =====================================================================
        // SHIFT CAST - INTO THE UNDERWORLD, OR BACK OUT OF IT
        // =====================================================================

        if (player.isShiftKeyDown()) {

            if (inUnderworld(player)) {
                leaveUnderworld(player, sl);                        // nothing spent
                return;
            }

            if (!Spells.payEssence(player, UNDERWORLD_COST)) return;

            enterUnderworld(player, sl);
            return;
        }

        // =====================================================================
        // NORMAL CAST - WRAITHS
        // =====================================================================

        Vec3 middle = player.getBoundingBox().getCenter();

        List<LivingEntity> enemies = Spells.enemiesAround(player, sl, middle, WRAITH_RADIUS);

        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no one near for the dead to take."));
            return;                                                 // nothing spent
        }

        if (!Spells.payEssence(player, WRAITHS_COST)) return;

        enemies.sort(Comparator.comparingDouble(
                (LivingEntity enemy) -> enemy.getBoundingBox().getCenter().distanceToSqr(middle)
        ));

        if (enemies.size() > WRAITH_MAX_ENEMIES) {
            enemies = new ArrayList<>(enemies.subList(0, WRAITH_MAX_ENEMIES));
        }

        summonWraiths(player, sl, enemies);
    }

    // =====================================================================================
    // WRAITHS (normal cast)
    // =====================================================================================

    /** How long a wraith takes to rise, and how fast it flies at its enemy after that (blocks per tick). */
    private static final int WRAITH_RISES = 12;
    private static final double WRAITH_SPEED = 0.9;

    /** All the wraiths of one cast rise within this many ticks, one after another. */
    private static final int WRAITHS_SPREAD = 20;

    /** The wraith: 3 blocks tall, its pivot under it, its face its front. Frame 0 = arms hanging, 2 = reaching out. */
    private static ModelFx wraithModel() {
        return ModelFx.of("pale_emperor/wraith").frames(3).smooth()
                .scale(1.1f).pivot(8, -16, 8).glow();
    }

    /** It rises out of the ground as it appears. It fades out at the end, while the flying one fades in. */
    private static final ModelFx WRAITH_RISING = wraithModel()
            .lifetime(WRAITH_RISES + 4).fade(0, 4)
            .key(0, pose().up(-0.5f).alpha(0f))
            .key(WRAITH_RISES, pose().up(0.35f).alpha(1f), ModelFx.Ease.OUT)
            .during(WRAITH_RISES / 2, WRAITH_RISES, pose().frame(1), ModelFx.Ease.OUT);       // its arms begin to lift

    /** Three wraiths for each enemy, in a ring around the caster, rising one after another. */
    private static void summonWraiths(Player player, ServerLevel sl, List<LivingEntity> enemies) {
        int total = enemies.size() * WRAITHS_PER_ENEMY;

        Vec3 feet = player.position();
        float yaw = player.getYRot();

        for (int i = 0; i < total; i++) {

            // going round the enemies again and again: each gets its first wraith before any gets its second
            LivingEntity enemy = enemies.get(i % enemies.size());

            double angle = Math.toRadians(yaw) + Math.PI * 2 * i / total;
            double radius = 2.2 + (i % 3) * 0.8;

            Vec3 spot = feet.add(
                    -Math.sin(angle) * radius,
                    0,
                    Math.cos(angle) * radius
            );

            // on the ground there. A caster high in the air gets them at the height of their feet
            Vec3 ground = Spells.groundAt(player, sl, spot);
            Vec3 rises = Math.abs(ground.y - feet.y) <= 3.0 ? ground : spot;

            Later.run(
                    sl,
                    i * WRAITHS_SPREAD / total,
                    () -> wraithRises(player, sl, rises, enemy)
            );
        }

        seal(sl, feet, yaw, 9f, WRAITHS_SPREAD + WRAITH_RISES + 20);

        Spells.sound(sl, feet, SoundEvents.WITHER_SPAWN, 0.5f, 1.6f);
    }

    private static void wraithRises(Player player, ServerLevel sl, Vec3 ground, LivingEntity enemy) {
        if (!Spells.casterStillHere(player, sl)) return;

        Vec3 toEnemy = enemy.position().subtract(ground);
        // it faces its enemy (one standing right on the spot has no direction: the way the caster looks, then)
        float yaw = toEnemy.x * toEnemy.x + toEnemy.z * toEnemy.z < 0.01 ? player.getYRot() : Spells.yawOf(toEnemy);

        ParticleShapes.model(sl, WRAITH_RISING, ground, yaw, 0f, 0f);

        // where it hangs when it has risen: that is where it flies from
        Vec3 hangs = ground.add(0, 0.35, 0);

        Later.run(
                sl,
                WRAITH_RISES,
                () -> wraithFlies(player, sl, hangs, enemy)
        );
    }

    /**
     * The wraith goes for its enemy. From here on its model is stuck to the enemy and only its keys bring it
     * in from where it rose, so it cannot miss: if the enemy runs, the wraith's whole way moves with it.
     */
    private static void wraithFlies(Player player, ServerLevel sl, Vec3 from, LivingEntity wanted) {
        if (!Spells.casterStillHere(player, sl)) return;

        // its enemy may be dead by now (another wraith was faster): it takes the nearest one that is left
        LivingEntity enemy = wanted;

        if (!isPrey(player, sl, enemy)) {
            enemy = null;

            double nearest = Double.MAX_VALUE;

            for (LivingEntity other : Spells.enemiesAround(player, sl, from, WRAITH_RADIUS)) {
                double distance = other.position().distanceToSqr(from);
                if (distance < nearest) {
                    nearest = distance;
                    enemy = other;
                }
            }

            if (enemy == null) return;                              // no one left: it just fades
        }

        Vec3 toEnemy = enemy.position().subtract(from);

        float away = (float) Math.sqrt(toEnemy.x * toEnemy.x + toEnemy.z * toEnemy.z);
        float height = (float) -toEnemy.y;                           // how much higher than the enemy it starts
        float yaw = away < 0.1f ? player.getYRot() : Spells.yawOf(toEnemy);

        int flight = (int) Math.max(6, Math.min(18, Math.round(toEnemy.length() / WRAITH_SPEED)));

        ModelFx flying = wraithModel()
                .lifetime(flight + 6).fade(3, 6)
                .key(0, pose().forward(-away).up(height).frame(1))                    // where it rose, seen from the enemy
                .key(flight, pose().forward(-0.2f).up(0f), ModelFx.Ease.IN)           // into it, faster and faster
                .key(flight + 6, pose().forward(1.0f))                                // and on through it as it fades
                .during(0, Math.max(2, flight / 2), pose().frame(2), ModelFx.Ease.OUT);       // arms out

        ParticleShapes.modelOn(sl, flying, enemy, Vec3.ZERO, yaw, 0f, 0f);

        Spells.sound(sl, from, SoundEvents.WITHER_SHOOT, 0.25f, 1.6f);

        LivingEntity prey = enemy;

        Later.run(
                sl,
                flight,
                () -> wraithSlams(player, sl, prey)
        );
    }

    private static void wraithSlams(Player player, ServerLevel sl, LivingEntity enemy) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (!isPrey(player, sl, enemy)) return;

        Vec3 middle = enemy.getBoundingBox().getCenter();

        Spells.strike(player, enemy, WRAITH_DAMAGE);

        ParticleShapes.burst(sl, PALE_SPARK, middle, 12, 0.05, 0.3);
        ParticleShapes.burst(sl, SpellFx.FIRE_SMOKE, middle, 4, 0.02, 0.08);
        Spells.sound(sl, middle, SoundEvents.WITHER_HURT, 0.5f, 1.4f);

        if (enemy.isAlive()) curse(sl, enemy);
    }

    private static boolean isPrey(Player player, ServerLevel sl, LivingEntity enemy) {
        return enemy.level() == sl && !enemy.isRemoved() && Spells.isEnemy(player, enemy);
    }

    // ---------- the curse ----------

    /** How many curses lie on this entity right now (0 once they have worn off). */
    public static int curses(LivingEntity entity) {
        if (entity.level().getGameTime() >= entity.getPersistentData().getLong(CURSES_UNTIL)) return 0;
        return entity.getPersistentData().getInt(CURSES);
    }

    /** One more curse, and all of them last their full time again. */
    private static void curse(ServerLevel sl, LivingEntity enemy) {
        enemy.getPersistentData().putInt(CURSES, Math.min(CURSE_MAX, curses(enemy) + 1));
        enemy.getPersistentData().putLong(CURSES_UNTIL, sl.getGameTime() + CURSE_TICKS);

        // it withers for as long as it is cursed (the extra damage it takes is added in PaleEmperorEvents)
        enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, CURSE_TICKS, 0));
    }

    // =====================================================================================
    // THE UNDERWORLD (sneak cast)
    // =====================================================================================

    /** True while the player is in the underworld of this ability. */
    public static boolean inUnderworld(Player player) {
        return player.getPersistentData().getBoolean(IN_UNDERWORLD);
    }

    private static void enterUnderworld(Player player, ServerLevel sl) {
        Vec3 feet = player.position();

        seal(sl, feet, player.getYRot(), 4f, 30);
        SpellFx.closingRing(sl, feet, 3.0, 0xE09CFFD2, 10, 0);
        Spells.sound(sl, feet, SoundEvents.WITHER_AMBIENT, 0.8f, 0.6f);

        player.getPersistentData().putBoolean(IN_UNDERWORLD, true);

        // unseen, untargeted, unhurt. It is kept up in underworldTick for as long as the player stays
        Concealment.hide(player, 100);

        player.sendSystemMessage(Component.literal("You step into the underworld. Deal damage or cast again to return."));
    }

    private static void leaveUnderworld(Player player, ServerLevel sl) {
        player.getPersistentData().remove(IN_UNDERWORLD);

        Concealment.reveal(player);                                  // (does nothing if they are back already)

        Vec3 feet = player.position();

        seal(sl, feet, player.getYRot(), 4f, 30);
        SpellFx.shockRing(sl, feet, 3.0, 0xE09CFFD2, 10, 0);
        Spells.sound(sl, feet, SoundEvents.WITHER_AMBIENT, 0.8f, 1.2f);

        player.sendSystemMessage(Component.literal("You return from the underworld."));
    }

    /** One tick in the underworld: keeps the player hidden, takes its price, and notices when they are out of it. */
    private static void underworldTick(Player player, ServerLevel sl) {
        if (!inUnderworld(player)) return;

        // something else brought them back (a normal attack does): tidy up
        if (!Concealment.isHidden(player)) {
            leaveUnderworld(player, sl);
            return;
        }

        if (Concealment.ticksLeft(player) < 40) Concealment.hide(player, 100);

        if (UNDERWORLD_DRAIN > 0 && player.tickCount % 20 == 0 && !player.isCreative()) {
            if (SoulCore.getSoulEssence(player) < UNDERWORLD_DRAIN) {
                player.sendSystemMessage(Component.literal("Your soul essence is spent."));
                leaveUnderworld(player, sl);
                return;
            }
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - UNDERWORLD_DRAIN);
        }
    }

    // =====================================================================================
    // EVENTS
    // =====================================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class PaleEmperorEvents {

        /** A mob blinded by the spikes only goes after what is right next to it. */
        @SubscribeEvent
        public static void onPaleEmperorBlindTarget(LivingChangeTargetEvent event) {
            LivingEntity mob = event.getEntity();
            LivingEntity target = event.getNewTarget();

            if (target == null || mob.level().isClientSide()) return;

            if (mob.level().getGameTime() >= mob.getPersistentData().getLong(BLIND_UNTIL)) return;

            if (mob.distanceToSqr(target) > BLIND_MOB_SEES * BLIND_MOB_SEES) event.setCanceled(true);
        }

        /**
         * The flight of ability 6 is counted down here and the underworld of ability 7 is kept up
         * (both run on even if the aspect is taken away meanwhile).
         */
        @SubscribeEvent
        public static void onPaleEmperorPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Player player = event.player;

            if (!(player.level() instanceof ServerLevel sl)) return;

            flightTick(player, sl);
            underworldTick(player, sl);
        }

        /** The cursed take more damage from everything, and whoever deals damage is back from the underworld. */
        @SubscribeEvent
        public static void onPaleEmperorHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();

            if (!(victim.level() instanceof ServerLevel sl)) return;

            int curses = curses(victim);

            if (curses > 0) {
                event.setAmount(event.getAmount() * (1f + CURSE_DAMAGE_PER_STACK * curses));
            }

            if (event.getSource().getEntity() instanceof Player attacker
                    && attacker != victim
                    && event.getAmount() > 0
                    && inUnderworld(attacker)) {
                leaveUnderworld(attacker, sl);
            }
        }

        // Models are forgotten by a player's game when they log in or change dimension, so the wings of a
        // flight that is still going are put on again. A second later: by then their game knows where they are.

        @SubscribeEvent
        public static void onPaleEmperorLogin(PlayerEvent.PlayerLoggedInEvent event) {
            wingsAgain(event.getEntity());
        }

        @SubscribeEvent
        public static void onPaleEmperorChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            wingsAgain(event.getEntity());
        }

        private static void wingsAgain(Player player) {
            if (!hasWings(player) || !(player.level() instanceof ServerLevel sl)) return;

            Later.run(sl, 20, () -> {
                if (player.isAlive() && !player.isRemoved() && player.level() == sl) {
                    showWings(sl, player, player.getPersistentData().getInt(FLIGHT_LEFT));
                }
            });
        }
    }

}
