package io.github.noeppi_noeppi.mods.bongo.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.noeppi_noeppi.libx.render.RenderHelper;
import io.github.noeppi_noeppi.mods.bongo.Bongo;
import io.github.noeppi_noeppi.mods.bongo.BongoMod;
import io.github.noeppi_noeppi.mods.bongo.Keybinds;
import io.github.noeppi_noeppi.mods.bongo.config.ClientConfig;
import io.github.noeppi_noeppi.mods.bongo.data.Team;
import io.github.noeppi_noeppi.mods.bongo.data.WinCondition;
import io.github.noeppi_noeppi.mods.bongo.task.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.DyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class RenderOverlay {

    public static final ResourceLocation BINGO_TEXTURE = new ResourceLocation(BongoMod.getInstance().modid, "textures/overlay/bingo.png");
    public static final ResourceLocation BINGO_SLOTS_TEXTURE = new ResourceLocation(BongoMod.getInstance().modid, "textures/overlay/bingo_slots.png");
    public static final ResourceLocation BEACON_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/beacon.png");
    public static final ResourceLocation COMPLETED_TEXTURE = new ResourceLocation(BongoMod.getInstance().modid, "textures/overlay/completed_rects.png");

    @SubscribeEvent
    public void renderChat(RenderGameOverlayEvent.Pre event) {
        // When some elements the bingo card occasionally there are render problems. So we
        // just hide man GUI parts when the bingo card is enlarged.
        if (Keybinds.BIG_OVERLAY.isKeyDown()) {
            if (Minecraft.getInstance().world != null) {
                Bongo bongo = Bongo.get(Minecraft.getInstance().world);
                if (bongo.active()) {
                    RenderGameOverlayEvent.ElementType type = event.getType();
                    if (type == RenderGameOverlayEvent.ElementType.CHAT
                            || type == RenderGameOverlayEvent.ElementType.DEBUG
                            || type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                            || type == RenderGameOverlayEvent.ElementType.HEALTH
                            || type == RenderGameOverlayEvent.ElementType.HEALTHMOUNT
                            || type == RenderGameOverlayEvent.ElementType.BOSSHEALTH
                            || type == RenderGameOverlayEvent.ElementType.BOSSINFO
                            || type == RenderGameOverlayEvent.ElementType.ARMOR
                            || type == RenderGameOverlayEvent.ElementType.JUMPBAR
                            || type == RenderGameOverlayEvent.ElementType.FOOD
                            || type == RenderGameOverlayEvent.ElementType.FPS_GRAPH) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        MatrixStack matrixStack = event.getMatrixStack();
        IRenderTypeBuffer buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        Minecraft mc = Minecraft.getInstance();
        if (mc.world != null && mc.player != null && mc.currentScreen == null && (!mc.gameSettings.showDebugInfo || Keybinds.BIG_OVERLAY.isKeyDown()) && event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Bongo bongo = Bongo.get(mc.world);
            Team team = bongo.getTeam(mc.player);
            if (bongo.active() && (!bongo.running() || team != null)) {
                double padding = 5;
                double px = mc.getMainWindow().getScaledHeight() / 3.5d;
                double x = padding;
                double y = padding;
                boolean itemNames = false;
                if (Keybinds.BIG_OVERLAY.isKeyDown()) {
                    px = Math.min(mc.getMainWindow().getScaledWidth() - (2 * padding), mc.getMainWindow().getScaledHeight() - (6 * padding));
                    x = (mc.getMainWindow().getScaledWidth() - px) / 2;
                    y = ((mc.getMainWindow().getScaledHeight() - px) / 2) - (2 * padding);
                    itemNames = true;
                } else {
                    float scale = (float) (double) ClientConfig.bongoMapScaleFactor.get();
                    matrixStack.scale(scale, scale, 1);
                }

                matrixStack.push();
                matrixStack.translate(x, y, 0);
                matrixStack.scale((float) px / 138, (float) px / 138, 1);

                mc.textureManager.bindTexture(BINGO_TEXTURE);
                AbstractGui.blit(matrixStack, 0, 0, 0, 0, 138, 138, 256, 256);

                matrixStack.translate(2, 2, 10);

                for (int ySlot = 0; ySlot < 5; ySlot++) {
                    for (int xSlot = 0; xSlot < 5; xSlot++) {
                        int slot = xSlot + (5 * ySlot);
                        List<Integer> colorCodes = new ArrayList<>();
                        for (DyeColor dc : DyeColor.values()) {
                            if (bongo.getTeam(dc).completed(slot))
                                colorCodes.add(dc.getColorValue());
                        }

                        matrixStack.push();
                        matrixStack.translate(xSlot * 27, ySlot * 27, 0);

                        renderCompleted(matrixStack, buffer, colorCodes);

                        matrixStack.pop();
                    }
                }

                matrixStack.translate(-2, -2, 10);

                mc.textureManager.bindTexture(BINGO_SLOTS_TEXTURE);
                for (int ySlot = 0; ySlot < 5; ySlot++) {
                    for (int xSlot = 0; xSlot < 5; xSlot++) {
                        int slot = xSlot + (5 * ySlot);

                        matrixStack.push();
                        matrixStack.translate(6 + (27 * xSlot), 6 + (27 * ySlot), 0);

                        bongo.task(slot).renderSlot(mc, matrixStack, buffer);

                        matrixStack.pop();
                    }
                }

                matrixStack.translate(7, 7, 0);

                for (int ySlot = 0; ySlot < 5; ySlot++) {
                    for (int xSlot = 0; xSlot < 5; xSlot++) {
                        int slot = xSlot + (5 * ySlot);
                        Task task = bongo.task(slot);

                        matrixStack.push();
                        matrixStack.translate(xSlot * 27, ySlot * 27, 0);

                        task.renderSlotContent(mc, matrixStack, buffer, itemNames);

                        matrixStack.pop();

                        if (itemNames) {
                            matrixStack.push();
                            matrixStack.translate((xSlot * 27) + 8, (ySlot * 27) + 4, 700);
                            matrixStack.scale(0.3f, 0.3f, 1);

                            RenderHelper.renderText(task.getTranslatedName(), matrixStack, buffer);

                            matrixStack.translate(0, 8 / 0.3, 10);

                            RenderHelper.renderText(task.getTranslatedContentName(), matrixStack, buffer);

                            matrixStack.pop();
                        }

                        if (team != null) {
                            if (team.completed(slot)) {
                                matrixStack.push();
                                matrixStack.translate(0, 0, 800);
                                mc.getTextureManager().bindTexture(BEACON_TEXTURE);
                                AbstractGui.blit(matrixStack, xSlot * 27, ySlot * 27, 90, 222, 16, 16, 256, 256);
                                matrixStack.pop();
                            } else if (team.locked(slot)) {
                                matrixStack.push();
                                matrixStack.translate(xSlot * 27, ySlot * 27, 800);
                                mc.getTextureManager().bindTexture(BEACON_TEXTURE);
                                matrixStack.scale(16f/15f, 16f/15f, 16f/15f);
                                AbstractGui.blit(matrixStack, 0, 0, 113, 222, 15, 15, 256, 256);
                                matrixStack.pop();
                            }
                        }
                    }
                }

                if (!itemNames) {
                    List<String> lines = new ArrayList<>();

                    if (bongo.getSettings().winCondition != WinCondition.DEFAULT) {
                        lines.add(I18n.format("bongo.wc." + bongo.getSettings().winCondition.id));
                    }

                    if (bongo.getSettings().teleportsPerTeam != 0 && !bongo.won()) {
                        boolean hasRunningTeam = team != null && bongo.running();
                        int tpLeft = hasRunningTeam ? team.teleportsLeft() : bongo.getSettings().teleportsPerTeam;
                        String tpLeftStr = tpLeft < 0 ? I18n.format("bongo.infinite") : Integer.toString(tpLeft);
                        lines.add(I18n.format(hasRunningTeam ? "bongo.tp_left": "bongo.tp_team", tpLeftStr));
                    }

                    if (bongo.running() || bongo.won()) {
                        long millis;
                        if (bongo.won()) {
                            millis = bongo.ranUntil() - bongo.runningSince();
                        } else {
                            millis = System.currentTimeMillis() - bongo.runningSince();
                        }
                        int decimal = (int) ((millis / 100) % 10);
                        int sec = (int) ((millis / 1000) % 60);
                        int min = (int) ((millis / 60000) % 60);
                        int hour = (int) millis / 3600000;
                        if (hour == 0) {
                            lines.add(I18n.format("bongo.timer") + min + ":" + sec + "." + decimal);
                        } else {
                            lines.add(I18n.format("bongo.timer") + hour + ":" + min + ":" + sec + "." + decimal);
                        }
                    }

                    if (!lines.isEmpty()) {
                        matrixStack.translate(0, 133, 800);
                        matrixStack.scale(1.3f, 1.3f, 1);

                        for (int i = 0; i < lines.size(); i++) {
                            mc.fontRenderer.drawString(matrixStack, lines.get(i), 0, (mc.fontRenderer.FONT_HEIGHT + 1) * i, 0xFFFFFF);
                        }
                    }
                }

                matrixStack.pop();
            }
        }
    }

    public void renderCompleted(MatrixStack matrixStack, IRenderTypeBuffer buffer, List<Integer> colorCodes) {
        if (colorCodes.isEmpty())
            return;
        int[][] rects;
        if (colorCodes.size() <= 1) {
            rects = RECTS_1;
        } else if (colorCodes.size() <= 2) {
            rects = RECTS_2;
        } else if (colorCodes.size() <= 4) {
            rects = RECTS_4;
        } else if (colorCodes.size() <= 6) {
            rects = RECTS_6;
        } else if (colorCodes.size() <= 8) {
            rects = RECTS_8;
        } else {
            rects = RECTS_16;
        }

        matrixStack.push();
        matrixStack.scale(26 / 24f, 26 / 24f, 0);
        Minecraft.getInstance().getTextureManager().bindTexture(COMPLETED_TEXTURE);

        for (int rect = 0; rect < rects.length; rect++) {
            if (rect >= colorCodes.size())
                break;

            int color = colorCodes.get(rect);
            float colorR = ((color >> 16) & 0xFF) / 255f;
            float colorG = ((color >> 8) & 0xFF) / 255f;
            float colorB = ((color) & 0xFF) / 255f;
            //noinspection deprecation
            GlStateManager.color4f(colorR, colorG, colorB, 1);
            AbstractGui.blit(matrixStack, rects[rect][0], rects[rect][1], 0, 0, rects[rect][2] - rects[rect][0], rects[rect][3] - rects[rect][1], 256, 256);
            //noinspection deprecation
            GlStateManager.color4f(1, 1, 1, 1);
        }
        matrixStack.pop();
    }

    private static final int[][] RECTS_1 = new int[][]{
            { 0, 0, 24, 24 }
    };

    private static final int[][] RECTS_2 = new int[][]{
            { 0, 0, 12, 24 },
            { 12, 0, 24, 24 }
    };

    private static final int[][] RECTS_4 = new int[][]{
            { 0, 0, 12, 12 },
            { 12, 0, 24, 12 },
            { 0, 12, 12, 24 },
            { 12, 12, 24, 24 }
    };

    private static final int[][] RECTS_6 = new int[][]{
            { 0, 0, 12, 8 },
            { 12, 0, 24, 8 },
            { 0, 8, 12, 16 },
            { 12, 8, 24, 16 },
            { 0, 16, 12, 24 },
            { 12, 16, 24, 24 }
    };

    private static final int[][] RECTS_8 = new int[][]{
            { 0, 0, 8, 8 },
            { 8, 0, 16, 8 },
            { 16, 0, 24, 8 },
            { 0, 8, 8, 16 },
            { 16, 8, 24, 16 },
            { 0, 16, 8, 24 },
            { 8, 16, 16, 24 },
            { 16, 16, 24, 24 }
    };

    private static final int[][] RECTS_16 = new int[][]{
            { 0, 0, 5, 4 },
            { 5, 0, 10, 4 },
            { 10, 0, 15, 4 },
            { 15, 0, 20, 4 },
            { 20, 0, 24, 5 },
            { 0, 4, 4, 9 },
            { 20, 5, 24, 10 },
            { 0, 9, 4, 14 },
            { 20, 10, 24, 15 },
            { 0, 14, 4, 19 },
            { 20, 15, 24, 10 },
            { 0, 19, 4, 24 },
            { 4, 20, 9, 24 },
            { 9, 20, 14, 24 },
            { 14, 20, 19, 24 },
            { 19, 20, 24, 24 },
    };
}
