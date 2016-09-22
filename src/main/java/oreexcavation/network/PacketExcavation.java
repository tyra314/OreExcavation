package oreexcavation.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.EventHandler;
import oreexcavation.handlers.MiningScheduler;
import org.apache.logging.log4j.Level;

public class PacketExcavation implements IMessage
{
	private NBTTagCompound tags = new NBTTagCompound();
	
	public PacketExcavation()
	{
	}
	
	public PacketExcavation(NBTTagCompound tags)
	{
		this.tags = tags;
	}
	
	@Override
	public void fromBytes(ByteBuf buf)
	{
		tags = ByteBufUtils.readTag(buf);
	}
	
	@Override
	public void toBytes(ByteBuf buf)
	{
		ByteBufUtils.writeTag(buf, tags);
	}
	
	public static class ServerHandler implements IMessageHandler<PacketExcavation,PacketExcavation>
	{
		@Override
		@SuppressWarnings("deprecation")
		public PacketExcavation onMessage(PacketExcavation message, MessageContext ctx)
		{
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			
			if(message.tags.getBoolean("cancel"))
			{
				MiningScheduler.INSTANCE.stopMining(player);
				return null;
			}
			
			int x = message.tags.getInteger("x");
			int y = message.tags.getInteger("y");
			int z = message.tags.getInteger("z");
			
			Block block = null;
			
			try
			{
				block = (Block)Block.REGISTRY.getObject(new ResourceLocation(message.tags.getString("block")));
			} catch(Exception e)
			{
				OreExcavation.logger.log(Level.INFO, "Recieved invalid block ID", e);
			}
			
			int meta = message.tags.getInteger("meta");
			
			if(player == null || block == null)
			{
				return null;
			}
			
			Block.getStateId(block.getDefaultState());
			MiningScheduler.INSTANCE.startMining(player, new BlockPos(x, y, z), block.getStateFromMeta(meta));
			
			return null;
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static class ClientHandler implements IMessageHandler<PacketExcavation,PacketExcavation>
	{
		@Override
		public PacketExcavation onMessage(PacketExcavation message, MessageContext ctx)
		{
			if(ExcavationSettings.mineMode < 0)
			{
				return null;
			} else if(ExcavationSettings.mineMode == 0)
			{
				if(!ExcavationKeys.veinKey.isKeyDown())
				{
					return null;
				}
			} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getMinecraft().thePlayer.isSneaking())
			{
				return null;
			}
			
			EventHandler.isExcavating = true;
			return new PacketExcavation(message.tags);
		}
	}
}
