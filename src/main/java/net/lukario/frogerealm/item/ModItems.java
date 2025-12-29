package net.lukario.frogerealm.item;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.item.custom.ModFoodProperties;
import net.lukario.frogerealm.item.custom.ShadowSword;
import net.lukario.frogerealm.item.custom.StrengthElixir;
import net.minecraft.world.food.FoodProperties;
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

    public static final RegistryObject<Item> DEPLETED_ENERGY_SHARD = ITEMS.register("depleted_energy_shard", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENERGY_SHARD = ITEMS.register("energy_shard",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .alwaysEdible()
                            .nutrition(4)
                            .saturationModifier(2.0f)
                            .fast()
                            // This is called inside the registry lambda, which is safer
                            .usingConvertsTo(DEPLETED_ENERGY_SHARD.get())
                            .build())
            )
    );

    public static final RegistryObject<Item> DIAMOND_CARROT = ITEMS.register("diamond_carrot", () -> new Item(new Item.Properties().food(ModFoodProperties.DIAMOND_CARROT)));

    public static final RegistryObject<Item> STRENGTH_ELIXIR = ITEMS.register("strength_elixir", () -> new Item(new Item.Properties().food(StrengthElixir.STRENGTH_ELIXIR)));



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
