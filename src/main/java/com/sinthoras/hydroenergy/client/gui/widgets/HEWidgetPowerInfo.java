package com.sinthoras.hydroenergy.client.gui.widgets;

import com.sinthoras.hydroenergy.client.gui.HEControllerContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.FluidRegistry;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@SideOnly(Side.CLIENT)
public class HEWidgetPowerInfo extends Gui {

    private HEControllerContainer controllerContainer;
    private int pixelX;
    private int pixelY;
    private int width;
    private int centerX;

    public HEWidgetPowerInfo(HEControllerContainer controllerContainer, int pixelX, int pixelY, int width) {
        this.controllerContainer = controllerContainer;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.width = width;
        centerX = pixelX + width / 2;
    }

    public void draw(Minecraft minecraft) {
        FontRenderer fontRenderer = minecraft.fontRenderer;

        long euStored = controllerContainer.getEnergyStored();
        long euCapacity = controllerContainer.getEnergyCapacity();
        float euRelative = euCapacity == 0 ? 0.0f : ((float)euStored) / ((float)euCapacity);

        minecraft.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        IIcon iconStill = FluidRegistry.WATER.getStillIcon();
        GL11.glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawTexturedBar(pixelX, pixelY + 10, width, 10, iconStill, euRelative);

        int slashWidth = fontRenderer.getStringWidth("/");
        int storedWidth = fontRenderer.getStringWidth("" + euStored + " EU ");
        fontRenderer.drawString("" + euStored + " EU / " + euCapacity + " EU", centerX - slashWidth / 2 - storedWidth, pixelY, Color.BLACK.getRGB());

        float percentage = euRelative * 100.0f;
        String relativeInfo = String.format("%.2f", percentage) + "%";
        int relativeInfoWidth = fontRenderer.getStringWidth(relativeInfo);
        fontRenderer.drawString(relativeInfo, centerX - relativeInfoWidth / 2, pixelY + 12, Color.WHITE.getRGB());

        long euPerTickIn = controllerContainer.getEnergyPerTickIn();
        long euPerTickOut = controllerContainer.getEnergyPerTickOut();
        String in = "IN: " + euPerTickIn + " EU/t";
        String out = "OUT: " + euPerTickOut + " EU/t";
        fontRenderer.drawString(in, pixelX, pixelY + 23, Color.BLACK.getRGB());
        int constStringWidth = fontRenderer.getStringWidth(out);
        fontRenderer.drawString(out, pixelX + width - constStringWidth, pixelY + 23, Color.BLACK.getRGB());

        // Reset color
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawTexturedBar(int pixelX, int pixelY, int width, int height, IIcon icon, float progress) {
        int pixelProgress = Math.round(width * progress);
        int iconHeight = icon.getIconHeight();
        int completeTextures = pixelProgress / iconHeight;
        for(int i=0;i<completeTextures;i++) {
            int tmpX = pixelX + i * iconHeight;
            drawTexturedModelRectFromIcon(tmpX, pixelY, icon, iconHeight, height);
        }
        int remainder = pixelProgress % iconHeight;
        if(remainder > 0) {
            int tmpX = pixelX + completeTextures * iconHeight;
            drawTexturedModelRectFromIcon(tmpX, pixelY, icon,  remainder, height);
        }
    }
}
