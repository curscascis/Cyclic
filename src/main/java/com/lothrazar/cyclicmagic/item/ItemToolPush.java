package com.lothrazar.cyclicmagic.item;

import com.lothrazar.cyclicmagic.IHasRecipe;
import com.lothrazar.cyclicmagic.util.UtilPlaceBlocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemToolPush  extends BaseTool implements IHasRecipe{

	@Override
	public void addRecipe() { 
		
	}
	@SuppressWarnings("unused")
	@Override
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer p, World worldObj, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		
		BlockPos resultPosition = UtilPlaceBlocks.pushBlock(worldObj, p, pos, side);
 
		return super.onItemUse(stack, p, worldObj, pos, hand, side, hitX, hitY, hitZ);// EnumActionResult.PASS;
	}
}
