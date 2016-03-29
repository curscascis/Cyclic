package com.lothrazar.cyclicmagic.item;

import java.util.ArrayList; 
import java.util.List;
import org.lwjgl.input.Keyboard;
import com.lothrazar.cyclicmagic.Const;
import com.lothrazar.cyclicmagic.SpellRegistry;
import com.lothrazar.cyclicmagic.spell.ISpell;
import com.lothrazar.cyclicmagic.spell.passive.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemCyclicWand extends Item{

	private static final String NBT_SPELLCURRENT = "spell_id";
	private static final String NBT_PASSIVECURRENT = "passive_id";
	private static final String NBT_UNLOCKS = "unlock_";

	public ItemCyclicWand(){

		this.setMaxStackSize(1);
		this.setHasSubtypes(true);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged){

		if(!slotChanged){
			return false;// only item data has changed, so do not animate
		}
		return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isFull3D(){

		return true;
	}

	@Override
	public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn){

		for(ISpell s : SpellRegistry.getSpellbook()){
			Spells.unlockSpell(stack, s.getID(), false);
		}
		//only these are unlocked
		List<Integer> spellbook = Variant.getSpellsFromVariant(Variant.getVariantFromMeta(stack));
		for(int id : spellbook){
			Spells.unlockSpell(stack, id, true);
		}
 
		Energy.rechargeBy(stack, Energy.START); 
		
		super.onCreated(stack, worldIn, playerIn);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack){

		String name = super.getUnlocalizedName() + "_" + Variant.getVariantFromMeta(stack).name().toLowerCase();

		return name;
	}

	@SideOnly(Side.CLIENT)
	public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems){

		for(int i = 0; i < Variant.values().length; i++){
			subItems.add(new ItemStack(itemIn, 1, i));
		}
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced){

		ISpell spell = SpellRegistry.getSpellFromID(Spells.getSpellCurrent(stack));

		int MAX = Energy.getMaximum(stack);

		String cost = EnumChatFormatting.DARK_GRAY + "[" + EnumChatFormatting.DARK_PURPLE +spell.getCost() + EnumChatFormatting.DARK_GRAY +"]";
		tooltip.add(EnumChatFormatting.GREEN + spell.getName()+" "+cost);

		if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)){

			tooltip.add(Energy.getCurrent(stack) + "/" + MAX);
			int reg = Energy.getRegen(playerIn.worldObj,stack);
			
			tooltip.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("wand.regen") + EnumChatFormatting.DARK_BLUE + reg);

			IPassiveSpell pcurrent = ItemCyclicWand.Spells.getPassiveCurrent(stack);
			if(pcurrent != null){
				tooltip.add(EnumChatFormatting.DARK_GRAY +StatCollector.translateToLocal("spellpassive.prefix")+ pcurrent.getName());
			}
		}
		else{
			tooltip.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("item.shift"));
		}

		super.addInformation(stack, playerIn, tooltip, advanced);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack par1ItemStack){

		return EnumRarity.UNCOMMON;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ){

		// If onItemUse returns false onItemRightClick will be called.
		// http://www.minecraftforge.net/forum/index.php?topic=31966.0
		// so if this casts and succeeds, the right click is cancelled
		return SpellRegistry.caster.tryCastCurrent(worldIn, playerIn, pos, side);
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn){

		// so this only happens IF either onItemUse did not fire at all, or it
		// fired and casting failed
		SpellRegistry.caster.tryCastCurrent(worldIn, playerIn, null, null);
		
		return super.onItemRightClick(itemStackIn, worldIn, playerIn);
	}

	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected){
 
		boolean perSecond = (worldIn.getTotalWorldTime() % Const.TICKS_PER_SEC == 0);
 
		if(worldIn.isRemote == false && perSecond){ 

			Energy.rechargeBy(stack, Energy.getRegen(worldIn,stack));
		}

		// if held by something not a player? such as custom npc/zombie/etc
		if(entityIn instanceof EntityPlayer == false){
			return;
		}
		EntityPlayer p = (EntityPlayer) entityIn;
		IPassiveSpell pcurrent = ItemCyclicWand.Spells.getPassiveCurrent(stack);
		if(pcurrent != null && pcurrent.canTrigger(p)){
			
			pcurrent.trigger(p);
		}

		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack){

		return 1; // Without this method, your inventory will NOT work!!!
	}

	private static NBTTagCompound getNBT(ItemStack stack){

		return stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
	}

	public static class Spells{
/*
		private static void unlockSpell(ItemStack stack, ISpell spell, boolean unlocked){

			unlockSpell(stack,spell.getID(), unlocked);
		}
		*/
		public static void unlockSpell(ItemStack stack, int spell_id, boolean unlocked){

			NBTTagCompound nbt = getNBT(stack);
			nbt.setBoolean(NBT_UNLOCKS + spell_id, unlocked);
			stack.setTagCompound(nbt);
		}

		public static void toggleSpell(ItemStack stack, int spell_id){

			NBTTagCompound nbt = getNBT(stack);
			String key = NBT_UNLOCKS + spell_id;
			nbt.setBoolean(key, !nbt.getBoolean(key));
			stack.setTagCompound(nbt);
		}

		public static boolean isSpellUnlocked(ItemStack stack, ISpell spell){

			return isSpellUnlocked(stack, spell.getID());
		}

		public static boolean isSpellUnlocked(ItemStack stack, int spell_id){

			if(stack == null){
				return false;
			}
			NBTTagCompound nbt = getNBT(stack);
			return nbt.getBoolean(NBT_UNLOCKS + spell_id);
		}

		public static int nextId(ItemStack stack, int spell_id){

			return nextId(stack, spell_id, 0);
		}

		public static int nextId(ItemStack stack, int spell_id, int infbreaker){

			int next;
 
			if(spell_id >= SpellRegistry.getSpellbook().size() - 1)
				next = 0;// (int)spells[0];
			else
				next = spell_id + 1;// (int)spells[spell_id+1];

			// dont infloop
			if(isSpellUnlocked(stack, next) == false && infbreaker < 100){

				infbreaker++;
				return nextId(stack, next, infbreaker);
			}

			infbreaker = 0;
			// this.setUnlocksFromByte(spells);
			return next;
		}

		public static int prevId(ItemStack stack, int spell_id){

			return prevId(stack, spell_id, 0);
		}

		public static int prevId(ItemStack stack, int spell_id, int infbreaker){

			int prev;

			if(spell_id == 0)
				prev = SpellRegistry.getSpellbook().size() - 1;
			else
				prev = spell_id - 1;
			// dont infloop
			if(isSpellUnlocked(stack, prev) == false && infbreaker < 100){

				infbreaker++;
				return prevId(stack, prev, infbreaker);
			}

			infbreaker = 0;

			return prev;
		}

		public static int getSpellCurrent(ItemStack stack){

			return getNBT(stack).getInteger(NBT_SPELLCURRENT);
		}

		public static void setSpellCurrent(ItemStack stack, int spell_id){

			NBTTagCompound tags = getNBT(stack);

			tags.setInteger(NBT_SPELLCURRENT, spell_id);

			stack.setTagCompound(tags);
		}

		public static int getPassiveCurrentID(ItemStack stack){

			return getNBT(stack).getInteger(NBT_PASSIVECURRENT);
		}

		public static IPassiveSpell getPassiveCurrent(ItemStack stack){

			if(stack == null){
				return null;
			}
			
			return SpellRegistry.Passives.getByID(getNBT(stack).getInteger(NBT_PASSIVECURRENT));
		}

		public static void togglePassive(ItemStack stack){

			NBTTagCompound tags = getNBT(stack);

			int current = tags.getInteger(NBT_PASSIVECURRENT);

			current++;
			if(current > 3){
				current = 0;
			}// TODO: fix hardcoded magic nums

			tags.setInteger(NBT_PASSIVECURRENT, current);

			stack.setTagCompound(tags);
		}
	}

	public static class Energy{

		public static final int START = 100; // what you get on crafted
		public static final int MAX_DEFAULT = 300;
		private static final String NBT_MANA = "mana";
		private static final String NBT_MAX = "energymax";
		private static final String NBT_LASTUSED = "used";

		public static final int RECHARGE_EXP_COST = 10;
		public static final int RECHARGE_MANA_AMT = 25;
		public static final int UPGRADE_EXP_COST = 500;

		public static int getMaximumLargest(){
			return 1000;//literally exists only to draw manabar
		}
		
		public static int getMaximum(ItemStack stack){

			int max = getNBT(stack).getInteger(NBT_MAX);
			if(max <= 0){
				max = MAX_DEFAULT;
			}
			return max;
		}
		public static boolean increaseMaximum(ItemStack stack, int by){
			int max = getMaximum(stack);

			if(max == getMaximumLargest()){
				return false;
			}
			int setNew = max + by;
			if(setNew > getMaximumLargest()){
				setNew = getMaximumLargest();
			}
			getNBT(stack).setInteger(NBT_MAX, setNew );
			
			return true;
		}
		private static int getRegen(World world, ItemStack stack){

			//1- a counter since it was last used for a spell (not counting inventory)
			// -> set to zero on use. 
			// -> increment by 1 each second (not tick)
			//2 a recharge level (rate)
			// when counter passes certain thresholds, it updates the level
			//3 when a recharge event happens, it checks the level and increments accordingly


			long lastUsed = Energy.getCooldownCounter(stack);
			
			long timeSinceLast = (world.getTotalWorldTime() - lastUsed) / Const.TICKS_PER_SEC;
			
			if(timeSinceLast < 0){
				return 20;//because -1 for never used
			}
		
			int rate = 0;
			if(timeSinceLast < 5){
				rate = 1;
			}
			else if(timeSinceLast < 10){
				rate = 3;
			}
			else if(timeSinceLast < 15){
				rate = 5;
			}
			else if(timeSinceLast < 20){
				rate = 7;
			}
			else if(timeSinceLast < 25){
				rate = 10;
			}
			else if(timeSinceLast < 30){
				rate = 15;
			}
			else{
				rate = 20;
			}
			return rate; 
		 
		}

		public static void setCooldownCounter(ItemStack stack, long i){

			NBTTagCompound tags = getNBT(stack);
			tags.setLong(NBT_LASTUSED, i);
			stack.setTagCompound(tags); 
		}

		public static long getCooldownCounter(ItemStack stack){
			
			NBTTagCompound tags = getNBT(stack);
			if(!tags.hasKey(NBT_LASTUSED) ){
				return -1;
			}

			return tags.getLong(NBT_LASTUSED);
		}

		public static int getCurrent(ItemStack stack){

			return getNBT(stack).getInteger(NBT_MANA);
		}

		public static void setCurrent(ItemStack stack, int m){

			if(m < 0){
				m = 0;
			}
			int MAX = getMaximum(stack);
			int filled = (int) Math.min(m, MAX);
			
			NBTTagCompound tags = getNBT(stack);
			tags.setInteger(NBT_MANA, filled); 
			stack.setTagCompound(tags); 
		}

		public static void drainBy(ItemStack stack, int m){

			Energy.setCurrent(stack, Energy.getCurrent(stack) - m);
		}

		public static void rechargeBy(ItemStack stack, int m){

			Energy.setCurrent(stack, Energy.getCurrent(stack) + m);
		}
	}
 
	public static class Timer{
		private final static String NBT = "casttimeout";
		
		public static int getSpellTimer(ItemStack wand){
 
			NBTTagCompound tags = getNBT(wand);

			return tags.getInteger(NBT);
		}
		public static void setSpellTimer(ItemStack wand,int current){

			NBTTagCompound tags = getNBT(wand);

			tags.setInteger(NBT,current);
		}

		public static boolean isBlockedBySpellTimer(ItemStack wand){

			int t = getSpellTimer(wand);
			return (t > 0);
		}
		public static void tickSpellTimer(ItemStack wand){

			int t = getSpellTimer(wand);
			if(t < 0){
				setSpellTimer(wand,0);
			}
			else if(t > 0){
				setSpellTimer(wand,t - 1);
			}
		}
	}
	
	public enum BuildType {
		FIRST, ROTATE, RANDOM, MATCH;

		private final static String NBT = "build";

		public static String getName(ItemStack wand){

			try{
				NBTTagCompound tags = getNBT(wand);

				return "button.build." + BuildType.values()[tags.getInteger(NBT)].toString().toLowerCase();

			}
			catch (Exception e){
				System.out.println(e.getMessage());
				return "button.build." + FIRST.toString().toLowerCase();
			}
		}

		public static int get(ItemStack wand){

			if(wand == null){
				return 0;
			}
			NBTTagCompound tags = getNBT(wand);

			return tags.getInteger(NBT);
		}

		public static void toggle(ItemStack wand){

			NBTTagCompound tags = getNBT(wand);
			int type = tags.getInteger(NBT);

			type++;

			if(type > MATCH.ordinal()){
				type = FIRST.ordinal();
			}

			tags.setInteger(NBT, type);
		} 
	}
	
	public static class InventoryRotation{

		private final static String NBT = "rotation";
		public static int get(ItemStack wand){

			if(wand == null){
				return 0;
			}
			NBTTagCompound tags = getNBT(wand);

			return tags.getInteger(NBT);
		}

		public static void set(ItemStack wand, int rot){

			NBTTagCompound tags = getNBT(wand);

			tags.setInteger(NBT, rot);
		}
	}

	public enum Variant {
		DIAMOND, LAPIS,EMERALD, REDSTONE;

		public int getMetadata(){
			return ordinal();
		}
 
		
		public String getResource(){

			return Const.TEXTURE_LOCATION + "cyclic_wand_" + this.name().toLowerCase();
		}

		public static Variant getVariantFromMeta(ItemStack stack){

			try{
				return Variant.values()[stack.getMetadata()];
			}
			catch (Exception e){
				// System.out.println("INVALID META::"+stack.getMetadata());
				return Variant.DIAMOND;// this is damage zero anyway
			}
		}

		public static List<Integer> getSpellsFromVariant(Variant v){

			List<Integer> active = new ArrayList<Integer>();
			//TODO: instead do a getGroupFromVariant.. this is temporary
			switch( v){
			case DIAMOND:	
				active = SpellRegistry.diamondGroup; 
				break;
			case EMERALD:
				active = SpellRegistry.emeraldGroup; 
				break; 
			case LAPIS:
				active = SpellRegistry.lapisGroup; 
				break; 
			case REDSTONE:
				active = SpellRegistry.redstoneGroup; 
				break;
			default:
				break;
			}
			return active;
		}
		
		public static void toggle(ItemStack heldItem){
			//TODO: instead do a getGroupFromVariant.. this is temporary
			switch( getVariantFromMeta(heldItem)){
			case DIAMOND:	
				heldItem.setItemDamage(Variant.EMERALD.ordinal());
				break;
			case EMERALD:
				heldItem.setItemDamage(Variant.REDSTONE.ordinal());
				break; 
			case REDSTONE:
				heldItem.setItemDamage(Variant.LAPIS.ordinal());
				break; 
			case LAPIS:
				heldItem.setItemDamage(Variant.DIAMOND.ordinal());
				break;
			default:
				break;
			}
		}
	}
}
