package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Looks that many abilities share, ready to call from any class (visual only, nothing here hurts anyone):
 *
 *   SpellFx.warningCircle(sl, ground, 4.0, 0xE0FF4A1A, 20);       // a turning rune circle: "something lands here"
 *   SpellFx.shockRing(sl, ground, 5.0, 0xF0FFE0B0, 9, 0);         // a ring racing outward along the ground
 *   SpellFx.fireBurst(sl, center, 3.5);                           // an explosion of fire
 *   SpellFx.tether(sl, THREAD, player, target, 100);              // a line that keeps joining two entities for 5 seconds
 *   SpellFx.tether(sl, THREAD, () -> SpellFx.handOf(player, sl), () -> SpellFx.topOf(target, sl), 100);   // ...hand to head
 *
 * Colors are ARGB (0xAARRGGBB); the alpha is how strong it is.
 * The models are the shared ones from models/model_fx/ (see docs/MODEL_FX.md).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class SpellFx {

    // flat plates, one block wide and white: .scale(...) is their width in blocks, .color(...) tints them
    public static final ModelFx RUNE_CIRCLE = ModelFx.of("rune_circle").glow().unshaded().seeThrough();
    public static final ModelFx SHOCK_RING = ModelFx.of("shock_ring").glow().unshaded().seeThrough();
    /** The flash of an explosion, about 1 block wide. */
    public static final ModelFx FIRE_BLAST = ModelFx.of("fire_blast").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Flames, 1 block wide and 2 tall, animated. Stretch them with .scale(width, height / 2, width). */
    public static final ModelFx FLAMES = ModelFx.of("flame_pillar").glow().unshaded().seeThrough();

    public static final ParticleFx FIRE_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFFB04A).endColor(0x00C01810)
            .size(0.11f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(14, 10).gravity(-0.03f).friction(0.9f)
            .glow();
    public static final ParticleFx FIRE_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xA0241818).endColor(0x00100808)
            .size(0.35f).endSize(1.0f).sizeRandom(0.3f)
            .lifetime(28, 14).gravity(-0.03f).friction(0.9f)
            .spin(2f).randomRotation();

    /** How often a tether is drawn again, in ticks. */
    private static final int TETHER_STEP = 2;

    private SpellFx() {}

    /** A ring racing outward along the ground until it is 'radius' wide. */
    public static void shockRing(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, int delay) {
        ModelFx ring = SHOCK_RING.color(color).delay(delay).lifetime(ticks)
                .key(0, ModelFx.pose().scale(0.6f))
                .key(ticks, ModelFx.pose().scale((float) (radius * 2.0)), ModelFx.Ease.OUT)
                .during(0, ticks, ModelFx.pose().alpha(0f), ModelFx.Ease.IN);        // stays bright, fades at the end
        ParticleShapes.model(sl, ring, ground.add(0, 0.07, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** A ring closing in on a point from 'radius' away (the opposite of shockRing): something is being drawn in. */
    public static void closingRing(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, int delay) {
        ModelFx ring = SHOCK_RING.color(color).delay(delay).lifetime(ticks)
                .key(0, ModelFx.pose().scale((float) (radius * 2.0)).alpha(0f))
                .key(ticks, ModelFx.pose().scale(0.6f).alpha(1f), ModelFx.Ease.IN);
        ParticleShapes.model(sl, ring, ground.add(0, 0.07, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** The turning rune circle that marks where something is about to land. */
    public static void warningCircle(ServerLevel sl, Vec3 ground, double radius, int color, int ticks) {
        runeCircle(sl, ground, radius, color, ticks, 5f);
    }

    /** A rune circle 'radius' wide lying on the ground for 'ticks' ticks, turning 'spin' degrees per tick. */
    public static void runeCircle(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, float spin) {
        ModelFx circle = RUNE_CIRCLE.color(color).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, Math.min(8, ticks / 2)).spin(spin)
                .key(0, ModelFx.pose().scale(0.3f).alpha(0f))
                .key(5, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK);
        ParticleShapes.model(sl, circle, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** A burst of fire: a ball of flame swelling and fading, a ring, embers and smoke. */
    public static void fireBurst(ServerLevel sl, Vec3 center, double radius) {
        ModelFx flash = FIRE_BLAST.spin(25f).lifetime(8)
                .key(0, ModelFx.pose().scale(0.8f))
                .key(8, ModelFx.pose().scale((float) (radius * 1.8)), ModelFx.Ease.OUT)   // ends about as wide as the burst
                .during(2, 8, ModelFx.pose().alpha(0f));
        ParticleShapes.model(sl, flash, center, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        shockRing(sl, center.add(0, -0.3, 0), radius * 1.1, 0xE0FFB060, 8, 0);
        ParticleShapes.burst(sl, FIRE_EMBER, center, (int) (radius * 10), 0.1, 0.12 + radius * 0.08);
        ParticleShapes.burst(sl, FIRE_SMOKE, center, (int) (radius * 3), 0.03, 0.12);
    }

    // =========================
    // Tethers: a line that follows two moving things
    // =========================

    /**
     * Draws 'line' (a SlashFx.line) from one point to another again and again for 'ticks' ticks. The two ends are
     * asked for every time, so the line follows whatever they are attached to. When either returns null the line is
     * gone for good (the thread snapped).
     *
     *   SpellFx.tether(sl, THREAD, () -> player.isAlive() ? player.getEyePosition() : null, () -> anchor, 60);
     */
    public static void tether(ServerLevel sl, SlashFx line, Supplier<Vec3> from, Supplier<Vec3> to, int ticks) {
        Vec3 a = from.get();
        Vec3 b = to.get();
        if (a == null || b == null) return;
        // each copy lives a little longer than the gap to the next one, so the line never blinks
        ParticleShapes.slashBetween(sl, line.lifetime(TETHER_STEP * 3).sweep(1), a, b);
        if (ticks > TETHER_STEP) {
            Later.run(sl, TETHER_STEP, () -> tether(sl, line, from, to, ticks - TETHER_STEP));
        }
    }

    /** A tether between the middles of two entities. It ends when either dies, unloads or leaves this dimension. */
    public static void tether(ServerLevel sl, SlashFx line, Entity from, Entity to, int ticks) {
        tether(sl, line, () -> middleOf(from, sl), () -> middleOf(to, sl), ticks);
    }


    // The three below give a tether its ends. Each returns null once the entity is dead, gone or in another
    // dimension, which is what ends the tether.

    /** The middle of an entity's body. */
    public static Vec3 middleOf(Entity entity, ServerLevel sl) {
        if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.level() != sl) return null;
        return entity.getBoundingBox().getCenter();
    }

    /** The top of an entity's head. */
    public static Vec3 topOf(Entity entity, ServerLevel sl) {
        if (middleOf(entity, sl) == null) return null;
        return entity.position().add(0, entity.getBbHeight(), 0);
    }

    /** Where a player's right hand is. */
    public static Vec3 handOf(Player player, ServerLevel sl) {
        if (middleOf(player, sl) == null) return null;
        return player.getEyePosition().add(Spells.rightOf(player).scale(0.3)).add(0, -0.4, 0);
    }
}
