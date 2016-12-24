package com.lothrazar.cyclicmagic.gui;
import com.lothrazar.cyclicmagic.util.Const;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerBaseMachine extends ContainerBase {
  public int playerOffsetX = 8;
  public int playerOffsetY = 84;
  protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 9; j++) {
        addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9,
            playerOffsetX + j * Const.SQ, /// X
            playerOffsetY + i * Const.SQ// Y
        ));
      }
    }
    for (int i = 0; i < 9; i++) {
      addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * Const.SQ, playerOffsetY + 4 + 3*Const.SQ));
    }
  }
}
