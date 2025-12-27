package net.lukario.frogerealm.item;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForgeRealm.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SOULTAB = CREATIVE_MODE_TAB.register("soul_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SOULCOIN.get()))
                    .title(Component.translatable("creativetab.forgerealmmod.soul_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModItems.SOULESSENCE.get());
                        output.accept(ModItems.SOULCOIN.get());
                        output.accept(ModItems.SHADOW_SWORD.get());

                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> EXTRASTAB = CREATIVE_MODE_TAB.register("extras_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.COIN.get()))
                    .withTabsBefore(SOULTAB.getId())
                    .title(Component.translatable("creativetab.forgerealmmod.extras_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModItems.COIN.get());

                    })
                    .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
