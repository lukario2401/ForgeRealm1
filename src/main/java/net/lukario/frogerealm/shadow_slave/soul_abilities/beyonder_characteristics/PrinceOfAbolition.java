package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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

    public static void princeOfAbolitionBribe(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        LivingEntity livingEntity = rayCast(player,sl);
        if (livingEntity==null)return;

        if (player.isShiftKeyDown()){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1));
        }else{
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1));
        }
    }

    public static void princeOfAbolitionCorrosion(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);
        LivingEntity livingEntity = rayCast(player,sl);
        if (livingEntity==null)return;

        if (player.isShiftKeyDown()){
            if (livingEntity instanceof Player){
                SoulCore.setCorruption((Player) livingEntity,SoulCore.getCorruption((Player) livingEntity)+10);
            }
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

    private static boolean canUseClass(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Prince Of Abolition");
    }

}
