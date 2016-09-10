package oreexcavation.handlers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import oreexcavation.BlockPos;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;

public class MiningAgent
{
	private ItemStack blockStack = null;
	private Item origTool = null; // Original tool the player was holding (must be the same to continue)
	private List<BlockPos> mined = new ArrayList<BlockPos>();
	private List<BlockPos> scheduled = new ArrayList<BlockPos>();
	private final EntityPlayerMP player;
	private final BlockPos origin;
	
	private Block block;
	private int meta;
	
	private boolean subtypes = true; // Ignore metadata
	
	public MiningAgent(EntityPlayerMP player, BlockPos origin, Block block, int meta)
	{
		this.player = player;
		this.origin = origin;
		
		this.block = block;
		this.meta = meta;
		
		if(m_createStack != null)
		{
			try
			{
				blockStack = (ItemStack)m_createStack.invoke(block, meta);
			} catch(Exception e){}
		}
		
		this.subtypes = blockStack == null? true : !blockStack.getHasSubtypes();
		
		ItemStack held = player.getHeldItem();
		origTool = held == null? null : held.getItem();
		
		for(int i = -1; i <= 1; i++)
		{
			for(int j = -1; j <= 1; j++)
			{
				for(int k = -1; k <= 1; k++)
				{
					appendBlock(origin.offset(i, j, k));
				}
			}
		}
	}
	
	/**
	 * Returns true if the miner is no longer valid or has completed
	 */
	public boolean tickMiner()
	{
		if(origin == null || player == null || !player.isEntityAlive() || mined.size() >= ExcavationSettings.mineLimit)
		{
			return true;
		}
		
		for(int n = 0; scheduled.size() > 0; n++)
		{
			if(n >= ExcavationSettings.mineSpeed || mined.size() >= ExcavationSettings.mineLimit)
			{
				break;
			}
			
			ItemStack heldStack = player.getHeldItem();
			Item heldItem = heldStack == null? null : heldStack.getItem();
			
			if(heldItem != origTool)
			{
				// Original tool has been swapped or broken
				return true;
			} else if(!hasEnergy(player))
			{
				return true;
			}
			
			BlockPos pos = scheduled.remove(0);
			
			if(pos == null)
			{
				continue;
			} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > ExcavationSettings.mineRange)
			{
				mined.add(pos);
				continue;
			}
			
			Block b = player.worldObj.getBlock(pos.getX(), pos.getY(), pos.getZ());
			int m = player.worldObj.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ());
			
			boolean flag = b == block && (subtypes || m == meta);
			
			if(!flag && blockStack != null)
			{
				ItemStack stack = null;
				
				try
				{
					stack = (ItemStack)m_createStack.invoke(b, m);
				} catch(Exception e){}
				
				if(stack != null && stack.getItem() == blockStack.getItem() && stack.getItemDamage() == blockStack.getItemDamage())
				{
					flag = true;
				}
			}
			
			if(flag)
			{
				if(!(ExcavationSettings.ignoreTools || b.canHarvestBlock(player, m)))
				{
					mined.add(pos);
					continue;
				} else if(player.theItemInWorldManager.tryHarvestBlock(pos.getX(), pos.getY(), pos.getZ()))
				{
					if(player.capabilities.isCreativeMode)
					{
						player.getFoodStats().addExhaustion(ExcavationSettings.exaustion);
					}
					
					for(int i = -1; i <= 1; i++)
					{
						for(int j = -1; j <= 1; j++)
						{
							for(int k = -1; k <= 1; k++)
							{
								appendBlock(pos.offset(i, j, k));
							}
						}
					}
				}
				
				mined.add(pos);
			}
		}
		
		return scheduled.size() <= 0 || mined.size() >= ExcavationSettings.mineLimit;
	}
	
	/**
	 * Appends a block position to the miners current pass
	 */
	public void appendBlock(BlockPos pos)
	{
		if(pos == null || mined.contains(pos) || scheduled.contains(pos))
		{
			return;
		} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > ExcavationSettings.mineRange)
		{
			return;
		}
		
		scheduled.add(pos);
	}
	
	private boolean hasEnergy(EntityPlayerMP player)
	{
		return player.getFoodStats().getFoodLevel() > 0 || ExcavationSettings.exaustion <= 0;
	}
	
	private static Method m_createStack = null;
	
	static
	{
		try
		{
			m_createStack = Block.class.getDeclaredMethod("func_149644_j", int.class);
			m_createStack.setAccessible(true);
		} catch(Exception e1)
		{
			try
			{
				m_createStack = Block.class.getDeclaredMethod("createStackedBlock", int.class);
				m_createStack.setAccessible(true);
			} catch(Exception e2)
			{
				OreExcavation.logger.log(Level.INFO, "Unable to use block hooks for excavation", e2);
			}
		}
	}
}
