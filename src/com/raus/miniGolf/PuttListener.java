package com.raus.miniGolf;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.raus.shortUtils.ShortUtils;

public class PuttListener implements Listener
{
	private final Main plugin = JavaPlugin.getPlugin(Main.class);

	@EventHandler
	public void onPutt(PlayerInteractEvent event)
	{
		// Cancel if interacting with environment
		if (ShortUtils.interacting(event))
		{
			return;
		}

		// Get item
		ItemStack item = event.getItem();
		if (item == null) { return; }

		// Get info
		Player ply = event.getPlayer();
		World world = ply.getWorld();
		Action act = event.getAction();
		Block block = event.getClickedBlock();
		ItemMeta meta = item.getItemMeta();

		// Get type of golf club
		boolean putter = ShortUtils.hasKey(meta, plugin.putterKey);
		boolean iron = ShortUtils.hasKey(meta, plugin.ironKey);
		boolean wedge = ShortUtils.hasKey(meta, plugin.wedgeKey);

		if (putter || iron || wedge)
		{
			// Cancel original tool
			ShortUtils.cancelOriginalTool(event);

			// Find entities
			List<Entity> ents = ply.getNearbyEntities(5.5, 5.5, 5.5);

			Location eye = ply.getEyeLocation();
			Vector dir = eye.getDirection();
			Vector loc = eye.toVector();

			for (Entity ent : ents)
			{
				// Look for golf balls
				PersistentDataContainer c = ent.getPersistentDataContainer();

				if (ent instanceof Snowball && c.has(plugin.parKey, PersistentDataType.INTEGER))
				{
					// Is golf ball in player's view?
					Vector vec = ent.getLocation().toVector().subtract(loc);

					if (dir.angle(vec) < 0.15f)
					{
						// Are we allowed to hit this ball?
						boolean skip = false;
						for (Entry<UUID, Snowball> entry : plugin.lastPlayerBall.entrySet())
						{
							// Find the ball and check the owner
							if (entry.getValue().equals(ent) &&
									!entry.getKey().equals(ply.getUniqueId()))
							{
								skip = true;
								break;
							}
						}
						if (skip) { continue; }

						// Are we hitting or picking up the golf ball?
						if (act == Action.LEFT_CLICK_AIR || act == Action.LEFT_CLICK_BLOCK)
						{
							// Hit golf ball
							dir.setY(0).normalize();

							boolean sneak = ply.isSneaking();
							boolean crit = ply.getVelocity().getY() < -0.08;
							if (iron)
							{
								dir.multiply(crit ? 1 : sneak ? 0.6666 : 0.8333);
							}
							else if (putter)
							{
								dir.multiply(crit ? 0.5 : sneak ? 0.1666 : 0.3333);
							}
							else if (wedge)
							{
								dir.multiply(crit ? 0 : sneak ? 0.125 : 0.25);
								dir.setY(0.15);
							}

							ent.setVelocity(dir);

							// Update par
							int par = c.get(plugin.parKey, PersistentDataType.INTEGER) + 1;
							c.set(plugin.parKey, PersistentDataType.INTEGER, par);
							ent.setCustomName("Par " + par);

							// Update last pos
							Location lastPos = ent.getLocation();
							c.set(plugin.xKey, PersistentDataType.DOUBLE, lastPos.getX());
							c.set(plugin.yKey, PersistentDataType.DOUBLE, lastPos.getY());
							c.set(plugin.zKey, PersistentDataType.DOUBLE, lastPos.getZ());

							// Add to map
							plugin.golfBalls.add((Snowball) ent);
							ent.setTicksLived(1);

							// Effects
							if (crit)
							{
								world.spawnParticle(Particle.CRIT, ent.getLocation(), 15, 0, 0, 0, 0.25);
							}
							world.playSound(ent.getLocation(), Sound.BLOCK_METAL_HIT, crit ? 1f : sneak ? 0.5f : 0.75f, 1.25f);
						}
						else if (ent.isValid())
						{
							// Drop golf ball
							ent.remove();
							world.dropItem(ent.getLocation(), plugin.golfBall());
						}
					}
				}
			}
		}
		else if (ShortUtils.hasKey(meta, plugin.ballKey))
		{
			// Cancel original tool
			ShortUtils.cancelOriginalTool(event);

			// Is player placing golf ball?
			if (act == Action.RIGHT_CLICK_BLOCK)
			{
				// Get spawn location
				Location loc;
				if (plugin.isBottomSlab(block))
				{
					loc = block.getLocation().add(0.5, 0.5 + plugin.floorOffset, 0.5);
				}
				else
				{
					loc = block.getLocation().add(0.5, 1 + plugin.floorOffset, 0.5);
				}

				// Spawn golf ball and set data
				Snowball ball = (Snowball) world.spawnEntity(loc, EntityType.SNOWBALL);
				ball.setGravity(false);
				PersistentDataContainer c = ball.getPersistentDataContainer();
				c.set(plugin.xKey, PersistentDataType.DOUBLE, loc.getX());
				c.set(plugin.yKey, PersistentDataType.DOUBLE, loc.getY());
				c.set(plugin.zKey, PersistentDataType.DOUBLE, loc.getZ());
				c.set(plugin.parKey, PersistentDataType.INTEGER, 0);
				ball.setCustomName("Par 0");
				ball.setCustomNameVisible(true);
				plugin.golfBalls.add(ball);

				// Remove golf ball from inventory
				if (ply.getGameMode() != GameMode.CREATIVE)
				{
					ItemStack itemInHand = event.getItem();
					itemInHand.setAmount(itemInHand.getAmount() - 1);
				}

				// Add last player ball
				plugin.lastPlayerBall.put(ply.getUniqueId(), ball);
			}
		}
		else if (ShortUtils.hasKey(meta, plugin.whistleKey))
		{
			// Return ball
			if (act == Action.RIGHT_CLICK_BLOCK || act == Action.RIGHT_CLICK_AIR)
			{
				// Get last player ball
				Snowball ball = plugin.lastPlayerBall.get(ply.getUniqueId());
				if (ball == null || !ball.isValid())
				{
					// Clean up
					plugin.lastPlayerBall.remove(ply.getUniqueId());
					return;
				}
				PersistentDataContainer c = ball.getPersistentDataContainer();

				// Read persistent data
				double x = c.get(plugin.xKey, PersistentDataType.DOUBLE);
				double y = c.get(plugin.yKey, PersistentDataType.DOUBLE);
				double z = c.get(plugin.zKey, PersistentDataType.DOUBLE);

				// Move ball to last location
				ball.setVelocity(new Vector(0, 0, 0));
				ball.teleport(new Location(world, x, y, z));
				ball.setGravity(false);

				// Sound
				world.playSound(ply.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.9f);
			}
		}
	}



