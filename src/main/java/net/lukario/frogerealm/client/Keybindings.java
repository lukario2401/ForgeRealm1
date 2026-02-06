package net.lukario.frogerealm.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class Keybindings {
    public static final Keybindings INSTANCE = new Keybindings();

    private Keybindings() {}

    private static final String CATEGORY = "key.categories." + ForgeRealm.MOD_ID;

    public final KeyMapping exampleKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".example_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_P, -1),
            CATEGORY
    );

    public final KeyMapping examplePacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".example_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_V, -1),
            CATEGORY
    );

    public final KeyMapping abilityOnePacketKey = new KeyMapping(
            "key." + ForgeRealm.MOD_ID + ".ability_one_packet_key",
            KeyConflictContext.IN_GAME,
            InputConstants.getKey(InputConstants.KEY_G, -1),
            CATEGORY
    );
}