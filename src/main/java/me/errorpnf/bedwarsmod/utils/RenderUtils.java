package me.errorpnf.bedwarsmod.utils;

import me.errorpnf.bedwarsmod.utils.formatting.FormatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class RenderUtils extends GuiScreen {
    private final int sizeX = 430;
    private final int sizeY = 224;

    public EntityOtherPlayerMP entityPlayer = null;
    private final ResourceLocation playerLocationSkin = null;
    private final ResourceLocation playerLocationCape = null;
    private final String skinType = null;

    public static void renderBlurredBackground(int width, int height, int x, int y, int blurWidth, int blurHeight) {
        if (!OpenGlHelper.isFramebufferEnabled()) return;

        float uMin = x / (float) width;
        float uMax = (x + blurWidth) / (float) width;
        float vMin = (height - y) / (float) height;
        float vMax = (height - y - blurHeight) / (float) height;

        blurOutputVert.bindFramebufferTexture();
        GlStateManager.color(1f, 1f, 1f, 1f);
        //Utils.setScreen(width*f, height*f, f);
        drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
        //Utils.setScreen(width, height, f);
        blurOutputVert.unbindFramebufferTexture();
    }

    public static void drawTexturedRect(float x, float y, float width, float height, int filter) {
        drawTexturedRect(x, y, width, height, 0, 1, 0, 1, filter);
    }

    public static void drawTexturedRect(
            float x,
            float y,
            float width,
            float height,
            float uMin,
            float uMax,
            float vMin,
            float vMax
    ) {
        drawTexturedRect(x, y, width, height, uMin, uMax, vMin, vMax, GL11.GL_LINEAR);
    }

    public static void drawTexturedRect(
            float x,
            float y,
            float width,
            float height,
            float uMin,
            float uMax,
            float vMin,
            float vMax,
            int filter
    ) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer
                .pos(x, y + height, 0.0D)
                .tex(uMin, vMax).endVertex();
        worldrenderer
                .pos(x + width, y + height, 0.0D)
                .tex(uMax, vMax).endVertex();
        worldrenderer
                .pos(x + width, y, 0.0D)
                .tex(uMax, vMin).endVertex();
        worldrenderer
                .pos(x, y, 0.0D)
                .tex(uMin, vMin).endVertex();
        tessellator.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GlStateManager.disableBlend();
    }

    public static Matrix4f createProjectionMatrix(int width, int height) {
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.setIdentity();
        projMatrix.m00 = 2.0F / (float) width;
        projMatrix.m11 = 2.0F / (float) (-height);
        projMatrix.m22 = -0.0020001999F;
        projMatrix.m33 = 1.0F;
        projMatrix.m03 = -1.0F;
        projMatrix.m13 = 1.0F;
        projMatrix.m23 = -1.0001999F;
        return projMatrix;
    }

    static Shader blurShaderHorz = null;
    static Framebuffer blurOutputHorz = null;
    static Shader blurShaderVert = null;
    static Framebuffer blurOutputVert = null;
    private static double lastBgBlurFactor = -1;

    public static void blurBackground() {
        if (!OpenGlHelper.isFramebufferEnabled()) return;

        int width = Minecraft.getMinecraft().displayWidth;
        int height = Minecraft.getMinecraft().displayHeight;

        if (blurOutputHorz == null) {
            blurOutputHorz = new Framebuffer(width, height, false);
            blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if (blurOutputVert == null) {
            blurOutputVert = new Framebuffer(width, height, false);
            blurOutputVert.setFramebufferFilter(GL11.GL_NEAREST);
        }
        if (blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
            blurOutputHorz.createBindFramebuffer(width, height);
            blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
        if (blurOutputVert.framebufferWidth != width || blurOutputVert.framebufferHeight != height) {
            blurOutputVert.createBindFramebuffer(width, height);
            blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }

        if (blurShaderHorz == null) {
            try {
                blurShaderHorz =
                        new Shader(
                                Minecraft.getMinecraft().getResourceManager(),
                                "blur",
                                Minecraft.getMinecraft().getFramebuffer(),
                                blurOutputHorz
                        );
                blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
                blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch (Exception ignored) {
            }
        }
        if (blurShaderVert == null) {
            try {
                blurShaderVert = new Shader(
                        Minecraft.getMinecraft().getResourceManager(),
                        "blur",
                        blurOutputHorz,
                        blurOutputVert
                );
                blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
                blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
            } catch (Exception ignored) {
            }
        }
        if (blurShaderHorz != null && blurShaderVert != null) {
            if (15 != lastBgBlurFactor) {
                blurShaderHorz.getShaderManager().getShaderUniform("Radius").set((float) 15);
                blurShaderVert.getShaderManager().getShaderUniform("Radius").set((float) 15);
                lastBgBlurFactor = 15;
            }
            GL11.glPushMatrix();
            blurShaderHorz.loadShader(0);
            blurShaderVert.loadShader(0);
            GlStateManager.enableDepth();
            GL11.glPopMatrix();

            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
        }
    }

    public static void drawStringCentered(FontRenderer fr, String str, float x, float y, boolean shadow, int colour) {
        int strLen = fr.getStringWidth(FormatUtils.unformat(str));

        float x2 = x - strLen / 2f;
        float y2 = y - fr.FONT_HEIGHT / 2f;

        GL11.glTranslatef(x2, y2, 0);
        fr.drawString(str, 0, 0, colour, shadow);
        GL11.glTranslatef(-x2, -y2, 0);
    }

    public static void drawStringCenteredScaledMaxWidth(
            String str,
            FontRenderer fr,
            float x,
            float y,
            boolean shadow,
            int len,
            int colour
    ) {
        int strLen = fr.getStringWidth(str);
        float factor = len / (float) strLen;
        factor = Math.min(1, factor);
        int newLen = Math.min(strLen, len);

        float fontHeight = 8 * factor;

        drawStringScaled(fr, str, x - newLen / 2, y - fontHeight / 2, shadow, colour, factor);
    }

    public static void drawStringCenteredScaled(
            FontRenderer fr,
            String str,
            float x,
            float y,
            boolean shadow,
            int colour,
            float scaleFactor
    ) {

        int strWidth = (int) (fr.getStringWidth(FormatUtils.unformat(str)) * scaleFactor);
        int strHeight = (int) (fr.FONT_HEIGHT * scaleFactor);

        float centerX = x - strWidth / 2f;
        float centerY = y - strHeight / 2f;

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0);
        GlStateManager.scale(scaleFactor, scaleFactor, 1);
        fr.drawString(str, 0, 0, colour, shadow);
        GlStateManager.scale(1 / scaleFactor, 1 / scaleFactor, 1);
        GL11.glPopMatrix();
    }

    public static void drawStringScaled(
            FontRenderer fr,
            String str,
            float x,
            float y,
            boolean shadow,
            int colour,
            float scale
    ) {

        int strLen = fr.getStringWidth(str);

        float x2 = x - strLen / 2f;
        float y2 = y - fr.FONT_HEIGHT / 2f;

        GL11.glPushMatrix();
        GL11.glTranslatef(x2, y2, 0);
        GlStateManager.scale(scale, scale, 1);
        fr.drawString(str, x / scale, y / scale, colour, shadow);
        GlStateManager.scale(1 / scale, 1 / scale, 1);
        GL11.glTranslatef(-x2, -y2, 0);
        GL11.glPopMatrix();
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        float i;
        if (left < right) {
            i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            i = top;
            top = bottom;
            bottom = i;
        }

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float j = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(g, h, j, f);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(left, bottom, 0.0).endVertex();
        worldRenderer.pos(right, bottom, 0.0).endVertex();
        worldRenderer.pos(right, top, 0.0).endVertex();
        worldRenderer.pos(left, top, 0.0).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawString(FontRenderer fontRenderer, String text, float x, float y, int color, boolean shadow) {
        // Get the width and height of the text
        int textWidth = fontRenderer.getStringWidth(text);
        int textHeight = fontRenderer.FONT_HEIGHT;

        // Calculate the translation to handle floating-point coordinates

        // Apply translation
        GL11.glTranslatef(x, y, 0);

        // Draw the string with the specified parameters
        fontRenderer.drawString(text, 0, 0, color, shadow);

        // Revert the translation
        GL11.glTranslatef(-x, -y, 0);
    }

    public static void drawStringLeftAligned(FontRenderer fontRenderer, String text, float x, float y, int color, boolean shadow) {
        // Get the width and height of the text
        int textWidth = fontRenderer.getStringWidth(text);
        int textHeight = fontRenderer.FONT_HEIGHT;

        float y2 = y - fontRenderer.FONT_HEIGHT / 2f;

        // Apply translation
        GL11.glTranslatef(x, y2, 0);

        // Draw the string with the specified parameters
        fontRenderer.drawString(text, 0, 0, color, shadow);

        // Revert the translation
        GL11.glTranslatef(-x, -y2, 0);
    }

    public static void drawStringWithNewlinesScaled(
            FontRenderer fr,
            String str,
            float x,
            float y,
            boolean shadow,
            int colour,
            float lineSpacing,
            float emptyLineHeight,
            float scaleFactor
    ) {

        String[] lines = str.split("\n");

        // calculate the height of each line with scaling
        int lineHeight = (int) (fr.FONT_HEIGHT * scaleFactor);

        float currentY = y;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                // for empty lines, use the specified empty line height
                currentY += emptyLineHeight;
            } else {

                // draw non-empty lines
                GL11.glPushMatrix();
                GL11.glTranslatef(x, currentY, 0);
                GlStateManager.scale(scaleFactor, scaleFactor, 1);
                fr.drawString(line, 0, 0, colour, shadow);
                GlStateManager.scale(1 / scaleFactor, 1 / scaleFactor, 1);
                GL11.glPopMatrix();

                currentY += lineHeight + lineSpacing;
            }
        }
    }
}
