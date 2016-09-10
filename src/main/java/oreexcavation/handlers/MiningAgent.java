package oreexcavation.handlers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.overrides.ToolOverride;
import oreexcavation.overrides.ToolOverrideHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public class MiningAgent
{
	private ItemStack blockStack = null;
	private Item origTool = null;
	private List<BlockPos> mined = new ArrayList<BlockPos>();
	private List<BlockPos> scheduled = new ArrayList<BlockPos>();
	private final EntityPlayerMP player;
	private final BlockPos origin;
	
	private Block block;
	private int meta;
	
	private ToolOverride toolProps;
	
	private boolean subtypes = true; // Ignore metadata
	
	public MiningAgent(EntityPlayerMP player, BlockPos origin, IBlockState state)
	{
		this.player = player;
		this.origin = origin;
		
		this.block = state.getBlock();
		this.meta = block.getMetaFromState(state);
		
		if(m_createStack != null)
		{
			try
			{
				blockStack = (ItemStack)m_createStack.invoke(block, state);
			} catch(Exception e){}
		}
		
		this.subtypes = blockStack == null? true : !blockStack.getHasSubtypes();
		
		ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
		origTool = held == null? null : held.getItem();
		
		if(held == null)
		{
			toolProps = new ToolOverride("", -1);
		} else
		{
			toolProps = ToolOverrideHandler.INSTANCE.getOverride(held);
			
			if(toolProps == null)
			{
				toolProps = new ToolOverride("", -1);
			}
		}
		
		for(int i = -1; i <= 1; i++)
		{
			for(int j = -1; j <= 1; j++)
			{
				for(int k = -1; k <= 1; k++)
				{
					appendBlock(origin.add(i, j, k));
				}
			}
		}
	}
	
	/**
	 * Returns true if the miner is no longer valid or has completed
	 */
	public boolean tickMiner()
	{
		if(origin == null || player == null || !player.isEntityAlive() || mined.size() >= toolProps.getLimit())
		{
			return true;
		}
		
		for(int n = 0; scheduled.size() > 0; n++)
		{
			if(n >= toolProps.getSpeed() || mined.size() >= toolProps.getLimit())
			{
				break;
			}
			
			ItemStack heldStack = player.getHeldItem(EnumHand.MAIN_HAND);
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
			} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange())
			{
				mined.add(pos);
				continue;
			}
			
			IBlockState s = player.worldObj.getBlockState(pos);
			Block b = s.getBlock();
			int m = b.getMetaFromState(s);
			
			boolean flag = b == block && (subtypes || m == meta);
			
			if(!flag && blockStack != null)
			{
				ItemStack stack = null;
				
				try
				{
					stack = (ItemStack)m_createStack.invoke(b, s);
				} catch(Exception e){}
				
				if(stack != null && stack.getItem() == blockStack.getItem() && stack.getItemDamage() == blockStack.getItemDamage())
				{
					flag = true;
				}
			}
			
			if(flag)
			{
				if(!(ExcavationSettings.ignoreTools || b.canHarvestBlock(player.worldObj, pos, player)))
				{
					mined.add(pos);
					continue;
				} else if(player.interactionManager.tryHarvestBlock(pos))
				{
					if(player.isCreative())
					{
						player.getFoodStats().addExhaustion(ExcavationSettings.exaustion);
					}
					
					for(int i = -1; i <= 1; i++)
					{
						for(int j = -1; j <= 1; j++)
						{
							for(int k = -1; k <= 1; k++)
							{
								appendBlock(pos.add(i, j, k));
							}
						}
					}
				}
				
				mined.add(pos);
			}
		}
		
		return scheduled.size() <= 0 || mined.size() >= toolProps.getLimit();
	}
	
	/**
	 * Appends a block position to the miners current pass
	 */
	public void appendBlock(BlockPos pos)
	{
		if(pos == null || mined.contains(pos) || scheduled.contains(pos))
		{
			return;
		} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange())
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
			m_createStack = Block.class.getDeclaredMethod("func_180643_i", IBlockState.class);
			m_createStack.setAccessible(true);
		} catch(Exception e1)
		{
			try
			{
				m_createStack = Block.class.getDeclaredMethod("createStackedBlock", IBlockState.class);
				m_createStack.setAccessible(true);
			} catch(Exception e2)
			{
				OreExcavation.logger.log(Level.INFO, "Unable to use block hooks for excavation", e2);
			}
		}
	}
}
