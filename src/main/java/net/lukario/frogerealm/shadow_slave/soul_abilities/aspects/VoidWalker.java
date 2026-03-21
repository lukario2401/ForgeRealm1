package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * =====================================================================
 *  VOID WALKER — Full 7-Ability Implementation
 * =====================================================================
 *
 *  Role    : Single-target DPS / Assassin
 *  Theme   : Void assassin bending space
 *  Strength: Insane burst, boss deletion, high combo damage
 *  Weakness: Low HP, no AOE, skill-dependent survival
 *
 *  ── Core Mechanic: VOID MARK ─────────────────────────────────────
 *  Stored on the TARGET entity as NBT.
 *    "VoidMarkStacks"   — int  : 0-5 stacks on the target
 *    "VoidMarkTimer"    — int  : ticks remaining before stacks expire
 *    "VoidFractured"    — int  : ticks of the Fractured debuff (bonus dmg taken)
 *    "VoidAnchorId"     — int  : entity ID of the player's Anchor target (on PLAYER)
 *
 *  ── Player NBT keys ──────────────────────────────────────────────
 *    "PhaseShiftActive"      — int  : ticks of intangibility remaining
 *    "PhaseShiftTarget"      — int  : entity ID to teleport behind on Phase Shift end
 *    "VoidOverdriveActive"   — int  : ticks of Void Overdrive remaining
 *    "VoidOverdriveDmgTaken" — flag : used by damage event to apply +25% penalty
 *    "CollapseStrikeCD"      — int  : cooldown ticks
 *    "Ability1CD"            — int  : Void Pierce cooldown
 *    "Ability2CD"            — int  : Phase Shift cooldown
 *    "Ability3CD"            — int  : Void Anchor cooldown
 *    "Ability5CD"            — int  : Rift Step cooldown (charges tracked separately)
 *    "Ability5Charges"       — int  : Rift Step charges (0-2 at late upgrade)
 *    "Ability6CD"            — int  : Void Consumption cooldown
 *    "Ability7CD"            — int  : Void Overdrive cooldown
 *    "RiftStepInvisCD"       — int  : brief invisibility after Rift Step
 *
 *  ── Soul Essence costs ───────────────────────────────────────────
 *    Ability 1 — Void Pierce        : 150
 *    Ability 2 — Phase Shift        : 250
 *    Ability 3 — Void Anchor        : 200
 *    Ability 4 — Collapse Strike    : 300
 *    Ability 5 — Rift Step          : 250
 *    Ability 6 — Void Consumption   : 350
 *    Ability 7 — Void Overdrive     : 1000
 *
 *  ── Cooldowns (ticks) ────────────────────────────────────────────
 *    Ability 1:  70   (~3.5 s)
 *    Ability 2:  200  (10 s)
 *    Ability 3:  400  (20 s)
 *    Ability 4:  varies (see Collapse Strike)
 *    Ability 5:  180  (9 s per charge)
 *    Ability 6:  300  (15 s)
 *    Ability 7:  2400 (2 min)
 * =====================================================================
 */
public class VoidWalker {

    // ─────────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    private static final int   MAX_VOID_MARKS       = 5;
    /** Ticks before void marks expire with no new application */
    private static final int   VOID_MARK_DURATION   = 200; // 10 seconds
    /** Damage multiplier per void mark stack (+8% each) */
    private static final float VOID_MARK_DMG_BONUS  = 0.08f;
    /** Rupture burst damage at max stacks */
    private static final float RUPTURE_DMG          = 28.0f;
    /** Collapse Strike base damage per stack */
    private static final float COLLAPSE_DMG_PER_STACK = 6.0f;

    // ─────────────────────────────────────────────────────────────────
    //  EVENT BUS
    // ─────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onPlayerAttack(AttackEntityEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!(event.getTarget() instanceof LivingEntity target)) return;
            if (!SoulCore.getAspect(player).equals("Void Walker")) return;
            if (player.level().isClientSide) return;

