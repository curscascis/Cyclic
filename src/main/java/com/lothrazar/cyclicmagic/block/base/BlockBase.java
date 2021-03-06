package com.lothrazar.cyclicmagic.block.base;
import java.util.List;
import com.lothrazar.cyclicmagic.util.UtilChat;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockBase extends Block {
  public static final String NBT_FLUIDSIZE = "fluidtotal";
  public static final String NBT_FLUIDTYPE = "fluidtype";
  public BlockBase(Material materialIn) {
    super(materialIn);
    this.setHardness(2.0F).setResistance(2.0F);//of course can/will be overwritten in most cases, but at least have a nonzero default
  }
  protected boolean isTransp = false;
  protected String myTooltip = null;
  protected void setTranslucent() {
    this.translucent = true;
    this.isTransp = true;
    this.setLightOpacity(0);
  }
  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
    if (myTooltip == null) {
      myTooltip = this.getUnlocalizedName() + ".tooltip";
    }
    tooltip.add(UtilChat.lang(myTooltip));
  }
  @Override
  public boolean isOpaqueCube(IBlockState state) {
    return !this.isTransp; // http://greyminecraftcoder.blogspot.ca/2014/12/transparent-blocks-18.html
  }
  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getBlockLayer() {
    if (this.isTransp)
      return BlockRenderLayer.TRANSLUCENT;
    else
      return super.getBlockLayer(); // SOLID
  }
}
