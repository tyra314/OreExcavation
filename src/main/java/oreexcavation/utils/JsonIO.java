package oreexcavation.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * IO methods for reading/writing JsonObjects to/from files plus other auxiliary manipulations.
 * Using these will also put aside malformed files for safe keeping
 */
public class JsonIO
{
	public static JsonObject ReadFromFile(File file)
	{
		if(file == null || !file.exists())
		{
			return new JsonObject();
		}
		
		try
		{
			FileReader fr = new FileReader(file);
			JsonObject json = new Gson().fromJson(fr, JsonObject.class);
			fr.close();
			return json;
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "An error occured while loading JSON from file:", e);
			
			int i = 0;
			File bkup = new File(file.getParent(), "malformed_" + file.getName() + i + ".json");
			
			while(bkup.exists())
			{
				i++;
				bkup = new File(file.getParent(), "malformed_" + file.getName() + i + ".json");
			}
			
			OreExcavation.logger.log(Level.ERROR, "Creating backup at: " + bkup.getAbsolutePath());
			CopyPaste(file, bkup);
			
			return new JsonObject(); // Just a safety measure against NPEs
		}
	}
	
	public static void WriteToFile(File file, JsonObject jObj)
	{
		try
		{
			if(!file.exists())
			{
				if(file.getParentFile() != null)
				{
					file.getParentFile().mkdirs();
				}
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(file);
			new GsonBuilder().setPrettyPrinting().create().toJson(jObj, fw);
			fw.close();
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "An error occured while saving JSON to file:", e);
			return;
		}
	}
	
	public static void CopyPaste(File fileIn, File fileOut)
	{
		try
		{
			FileReader fr = new FileReader(fileIn);
			FileWriter fw = new FileWriter(fileOut);
			
			char[] buf = new char[256];
			while(fr.ready())
			{
				fr.read(buf);
				fw.write(buf);
			}
			
			fr.close();
			fw.close();
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.ERROR, "Failed copy paste", e);
		}
	}
}
