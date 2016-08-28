package com.lothrazar.cyclicmagic.gui.wand;
import java.util.ArrayList;
import java.util.List;
import com.lothrazar.cyclicmagic.ModMain;
import com.lothrazar.cyclicmagic.gui.ITooltipButton;
import com.lothrazar.cyclicmagic.item.ItemCyclicWand;
import com.lothrazar.cyclicmagic.net.PacketWandGui;
import com.lothrazar.cyclicmagic.util.UtilSpellCaster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ButtonBuildToggle extends GuiButton implements ITooltipButton {
  private final EntityPlayer thePlayer;
  public ButtonBuildToggle(EntityPlayer p, int buttonId, int x, int y, int width) {
    super(buttonId, x, y, width, 20, "");
    thePlayer = p;
  }
  @SideOnly(Side.CLIENT)
  @Override
  public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
    boolean pressed = super.mousePressed(mc, mouseX, mouseY);
    if (pressed) {
      ModMain.network.sendToServer(new PacketWandGui(PacketWandGui.WandAction.BUILDTYPE));
    }
    return pressed;
  }
  @SideOnly(Side.CLIENT)
  @Override
  public void drawButton(Minecraft mc, int mouseX, int mouseY) {
    this.displayString = I18n.format(ItemCyclicWand.BuildType.getName(UtilSpellCaster.getPlayerWandIfHeld(thePlayer)));
    super.drawButton(mc, mouseX, mouseY);
  }
  @Override
  public List<String> getTooltips() {
    List<String> tooltips = new ArrayList<String>();
    String key = ItemCyclicWand.BuildType.getName(UtilSpellCaster.getPlayerWandIfHeld(thePlayer)) + ".tooltip";
    tooltips.add(I18n.format(key));
    return tooltips;
  }
}
