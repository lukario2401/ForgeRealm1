package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class HandOfOrder {


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




    private static boolean canUseCharacteristic(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Hand Of Order");
    }
}
