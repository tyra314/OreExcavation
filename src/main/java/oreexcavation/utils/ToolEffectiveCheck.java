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
		
		if(ExcavationSettings.openHand && player.getHeldItem() == null && block.getMaterial().isToolNotRequired())
		{
			return true;
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
		} else
		{
			return block.canHarvestBlock(player, metadata);
		}
	}
}
