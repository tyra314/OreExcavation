package oreexcavation.handlers;

import java.util.Arrays;
import net.minecraftforge.common.config.Configuration;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;

public class ConfigHandler
{
	public static Configuration config;
	
	public static void initConfigs()
	{
		if(config == null)
		{
			OreExcavation.logger.log(Level.ERROR, "Config attempted to be loaded before it was initialised!");
			return;
		}
		
		config.load();
		
		ExcavationSettings.hideUpdates = config.getBoolean("Hide Updates", Configuration.CATEGORY_GENERAL, false, "Hides update notifications");
		ExcavationSettings.mineLimit = config.getInt("Limit", Configuration.CATEGORY_GENERAL, 128, 1, Integer.MAX_VALUE, "The maximum number of blocks that can be excavated at once");
		ExcavationSettings.mineSpeed = config.getInt("Speed", Configuration.CATEGORY_GENERAL, 64, 1, Integer.MAX_VALUE, "How many blocks per tick can be excavated");
		ExcavationSettings.mineRange = config.getInt("Range", Configuration.CATEGORY_GENERAL, 16, 1, Integer.MAX_VALUE, "How far from the origin an excavation can travel");
		ExcavationSettings.exaustion = config.getFloat("Exaustion", Configuration.CATEGORY_GENERAL, 0.1F, 0F, Float.MAX_VALUE, "Amount of exaustion per block excavated");
		ExcavationSettings.openHand = config.getBoolean("Open Hand", Configuration.CATEGORY_GENERAL, true, "Allow excavation with an open hand");
		ExcavationSettings.ignoreTools = config.getBoolean("Ignore Tool", Configuration.CATEGORY_GENERAL, false, "Ignores whether or not the held tool is valid");
		ExcavationSettings.mineMode = config.getInt("Mode", Configuration.CATEGORY_GENERAL, 1, -1, 2, "Excavation mode (-1 Disabled, 0 = Normal, 1 = Sneak, 2 = Always)");
		
		String [] tbl = config.getStringList("Tool Blacklist", Configuration.CATEGORY_GENERAL, new String[0], "Tools blacklisted from excavating");
		String [] bbl = config.getStringList("Block Blacklist", Configuration.CATEGORY_GENERAL, new String[0], "Blocks blacklisted from being excavated");
		
		ExcavationSettings.toolBlacklist.clear();
		ExcavationSettings.toolBlacklist.addAll(Arrays.asList(tbl));
		
		ExcavationSettings.blockBlacklist.clear();
		ExcavationSettings.blockBlacklist.addAll(Arrays.asList(bbl));
		
		config.save();
		
		OreExcavation.logger.log(Level.INFO, "Loaded configs...");
	}
}
