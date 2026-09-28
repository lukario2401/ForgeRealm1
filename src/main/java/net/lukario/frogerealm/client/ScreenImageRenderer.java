package net.lukario.frogerealm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client side of ScreenImages: keeps the list of pictures on screen, counts down their time
 * and draws them (called every frame by HudLayerHook). Pictures are drawn in the order they
 * were first shown, so later ones end up on top.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScreenImageRenderer {

    private static final int FADE_TICKS = 5;

    private static class Image {
        ResourceLocation texture;
        ScreenAnchor anchor;
        int x, y, width, height;
        int age;       // ticks since shown
        int ticksLeft; // -1 = forever
    }

    private static final Map<String, Image> IMAGES = new LinkedHashMap<>();

    public static void show(String id, ResourceLocation texture, ScreenAnchor anchor,
                            int x, int y, int width, int height, int durationTicks) {
        Image old = IMAGES.get(id);
        Image image = new Image();
        image.texture = texture;
        image.anchor = anchor;
        image.x = x;
        image.y = y;
        image.width = width;
        image.height = height;
        // replacing a picture that's already fully visible shouldn't make it flash
        image.age = (old != null && old.age >= FADE_TICKS) ? FADE_TICKS : 0;
        image.ticksLeft = durationTicks < 0 ? -1 : Math.max(1, durationTicks);
        IMAGES.put(id, image);
    }

    /** Fade out after delayTicks. An id ending with * matches every id that starts with the rest. */
    public static void hide(String id, int delayTicks) {
        int ticks = Math.max(0, delayTicks) + FADE_TICKS;
        boolean wildcard = id.endsWith("*");
        String prefix = wildcard ? id.substring(0, id.length() - 1) : id;

        for (Map.Entry<String, Image> entry : IMAGES.entrySet()) {
            boolean matches = wildcard ? entry.getKey().startsWith(prefix) : entry.getKey().equals(id);
            if (!matches) continue;
            Image image = entry.getValue();
            if (image.ticksLeft < 0 || image.ticksLeft > ticks) {
                image.ticksLeft = ticks;
            }
        }
    }

    public static void clear() {
        IMAGES.clear();
    }

    @SubscribeEvent
    public static void onScreenImageClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (IMAGES.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) { // left the world
            clear();
            return;
        }
        if (minecraft.isPaused()) return;

        Iterator<Image> it = IMAGES.values().iterator();
        while (it.hasNext()) {
            Image image = it.next();
            image.age++;
            if (image.ticksLeft > 0) {
                image.ticksLeft--;
                if (image.ticksLeft == 0) it.remove();
            }
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        if (IMAGES.isEmpty()) return;

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (Image image : IMAGES.values()) {
            float alpha = Mth.clamp((image.age + partialTick) / FADE_TICKS, 0f, 1f);
            if (image.ticksLeft >= 0) {
                alpha = Math.min(alpha, Mth.clamp((image.ticksLeft - partialTick) / FADE_TICKS, 0f, 1f));
            }
            if (alpha <= 0f) continue;

            int drawX = Math.round(image.anchor.fractionX * (screenWidth - image.width)) + image.x;
            int drawY = Math.round(image.anchor.fractionY * (screenHeight - image.height)) + image.y;

            guiGraphics.setColor(1f, 1f, 1f, alpha);
            // UV 0..w out of w -> always draws the whole PNG, whatever its resolution
            guiGraphics.blit(image.texture, drawX, drawY, image.width, image.height,
                    0f, 0f, image.width, image.height, image.width, image.height);
        }

        guiGraphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
