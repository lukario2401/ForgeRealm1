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
        for (int i = 0; i < count; i++) {
            int arm = i % arms;
            double along = random.nextDouble(); // 0 = inside, 1 = outside
            double radius = innerRadius + (outerRadius - innerRadius) * along + (random.nextDouble() - 0.5) * 0.3;
            double angle = Math.PI * 2 * ((double) arm / arms + turns * along) + (random.nextDouble() - 0.5) * 0.2;
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
