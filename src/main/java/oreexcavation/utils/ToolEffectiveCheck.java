package oreexcavation.utils;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import oreexcavation.core.ExcavationSettings;

public class ToolEffectiveCheck
{
	public static boolean canHarvestBlock(World world, Block block, int metadata, BlockPos pos, EntityPlayer player)
	{
		if(world == null || block == null || pos == null || player == null)
		{
			return false;
		}
		
		if(!ExcavationSettings.openHand && player.getHeldItem() == null)
		{
			return false;
		} else if(ExcavationSettings.toolClass)
		{
			ItemStack held = player.getHeldItem();
			
			if(held == null)
			{
				return false;
			}
			
			if(held.getItem() instanceof ItemShears && block instanceof IShearable)
			{
				return true;
			}
			
			for(String type : held.getItem().getToolClasses(held))
			{
				if(block.isToolEffective(type, metadata))
				{
					return true;
				}
			}
			
			return false;
		} else if(ExcavationSettings.altTools)
		{
			ItemStack held = player.getHeldItem();
			
			if(held != null && held.getItem().func_150893_a(held, block) > 1F)
			{
				return true;
			}
		}
		
		return block.canHarvestBlock(player, metadata);
	}
}
