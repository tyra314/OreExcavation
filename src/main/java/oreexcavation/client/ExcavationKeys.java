package oreexcavation.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import oreexcavation.core.OreExcavation;
import org.lwjgl.input.Keyboard;

public class ExcavationKeys
{
	public static final KeyBinding veinKey = new KeyBinding(OreExcavation.MODID + ".key", Keyboard.KEY_GRAVE, OreExcavation.NAME);
	
	public static void registerKeys()
	{
		ClientRegistry.registerKeyBinding(veinKey);
	}
}
