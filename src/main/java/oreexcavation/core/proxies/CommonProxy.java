package oreexcavation.core.proxies;

import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.EventHandler;
import oreexcavation.network.PacketExcavation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy
{
	public boolean isClient()
	{
		return false;
	}
	
	public void registerHandlers()
	{
		EventHandler handler = new EventHandler();
		MinecraftForge.EVENT_BUS.register(handler);
		
		OreExcavation.instance.network.registerMessage(PacketExcavation.ServerHandler.class, PacketExcavation.class, 0, Side.SERVER);
	}
}
