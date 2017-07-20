package com.lothrazar.cyclicmagic.component.hydrator;
import javax.annotation.Nullable;
import com.lothrazar.cyclicmagic.block.base.TileEntityBaseMachineInvo;
import com.lothrazar.cyclicmagic.gui.ITileRedstoneToggle;
import com.lothrazar.cyclicmagic.util.UtilItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class TileEntityHydrator extends TileEntityBaseMachineInvo implements ITileRedstoneToggle, ITickable, IFluidHandler {
  private static final String NBT_TANK = "tank";
  public static final int TANK_FULL = 10000;
  private static final int FLUID_PER_RECIPE = 100;
  private static final int SLOT_INFLUID = 8;
  public final static int TIMER_FULL = 40;
  public static enum Fields {
    REDSTONE, TIMER
  }
  public FluidTank tank = new FluidTank(TANK_FULL);
  private int[] hopperInput = { 0, 1, 2, 3 }; 
  private int[] hopperOutput = { 4, 5, 6, 7 }; 
  private InventoryCrafting crafting = new InventoryCrafting(new ContainerDummy(), 1, 1);
  public TileEntityHydrator() {
    super(4 + 4 + 1);// in, out,  fluid transfer
    timer = TIMER_FULL;
  }
  private int needsRedstone = 1;
  @Override
  public int[] getFieldOrdinals() {
    return super.getFieldArray(Fields.values().length);
  }
  @Override
  public void update() {
    if (!isRunning()) { return; }
    //ignore timer when filling up water
    tryFillTankFromItems();
    if (this.getCurrentFluid() == 0) { return; }
    if (this.updateTimerIsZero()) { // time to burn!
      this.spawnParticlesAbove();
      for (int i = 0; i < 4; i++) {
        if (tryProcessRecipe(i)) {
          break;//keep going until one works then stop
        }
      }
    }
  }
  public boolean tryProcessRecipe(int slot) {
    ItemStack s = this.getStackInSlot(slot);
    this.crafting.setInventorySlotContents(0, s);
    IRecipe rec = CraftingManager.findMatchingRecipe(crafting, this.world);
    if (rec != null && this.getCurrentFluid() >= FLUID_PER_RECIPE) {
      this.tank.drain(FLUID_PER_RECIPE, true);
      this.sendOutputItem(rec.getRecipeOutput());
      s.shrink(1);
      this.timer = TIMER_FULL;
      return true;
    }
    return false;
  }
  @Override
  public int[] getSlotsForFace(EnumFacing side) {
    if (side == EnumFacing.UP)
      return hopperInput;
    return hopperOutput;
  }
  public void tryFillTankFromItems() {
    ItemStack maybeBucket = this.getStackInSlot(SLOT_INFLUID);
    FluidStack f = FluidUtil.getFluidContained(maybeBucket);
    IFluidHandlerItem bucketHandler = FluidUtil.getFluidHandler(maybeBucket);
    if (f != null && bucketHandler != null && f.getFluid().equals(FluidRegistry.WATER)) {
      //https://github.com/BluSunrize/ImmersiveEngineering/blob/fc022675bb550318cbadc879b3f28dde511e29c3/src/main/java/blusunrize/immersiveengineering/common/blocks/wooden/TileEntityWoodenBarrel.java
      FluidActionResult r = FluidUtil.tryEmptyContainer(maybeBucket, tank, Fluid.BUCKET_VOLUME, null, true);
      //in the case of a full bucket, it becomes empty. 
      //also supports any other fluid holding item, simply draining that fixed amount each round
      if (r.success) {
        this.setInventorySlotContents(SLOT_INFLUID, r.result);
      }
    }
  }
 
  public int getCurrentFluid() {
    if (this.tank.getFluid() == null) { return 0; }
    return this.tank.getFluid().amount;
  }
  public void sendOutputItem(ItemStack itemstack) {
    for (int i = 3 + 1; i < 8; i++) {
      if (!itemstack.isEmpty() && itemstack.getMaxStackSize() != 0) {
        itemstack = tryMergeStackIntoSlot(itemstack, i);
      }
    }
    if (!itemstack.isEmpty() && itemstack.getMaxStackSize() != 0) { //FULL
      UtilItemStack.dropItemStackInWorld(this.getWorld(), this.pos.up(), itemstack);
    }
  }
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
    tagCompound.setInteger(NBT_REDST, this.needsRedstone);
    tagCompound.setTag(NBT_TANK, tank.writeToNBT(new NBTTagCompound()));
    return super.writeToNBT(tagCompound);
  }
  @Override
  public void readFromNBT(NBTTagCompound tagCompound) {
    super.readFromNBT(tagCompound);
    this.needsRedstone = tagCompound.getInteger(NBT_REDST);
    tank.readFromNBT(tagCompound.getCompoundTag(NBT_TANK));
  }
  @Override
  public int getField(int id) {
    switch (Fields.values()[id]) {
      case REDSTONE:
        return this.needsRedstone;
      case TIMER:
        return this.timer;
    }
    return -1;
  }
  @Override
  public void setField(int id, int value) {
    switch (Fields.values()[id]) {
      case REDSTONE:
        this.needsRedstone = value;
      break;
      case TIMER:
        this.timer = value;
      break;
    }
  }
  @Override
  public int getFieldCount() {
    return Fields.values().length;
  }
  @Override
  public void toggleNeedsRedstone() {
    int val = this.needsRedstone + 1;
    if (val > 1) {
      val = 0;//hacky lazy way
    }
    this.setField(Fields.REDSTONE.ordinal(), val);
  }
  public boolean onlyRunIfPowered() {
    return this.needsRedstone == 1;
  }
  /******************************
   * fluid properties here
   ******************************/
  @Override
  public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      return true;
    return super.hasCapability(capability, facing);
  }
  @Override
  public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      return (T) tank;
    return super.getCapability(capability, facing);
  }
  public static class ContainerDummy extends Container {
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
      return false;
    }
  }
  @Override
  public IFluidTankProperties[] getTankProperties() {
    FluidTankInfo info = tank.getInfo();
    return new IFluidTankProperties[] { new FluidTankProperties(info.fluid, info.capacity, true, true) };
  }
  @Override
  public int fill(FluidStack resource, boolean doFill) {
    return tank.fill(resource, doFill);
  }
  @Override
  public FluidStack drain(FluidStack resource, boolean doDrain) {
    return tank.drain(resource, doDrain);
  }
  @Override
  public FluidStack drain(int maxDrain, boolean doDrain) {
    return tank.drain(maxDrain, doDrain);
  }
}