            VoidWalker.applyVoidMarks(target, player, 1);
        }

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;
            if (!(entity.level() instanceof ServerLevel level)) return;

            tickVoidMarks(entity, level);
            tickFractured(entity);

            if (entity instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Void Walker")) return;
                tickCooldowns(player);
                tickPhaseShift(player, level);
                tickVoidOverdrive(player, level);
                tickRiftStepInvis(player);
                // Velocity-modifying and charge-refill calls wired directly here
                tickVoidAnchorPull(player, level);
                tickRiftStepCharges(player);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            // Extend Void Overdrive duration on kill (ascension >= 7)
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (player.level().isClientSide) return;
            if (!SoulCore.getAspect(player).equals("Void Walker")) return;
            onKillDuringOverdrive(player);
        }

        /**
         * Intercepts incoming damage to:
         *  1. Apply Void Mark amplification (on enemies hit by the Void Walker player).
         *  2. Block all damage during Phase Shift.
         *  3. Apply +25% self-damage penalty during Void Overdrive.
         */
        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;

            CompoundTag nbt = entity.getPersistentData();

            // ── Phase Shift: negate all incoming damage to the player ──
            if (entity instanceof Player player) {
                if (player.getPersistentData().getInt("PhaseShiftActive") > 0) {
                    event.setCanceled(true);
                    return;
                }
                // Void Overdrive penalty: player takes +25% damage
                if (player.getPersistentData().getInt("VoidOverdriveActive") > 0) {
                    event.setAmount(event.getAmount() * 1.25f);
                }
            }

            // ── Void Mark amplification on targets ────────────────────
            int stacks = nbt.getInt("VoidMarkStacks");
            if (stacks > 0) {
                float bonus = 1.0f + (stacks * VOID_MARK_DMG_BONUS);
                event.setAmount(event.getAmount() * bonus);
            }

            // ── Fractured debuff: +20% bonus damage taken ─────────────
            if (nbt.getInt("VoidFractured") > 0) {
                event.setAmount(event.getAmount() * 1.20f);
            }

        }

        // ── Void Mark timer decay ─────────────────────────────────────
        private static void tickVoidMarks(LivingEntity entity, ServerLevel level) {
            CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("VoidMarkTimer")) return;

            int timer = nbt.getInt("VoidMarkTimer");
            if (timer > 0) {
                nbt.putInt("VoidMarkTimer", timer - 1);
                // Ambient void particles proportional to stacks
                int stacks = nbt.getInt("VoidMarkStacks");
                if (stacks > 0 && timer % 15 == 0) {
                    level.sendParticles(ParticleTypes.PORTAL,
                            entity.getX(), entity.getY() + 1.0, entity.getZ(),
                            stacks, 0.3, 0.3, 0.3, 0.04);
                }
            } else {
                nbt.remove("VoidMarkTimer");
                nbt.remove("VoidMarkStacks");
            }
        }

        // ── Fractured debuff decay ────────────────────────────────────
        private static void tickFractured(LivingEntity entity) {
            CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("VoidFractured")) return;
            int t = nbt.getInt("VoidFractured");
            if (t > 0) nbt.putInt("VoidFractured", t - 1);
            else nbt.remove("VoidFractured");
        }

        // ── Cooldown tick-down ────────────────────────────────────────
        private static void tickCooldowns(Player player) {
            CompoundTag nbt = player.getPersistentData();
            for (String key : List.of(
                    "Ability1CD", "Ability2CD", "Ability3CD", "CollapseStrikeCD",
                    "Ability5CD", "Ability6CD", "Ability7CD")) {
                if (nbt.contains(key)) {
                    int v = nbt.getInt(key);
                    if (v > 0) nbt.putInt(key, v - 1);
                    else nbt.remove(key);

                    if (SoulCore.getAscensionStage(player) == 8) {
                        v = nbt.getInt(key);
                        if (v > 0) nbt.putInt(key, 0);
                        else nbt.remove(key);
                    }
                }
            }
        }

        // ── Phase Shift tick ─────────────────────────────────────────
        private static void tickPhaseShift(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("PhaseShiftActive");
            if (ticks <= 0) return;

            ticks--;
            nbt.putInt("PhaseShiftActive", ticks);

            // Intangibility particles
            level.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    6, 0.4, 0.6, 0.4, 0.05);

            // On expiry: late upgrade — teleport behind anchor/locked target
            if (ticks == 0) {
                nbt.remove("PhaseShiftActive");
                int ascension = SoulCore.getAscensionStage(player);
                if (ascension >= 5 && nbt.contains("PhaseShiftTarget")) {
                    int targetId = nbt.getInt("PhaseShiftTarget");
                    nbt.remove("PhaseShiftTarget");
                    LivingEntity anchor = findEntityById(level, targetId);
                    if (anchor != null) {
                        teleportBehind(player, anchor, level);
                        player.addEffect(new MobEffectInstance(
                                MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
                    }
                }
            }
        }

        // ── Void Overdrive tick ───────────────────────────────────────
        private static void tickVoidOverdrive(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("VoidOverdriveActive");
            if (ticks <= 0) return;

            ticks--;
            nbt.putInt("VoidOverdriveActive", ticks);

            // Void trail particles
            if (ticks % 3 == 0) {
                level.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 0.1, player.getZ(),
                        4, 0.2, 0.1, 0.2, 0.02);
            }

            // Continuous attack speed & speed buff
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,       30, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,  30, 1, false, false));

            // Late upgrade: lifesteal — heal a small amount every second
            if (SoulCore.getAscensionStage(player) >= 7 && ticks % 20 == 0) {
                player.heal(1.5f);
            }

            if (ticks == 0) {
                nbt.remove("VoidOverdriveActive");
                level.sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1, player.getZ(),
                        10, 0.5, 0.5, 0.5, 0.02);
            }
        }

        // ── Rift Step invisibility tick ───────────────────────────────
        private static void tickRiftStepInvis(Player player) {
            CompoundTag nbt = player.getPersistentData();
            int t = nbt.getInt("RiftStepInvisCD");
            if (t <= 0) return;
            t--;
            nbt.putInt("RiftStepInvisCD", t);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORE MECHANIC — VOID MARK  (internal, called by all abilities)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Applies {@code amount} Void Mark stacks to a target.
     * At MAX_VOID_MARKS triggers Rupture: a large burst of void damage.
     */
    public static void applyVoidMarks(LivingEntity target, Player source, int amount) {
        if (target.level().isClientSide) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        CompoundTag nbt   = target.getPersistentData();
        int stacks        = nbt.getInt("VoidMarkStacks") + amount;

        if (stacks >= MAX_VOID_MARKS) {
            // ── RUPTURE ───────────────────────────────────────────────
            nbt.putInt("VoidMarkStacks", 0);
            nbt.remove("VoidMarkTimer");

            int ascension  = SoulCore.getAscensionStage(source);
            float ruptDmg  = RUPTURE_DMG + ascension * 2.0f;
            target.hurt(level.damageSources().playerAttack(source), ruptDmg);

            level.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX(), target.getY() + 1, target.getZ(),
                    3, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    20, 0.6, 0.8, 0.6, 0.15);
            level.playSound(null, target.getX(),target.getY(),target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.6f);

            // During Void Overdrive, reset Collapse Strike CD on Rupture
            if (source.getPersistentData().getInt("VoidOverdriveActive") > 0) {
                source.getPersistentData().remove("CollapseStrikeCD");
            }

        } else {
            // ── Stack up ──────────────────────────────────────────────
            nbt.putInt("VoidMarkStacks", stacks);
            nbt.putInt("VoidMarkTimer",  VOID_MARK_DURATION);

            level.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1.2, target.getZ(),
                    stacks * 2, 0.25, 0.25, 0.25, 0.03);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 1 — VOID PIERCE  (Active, ~3.5 s CD, 150 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Dashes the player through the nearest enemy in sight, dealing light damage
     * and applying 1 Void Mark (2 at ascension ≥ 6).
     * During Void Overdrive, applies max Void Marks instantly instead.
     */
    public static void voidWalkerAbilityOneUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability1CD", 70, 150)) return;

        int ascension   = SoulCore.getAscensionStage(player);
        LivingEntity target = findClosestInSight(player, level, 16.0);
        if (target == null) return;

        // Teleport the player to just past the target (dash-through)
        Vec3 dir        = target.position().subtract(player.position()).normalize();
        Vec3 dashPos    = target.position().add(dir.scale(1.5));
        player.teleportTo(dashPos.x, dashPos.y, dashPos.z);

        // Brief i-frame via invincibility ticks (ascension ≥ 6)
        if (ascension >= 6) {
            player.invulnerableTime = 10;
        }

        // Damage
        float dmg = 6.0f + ascension * 0.5f;
        target.hurt(level.damageSources().playerAttack(player), dmg);

        // Void Overdrive: instantly max marks
        boolean overdrive = player.getPersistentData().getInt("VoidOverdriveActive") > 0;
        int marksToApply  = overdrive ? MAX_VOID_MARKS : (ascension >= 6 ? 2 : 1);
        applyVoidMarks(target, player, marksToApply);

        // Dash particles trailing through target
        for (int i = 0; i < 6; i++) {
            Vec3 trailPos = player.position().subtract(dir.scale(i * 0.3));
            level.sendParticles(ParticleTypes.PORTAL,
                    trailPos.x, trailPos.y + 1, trailPos.z, 2, 0.1, 0.2, 0.1, 0.03);
        }
        level.playSound(null, target.blockPosition(),
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.6f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-150);
        setCooldown(player, "Ability1CD", 70);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 2 — PHASE SHIFT  (Active, 10 s CD, 250 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Makes the player intangible for ~0.5 seconds (10 ticks), cleansing
     * all negative effects. Damage during this window is cancelled via the
     * LivingDamageEvent above.
     * Late upgrade (ascension ≥ 5): on expiry, teleports behind the Void Anchor
     * target and grants a speed burst.
     */
    public static void voidWalkerAbilityTwoUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability2CD", 200, 250)) return;

        CompoundTag nbt = player.getPersistentData();
        nbt.putInt("PhaseShiftActive", 100); // 0.5 seconds

        // Cleanse all negative effects
        player.getActiveEffects().stream()
                // Add .value() to access the MobEffect inside the Holder
                .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(e -> e.getEffect())
                .toList()
                .forEach(player::removeEffect);

        // Late upgrade: store anchor target to teleport behind on expiry
        int ascension = SoulCore.getAscensionStage(player);
        if (ascension >= 5 && nbt.contains("VoidAnchorId")) {
            nbt.putInt("PhaseShiftTarget", nbt.getInt("VoidAnchorId"));
        }

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                15, 0.5, 0.8, 0.5, 0.08);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-250);
        setCooldown(player, "Ability2CD", 200);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 3 — VOID ANCHOR  (Active, 20 s CD, 200 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Locks onto the nearest enemy in sight as your Anchor target.
     * While anchored:
     *  - You deal +20% damage to that target (handled in applyVoidMarks and damage).
     *  - Your movement is gently pulled toward them each tick.
     * Late upgrade (ascension ≥ 5): Anchor target is revealed through walls
     * (Glowing effect) and you gain a passive +10% crit bonus (visual tag only —
     * hook this into your crit system via "VoidAnchorCrit" NBT on the player).
     */
    public static void voidWalkerAbilityThreeUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability3CD", 400, 200)) return;

        LivingEntity target = findClosestInSight(player, level, 20.0);
        if (target == null) return;

        CompoundTag nbt = player.getPersistentData();
        nbt.putInt("VoidAnchorId", target.getId());

        // Late upgrade: Glowing (reveals through walls) + crit flag
        int ascension = SoulCore.getAscensionStage(player);
        if (ascension >= 5) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false));
            nbt.putBoolean("VoidAnchorCrit", true);
        } else {
            nbt.remove("VoidAnchorCrit");
        }

        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                12, 0.5, 0.8, 0.5, 0.06);
        level.playSound(null, target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-200);
        setCooldown(player, "Ability3CD", 400);
    }

    /**
     * Called every tick (from Events.onLivingTick → tickCooldowns path is not enough —
     * hook this directly in onLivingTick for the player if you want the pull).
     * Gently pulls the player toward their Void Anchor target.
     * Called internally by the tick handler.
     */
    public static void tickVoidAnchorPull(Player player, ServerLevel level) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains("VoidAnchorId")) return;

        LivingEntity anchor = findEntityById(level, nbt.getInt("VoidAnchorId"));
        if (anchor == null || !anchor.isAlive()) {
            nbt.remove("VoidAnchorId");
            nbt.remove("VoidAnchorCrit");
            return;
        }

        // Gentle pull: nudge velocity toward anchor if more than 4 blocks away
        double dist = player.distanceTo(anchor);
        if (dist > 4.0) {
            Vec3 pull = anchor.position().subtract(player.position()).normalize().scale(0.04);
            player.setDeltaMovement(player.getDeltaMovement().add(pull));
            player.hurtMarked = true;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 4 — COLLAPSE STRIKE  (Active, variable CD, 300 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Consumes ALL Void Marks on the Anchor target (or nearest in sight) for a
     * burst of damage scaled to the number of stacks consumed.
     *   1 stack  → 6 dmg
     *   5 stacks → 30 dmg  (+ Rupture already fired by applyVoidMarks at 5)
     *
     * Late upgrade (ascension ≥ 6): applies Fractured (60 ticks / 3 sec, +20% dmg taken).
     * During Void Overdrive: no cooldown.
     */
    public static void voidWalkerAbilityFourUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "CollapseStrikeCD", 0, 300)) return;

        // Find target: prefer Anchor, fall back to nearest in sight
        LivingEntity target = getAnchorTarget(player, level);
        if (target == null) target = findClosestInSight(player, level, 12.0);
        if (target == null) return;

        CompoundTag tnbt = target.getPersistentData();
        int stacks       = tnbt.getInt("VoidMarkStacks");
        if (stacks < 1) stacks = 1; // always deal at least one hit

        // Consume stacks
        tnbt.putInt("VoidMarkStacks", 0);
        tnbt.remove("VoidMarkTimer");

        int ascension = SoulCore.getAscensionStage(player);
        float dmg     = COLLAPSE_DMG_PER_STACK * stacks + ascension * 1.5f;

        target.hurt(level.damageSources().playerAttack(player), dmg);

        // Late upgrade: Fractured debuff
        if (ascension >= 6) {
            tnbt.putInt("VoidFractured", 60);
        }

        // Dramatic collapse particles
        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 1, target.getZ(),
                stacks, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                stacks * 4, 0.5, 0.8, 0.5, 0.1);
        level.playSound(null, player.getX(),player.getY(),player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.4f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-300);

        // Void Overdrive: no cooldown; otherwise scale CD with stacks used
        boolean overdrive = player.getPersistentData().getInt("VoidOverdriveActive") > 0;
        if (!overdrive) {
            int cd = Math.max(60, 160 - stacks * 20); // fewer stacks = longer wait
            setCooldown(player, "CollapseStrikeCD", cd);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 5 — RIFT STEP  (Active, 9 s CD per charge, 250 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Instantly teleports the player to their Anchor target (or nearest in sight),
     * resets Void Pierce cooldown, and applies 1 Void Mark.
     * Late upgrade (ascension ≥ 6): 2 charges; grants brief invisibility on use.
     */
    public static void voidWalkerAbilityFiveUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!SoulCore.getAspect(player).equals("Void Walker")) return;
        if (SoulCore.getSoulEssence(player) < 250) return;

        CompoundTag nbt = player.getPersistentData();
        int ascension   = SoulCore.getAscensionStage(player);
        int maxCharges  = ascension >= 6 ? 2 : 1;

        // Initialise charges if missing
        if (!nbt.contains("Ability5Charges")) {
            nbt.putInt("Ability5Charges", maxCharges);
        }

        int charges = nbt.getInt("Ability5Charges");
        if (charges <= 0) return; // no charges available

        LivingEntity target = getAnchorTarget(player, level);
        if (target == null) target = findClosestInSight(player, level, 30.0);
        if (target == null) return;

        // Departure particles
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                10, 0.3, 0.6, 0.3, 0.06);

        // Teleport just in front of target
        Vec3 dir     = target.position().subtract(player.position()).normalize();
        Vec3 arrPos  = target.position().subtract(dir.scale(1.2));
        player.teleportTo(arrPos.x, arrPos.y, arrPos.z);

        // Arrival particles
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                10, 0.3, 0.6, 0.3, 0.06);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);

        applyVoidMarks(target, player, 1);

        // Reset Void Pierce CD
        nbt.remove("Ability1CD");

        // Late upgrade: brief invisibility
        if (ascension >= 6) {
            nbt.putInt("RiftStepInvisCD", 40); // 2 seconds
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-250);

        // Consume a charge; start recharge CD
        nbt.putInt("Ability5Charges", charges - 1);
        setCooldown(player, "Ability5CD", 180);
    }

    /**
     * Refills one Rift Step charge when the CD expires. Call this from your
     * tick handler after tickCooldowns, or wire it into the CD system.
     */
    public static void tickRiftStepCharges(Player player) {
        CompoundTag nbt = player.getPersistentData();
        if (nbt.contains("Ability5CD")) return; // still on CD

        int ascension  = SoulCore.getAscensionStage(player);
        int maxCharges = ascension >= 6 ? 2 : 1;
        int charges    = nbt.getInt("Ability5Charges");

        if (charges < maxCharges) {
            nbt.putInt("Ability5Charges", charges + 1);
            // Restart CD if still not full
            if (charges + 1 < maxCharges) {
                setCooldown(player, "Ability5CD", 180);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 6 — VOID CONSUMPTION  (Active, 15 s CD, 350 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finisher ability. Targets must be below 30% HP (40% at ascension ≥ 7).
     *  - Deals massive damage.
     *  - On kill: heals 20% max HP and resets Collapse Strike CD.
     *  - Late upgrade: also grants Haste II for 4 seconds.
     */
    public static void voidWalkerAbilitySixUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability6CD", 300, 350)) return;

        LivingEntity target = getAnchorTarget(player, level);
        if (target == null) target = findClosestInSight(player, level, 12.0);
        if (target == null) return;

        int ascension   = SoulCore.getAscensionStage(player);
        float threshold = ascension >= 7 ? 0.40f : 0.30f;
        float hpPercent = target.getHealth() / target.getMaxHealth();

        if (hpPercent > threshold) {
            // Not below threshold — tell the player visually but don't consume resources
            level.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + 2, target.getZ(),
                    5, 0.2, 0.2, 0.2, 0.02);
            return;
        }

        float dmg = 20.0f + ascension * 3.0f + (target.getMaxHealth() * 0.15f);
        boolean willKill = target.getHealth() <= dmg;

        target.hurt(level.damageSources().playerAttack(player), dmg);

        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 1, target.getZ(),
                5, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                16, 0.6, 0.8, 0.6, 0.12);
        level.playSound(null, player.getX(),player.getY(),player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);

        if (willKill) {
            // Heal 20% of player's max HP
            player.heal(player.getMaxHealth() * 0.20f);

            // Reset Collapse Strike CD
            player.getPersistentData().remove("CollapseStrikeCD");

            // Late upgrade: Haste II for 4 seconds
            if (ascension >= 7) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 1, false, false));
            }

            level.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 2, player.getZ(),
                    4, 0.4, 0.3, 0.4, 0.05);
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-350);
        setCooldown(player, "Ability6CD", 300);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 7 — VOID OVERDRIVE  (Ultimate, 2 min CD, 1000 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Activates Void Overdrive for 10 seconds (200 ticks).
     * While active (all handled in tickVoidOverdrive):
     *  - All abilities apply max Void Marks instantly.
     *  - Collapse Strike has no cooldown.
     *  - +50% attack speed (Haste III).
     *  - Void trails on movement.
     *  - Player takes +25% damage (LivingDamageEvent).
     * Late upgrade (ascension ≥ 7):
     *  - Kills extend duration by 3 seconds.
     *  - Lifesteal every second.
     */
    public static void voidWalkerAbilitySevenUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability7CD", 2400, 1000)) return;

        player.getPersistentData().putInt("VoidOverdriveActive", 200); // 10 seconds

        // Activation burst
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1, player.getZ(),
                6, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                40, 1.2, 1.5, 1.2, 0.15);
        level.playSound(null, player.getX(),player.getY(),player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.4f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-1000);
        setCooldown(player, "Ability7CD", 2400);
    }

    /**
     * Called externally when a kill happens (hook into LivingDeathEvent).
     * Extends Void Overdrive duration by 60 ticks (3 s) per kill at ascension ≥ 7.
     */
    public static void onKillDuringOverdrive(Player player) {
        CompoundTag nbt = player.getPersistentData();
        int ticks = nbt.getInt("VoidOverdriveActive");
        if (ticks <= 0) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        nbt.putInt("VoidOverdriveActive", ticks + 60);
    }

    // ─────────────────────────────────────────────────────────────────
    //  DAMAGE AMPLIFICATION — hook into your attack system
    // ─────────────────────────────────────────────────────────────────

    /**
     * Call this when the Void Walker player deals any attack damage to a target.
     * Returns the final amplified damage value accounting for:
     *  - Void Mark stacks (+8% each)
     *  - Void Anchor bonus (+20% if target is the Anchor)
     *  - Void Overdrive (marks already maxed, handled passively)
     */
    public static float amplifyDamage(Player player, LivingEntity target, float baseDamage) {
        CompoundTag tnbt = target.getPersistentData();
        int stacks       = tnbt.getInt("VoidMarkStacks");
        float multiplier = 1.0f + (stacks * VOID_MARK_DMG_BONUS);

        // +20% to Anchor target
        CompoundTag pnbt = player.getPersistentData();
        if (pnbt.contains("VoidAnchorId") && pnbt.getInt("VoidAnchorId") == target.getId()) {
            multiplier += 0.20f;
        }

        return baseDamage * multiplier;
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    private static boolean validateUse(Player player, String cdKey, int cdTicks, int essenceCost) {
        if (!SoulCore.getAspect(player).equals("Void Walker")) return false;
        if (SoulCore.getSoulEssence(player) < essenceCost) return false;
        CompoundTag nbt = player.getPersistentData();
        return !nbt.contains(cdKey) || nbt.getInt(cdKey) <= 0;
    }

    private static void setCooldown(Player player, String cdKey, int ticks) {
        player.getPersistentData().putInt(cdKey, ticks);
    }

    private static AABB centeredBox(Vec3 pos, double halfSize) {
        return new AABB(
                pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
    }

    /** Ray-march from player eye to find the first LivingEntity within maxDist. */
    private static LivingEntity findClosestInSight(Player player, ServerLevel level, double maxDist) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (double d = 1.0; d <= maxDist; d += 0.4) {
            Vec3 pos = eye.add(look.scale(d));
            AABB box = centeredBox(pos, 0.9);
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());
            if (!found.isEmpty()) return found.get(0);
        }
        return null;
    }

    /** Returns the player's current Anchor target, or null if dead/gone. */
    private static LivingEntity getAnchorTarget(Player player, ServerLevel level) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains("VoidAnchorId")) return null;
        LivingEntity anchor = findEntityById(level, nbt.getInt("VoidAnchorId"));
        if (anchor == null || !anchor.isAlive()) {
            nbt.remove("VoidAnchorId");
            nbt.remove("VoidAnchorCrit");
            return null;
        }
        return anchor;
    }

    /** Finds a LivingEntity by its numeric entity ID within the given level. */
    private static LivingEntity findEntityById(ServerLevel level, int id) {
        // Search a broad area — entity IDs are unique per level
        AABB searchBox = new AABB(-30000, -64, -30000, 30000, 320, 30000);
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.getId() == id);
        return all.isEmpty() ? null : all.get(0);
    }

    /** Teleports the player to just behind the target, facing them. */
    private static void teleportBehind(Player player, LivingEntity target, ServerLevel level) {
        Vec3 dir     = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 behind  = target.position().subtract(dir.scale(1.5));
        player.teleportTo(behind.x, behind.y, behind.z);
        level.sendParticles(ParticleTypes.PORTAL,
                behind.x, behind.y + 1, behind.z,
                8, 0.2, 0.5, 0.2, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }
}