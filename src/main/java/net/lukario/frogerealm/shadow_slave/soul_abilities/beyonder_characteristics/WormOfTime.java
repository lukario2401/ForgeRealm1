package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.combat.Spells;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WormOfTime {

    public static void wormOfTimeAbility1(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);


        if (!player.isShiftKeyDown()){

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,24);

            int healthStealAmount = 15;

            if (livingEntity.getHealth()>healthStealAmount){
                livingEntity.setHealth(livingEntity.getHealth()-15);
                player.setHealth(player.getHealth()+15);
            }else{
                player.setHealth((player.getHealth()+livingEntity.getHealth()));
                livingEntity.kill();
            }


        }else{
            ScreenImages.hide(player, "my_pic_1123123123123");
            ScreenImages.show(player, "my_pic_1123123123123", "amber_gemstone",
                    ScreenAnchor.TOP_LEFT, 10, 10, 32, 60);
        }
    }



    private static boolean canUseCharacteristic(Player player, boolean bypassClassCheck){
        if (bypassClassCheck) return true;
        return "Worm Of Time".equals(SoulCore.getAspect(player));
    }
}
