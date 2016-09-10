package oreexcavation.handlers;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import oreexcavation.BlockPos;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.network.PacketExcavation;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EventHandler
{
	
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.modID.equals(OreExcavation.MODID))
		{
			ConfigHandler.config.save();
			ConfigHandler.initConfigs();
		}
	}
	
	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(event.world.isRemote || !(event.getPlayer() instanceof EntityPlayerMP))
		{
			return;
		}
		
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		
		if(player.getHeldItem() == null && !ExcavationSettings.openHand)
		{
			return;
		} else if(player.getHeldItem() != null && ExcavationSettings.toolBlacklist.contains(Item.itemRegistry.getNameForObject(player.getHeldItem().getItem())))
		{
			return;
		} else if(ExcavationSettings.blockBlacklist.contains(Block.blockRegistry.getNameForObject(event.block)))
		{
			return;
		}
		
		if(ExcavationSettings.ignoreTools || event.block.canHarvestBlock(event.getPlayer(), event.blockMetadata))
		{
			MiningAgent agent = MiningScheduler.INSTANCE.getActiveAgent(player.getUniqueID());
			
			if(agent == null)
			{
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("x", event.x);
				tag.setInteger("y", event.y);
				tag.setInteger("z", event.z);
				tag.setString("block", Block.blockRegistry.getNameForObject(event.block));
				tag.setInteger("meta", event.blockMetadata);
				OreExcavation.instance.network.sendTo(new PacketExcavation(tag), player);
			} else
			{
				// Skip client side check
				agent.appendBlock(new BlockPos(event.x, event.y, event.z));
			}
		}
	}
	
	@SubscribeEvent
	public void onTick(TickEvent.WorldTickEvent event)
	{
		if(event.phase != TickEvent.Phase.END || event.world.isRemote)
		{
			return;
		}
		
		MiningScheduler.INSTANCE.tickAgents();
	}

	public static boolean isExcavating = false;
	private static int cTick = 0;
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientTick(TickEvent.ClientTickEvent event)
	{
		cTick = (cTick + 1)%10;
		
		if(cTick != 0 || Minecraft.getMinecraft().thePlayer == null || !isExcavating)
		{
			return;
		}
		
		boolean canContinue = true;
		
		if(ExcavationSettings.mineMode < 0)
		{
			canContinue = false;
		} else if(ExcavationSettings.mineMode == 0)
		{
			if(!ExcavationKeys.veinKey.isPressed())
			{
				canContinue = false;
			}
		} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getMinecraft().thePlayer.isSneaking())
		{
			canContinue = false;
		}
		
		if(!canContinue)
		{
			isExcavating = false;
			NBTTagCompound tags = new NBTTagCompound();
			tags.setBoolean("cancel", true);
			OreExcavation.instance.network.sendToServer(new PacketExcavation(tags));
		}
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		if(event.world.isRemote || MinecraftServer.getServer().isServerRunning())
		{
			return;
		}
		
		MiningScheduler.INSTANCE.resetAll();
	}
}
