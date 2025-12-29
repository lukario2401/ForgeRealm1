package net.lukario.frogerealm.item.custom;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class StrengthElixir extends Item {


    public StrengthElixir(Properties pProperties) {
        super(pProperties);
    }

    public static final FoodProperties STRENGTH_ELIXIR =
            new FoodProperties.Builder()
                    .alwaysEdible()
                    .nutrition(0)
                    .saturationModifier(0f)
                    .build();

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.DRINK;
    }

    //gotta fix this ^^^
}
