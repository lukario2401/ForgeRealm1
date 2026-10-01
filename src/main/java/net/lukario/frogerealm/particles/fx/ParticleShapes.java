package net.lukario.frogerealm.particles.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Spawn particles in shapes. Works with ParticleFx AND vanilla particles (ParticleTypes.FLAME, ...),
 * and from server code (sent to nearby players) or client code (spawned locally).
 *
 *   ParticleShapes.cone(sl, GOLD_SPARK, player.getEyePosition(), player.getLookAngle(), 60, 100, 0.4, 0.8);
 *   ParticleShapes.burst(sl, GOLD_SPARK, target.position(), 40, 0.1, 0.3);
 *   ParticleShapes.line(sl, GOLD_SPARK, from, to, 4);
 *   ParticleShapes.ring(sl, GOLD_SPARK, center, 3.0, 40, 0.0);
 *   ParticleShapes.sphere(sl, GOLD_SPARK, center, 2.0, 80);
 *   ParticleShapes.spiral(sl, GOLD_SPARK, feet, 4, 150, 0.5, 4.0, 0.8, 0.1);          // swirling galaxy
 *   ParticleShapes.slash(sl, SWING, target.getBoundingBox().getCenter(), player);   // SlashFx trails
 *   ParticleShapes.slashBetween(sl, CUT, from, to);                                 // straight cut from A to B
 *   ParticleShapes.alongSlash(sl, SHARD, SWING, center, yaw, 0, 0, 6, 20, 0.08, 0.03); // shards breaking off a slash
 *
 * For effects in stages (this now, that 5 ticks later) use combat.Later.run(serverLevel, 5, () -> ...).
 *
 * Speeds are in blocks per tick. With friction 1 and no gravity a particle travels
 * speed * lifetime blocks, so for a 10 block cone with a 20 tick particle use speed 0.5.
 */
public final class ParticleShapes {

    private ParticleShapes() {}

    /** One particle at an exact position with an exact velocity. Everything else is built on this. */
    public static void spawn(Level level, ParticleOptions particle, Vec3 position, Vec3 velocity) {
        if (level instanceof ServerLevel serverLevel) {
            // count 0 = vanilla's "use the offset as the exact velocity" mode
            serverLevel.sendParticles(particle, position.x, position.y, position.z, 0,
                    velocity.x, velocity.y, velocity.z, 1.0);
        } else {
            level.addParticle(particle, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    /** Standing still at a position. */
    public static void spawn(Level level, ParticleOptions particle, Vec3 position) {
        spawn(level, particle, position, Vec3.ZERO);
    }

    /**
     * Sprays particles from origin into a cone around direction.
     * @param angleDegrees full opening angle of the cone (90 = 45 degrees to each side)
     * @param minSpeed/maxSpeed each particle gets a random speed in this range (different speeds fill the cone)
     */
    public static void cone(Level level, ParticleOptions particle, Vec3 origin, Vec3 direction,
                            double angleDegrees, int count, double minSpeed, double maxSpeed) {
        RandomSource random = level.getRandom();
        Vec3 forward = direction.normalize();
        for (int i = 0; i < count; i++) {
            Vec3 dir = randomDirectionInCone(random, forward, angleDegrees);
            double speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
            spawn(level, particle, origin, dir.scale(speed));
        }
    }

    /** Explodes outward in every direction. */
    public static void burst(Level level, ParticleOptions particle, Vec3 center, int count,
                             double minSpeed, double maxSpeed) {
        cone(level, particle, center, new Vec3(0, 1, 0), 360, count, minSpeed, maxSpeed);
    }

    /** Still particles along a straight line. perBlock = how many per block of length. */
    public static void line(Level level, ParticleOptions particle, Vec3 from, Vec3 to, double perBlock) {
        Vec3 delta = to.subtract(from);
        int count = Math.max(1, (int) Math.ceil(delta.length() * perBlock));
        for (int i = 0; i <= count; i++) {
            spawn(level, particle, from.add(delta.scale((double) i / count)));
        }
    }

    /**
     * Flat horizontal circle. outwardSpeed > 0 makes it expand like a shockwave,
     * negative pulls inward, 0 stays still.
     */
    public static void ring(Level level, ParticleOptions particle, Vec3 center, double radius, int count,
                            double outwardSpeed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            spawn(level, particle, center.add(out.scale(radius)), out.scale(outwardSpeed));
        }
    }

    /**
     * Flat spiral "galaxy" of particles around center (lying horizontally).
     * @param arms        number of spiral arms
     * @param count       particles in total
     * @param turns       how far each arm winds around from the inside to the outside (1 = a full circle)
     * @param swirlSpeed  blocks per tick at the outer edge, sideways around the center (0 = still,
     *                    negative = the other way). Particles move in straight lines, so it swirls outward.
     */
    public static void spiral(Level level, ParticleOptions particle, Vec3 center, int arms, int count,
                              double innerRadius, double outerRadius, double turns, double swirlSpeed) {
        RandomSource random = level.getRandom();
        arms = Math.max(1, arms);
        double phase = random.nextDouble() * Math.PI * 2; // every spiral starts at a different angle
        for (int i = 0; i < count; i++) {
            int arm = i % arms;
            double along = random.nextDouble(); // 0 = inside, 1 = outside
            double radius = innerRadius + (outerRadius - innerRadius) * along + (random.nextDouble() - 0.5) * 0.3;
            double angle = phase + Math.PI * 2 * ((double) arm / arms + turns * along) + (random.nextDouble() - 0.5) * 0.2;
            Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 sideways = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
            double speed = swirlSpeed * radius / Math.max(outerRadius, 0.001);
            spawn(level, particle, center.add(out.scale(radius)), sideways.scale(speed));
        }
    }

    /** Still particles spread over the surface of a sphere. */
    public static void sphere(Level level, ParticleOptions particle, Vec3 center, double radius, int count) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 dir = randomDirectionInCone(random, new Vec3(0, 1, 0), 360);
            spawn(level, particle, center.add(dir.scale(radius)));
        }
    }

