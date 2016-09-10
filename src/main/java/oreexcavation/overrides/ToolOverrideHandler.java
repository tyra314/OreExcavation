package oreexcavation.overrides;

import java.util.ArrayList;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.utils.JsonHelper;
import net.minecraft.item.ItemStack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ToolOverrideHandler
{
	public static ToolOverrideHandler INSTANCE = new ToolOverrideHandler();
	private ArrayList<ToolOverride> list = new ArrayList<ToolOverride>();
	
	private ToolOverrideHandler()
	{
	}
	
	public void loadOverrides(JsonObject json)
	{
		list.clear();
		for(JsonElement je : JsonHelper.GetArray(json, "overrides"))
		{
			if(je == null || !je.isJsonObject())
			{
				continue;
			}
			
			JsonObject jo = je.getAsJsonObject();
			ToolOverride to = new ToolOverride(JsonHelper.GetString(jo, "itemID", ""), JsonHelper.GetNumber(jo, "metadata", -1).intValue());
			to.setValues(JsonHelper.GetNumber(jo, "speed", ExcavationSettings.mineSpeed).intValue(), JsonHelper.GetNumber(jo, "limit", ExcavationSettings.mineLimit).intValue(), JsonHelper.GetNumber(jo, "range", ExcavationSettings.mineRange).intValue());
			list.add(to);
		}
	}
	
	public ToolOverride getOverride(ItemStack stack)
	{
		for(ToolOverride o : list)
		{
			if(o.isApplicable(stack))
			{
				return o;
			}
		}
		
		return null;
	}
	
	public JsonObject getDefaultOverrides()
	{
		JsonObject json = new JsonObject();
		JsonArray jAry = new JsonArray();
		
		JsonObject jo = new JsonObject();
		jo.addProperty("itemID", "minecraft:wooden_pickaxe");
		jo.addProperty("metadata", -1);
		jo.addProperty("speed", 1);
		jo.addProperty("limit", 0);
		jo.addProperty("range", 0);
		jAry.add(jo);
		
		jo = new JsonObject();
		jo.addProperty("itemID", "minecraft:wooden_shovel");
		jo.addProperty("metadata", -1);
		jo.addProperty("speed", 1);
		jo.addProperty("limit", 0);
		jo.addProperty("range", 0);
		jAry.add(jo);
		
		jo = new JsonObject();
		jo.addProperty("itemID", "minecraft:wooden_axe");
		jo.addProperty("metadata", -1);
		jo.addProperty("speed", 1);
		jo.addProperty("limit", 0);
		jo.addProperty("range", 0);
		jAry.add(jo);
		
		json.add("overrides", jAry);
		return json;
	}
}
