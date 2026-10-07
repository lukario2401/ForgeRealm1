package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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

}
