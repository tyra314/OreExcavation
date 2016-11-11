package oreexcavation.handlers;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.network.PacketExcavation;
import oreexcavation.utils.ToolEffectiveCheck;

public class EventHandler
{
	public static MiningAgent captureAgent;
	public static boolean skipNext = false;
	
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.getModID().equals(OreExcavation.MODID))
		{
			ConfigHandler.config.save();
			ConfigHandler.initConfigs();
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onEntitySpawn(EntityJoinWorldEvent event)
	{
		if(event.getWorld().isRemote || event.getEntity().isDead || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null)
		{
			if(event.getEntity() instanceof EntityItem)
			{
				EntityItem eItem = (EntityItem)event.getEntity();
				ItemStack stack = eItem.getEntityItem();
				
				captureAgent.addItemDrop(stack);
				
				event.setCanceled(true);
			} else if(event.getEntity() instanceof EntityXPOrb)
			{
				EntityXPOrb orb = (EntityXPOrb)event.getEntity();
				
				captureAgent.addExperience(orb.getXpValue());
				
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(event.getWorld().isRemote || !(event.getPlayer() instanceof EntityPlayerMP) || event.getPlayer() instanceof FakePlayer)
		{
			return;
		}
		
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		
		if(player.getHeldItem(EnumHand.MAIN_HAND) == null && !ExcavationSettings.openHand)
		{
			return;
		} else if(isToolBlacklisted(player.getHeldItem(EnumHand.MAIN_HAND)) != ExcavationSettings.invertTBlacklist)
		{
			return;
		} else if(isBlockBlacklisted(event.getState().getBlock()) != ExcavationSettings.invertBBlacklist)
		{
			return;
		} else if(event.getWorld().isAirBlock(event.getPos()))
		{
			return;
		}
		
		BlockPos p = event.getPos();
		IBlockState s = event.getState();
		Block b = s.getBlock();
		int m = b.getMetaFromState(s);
		
		if(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(event.getWorld(), s, p, player))
		{
			MiningAgent agent = MiningScheduler.INSTANCE.getActiveAgent(player.getUniqueID());
			
			if(agent == null)
			{
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("x", p.getX());
				tag.setInteger("y", p.getY());
				tag.setInteger("z", p.getZ());
				tag.setString("block", Block.REGISTRY.getNameForObject(b).toString());
				tag.setInteger("meta", m);
				OreExcavation.instance.network.sendTo(new PacketExcavation(tag), player);
			} else
			{
				agent.appendBlock(p);
			}
		}
	}
	
	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event)
	{
		if(event.phase != TickEvent.Phase.END)
		{
			return;
		}
		
		if(skipNext)
		{
			skipNext = false;
			return;
		}
		
		MiningScheduler.INSTANCE.tickAgents();
		captureAgent = null;
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
			if(!ExcavationKeys.excavateKey.isKeyDown())
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
		if(event.getWorld().isRemote || event.getWorld().getMinecraftServer().isServerRunning())
		{
			return;
		}
		
		MiningScheduler.INSTANCE.resetAll();
		captureAgent = null;
	}
	
	public boolean isBlockBlacklisted(Block block)
	{
		if(block == null || block == Blocks.AIR)
		{
			return false;
		}
		
		if(ExcavationSettings.blockBlacklist.contains(Block.REGISTRY.getNameForObject(block).toString()))
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
		
		if(ExcavationSettings.toolBlacklist.contains(Item.REGISTRY.getNameForObject(stack.getItem()).toString()))
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
