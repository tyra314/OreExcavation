package oreexcavation.overrides;

import oreexcavation.core.ExcavationSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class ToolOverride
{
	private final String itemID;
	private final int metadata;
	
	private int speed = 64;
	private int limit = 128;
	private int range = 16;
	
	public ToolOverride(String itemID, int metadata)
	{
		this.itemID = itemID;
		this.metadata = metadata;
		
		this.speed = ExcavationSettings.mineSpeed;
		this.limit = ExcavationSettings.mineLimit;
		this.range = ExcavationSettings.mineRange;
	}
	
	public ToolOverride setValues(int speed, int limit, int range)
	{
		this.speed = Math.max(1, speed);
		this.limit = limit;
		this.range = range;
		return this;
	}
	
	public boolean isApplicable(ItemStack stack)
	{
		if(stack == null)
		{
			return false;
		}
		
		String id = Item.itemRegistry.getNameForObject(stack.getItem());
		int m = stack.getItemDamage();
		
		if(itemID != null && itemID.length() > 0 && !id.equalsIgnoreCase(itemID))
		{
			return false;
		} else if(!(metadata == OreDictionary.WILDCARD_VALUE) && metadata >= 0 && m != metadata)
		{
			return false;
		}
		
		return true;
	}
	
	public int getSpeed()
	{
		return speed;
	}
	
	public int getLimit()
	{
		return limit;
	}
	
	public int getRange()
	{
		return range;
	}
}
