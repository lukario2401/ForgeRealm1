package net.lukario.frogerealm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CShowHudOverlayPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side renderer for things abilities want to show in the middle of the screen
 * (countdown numbers, textures, ...). The server triggers it with CShowHudOverlayPacket.
 *
 * To add a new kind of overlay: add a value to CShowHudOverlayPacket.Mode,
 * a factory method there, and a case in render() below.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AbilityHudOverlay {

    // Countdown number look
    private static final float NUMBER_SCALE = 5.0f;     // 1.0 = normal chat text size
    private static final float NUMBER_POP_SCALE = 1.6f; // number starts this much bigger and shrinks in
    private static final float NUMBER_POP_TICKS = 5f;
    private static final float NUMBER_FADE_TICKS = 5f;

    // Texture look
    private static final int TEXTURE_DRAW_SIZE = 128;   // size on screen in GUI pixels (any square PNG works)
    private static final float TEXTURE_FADE_TICKS = 10f;

    // What is currently shown
    private static CShowHudOverlayPacket.Mode mode = null;
    private static int number;
    private static int color;
    private static ResourceLocation texture;
    private static int totalTicks;
    private static int ticksLeft;

    public static void show(CShowHudOverlayPacket.Mode newMode, int newNumber, int newColor, ResourceLocation newTexture, int durationTicks) {
        mode = newMode;
        number = newNumber;
        color = newColor;
        texture = newTexture;
        totalTicks = Math.max(1, durationTicks);
        ticksLeft = totalTicks;
    }

    public static void clear() {
        mode = null;
        ticksLeft = 0;
    }

    @SubscribeEvent
    public static void onAbilityHudClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mode == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) { // left the world
            clear();
            return;
        }
        if (minecraft.isPaused()) return;

        ticksLeft--;
        if (ticksLeft <= 0) clear();
    }

    // Called every frame by HudLayerHook.
    public static void render(GuiGraphics guiGraphics) {
        if (mode == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float age = (totalTicks - ticksLeft) + partialTick; // ticks since it appeared
        float remaining = ticksLeft - partialTick;          // ticks until it disappears

        switch (mode) {
            case NUMBER -> renderNumber(guiGraphics, minecraft.font, age, remaining);
            case TEXTURE -> renderTexture(guiGraphics, age, remaining);
        }
    }

    private static void renderNumber(GuiGraphics guiGraphics, Font font, float age, float remaining) {
        float alpha = Mth.clamp(remaining / NUMBER_FADE_TICKS, 0f, 1f);
        int a = (int) (alpha * 255);
        if (a < 8) return; // very low alpha is drawn fully opaque by Minecraft's font renderer

        float pop = easeOut(Mth.clamp(age / NUMBER_POP_TICKS, 0f, 1f));
        float scale = NUMBER_SCALE * Mth.lerp(pop, NUMBER_POP_SCALE, 1f);

        String text = String.valueOf(number);
        int argb = (a << 24) | (color & 0xFFFFFF);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight() / 2f, 0f);
        guiGraphics.pose().scale(scale, scale, 1f);
        // digits are 7px tall, so -3.5 centers them vertically
        guiGraphics.drawString(font, text, -font.width(text) / 2f, -3.5f, argb, true);
        guiGraphics.pose().popPose();
    }

    private static void renderTexture(GuiGraphics guiGraphics, float age, float remaining) {
        if (texture == null) return;

        float fadeIn = Mth.clamp(age / TEXTURE_FADE_TICKS, 0f, 1f);
        float fadeOut = Mth.clamp(remaining / TEXTURE_FADE_TICKS, 0f, 1f);
        float alpha = Math.min(fadeIn, fadeOut);
        if (alpha <= 0f) return;

        int size = TEXTURE_DRAW_SIZE;
        int x = (guiGraphics.guiWidth() - size) / 2;
        int y = (guiGraphics.guiHeight() - size) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1f, 1f, 1f, alpha);
        // UV 0..size out of size -> always draws the whole PNG, whatever its resolution
        guiGraphics.blit(texture, x, y, size, size, 0f, 0f, size, size, size, size);
        guiGraphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
}
