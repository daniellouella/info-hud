package net.louella.gui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class InfoHUDDisplay implements ClientModInitializer {
    private static final int TEXT_PADDING = 5, PADDING = 3, SHADOW_OFFSET = 1;
    private static final Identifier HUD_ID = Identifier.of("louella", "infohud");

    private boolean showCoordinates = true, showFacing = true, showBiome = true, showDayCount = true, showFPS = false;

    // Keybinds
    private KeyBinding toggleCoordinatesKey;
    private KeyBinding toggleFacingKey;
    private KeyBinding toggleBiomeKey;
    private KeyBinding toggleDayKey;
    private KeyBinding toggleFpsKey;

    @Override
    public void onInitializeClient() {
        toggleCoordinatesKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.infohud.toggle_coordinates", GLFW.GLFW_KEY_K, "key.categories.infohud")
        );
        toggleFacingKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.infohud.toggle_facing", GLFW.GLFW_KEY_V, "key.categories.infohud")
        );
        toggleBiomeKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.infohud.toggle_biome", GLFW.GLFW_KEY_B, "key.categories.infohud")
        );
        toggleDayKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.infohud.toggle_day", GLFW.GLFW_KEY_N, "key.categories.infohud")
        );
        toggleFpsKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.infohud.toggle_fps", GLFW.GLFW_KEY_O, "key.categories.infohud")
        );

        HudElementRegistry.addLast(HUD_ID, new HudElement() {
            @Override
            public void render(DrawContext ctx, RenderTickCounter tickCounter) {
                drawInfoHud(ctx);
            }
        });
    }

    private void drawInfoHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.inGameHud.getDebugHud().shouldShowDebugHud()) return;
        if (client.currentScreen != null && !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen))
            return;

        handleKeyToggles();

        List<String> leftLines = new ArrayList<>();
        if (showCoordinates) leftLines.add(Text.translatable(
                "hud.infohud.position",
                MathHelper.floor(player.getX()),
                MathHelper.floor(player.getY()),
                MathHelper.floor(player.getZ())
        ).getString());

        if (showFacing) leftLines.add(Text.translatable(
                "hud.infohud.facing",
                getFacingDirection(player.getYaw())
        ).getString());

        if (showBiome) leftLines.add(Text.translatable(
                "hud.infohud.biome",
                getBiomeName(player)
        ).getString());

        if (showDayCount) leftLines.add(Text.translatable(
                "hud.infohud.day",
                getDayCount(client)
        ).getString());

        List<String> rightLines = new ArrayList<>();
        if (showFPS) rightLines.add(Text.translatable(
                "hud.infohud.fps",
                client.getCurrentFps()
        ).getString());

        renderLines(ctx, client, leftLines, true);
        renderLines(ctx, client, rightLines, false);
    }

    private void handleKeyToggles() {
        if (toggleCoordinatesKey.wasPressed()) showCoordinates = !showCoordinates;
        if (toggleFacingKey.wasPressed())      showFacing = !showFacing;
        if (toggleBiomeKey.wasPressed())       showBiome = !showBiome;
        if (toggleDayKey.wasPressed())         showDayCount = !showDayCount;
        if (toggleFpsKey.wasPressed())         showFPS = !showFPS;
    }

    private void renderLines(DrawContext ctx, MinecraftClient client, List<String> lines, boolean left) {
        if (lines.isEmpty()) return;
        int fh = client.textRenderer.fontHeight;
        int w = lines.stream().mapToInt(client.textRenderer::getWidth).max().orElse(0);
        int h = lines.size() * fh + PADDING * 2;
        int x0 = left ? 0 : client.getWindow().getScaledWidth() - w - TEXT_PADDING * 2;
        int y0 = client.getWindow().getScaledHeight() / 10;
        ctx.fill(x0, y0 - PADDING, x0 + w + TEXT_PADDING * 2, y0 + h - PADDING - SHADOW_OFFSET,
                MathHelper.floor(client.options.getTextBackgroundOpacity().getValue().floatValue() * 0.9F * 255) << 24);
        int y = y0;
        for (String line : lines) {
            ctx.drawText(client.textRenderer, line, x0 + TEXT_PADDING, y, 0xFFFFFFFF, true);
            y += fh;
        }
    }

    private String getFacingDirection(float yaw) {
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 45 || yaw >= 315) return Text.translatable("hud.infohud.south").getString();
        if (yaw < 135) return Text.translatable("hud.infohud.west").getString();
        if (yaw < 225) return Text.translatable("hud.infohud.north").getString();
        return Text.translatable("hud.infohud.east").getString();
    }

    private String getBiomeName(PlayerEntity player) {
        Identifier id = MinecraftClient.getInstance().world.getBiome(player.getBlockPos())
                .getKey().get().getValue();
        String raw = id.toString().replaceFirst("^minecraft:", "");
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split("_")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private int getDayCount(MinecraftClient client) {
        return (int) (client.world.getTimeOfDay() / 24000L);
    }
}