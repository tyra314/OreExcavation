package oreexcavation.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

public class MiningScheduler
{
	public static final MiningScheduler INSTANCE = new MiningScheduler();
	
	private HashMap<UUID,MiningAgent> agents = new HashMap<UUID,MiningAgent>();
	
	private MiningScheduler()
	{
	}
	
	public MiningAgent getActiveAgent(UUID uuid)
	{
		return agents.get(uuid);
	}
	
	public void stopMining(EntityPlayerMP player)
	{
		agents.remove(player.getUniqueID());
	}
	
	public void startMining(EntityPlayerMP player, BlockPos pos, IBlockState state)
	{
		MiningAgent existing = agents.get(player.getUniqueID());
		
		if(existing != null)
		{
			existing.appendBlock(pos);
		} else
		{
			existing = new MiningAgent(player, pos, state);
			agents.put(player.getUniqueID(), existing);
		}
	}
	
	public void tickAgents()
	{
		List<Entry<UUID,MiningAgent>> list = new ArrayList<Entry<UUID,MiningAgent>>(agents.entrySet());
		
		for(int i = list.size() - 1; i >= 0; i--)
		{
			Entry<UUID,MiningAgent> entry = list.get(i);
			
			if(entry.getValue().tickMiner())
			{
				agents.remove(entry.getKey());
			}
		}
	}
	
	public void resetAll()
	{
		agents.clear();
	}
}
