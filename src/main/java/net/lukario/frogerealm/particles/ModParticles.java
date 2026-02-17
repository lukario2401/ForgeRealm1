package net.lukario.frogerealm.particles;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ForgeRealm.MOD_ID);

    public static final RegistryObject<SimpleParticleType> VOID_RIFT =
            PARTICLES.register("void_rift",
                    () -> new SimpleParticleType(true));
}
