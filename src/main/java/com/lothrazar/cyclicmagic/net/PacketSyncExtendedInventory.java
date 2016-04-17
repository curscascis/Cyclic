package com.lothrazar.cyclicmagic.net;

import com.lothrazar.cyclicmagic.ModMain;
import com.lothrazar.cyclicmagic.inventory.PlayerHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketSyncExtendedInventory implements IMessage, IMessageHandler<PacketSyncExtendedInventory, IMessage> {

	int				slot;
	int				playerId;
	ItemStack	bauble	= null;

	public PacketSyncExtendedInventory() {}

	public PacketSyncExtendedInventory(EntityPlayer player, int slot) {
		this.slot = slot;
		this.bauble = PlayerHandler.getPlayerInventory(player).getStackInSlot(slot);
		this.playerId = player.getEntityId();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeByte(slot);
		buffer.writeInt(playerId);
		ByteBufUtils.writeItemStack(buffer, bauble);
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		slot = buffer.readByte();
		playerId = buffer.readInt();
		bauble = ByteBufUtils.readItemStack(buffer);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public IMessage onMessage(final PacketSyncExtendedInventory message, MessageContext ctx) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			public void run() {
				processMessage(message);
			}
		});
		return null;
	}

	@SideOnly(Side.CLIENT)
	void processMessage(PacketSyncExtendedInventory message) {

		World world = ModMain.proxy.getClientWorld();
		if (world == null)
			return;
		Entity p = world.getEntityByID(message.playerId);
		if (p != null && p instanceof EntityPlayer) {
			PlayerHandler.getPlayerInventory((EntityPlayer) p).stackList[message.slot] = message.bauble;
		}
		return;
	}

}