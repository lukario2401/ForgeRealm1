package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WormOfTime {

    public static void wormOfTimeTimeAccumulation(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);


        if (!player.isShiftKeyDown()){
            ScreenImages.hide(player, "my_pic_3223123123123");
            ScreenImages.show(player, "my_pic_3223123123123", "perfect_ruby_material",
                    ScreenAnchor.TOP_LEFT, 10, 10, 32, 60);

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
