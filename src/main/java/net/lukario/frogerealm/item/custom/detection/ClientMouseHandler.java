package net.lukario.frogerealm.item.custom.detection;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "forgerealmmod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientMouseHandler {

    @SubscribeEvent
    public static void onMouseClickPre(InputEvent.MouseButton.Pre event) {
        // Check if the button is LEFT click
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
            if (pressed) {
                // --- LEFT CLICK PRESSED ---
                System.out.println("Left click detected!");
                // Here you can call your firework method, or do anything client-side
            }
        }

        // Optional: detect RIGHT click
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
            if (pressed) {
                System.out.println("Right click detected!");
            }
        }
    }
}
