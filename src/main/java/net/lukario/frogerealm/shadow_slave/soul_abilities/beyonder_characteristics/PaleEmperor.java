package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.SpellFx;
import net.lukario.frogerealm.combat.Spells;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.root.Freeze;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PaleEmperor {

    private static final SlashFx CHAIN = SlashFx.line("slash/smooth")
            .color(0xC0F5C211).core(0xC0AADD20)              // outside and middle
            .width(0.12f).taper(SlashFx.Taper.UNIFORM);

    private static final ModelFx GATE = ModelFx.of("pale_emperor/underworld_gate")      // models/model_fx/rune.json
            .scale(2f)                  // 1 = Blockbench size (16 px = 1 block)
            .pivot(8, 0, 8)               // pixels, Blockbench coordinates
            .glow()                       // full bright, visible at night
            .aura(0xFF0000, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
            .lifetime(500)                 // ticks (20 = 1 second)
            .fade(5, 10);
    private static final ModelFx GATE_VOID = ModelFx.of("pale_emperor/underworld_void")      // models/model_fx/rune.json
            .scale(2f)                  // 1 = Blockbench size (16 px = 1 block)
            .pivot(8, 0, 8)               // pixels, Blockbench coordinates
            .glow()                       // full bright, visible at night
            .aura(0xFF0000, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
            .lifetime(500)                 // ticks (20 = 1 second)
            .fade(5, 10);
    private static final ModelFx GATE_DOOR_LEFT = ModelFx.of("pale_emperor/underworld_gate_door_left")      // models/model_fx/rune.json
            .scale(2f)                  // 1 = Blockbench size (16 px = 1 block)
            .pivot(8, 0, 8)               // pixels, Blockbench coordinates
            .glow()                       // full bright, visible at night
            .aura(0xFF0000, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
            .lifetime(500)                 // ticks (20 = 1 second)
            .fade(5, 10);
    private static final ModelFx GATE_DOOR_RIGHT = ModelFx.of("pale_emperor/underworld_gate_door_right")      // models/model_fx/rune.json
            .scale(2f)                  // 1 = Blockbench size (16 px = 1 block)
            .pivot(8, 0, 8)               // pixels, Blockbench coordinates
            .glow()                       // full bright, visible at night
            .aura(0xFF0000, 0.1f, 3)    // glowing outline: ARGB color, thickness in blocks, softness
            .lifetime(500)                 // ticks (20 = 1 second)
            .fade(5, 10);

    public static void paleEmperorRoot(Player player, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 1250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        int duration = 80;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-1250);

        LivingEntity enemy = Spells.aimEnemy(player,sl,32);
        Root.apply(enemy,duration, RootRestriction.EVERYTHING);

        SpellFx.tether(sl, CHAIN, player, enemy, duration);
    }


    public static void paleEmperorDoor(Player player, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 11250) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-11250);

        Vec3 position = player.position().add(0,2,0);

        ModelFx gate = GATE;
        ModelFx gate_void = GATE_VOID;
        ModelFx gate_door_left = GATE_DOOR_LEFT.key(100, ModelFx.pose().up(-6), ModelFx.Ease.IN);
        ModelFx gate_door_right = GATE_DOOR_RIGHT.key(100, ModelFx.pose().up(-6), ModelFx.Ease.IN);

        for (int i = 100; i<=500; i+=10){
            Later.run(sl, i, () -> pale_emp_gate_pull(sl, player, position));
        }

        ParticleShapes.model(sl, gate, position, player.getYRot(), 0f, 0f);
        ParticleShapes.model(sl, gate_void, position, player.getYRot(), 0f, 0f);
        ParticleShapes.model(sl, gate_door_left, position, player.getYRot(), 0f, 0f);
        ParticleShapes.model(sl, gate_door_right, position, player.getYRot(), 0f, 0f);


        ///particle forgerealmmod:model{model:"forgerealmmod:model_fx/pale_emperor/gate",glow:1b,lifetime:100,keys:[{tick:20,yaw:90f,ease:"in"}]} ~ ~2 ~ 0 0 0 0 1


    }

    private static void pale_emp_gate_pull(ServerLevel sl, Player player, Vec3 position){
        List<LivingEntity> entities = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position,position).inflate(12),
                e -> e != player
        );

        for (LivingEntity livingEntity : entities){
            Spells.pull(livingEntity, position,1,0.1);
        }
        List<LivingEntity> entitiess = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position,position).inflate(6),
                e -> e != player
        );

        for (LivingEntity livingEntity : entitiess){
            Spells.pull(livingEntity, position,1,0.1);
        }

        List<LivingEntity> enemies = sl.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position,position).inflate(2),
                e -> e != player
        );

        for (LivingEntity livingEntity : enemies){
            livingEntity.hurt(sl.damageSources().playerAttack(player),4);
        }
    }

    private static boolean canUseCharacteristic(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Pale Emperor");
    }
}
