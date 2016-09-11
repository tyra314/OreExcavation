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
	private float exaustion = 0.1F;
	private int experience = 0;
	
	public ToolOverride(String itemID, int metadata)
	{
		this.itemID = itemID;
		this.metadata = metadata;
		
		this.speed = ExcavationSettings.mineSpeed;
		this.limit = ExcavationSettings.mineLimit;
		this.range = ExcavationSettings.mineRange;
		this.exaustion = ExcavationSettings.exaustion;
		this.experience = ExcavationSettings.experience;
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
	
	public void setSpeed(int value)
	{
		this.speed = value;
	}
	
	public int getSpeed()
	{
		return speed;
	}
	
	public void setLimit(int value)
	{
		this.limit = value;
	}
	
	public int getLimit()
	{
		return limit;
	}
	
	public void setRange(int value)
	{
		this.range = value;
	}
	
	public int getRange()
	{
		return range;
	}
	
	public void setExaustion(float value)
	{
		this.exaustion = value;
	}
	
	public float getExaustion()
	{
		return exaustion;
	}
	
	public void setExperience(int value)
	{
		this.experience = value;
	}
	
	public int getExperience()
	{
		return experience;
	}
}
