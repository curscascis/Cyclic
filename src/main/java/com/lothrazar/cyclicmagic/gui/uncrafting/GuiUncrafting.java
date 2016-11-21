package com.lothrazar.cyclicmagic.gui.uncrafting;
import com.lothrazar.cyclicmagic.block.tileentity.TileMachineUncrafter;
import com.lothrazar.cyclicmagic.block.tileentity.TileMachineUncrafter.Fields;
import com.lothrazar.cyclicmagic.gui.GuiBaseContanerProgress;
import com.lothrazar.cyclicmagic.gui.GuiButtonMachineRedstone;
import com.lothrazar.cyclicmagic.util.Const;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiUncrafting extends GuiBaseContanerProgress {
  private TileMachineUncrafter tile;
  private GuiButtonMachineRedstone redstoneBtn;
  public GuiUncrafting(InventoryPlayer inventoryPlayer, TileMachineUncrafter tileEntity) {
    super(new ContainerUncrafting(inventoryPlayer, tileEntity), tileEntity);
    tile = tileEntity;
  }
  public String getTitle() {
    return "tile.uncrafting_block.name";
  }
  @Override
  public void initGui() {
    super.initGui();
    redstoneBtn = new GuiButtonMachineRedstone(0,
        this.guiLeft + 8,
        this.guiTop + 8, this.tile.getPos());
    redstoneBtn.setTextureIndex(tile.getField(Fields.REDSTONE.ordinal()));
    this.buttonList.add(redstoneBtn);
  }
  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    int u = 0, v = 0;
    this.mc.getTextureManager().bindTexture(Const.Res.SLOT);
    //first draw the zero slot
    Gui.drawModalRectWithCustomSizedTexture(this.guiLeft + ContainerUncrafting.SLOTX_START - 1 , this.guiTop + ContainerUncrafting.SLOTY - 1, u, v, Const.SQ, Const.SQ, Const.SQ, Const.SQ);

    int xPrefix = 2*Const.SQ+Const.padding;
    int yPrefix = 16;
    for (int i = 0; i < TileMachineUncrafter.SLOT_ROWS; i++) {
      for (int j = 0; j < TileMachineUncrafter.SLOT_COLS; j++) {
        Gui.drawModalRectWithCustomSizedTexture(this.guiLeft + xPrefix - 1 + j* Const.SQ, this.guiTop + yPrefix - 1+i* Const.SQ, u, v, Const.SQ, Const.SQ, Const.SQ, Const.SQ);
        
      }
    }
//    for (int k = 0; k < this.tile.getSizeInventory(); k++) {
//      Gui.drawModalRectWithCustomSizedTexture(this.guiLeft + ContainerUncrafting.SLOTX_START - 1 + k * Const.SQ, this.guiTop + ContainerUncrafting.SLOTY - 1, u, v, Const.SQ, Const.SQ, Const.SQ, Const.SQ);
//    }
  }
  @SideOnly(Side.CLIENT)
  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    redstoneBtn.setState(tile.getField(Fields.REDSTONE.ordinal()));
    super.drawGuiContainerForegroundLayer(mouseX, mouseY);
  }
  public int getProgressX() {
    return this.guiLeft + 10;
  }
  public int getProgressY() {
    return this.guiTop + 9 + 3 * Const.SQ + 5;
  }
  public int getProgressCurrent() {
    return tile.getTimer();
  }
  public int getProgressMax() {
    return TileMachineUncrafter.TIMER_FULL;
  }
}
