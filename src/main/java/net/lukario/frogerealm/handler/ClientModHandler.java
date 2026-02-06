package net.lukario.frogerealm.handler;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.client.Keybindings;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModHandler {
//    @SubscribeEvent
//    public static void clientSetup(FMLClientSetupEvent event) {
//        event.enqueueWork(() -> {
//            MenuScreens.register(MenuInit.EXAMPLE_MENU.get(), ExampleMenuScreen::new);
//            MenuScreens.register(MenuInit.EXAMPLE_ENERGY_GENERATOR_MENU.get(), ExampleEnergyGeneratorScreen::new);
//            MenuScreens.register(MenuInit.EXAMPLE_SIDED_INVENTORY_MENU.get(), ExampleSidedInventoryScreen::new);
//            MenuScreens.register(MenuInit.EXAMPLE_FLUID_MENU.get(), ExampleFluidScreen::new);
//            MenuScreens.register(MenuInit.EXAMPLE_BER_MENU.get(), ExampleBERScreen::new);
//            MenuScreens.register(MenuInit.EXAMPLE_FLUID_BER_MENU.get(), ExampleFluidBERScreen::new);
//        });
//    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybindings.INSTANCE.exampleKey);
        event.register(Keybindings.INSTANCE.examplePacketKey);
    }

}