package net.lukario.frogerealm.item;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.item.custom.ShadowSword;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ForgeRealm.MOD_ID);

    public static final RegistryObject<Item> SOULESSENCE = ITEMS.register("soulessence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COIN = ITEMS.register("coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOULCOIN = ITEMS.register("soulcoin", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SHADOW_SWORD = ITEMS.register(
            "shadow_sword",
            () -> new ShadowSword(
                    Tiers.IRON,
                    new Item.Properties().attributes(
                            SwordItem.createAttributes(
                                    Tiers.IRON,
                                    12,      // extra damage
                                    -1.2F   // attack speed
                            )
                    )
            )
    );

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
