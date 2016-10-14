package oreexcavation.handlers;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.oredict.OreDictionary;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.network.PacketExcavation;
import oreexcavation.utils.BlockPos;
import oreexcavation.utils.ToolEffectiveCheck;
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
		if(event.world.isRemote || !(event.getPlayer() instanceof EntityPlayerMP) || event.getPlayer() instanceof FakePlayer)
		{
			return;
		}
		
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		
		if(player.getHeldItem() == null && !ExcavationSettings.openHand)
		{
			return;
		} else if(isToolBlacklisted(player.getHeldItem()) != ExcavationSettings.invertTBlacklist)
		{
			return;
		} else if(isBlockBlacklisted(event.block) != ExcavationSettings.invertBBlacklist)
		{
			return;
		} else if(event.world.isAirBlock(event.x, event.y, event.z))
		{
			return;
		}
		
		if(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(event.world, event.block, event.blockMetadata, new BlockPos(event.x, event.y, event.z), player))
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
		
		if(cTick != 0 || Minecraft.getMinecraft().thePlayer == null || !isExcavating || !ExcavationSettings.mustHold)
		{
			return;
		}
		
		boolean canContinue = true;
		
		if(ExcavationSettings.mineMode < 0)
		{
			canContinue = false;
		} else if(ExcavationSettings.mineMode == 0)
		{
			if(!ExcavationKeys.excavateKey.getIsKeyPressed())
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
	
	public boolean isBlockBlacklisted(Block block)
	{
		if(block == null || block == Blocks.air)
		{
			return false;
		}
		
		if(ExcavationSettings.blockBlacklist.contains(Block.blockRegistry.getNameForObject(block)))
		{
			return true;
		}
		
		Item itemBlock = Item.getItemFromBlock(block);
		
		if(itemBlock == null)
		{
			return false;
		}
		
		int[] oreIDs =  OreDictionary.getOreIDs(new ItemStack(block));
		
		for(int id : oreIDs)
		{
			if(ExcavationSettings.blockBlacklist.contains(OreDictionary.getOreName(id)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isToolBlacklisted(ItemStack stack)
	{
		if(stack == null || stack.getItem() == null)
		{
			return false;
		}
		
		if(ExcavationSettings.toolBlacklist.contains(Item.itemRegistry.getNameForObject(stack.getItem())))
		{
			return true;
		}
		
		int[] oreIDs = OreDictionary.getOreIDs(stack);
		
		for(int id : oreIDs)
		{
			if(ExcavationSettings.toolBlacklist.contains(OreDictionary.getOreName(id)) != ExcavationSettings.invertTBlacklist)
			{
				return true;
			}
		}
		
		return false;
	}
}
