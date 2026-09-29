package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.menu.AbilityMenu;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class HandOfOrder {

    // Ability 2 - Decree (menu with 3 choices)
    private static final int DECREE_COST = 1250;
    private static final float DECREE_EXPLOSION_POWER = 4.0f;   // TNT is 4
    private static final int DECREE_SPEED_TICKS = 200;          // 10 seconds
    private static final int DECREE_SPEED_LEVEL = 2;            // 0 = Speed I, 2 = Speed III
    private static final float DECREE_JUDGEMENT_CHANCE = 0.30f;
    private static final float DECREE_JUDGEMENT_DAMAGE = 50f;
    private static final double DECREE_JUDGEMENT_RANGE = 16.0;

    // Icons are in assets/forgerealmmod/textures/gui/ability_menus/hand_of_order/
    // x, y = icon center relative to the middle of the screen -> triangle
    private static final AbilityMenu DECREE_MENU = AbilityMenu.create("hand_of_order_decree")
            .title("Decree of Order")
            .option("ability_menus/hand_of_order/explosion", "Detonate",    0, -46, HandOfOrder::decreeExplosion)
            .option("ability_menus/hand_of_order/speed",     "Haste",     -46,  30, HandOfOrder::decreeSpeed)
            .option("ability_menus/hand_of_order/judgement", "Judgement",  46,  30, HandOfOrder::decreeJudgement);


    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HandOfOrderEvents{


    @SubscribeEvent
    public static void onHandOfOrderEventTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel sl)) return;

        if (player.getPersistentData().contains("Hand_Of_order_damage_boost")) {

            int duration = player.getPersistentData().getInt("Hand_Of_order_damage_boost");
            if (duration > 0) {
                player.sendSystemMessage(Component.literal(""+duration));
                player.getPersistentData().putInt("Hand_Of_order_damage_boost", duration - 1);
            }
        }

        if (player.getPersistentData().contains("Hand_Of_order_defense_boost")) {
            int duration = player.getPersistentData().getInt("Hand_Of_order_defense_boost");
            if (duration > 0) {
                player.sendSystemMessage(Component.literal(""+duration));
                player.getPersistentData().putInt("Hand_Of_order_defense_boost", duration - 1);
            }
        }



        if (!SoulCore.getAspect(player).equals("Hand Of Order")) return;

    }
}
    //Ability 1
    public static void handOfOrderBuff(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);


        if (player.isShiftKeyDown()){
            int x = 10;
            if (player.getPersistentData().getInt("Hand_Of_order_damage_boost")>0){
                x+=32;
            }
            ScreenImages.hide(player, "my_pic_3223123123123");
            ScreenImages.show(player, "my_pic_3223123123123", "amber_material",
                    ScreenAnchor.TOP_LEFT, x, 10, 32, 70);


            player.getPersistentData().putInt("Hand_Of_order_defense_boost",  60);
        }else{
            int x = 10;
            if (player.getPersistentData().getInt("Hand_Of_order_defense_boost")>0){
                x+=32;
            }
            ScreenImages.hide(player, "my_pic_1123123123123");
            ScreenImages.show(player, "my_pic_1123123123123", "perfect_ruby_gemstone",
                    ScreenAnchor.TOP_LEFT, x, 10, 32, 70);

            player.getPersistentData().putInt("Hand_Of_order_damage_boost",  60);
        }
    }




    //Ability 2
    public static void handOfOrderDecree(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < DECREE_COST) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        // essence is only spent once an option is picked (Esc = free)
        DECREE_MENU.open(player);
    }

    // Top: explosion around the caster. Caster is the explosion's source, so it isn't hurt;
    // ExplosionInteraction.NONE means no blocks break.
    private static void decreeExplosion(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, DECREE_COST)) return;
        sl.explode(player, player.getX(), player.getY() + 0.5, player.getZ(),
                DECREE_EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
    }

    // Bottom left: speed boost
    private static void decreeSpeed(ServerPlayer player, ServerLevel sl) {
        if (!payEssence(player, DECREE_COST)) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DECREE_SPEED_TICKS, DECREE_SPEED_LEVEL));
        sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 20, 0.4, 0.05, 0.4, 0.05);
        sl.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // Bottom right: 30% chance of a huge hit on whoever is in front
    private static void decreeJudgement(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = findTargetInFront(player, sl, DECREE_JUDGEMENT_RANGE);
        if (target == null) {
            player.displayClientMessage(Component.literal("No one stands before you."), true);
            return; // no essence spent
        }
        if (!payEssence(player, DECREE_COST)) return;

        if (player.getRandom().nextFloat() < DECREE_JUDGEMENT_CHANCE) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), DECREE_JUDGEMENT_DAMAGE);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.6, 0.4, 0.3);
            sl.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.6f);
            player.displayClientMessage(Component.literal("Judgement!"), true);
        } else {
            sl.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            sl.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
            player.displayClientMessage(Component.literal("The judgement failed."), true);
        }
    }

    // First living entity the player is looking at, stopped by blocks
    private static LivingEntity findTargetInFront(Player player, ServerLevel sl, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));

        // don't hit through walls
        end = sl.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        AABB searchBox = new AABB(start, end).inflate(1.0);
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
            var hit = entity.getBoundingBox().inflate(0.3).clip(start, end);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = entity;
                }
            }
        }
        return closest;
    }

    private static boolean payEssence(Player player, float cost) {
        if (SoulCore.getSoulEssence(player) < cost) {
            player.displayClientMessage(Component.literal("Not enough soul essence."), true);
            return false;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - cost);
        return true;
    }

    private static boolean canUseCharacteristic(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Hand Of Order");
    }
}