    /**
     * Plays a SlashFx at center. yaw/pitch/roll (degrees) are added to the slash's own rotation,
     * so a preset like .rotation(0, 0, -40) keeps its tilt whichever way it's played.
     */
    public static void slash(Level level, SlashFx slash, Vec3 center, float yaw, float pitch, float roll) {
        spawn(level, slash.rotated(yaw, pitch, roll), center);
    }

    /** Plays a SlashFx at center, turned the way the entity is facing (left/right only). */
    public static void slash(Level level, SlashFx slash, Vec3 center, Entity facing) {
        slash(level, slash, center, facing.getYRot(), 0f, 0f);
    }

    /**
     * A straight SlashFx.line(...) from one point to another: it draws itself from 'from' toward 'to'.
     * Sets the length and direction for you (the preset's own radius and rotation are ignored).
     */
    public static void slashBetween(Level level, SlashFx line, Vec3 from, Vec3 to) {
        slashBetween(level, line, from, to, Vec3.ZERO);
    }

    /**
     * Same, and 'across' says which way the width lies. Only matters for .flat() lines, e.g. a sideways
     * vector makes a flat line lie on the ground like a crack. Vec3.ZERO = don't care.
     */
    public static void slashBetween(Level level, SlashFx line, Vec3 from, Vec3 to, Vec3 across) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) return;
        Vec3 dir = delta.scale(1.0 / length);

        // the slash's 'forward' must be perpendicular to the line; its 'right' then becomes the line itself
        Vec3 forward = dir.cross(across);
        if (forward.lengthSqr() < 1.0E-8) {
            forward = dir.cross(Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        }
        forward = forward.normalize();
        float pitch = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, forward.y))));
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));

        double yawRad = Math.toRadians(yaw);
        Vec3 flatRight = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 flatUp = flatRight.cross(Vec3.directionFromRotation(pitch, yaw)).normalize();
        float roll = (float) Math.toDegrees(Math.atan2(dir.dot(flatUp), dir.dot(flatRight)));

        spawn(level, line.rotation(yaw, pitch, roll).radius((float) (length / 2)), from.add(to).scale(0.5));
    }

    /**
     * Spawns particles along a slash's path, e.g. shards breaking off it. Pass the same slash, center and
     * yaw/pitch/roll you played it with.
     * @param ticksIn      how long the slash has been visible (only matters if it spins)
     * @param outwardSpeed pushes them away from the arc's center (lines: off to either side), blocks per tick
     * @param randomSpeed  extra random kick in any direction
     */
    public static void alongSlash(Level level, ParticleOptions particle, SlashFx slash, Vec3 center,
                                  float yaw, float pitch, float roll, float ticksIn, int count,
                                  double outwardSpeed, double randomSpeed) {
        SlashFx fx = slash.rotated(yaw, pitch, roll);
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            float t = random.nextFloat();
            Vec3 out = fx.outwardAt(t, ticksIn);
            if (fx.shape() == SlashFx.Shape.LINE && random.nextBoolean()) out = out.scale(-1);
            Vec3 position = center.add(fx.pointAt(t, ticksIn))
                    .add(out.scale((random.nextDouble() - 0.5) * fx.widthBlocks() * 0.6)); // anywhere across its width
            Vec3 kick = randomDirectionInCone(random, out, 360).scale(random.nextDouble() * randomSpeed);
            spawn(level, particle, position, out.scale(outwardSpeed * (0.5 + random.nextDouble())).add(kick));
        }
    }

    /** Random unit vector at most angleDegrees/2 away from forward (uniform over the cone). */
    public static Vec3 randomDirectionInCone(RandomSource random, Vec3 forward, double angleDegrees) {
        double halfAngle = Math.toRadians(Math.min(angleDegrees, 360) / 2.0);
        double cosMin = Math.cos(halfAngle);
        double cosTheta = 1 - random.nextDouble() * (1 - cosMin);
        double sinTheta = Math.sqrt(Math.max(0, 1 - cosTheta * cosTheta));
        double phi = random.nextDouble() * Math.PI * 2;

        // two directions perpendicular to forward
        Vec3 helper = Math.abs(forward.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = forward.cross(helper).normalize();
        Vec3 up = right.cross(forward).normalize();

        return forward.scale(cosTheta)
                .add(right.scale(sinTheta * Math.cos(phi)))
                .add(up.scale(sinTheta * Math.sin(phi)));
    }
}
