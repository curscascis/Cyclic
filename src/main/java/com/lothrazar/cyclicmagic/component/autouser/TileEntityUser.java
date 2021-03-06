package com.lothrazar.cyclicmagic.component.autouser;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.block.base.TileEntityBaseMachineInvo;
import com.lothrazar.cyclicmagic.gui.ITilePreviewToggle;
import com.lothrazar.cyclicmagic.gui.ITileRedstoneToggle;
import com.lothrazar.cyclicmagic.gui.ITileSizeToggle;
import com.lothrazar.cyclicmagic.util.UtilEntity;
import com.lothrazar.cyclicmagic.util.UtilFakePlayer;
import com.lothrazar.cyclicmagic.util.UtilFluid;
import com.lothrazar.cyclicmagic.util.UtilInventoryTransfer;
import com.lothrazar.cyclicmagic.util.UtilItemStack;
import com.lothrazar.cyclicmagic.util.UtilShape;
import com.lothrazar.cyclicmagic.util.UtilWorld;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidActionResult;

public class TileEntityUser extends TileEntityBaseMachineInvo implements ITileRedstoneToggle, ITileSizeToggle, ITilePreviewToggle, ITickable {
  //vazkii wanted simple block breaker and block placer. already have the BlockBuilder for placing :D
  //of course this isnt standalone and hes probably found some other mod by now but doing it anyway https://twitter.com/Vazkii/status/767569090483552256
  // fake player idea ??? https://gitlab.prok.pw/Mirrors/minecraftforge/commit/f6ca556a380440ededce567f719d7a3301676ed0
  private static final String NBT_LR = "lr";
  private static final int MAX_SIZE = 4;//9x9 area 
  public final static int TIMER_FULL = 120;
  private static final int MAX_SPEED = 20;
  public static int maxHeight = 10;
  private int[] hopperInput = { 0, 1, 2 };// all slots for all faces
  private int[] hopperOutput = { 3, 4, 5, 6, 7, 8 };// all slots for all faces
  private int[] hopperInputFuel = { 9 };// all slots for all faces
  //  final int RADIUS = 4;//center plus 4 in each direction = 9x9
  private int rightClickIfZero = 0;
  private WeakReference<FakePlayer> fakePlayer;
  private UUID uuid;
  private int needsRedstone = 1;
  private int renderParticles = 0;
  private int toolSlot = 0;
  private int size;
  public int yOffset = 0;
  public static enum Fields {
    TIMER, SPEED, REDSTONE, LEFTRIGHT, SIZE, RENDERPARTICLES, FUEL, FUELMAX, Y_OFFSET;
  }
  public TileEntityUser() {
    super(10);
    timer = TIMER_FULL;
    speed = SPEED_FUELED;
    this.setFuelSlot(9);
  }
  @Override
  public int[] getFieldOrdinals() {
    return super.getFieldArray(Fields.values().length);
  }
  @Override
  public void update() {
    if (!isRunning()) {
      return;
    }
    this.shiftAllUp(7);
    this.spawnParticlesAbove();
    this.updateFuelIsBurning();
    boolean triggered = this.updateTimerIsZero();
    if (world instanceof WorldServer) {
      verifyUuid(world);
      if (fakePlayer == null) {
        fakePlayer = UtilFakePlayer.initFakePlayer((WorldServer) world, this.uuid);
        if (fakePlayer == null) {
          ModCyclic.logger.error("Fake player failed to init ");
          return;
        }
      }
      //fake player facing the same direction as tile. for throwables
      fakePlayer.get().rotationYaw = UtilEntity.getYawFromFacing(this.getCurrentFacing());
      tryEquipItem();
      if (triggered) {
        timer = TIMER_FULL;
        try {
          BlockPos targetPos = this.getTargetPos();//this.getCurrentFacingPos();/
          if (rightClickIfZero == 0) {//right click entities and blocks
            this.rightClickBlock(targetPos);
          }
          interactEntities(targetPos);
        }
        catch (Exception e) {//exception could come from external third party block/mod/etc
          ModCyclic.logger.error("Automated User Error");
          ModCyclic.logger.error(e.getLocalizedMessage());
          e.printStackTrace();
        }
      } //timer == 0 block
      //      else {
      //        timer = 1;//allows it to run on a pulse
      //      }
    }
  }
  private void interactEntities(BlockPos targetPos) {
    BlockPos entityCenter = getTargetCenter();
    int vRange = 1;
    AxisAlignedBB entityRange = UtilEntity.makeBoundingBox(entityCenter.getX() + 0.5, entityCenter.getY(), entityCenter.getZ() + 0.5, size, vRange);
    List<? extends Entity> living = world.getEntitiesWithinAABB(EntityLivingBase.class, entityRange);
    List<? extends Entity> carts = world.getEntitiesWithinAABB(EntityMinecart.class, entityRange);
    List<Entity> all = new ArrayList<Entity>(living);
    all.addAll(carts);//works since  they share a base class but no overlap
    if (rightClickIfZero == 0) {//right click entities and blocks
      this.getWorld().markChunkDirty(targetPos, this);
      for (Entity ent : all) {//both living and minecarts
        // on the line below: NullPointerException  at com.lothrazar.cyclicmagic.block.tileentity.TileMachineUser.func_73660_a(TileMachineUser.java:101)
        if (world.isRemote == false &&
            ent != null && ent.isDead == false
            && fakePlayer != null && fakePlayer.get() != null) {
          validateTool(); //recheck this at every step so we dont go negative
          if (EnumActionResult.FAIL != fakePlayer.get().interactOn(ent, EnumHand.MAIN_HAND)) {
            this.tryDumpFakePlayerInvo();
            break;//dont do every entity in teh whole batch
          }
        }
      }
    }
    else { // left click entities and blocks 
      ItemStack held = fakePlayer.get().getHeldItemMainhand();
      fakePlayer.get().onGround = true;
      for (Entity e : living) {// only living, not minecarts
        EntityLivingBase ent = (EntityLivingBase) e;
        if (e == null) {
          continue;
        } //wont happen eh
        fakePlayer.get().attackTargetEntityWithCurrentItem(ent);
        //THANKS TO FORUMS http://www.minecraftforge.net/forum/index.php?topic=43152.0
        IAttributeInstance damage = new AttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (!held.isEmpty()) {
          for (AttributeModifier modifier : held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            damage.applyModifier(modifier);
          }
        }
        float dmgVal = (float) damage.getAttributeValue();
        float f1 = EnchantmentHelper.getModifierForCreature(held, (ent).getCreatureAttribute());
        ent.attackEntityFrom(DamageSource.causePlayerDamage(fakePlayer.get()), dmgVal + f1);
      }
    }
  }
  private void rightClickBlock(BlockPos targetPos) {
    //    ItemStack maybeTool = fakePlayer.get().getHeldItemMainhand();
    if (rightClickFluidAttempt(targetPos)) {
      return;
    }
    else if (Block.getBlockFromItem(fakePlayer.get().getHeldItemMainhand().getItem()) == Blocks.AIR) { //a non block item
      //dont ever place a block. they want to use it on an entity
      EnumActionResult r = fakePlayer.get().interactionManager.processRightClickBlock(fakePlayer.get(), world, fakePlayer.get().getHeldItemMainhand(), EnumHand.MAIN_HAND, targetPos, EnumFacing.UP, .5F, .5F, .5F);
      if (r != EnumActionResult.SUCCESS) {
        r = fakePlayer.get().interactionManager.processRightClick(fakePlayer.get(), world, fakePlayer.get().getHeldItemMainhand(), EnumHand.MAIN_HAND);
        if (r != EnumActionResult.SUCCESS) {
          ActionResult<ItemStack> res = fakePlayer.get().getHeldItemMainhand().getItem().onItemRightClick(world, fakePlayer.get(), EnumHand.MAIN_HAND);
          if (res == null || res.getType() != EnumActionResult.SUCCESS) {
            //this item onrightclick would/should/could work for GLASS_BOTTLE...except
            //it uses player Ray Trace to get target. which is null for fakes
            //TODO: maybe one solution is to extend FakePlayer to run a rayrace somehow
            //but how to set/manage current lookpos
            //so hakcy time
            if (fakePlayer.get().getHeldItemMainhand().getItem() == Items.GLASS_BOTTLE && world.getBlockState(targetPos).getMaterial() == Material.WATER) {
              ItemStack itemstack = fakePlayer.get().getHeldItemMainhand();
              EntityPlayer p = fakePlayer.get();
              world.playSound(p, p.posX, p.posY, p.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
              //  return new ActionResult(EnumActionResult.SUCCESS,
              itemstack.shrink(1);
              //UtilItemStack.turnBottleIntoItem(itemstack, p,
              ItemStack is = new ItemStack(Items.POTIONITEM);
              PotionUtils.addPotionToItemStack(is, PotionTypes.WATER);
              this.tryDumpStacks(Arrays.asList(is));
              //);
            }
          }
        }
      }
    }
  }
  private void tryDumpStacks(List<ItemStack> toDump) {
    ArrayList<ItemStack> toDrop = UtilInventoryTransfer.dumpToIInventory(toDump, this, 3, 9);
    ///only drop now that its full
    BlockPos dropHere = getTargetPos();
    for (ItemStack s : toDrop) {
      if (!s.isEmpty()) {//&& !s.equals(fakePlayer.get().getHeldItemMainhand())
        EntityItem entityItem = UtilItemStack.dropItemStackInWorld(world, dropHere, s.copy());
        if (entityItem != null) {
          entityItem.setVelocity(0, 0, 0);
        }
        s.setCount(0);
      }
    }
  }
  private void tryDumpFakePlayerInvo() {
    ArrayList<ItemStack> toDrop = new ArrayList<ItemStack>();
    for (ItemStack s : fakePlayer.get().inventory.mainInventory) {
      if (!s.isEmpty() && !s.equals(fakePlayer.get().getHeldItemMainhand())) {
        toDrop.add(s);
      }
    }
    tryDumpStacks(toDrop);
  }
  private boolean rightClickFluidAttempt(BlockPos targetPos) {
    ItemStack maybeTool = fakePlayer.get().getHeldItemMainhand();
    if (maybeTool != null && !maybeTool.isEmpty() && UtilFluid.stackHasFluidHandler(maybeTool)) {
      if (UtilFluid.hasFluidHandler(world.getTileEntity(targetPos), this.getCurrentFacing().getOpposite())) {//tile has fluid
        ItemStack originalRef = maybeTool.copy();
        int hack = (maybeTool.getCount() == 1) ? 1 : 0;//HAX: if bucket stack size is 1, it somehow doesnt work so yeah. good enough EH?
        maybeTool.grow(hack);
        boolean success = UtilFluid.interactWithFluidHandler(fakePlayer.get(), this.world, targetPos, this.getCurrentFacing().getOpposite());
        if (success) {
          if (UtilFluid.isEmptyOfFluid(originalRef)) { //original was empty.. maybe its full now IDK
            maybeTool.shrink(1 + hack);
          }
          else {//original had fluid in it. so make sure we drain it now hey
            ItemStack drained = UtilFluid.drainOneBucket(maybeTool.splitStack(1));
            // drained.setCount(1);
            // UtilItemStack.dropItemStackInWorld(this.world, getCurrentFacingPos(), drained);
            maybeTool.shrink(1 + hack);
          }
          this.tryDumpFakePlayerInvo();
        }
      }
      else {//no tank, just open world
        //dispense stack so either pickup or place liquid
        if (UtilFluid.isEmptyOfFluid(maybeTool)) {
          FluidActionResult res = UtilFluid.fillContainer(world, targetPos, maybeTool, this.getCurrentFacing());
          if (res != FluidActionResult.FAILURE) {
            maybeTool.shrink(1);
            UtilItemStack.dropItemStackInWorld(this.world, getCurrentFacingPos(), res.getResult());
            return true;
          }
        }
        else {
          ItemStack drainedStackOrNull = UtilFluid.dumpContainer(world, targetPos, maybeTool);
          if (drainedStackOrNull != null) {
            maybeTool.shrink(1);
            UtilItemStack.dropItemStackInWorld(this.world, getCurrentFacingPos(), drainedStackOrNull);
          }
        }
        return true;
      }
    }
    return false;
  }
  /**
   * detect if tool stack is empty or destroyed and reruns equip
   */
  private void validateTool() {
    ItemStack maybeTool = getStackInSlot(toolSlot);
    if (!maybeTool.isEmpty() && maybeTool.getCount() < 0) {
      maybeTool = ItemStack.EMPTY;
      fakePlayer.get().setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
      inv.set(toolSlot, ItemStack.EMPTY);
    }
  }
  private ItemStack tryEquipItem() {
    ItemStack maybeTool = getStackInSlot(toolSlot);
    if (!maybeTool.isEmpty()) {
      //do we need to make it null
      if (maybeTool.getCount() <= 0) {
        maybeTool = ItemStack.EMPTY;
      }
    }
    fakePlayer.get().setPosition(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ());//seems to help interact() mob drops like milk
    fakePlayer.get().onUpdate();//trigger   ++this.ticksSinceLastSwing; among other things
    if (maybeTool.isEmpty()) {//null for any reason
      fakePlayer.get().setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
      inv.set(toolSlot, ItemStack.EMPTY);
    }
    else {
      //so its not null
      if (!maybeTool.equals(fakePlayer.get().getHeldItem(EnumHand.MAIN_HAND))) {
        fakePlayer.get().setHeldItem(EnumHand.MAIN_HAND, maybeTool);
      }
    }
    return maybeTool;
  }
  private void verifyUuid(World world) {
    if (uuid == null) {
      uuid = UUID.randomUUID();
      IBlockState state = world.getBlockState(this.pos);
      world.notifyBlockUpdate(pos, state, state, 3);
    }
  }
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    if (uuid != null) {
      compound.setString(NBTPLAYERID, uuid.toString());
    }
    compound.setInteger(NBT_REDST, needsRedstone);
    compound.setInteger(NBT_LR, rightClickIfZero);
    compound.setInteger(NBT_SIZE, size);
    compound.setInteger(NBT_RENDER, renderParticles);
    compound.setInteger("yoff", yOffset);
    return super.writeToNBT(compound);
  }
  @Override
  public void readFromNBT(NBTTagCompound compound) {
    super.readFromNBT(compound);
    if (compound.hasKey(NBTPLAYERID)) {
      uuid = UUID.fromString(compound.getString(NBTPLAYERID));
    }
    needsRedstone = compound.getInteger(NBT_REDST);
    rightClickIfZero = compound.getInteger(NBT_LR);
    size = compound.getInteger(NBT_SIZE);
    renderParticles = compound.getInteger(NBT_RENDER);
    yOffset = compound.getInteger("yoff");
  }
  @Override
  public int[] getSlotsForFace(EnumFacing side) {
    if (side == EnumFacing.UP)
      return hopperInput;
    if (side == EnumFacing.DOWN)
      return hopperOutput;
    return hopperInputFuel;
  }
  @Override
  public int getField(int id) {
    switch (Fields.values()[id]) {
      case SPEED:
        return getSpeed();
      case TIMER:
        return getTimer();
      case REDSTONE:
        return this.needsRedstone;
      case SIZE:
        return this.size;
      case LEFTRIGHT:
        return this.rightClickIfZero;
      case FUEL:
        return this.getFuelCurrent();
      case FUELMAX:
        return this.getFuelMax();
      case RENDERPARTICLES:
        return this.renderParticles;
      case Y_OFFSET:
        return this.yOffset;
    }
    return 0;
  }
  @Override
  public void setField(int id, int value) {
    switch (Fields.values()[id]) {
      case Y_OFFSET:
        if (value > 1) {
          value = -1;
        }
        this.yOffset = value;
      break;
      case SPEED:
        this.setSpeed(value);
      break;
      case TIMER:
        if (value < 0) {
          value = 0;
        }
        timer = Math.min(value, TIMER_FULL);
      break;
      case REDSTONE:
        this.needsRedstone = value;
      break;
      case LEFTRIGHT:
        if (value > 1) {
          value = 0;
        }
        this.rightClickIfZero = value;
      break;
      case SIZE:
        this.size = value;
      break;
      case FUEL:
        this.setFuelCurrent(value);
      break;
      case FUELMAX:
        this.setFuelMax(value);
      break;
      case RENDERPARTICLES:
        this.renderParticles = value % 2;
      break;
      default:
      break;
    }
  }
  @Override
  public int getFieldCount() {
    return Fields.values().length;
  }
  @Override
  public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
    // Extracts data from a packet (S35PacketUpdateTileEntity) that was sent
    // from the server. Called on client only.
    this.readFromNBT(pkt.getNbtCompound());
    super.onDataPacket(net, pkt);
  }
  public int getTimer() {
    return timer;
  }
  @Override
  public void toggleNeedsRedstone() {
    int val = (this.needsRedstone == 1) ? 0 : 1;
    this.setField(Fields.REDSTONE.ordinal(), val);
  }
  public boolean onlyRunIfPowered() {
    return this.needsRedstone == 1;
  }
  @Override
  public void toggleSizeShape() {
    this.size++;
    if (this.size > MAX_SIZE) {
      this.size = 0;
    }
  }
  @Override
  public int getSpeed() {
    return speed;
  }
  @Override
  public void setSpeed(int value) {
    //    System.out.println(value);
    if (value < 1) {
      value = 1;
    }
    speed = Math.min(value, MAX_SPEED);
  }
  private BlockPos getTargetPos() {
    BlockPos targetPos = UtilWorld.getRandomPos(getWorld().rand, getTargetCenter(), this.size);
    return targetPos;
  }
  public BlockPos getTargetCenter() {
    //move center over that much, not including exact horizontal
    return this.getPos().offset(this.getCurrentFacing(), this.size + 1).offset(EnumFacing.UP, yOffset);
  }
  @Override
  public void togglePreview() {
    this.renderParticles = (renderParticles + 1) % 2;
  }
  @Override
  public List<BlockPos> getShape() {
    return UtilShape.squareHorizontalHollow(getTargetCenter(), this.size);
  }
  @Override
  public boolean isPreviewVisible() {
    return this.getField(Fields.RENDERPARTICLES.ordinal()) == 1;
  }
}