package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.AbilityHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client packet that tells the client to draw something in the middle of the screen.
 * The actual drawing happens in {@link AbilityHudOverlay}.
 *
 * Usage from any ability (server side):
 *   PacketHandler.sendToPlayer(CShowHudOverlayPacket.number(3, 0xFFFFFF, 20), serverPlayer);
 *   PacketHandler.sendToPlayer(CShowHudOverlayPacket.texture(MY_TEXTURE, 100), serverPlayer);
 */
public class CShowHudOverlayPacket {

    public enum Mode {
        NUMBER,   // draws a big number (countdowns)
        TEXTURE   // draws a texture (any PNG in assets/forgerealmmod/textures/...)
    }

    private final Mode mode;
    private final int number;
    private final int color;
    private final ResourceLocation texture;
    private final int durationTicks;

    private CShowHudOverlayPacket(Mode mode, int number, int color, ResourceLocation texture, int durationTicks) {
        this.mode = mode;
        this.number = number;
        this.color = color;
        this.texture = texture;
        this.durationTicks = durationTicks;
    }

    /** Big number in the middle of the screen. color is 0xRRGGBB. */
    public static CShowHudOverlayPacket number(int number, int color, int durationTicks) {
        return new CShowHudOverlayPacket(Mode.NUMBER, number, color, null, durationTicks);
    }

    /** Texture in the middle of the screen, e.g. forgerealmmod:textures/gui/something.png */
    public static CShowHudOverlayPacket texture(ResourceLocation texture, int durationTicks) {
        return new CShowHudOverlayPacket(Mode.TEXTURE, 0, 0xFFFFFF, texture, durationTicks);
    }

    public CShowHudOverlayPacket(FriendlyByteBuf buffer) {
        this.mode = buffer.readEnum(Mode.class);
        this.number = buffer.readInt();
        this.color = buffer.readInt();
        this.texture = mode == Mode.TEXTURE ? buffer.readResourceLocation() : null;
        this.durationTicks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(mode);
        buffer.writeInt(number);
        buffer.writeInt(color);
        if (mode == Mode.TEXTURE) {
            buffer.writeResourceLocation(texture);
        }
        buffer.writeInt(durationTicks);
    }

    public void handle(CustomPayloadEvent.Context context) {
        // Only the client can draw; AbilityHudOverlay is client-only code.
        if (FMLEnvironment.dist.isClient()) {
            AbilityHudOverlay.show(mode, number, color, texture, durationTicks);
        }
    }
}
