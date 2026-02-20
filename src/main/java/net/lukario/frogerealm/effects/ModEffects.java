package net.lukario.frogerealm.effects;

import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "frogerealm");

    // This is your actual registry object
    public static final RegistryObject<MobEffect> SHADOW_MARK = MOB_EFFECTS.register("shadow_mark",
            () -> new ShadowSlaveAspect.ShadowMarkEffect());

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}