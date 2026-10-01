package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.MeleeCombo;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PrinceOfAbolition {

    // =========================
    // Melee combo "Edgeflow" (hitting mobs as Prince Of Abolition), registered in combat/MeleeCombos
    // 1 = crossing cut on the target, 2 = rend along the ground, 3 = wide sweep in front,
    // 4 = tall cleave down onto the target, 5 = ring around you. Then it starts over.
    // Every slash starts as a dotted line, turns into a solid blade of light, then shatters into flakes.
    // =========================

    private static final int EDGE_GLOW = 0xF0FFF1D6;   // warm white body
    private static final int EDGE_TAIL = 0x90FFCF94;   // dimmer, warmer tail
    private static final int EDGE_HEAD = 0xFFFFFFFF;   // pure white front
    private static final int EDGE_LEAD = 0xD8FFE2AE;   // dotted lead-in lines

    /** The yellow/orange spikes around whatever gets hit. Set to false to hide them. */
    private static final boolean SHOW_HIT_RAYS = true;

    // Hit 1: two thin cuts crossing on the target (dotted first, then solid)
    private static final SlashFx EDGE_X_LEAD = SlashFx.line("slash/dashed")
            .color(EDGE_LEAD).width(0.11f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(4).sweep(1);
    private static final SlashFx EDGE_X_CUT = SlashFx.line("slash/smooth")
            .color(0xC8FFC878).core(0xFFFFFFFF)
            .width(0.1f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(20).sweep(2).delay(2);

    // Hit 2: a dotted line runs along the ground, then a jagged blade of light tears along it
    private static final float REND_LENGTH = 10f;
    private static final SlashFx EDGE_REND_LEAD = SlashFx.line("slash/dashed")
            .color(EDGE_LEAD).width(0.14f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(2);
    private static final SlashFx EDGE_REND = SlashFx.line("slash/shatter")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .width(1.4f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.3f)
            .lifetime(14).sweep(4).delay(3);
    private static final SlashFx EDGE_REND_CORE = SlashFx.line("slash/smooth")
            .color(0xE0FFF4E0).core(0xFFFFFFFF)
            .width(0.16f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(16).sweep(3).delay(3);

    // Hit 3: wide crescent sweeping around the front
    private static final SlashFx EDGE_SWEEP_LEAD = SlashFx.arc("slash/dashed_fine")
            .color(EDGE_LEAD).radius(2.75f).arc(250f).width(0.12f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(3);
    private static final SlashFx EDGE_SWEEP = SlashFx.arc("slash/edge")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .radius(2.6f).arc(225f).width(0.85f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.18f)
            .lifetime(10).sweep(3).delay(2);

    // Hit 4: tall slanted cleave from high above down onto the target, then it shatters
    private static final SlashFx EDGE_CLEAVE_LEAD = SlashFx.arc("slash/smooth")
            .color(0xE8FFF0D0).core(0xFFFFFFFF)
            .radius(2.55f).arc(135f).width(0.12f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(6).sweep(2)
            .rotation(0f, -30f, -72f);                  // standing up, slanted, coming down in front
    private static final SlashFx EDGE_CLEAVE = SlashFx.arc("slash/edge")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .radius(2.45f).arc(130f).width(0.8f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.18f)
            .lifetime(9).sweep(2).delay(2)
            .rotation(0f, -30f, -72f);
    private static final SlashFx EDGE_CLEAVE_BREAK = SlashFx.arc("slash/shatter")
            .color(0xB8FFF1D6).tailColor(0x50FFCF94).headColor(0xC8FFFFFF)
            .radius(2.45f).arc(120f).width(1.0f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.26f)
            .lifetime(6).sweep(1).delay(7)
            .rotation(0f, -30f, -72f);

    // Hit 5: dotted circle -> thin ring of light -> thick spinning blade around you
    private static final SlashFx EDGE_RING_LEAD = SlashFx.arc("slash/dashed_fine")
            .color(EDGE_LEAD).radius(2.7f).arc(360f).width(0.11f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(3);
    private static final SlashFx EDGE_RING_LINE = SlashFx.arc("slash/smooth")
            .color(0xF0FFF1D6).core(0xFFFFFFFF)
            .radius(2.6f).arc(345f).width(0.13f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(8).sweep(2).delay(2);
    private static final SlashFx EDGE_RING = SlashFx.arc("slash/edge")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .radius(2.5f).arc(300f).width(1.1f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.25f)
            .lifetime(11).sweep(3).delay(4).spin(14f);

    // Hit sparks (yellow -> orange-red spikes) around whatever gets hit
    private static final SlashFx EDGE_HIT_RAY = SlashFx.line("slash/smooth")
            .color(0xFFFFC23A).tailColor(0xFFFFF27A).headColor(0xFFFF6A1E).core(0xC0FFFFE0)
            .width(0.1f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(7).sweep(2);

    // Broken pieces of the blades, red embers and red mist
    private static final ParticleFx EDGE_FLAKE = ParticleFx.of("fx/flake")
            .color(0xFFFFF4DE).endColor(0x00FFE2B0)
            .size(0.17f).endSize(0.06f).sizeRandom(0.45f)
            .lifetime(12, 10).gravity(-0.05f).friction(0.88f)
            .glow().randomRotation();
    private static final ParticleFx EDGE_SLIVER = ParticleFx.of("fx/sliver")
            .color(0xFFFFF4DE).endColor(0x00FFE2B0)
            .size(0.22f).endSize(0.08f).sizeRandom(0.4f)
            .lifetime(10, 8).gravity(-0.05f).friction(0.88f)
            .glow().randomRotation();
    private static final ParticleFx EDGE_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFF3A24).endColor(0x00A01010)
            .size(0.035f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 16).friction(0.93f)
            .glow();
    private static final ParticleFx EDGE_MIST = ParticleFx.of("fx/smoke")
            .color(0x88A01C1C).endColor(0x00501010)
            .size(0.3f).endSize(0.75f).sizeRandom(0.3f)
            .lifetime(26, 12).gravity(-0.02f).friction(0.92f)
            .spin(2f).randomRotation();
    private static final ParticleFx EDGE_GROUND_MIST = ParticleFx.of("fx/smoke")
            .color(0x806A2A1C).endColor(0x00402010)
            .size(0.28f).endSize(0.55f).sizeRandom(0.3f)
            .lifetime(30, 10).friction(0.85f)
            .randomRotation();

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forAspect("Prince Of Abolition")
            .step(PrinceOfAbolition::edgeCrossCut)
            .step(PrinceOfAbolition::edgeGroundRend)
            .step(PrinceOfAbolition::edgeWideSweep)
            .step(PrinceOfAbolition::edgeCleave)
            .step(PrinceOfAbolition::edgeRing)
            .resetAfter(40)
            .minCharge(0.8f);

    // Ability 1
    public static void princeOfAbolitionBribe(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        LivingEntity livingEntity = rayCast(player,sl);
        if (livingEntity==null)return;

        if (player.isShiftKeyDown()){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 520, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 520, 1));
        }else{
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));
        }
    }

    // ability 2
    public static void princeOfAbolitionCorrosion(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        LivingEntity livingEntity = rayCast(player,sl);
        if (livingEntity==null)return;

        if (player.isShiftKeyDown()){
            SoulCore.setCorruption(livingEntity,SoulCore.getCorruption(livingEntity)+10);

        }else{
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 180, 2));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 180, 2));
        }
    }

    //Ability 3
    public static void princeOfAbolitionDistortion(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        LivingEntity livingEntity = rayCast(player,sl);
        if (livingEntity==null)return;

        if (player.isShiftKeyDown()){
            SoulCore.setCorruption(livingEntity,SoulCore.getCorruption(livingEntity)+10);

        }else{
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 180, 2));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 180, 2));
        }
    }

    private static LivingEntity rayCast(Player player, ServerLevel sl){

        Vec3 location = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        for (float i = 0; i < 16; i+=0.5f) {

            Vec3 current = location.add(direction.scale(i));

            BlockPos blockPos = new BlockPos(Mth.floor(current.x), Mth.floor(current.y), Mth.floor(current.z));
            BlockState blockState = sl.getBlockState(blockPos);

            if (blockState.getBlock().defaultBlockState().isSolid()){
                break;
            }

            List<LivingEntity> entities = sl.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(current, current).inflate(0.5),
                    e -> e != player
            );

            if (!entities.isEmpty()) {
                return entities.getFirst();
            }
        }
        return null;
    }

    // =========================
    // Melee combo hits
    // =========================

    private static final Vec3 UP = new Vec3(0, 1, 0);

    private static Vec3 flatLook(Player player) {
        return Vec3.directionFromRotation(0f, player.getYRot());
    }

    // Hit 1: two thin cuts cross on the target, red mist bursts out of it
    private static void edgeCrossCut(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 hit = target.getBoundingBox().getCenter();
        Vec3 look = flatLook(player);
        Vec3 side = look.cross(UP).normalize();

        // one long cut rising one way, a shorter one rising the other way, both through the target
        double[][] cuts = {
                {Math.toRadians(38 + random.nextFloat() * 17), 2.3 + random.nextFloat() * 0.3},
                {Math.toRadians(180 - (28 + random.nextFloat() * 17)), 1.7 + random.nextFloat() * 0.3}};
        for (int i = 0; i < cuts.length; i++) {
            double angle = cuts[i][0];
            double half = cuts[i][1];
            Vec3 dir = side.scale(Math.cos(angle)).add(UP.scale(Math.sin(angle)))
                    .add(look.scale((random.nextFloat() - 0.5f) * 0.5f)).normalize();
            Vec3 center = hit.add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.12));
            Vec3 from = center.subtract(dir.scale(half));
            Vec3 to = center.add(dir.scale(half));
            ParticleShapes.slashBetween(sl, EDGE_X_LEAD.delay(i), from, to);
            ParticleShapes.slashBetween(sl, EDGE_X_CUT.delay(2 + i), from, to);
        }

        Vec3 feet = target.position();
        Later.run(sl, 3, () -> {
            for (int i = 0; i < 7; i++) {
                Vec3 velocity = UP.scale(0.05 + random.nextDouble() * 0.06)
                        .add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.01 + random.nextDouble() * 0.04));
                ParticleShapes.spawn(sl, EDGE_MIST, hit.add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.25)), velocity);
            }
            for (int i = 0; i < 4; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                ParticleShapes.spawn(sl, EDGE_GROUND_MIST, feet.add(0, 0.2, 0).add(out.scale(0.3)), out.scale(0.05).add(0, 0.005, 0));
            }
        });
        edgeHitRays(sl, hit, random, 3);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.5f);
        Later.run(sl, 2, () -> sl.playSound(null, BlockPos.containing(hit), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.3f));
    }

    // Hit 2: a dotted line shoots along the ground, then a jagged blade of light tears along it
    private static void edgeGroundRend(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 look = flatLook(player);
        Vec3 side = look.cross(UP).normalize();
        Vec3 feet = player.position().add(0, 0.06, 0);
        Vec3 start = feet.add(look.scale(0.7));
        Vec3 end = feet.add(look.scale(0.7 + REND_LENGTH));

        ParticleShapes.slashBetween(sl, EDGE_REND_LEAD, start.add(0, 0.12, 0), end.add(0, 0.12, 0));
        ParticleShapes.slashBetween(sl, EDGE_REND, start.add(0, 0.3, 0), end.add(0, 0.3, 0));
        ParticleShapes.slashBetween(sl, EDGE_REND_CORE, start.add(0, 0.3, 0), end.add(0, 0.3, 0));

        // pieces break off along the blade, a moment after it passes each spot
        for (int wave = 0; wave < 4; wave++) {
            int w = wave;
            Later.run(sl, 6 + wave, () -> {
                for (int i = 0; i < 7; i++) {
                    double t = (w + random.nextDouble()) / 4.0;
                    Vec3 position = start.add(end.subtract(start).scale(t))
                            .add(side.scale((random.nextDouble() - 0.5) * 0.9)).add(0, 0.1, 0);
                    Vec3 velocity = UP.scale(0.03 + random.nextDouble() * 0.07)
                            .add(side.scale((random.nextDouble() - 0.5) * 0.08));
                    ParticleShapes.spawn(sl, random.nextFloat() < 0.6f ? EDGE_FLAKE : EDGE_SLIVER, position, velocity);
                }
            });
        }
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 5);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
        Later.run(sl, 3, () -> sl.playSound(null, BlockPos.containing(start), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f));
    }

    // Hit 3: a wide crescent sweeps around the front and breaks into flakes
    private static void edgeWideSweep(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 center = player.position().add(0, 1.0, 0);
        float yaw = player.getYRot();
        SlashFx sweep = EDGE_SWEEP.varied(random, 20f, 6f, 12f);       // left or right, a little tilted

        ParticleShapes.slash(sl, EDGE_SWEEP_LEAD.alignedWith(sweep), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, sweep, center, yaw, 0f, 0f);
        Later.run(sl, 8, () -> {
            ParticleShapes.alongSlash(sl, EDGE_FLAKE, sweep, center, yaw, 0f, 0f, 6f, 12, 0.06, 0.03);
            ParticleShapes.alongSlash(sl, EDGE_SLIVER, sweep, center, yaw, 0f, 0f, 6f, 6, 0.06, 0.03);
            sl.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        });
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 3);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.0f);
    }

    // Hit 4: a tall slanted cleave comes down from high above onto the target, then shatters
    private static void edgeCleave(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        float yaw = player.getYRot();
        // the arc's center sits between you and the target, so the blade comes down right through it
        Vec3 center = target.position().subtract(flatLook(player).scale(2.0)).add(0, 1.1, 0);
        SlashFx cleave = EDGE_CLEAVE.varied(random, 15f, 8f, 12f);     // slanted to either side

        ParticleShapes.slash(sl, EDGE_CLEAVE_LEAD.alignedWith(cleave), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, cleave, center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, EDGE_CLEAVE_BREAK.alignedWith(cleave), center, yaw, 0f, 0f);
        Later.run(sl, 7, () -> {
            ParticleShapes.alongSlash(sl, EDGE_FLAKE, cleave, center, yaw, 0f, 0f, 5f, 14, 0.05, 0.04);
            ParticleShapes.alongSlash(sl, EDGE_SLIVER, cleave, center, yaw, 0f, 0f, 5f, 8, 0.05, 0.04);
            sl.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.3f);
        });
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 3);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
        Later.run(sl, 2, () -> sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.9f));
    }

    // Hit 5: dotted circle -> thin ring of light -> thick blade spinning around you, then flakes
    private static void edgeRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 center = player.position().add(0, 0.95, 0);
        float yaw = player.getYRot();
        SlashFx ring = EDGE_RING.varied(random, 180f, 5f, 8f);         // starts anywhere, spins either way

        ParticleShapes.slash(sl, EDGE_RING_LEAD.alignedWith(ring), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, EDGE_RING_LINE.alignedWith(ring), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, ring, center, yaw, 0f, 0f);
        Later.run(sl, 11, () -> {
            ParticleShapes.alongSlash(sl, EDGE_FLAKE, ring, center, yaw, 0f, 0f, 7f, 16, 0.07, 0.03);
            ParticleShapes.alongSlash(sl, EDGE_SLIVER, ring, center, yaw, 0f, 0f, 7f, 8, 0.07, 0.03);
            sl.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.1f);
        });
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 5);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.9f);
        Later.run(sl, 4, () -> sl.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6f, 1.4f));
    }

    // Yellow/orange spikes shooting out around the hit, plus a few red embers
    private static void edgeHitRays(ServerLevel sl, Vec3 center, RandomSource random, int delay) {
        if (SHOW_HIT_RAYS) {
            for (int i = 0; i < 7; i++) {
                Vec3 dir = ParticleShapes.randomDirectionInCone(random, UP, 360);
                double inner = 0.55 + random.nextDouble() * 0.25;
                double outer = inner + 0.55 + random.nextDouble() * 0.3;
                ParticleShapes.slashBetween(sl, EDGE_HIT_RAY.delay(delay), center.add(dir.scale(inner)), center.add(dir.scale(outer)));
            }
        }
        Later.run(sl, delay, () -> {
            for (int i = 0; i < 6; i++) {
                Vec3 position = center.add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.3));
                Vec3 velocity = ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.02 + random.nextDouble() * 0.05);
                ParticleShapes.spawn(sl, EDGE_EMBER, position, velocity);
            }
        });
    }

    private static boolean canUseClass(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Prince Of Abolition");
    }

}
