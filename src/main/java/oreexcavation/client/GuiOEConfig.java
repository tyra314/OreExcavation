package oreexcavation.client;

import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.ConfigHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class GuiOEConfig extends GuiConfig
{
	public GuiOEConfig(GuiScreen parent)
	{
		super(parent, new ConfigElement(ConfigHandler.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(), OreExcavation.MODID, false, false, OreExcavation.NAME);
	}
}
