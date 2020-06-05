package com.raus.miniGolf;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class UnloadListener implements Listener
{
	private final Main plugin = JavaPlugin.getPlugin(Main.class);

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event)
	{
		// Get entities inside chunk
		Entity[] ents = event.getChunk().getEntities();

		// Find golf balls
		for (Entity ent : ents)
		{
			if (ent instanceof Snowball)
			{
				// Check if golf ball
				PersistentDataContainer c = ent.getPersistentDataContainer();
				if (!c.has(plugin.parKey, PersistentDataType.INTEGER))
				{
					return;
				}

				// Drop golf ball
				ent.getWorld().dropItem(ent.getLocation(), plugin.golfBall());
				ent.remove();
			}
		}
	}
}