	/*@EventHandler
	private void onDispense(BlockDispenseEvent event)
	{
		// Check if golf ball
		ItemStack item = event.getItem();

		PersistentDataContainer c = item.getItemMeta().getPersistentDataContainer();
		if (!c.has(plugin.ballKey(), PersistentDataType.BYTE))
		{
			return;
		}

		// Get info
		Block block = event.getBlock();
		Directional directional = (Directional) block.getBlockData();

		// Get direction
		Location loc = block.getLocation();
		switch (directional.getFacing())
		{
		case UP:
			loc.add(0.5, 1 + floorOffset, 0.5);
			break;

		case DOWN:
			loc.add(0, -floorOffset, 0);
			break;

		case NORTH:
			loc.add(0.5, 1.01, 0.5);
			break;

		case SOUTH:
			loc.add(0.5, 1.01, 0.5);
			break;

		case EAST:
			loc.add(0.5, 1.01, 0.5);
			break;

		case WEST:
			loc.add(0.5, 1.01, 0.5);
			break;

		default:
			break;
		}

		// Spawn golf ball
		Snowball ball = (Snowball) block.getWorld().spawnEntity(loc, EntityType.SNOWBALL);
		ball.setGravity(false);
		c = ball.getPersistentDataContainer();
		c.set(xKey, PersistentDataType.DOUBLE, loc.getX());
		c.set(yKey, PersistentDataType.DOUBLE, loc.getY());
		c.set(zKey, PersistentDataType.DOUBLE, loc.getZ());
		c.set(parKey, PersistentDataType.INTEGER, 0);
		ball.setCustomName("Par 0");
		ball.setCustomNameVisible(true);
		golfBalls.add(ball);

		item.setAmount(item.getAmount() - 1);
		event.setCancelled(true);
	}*/
}