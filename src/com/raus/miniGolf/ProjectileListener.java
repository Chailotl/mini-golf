package com.raus.miniGolf;

import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class ProjectileListener implements Listener
{
	private final Main plugin = JavaPlugin.getPlugin(Main.class);

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event)
	{
		Entity ent = event.getEntity();
		if (ent instanceof Snowball)
		{
			// Check if golf ball
			PersistentDataContainer c = ent.getPersistentDataContainer();
			if (!c.has(plugin.parKey, PersistentDataType.INTEGER))
			{
				return;
			}

			// Get info
			Location loc = ent.getLocation();
			Vector vel = ent.getVelocity();
			World world = ent.getWorld();

			// Spawn new golf ball
			Snowball ball = (Snowball) world.spawnEntity(loc, EntityType.SNOWBALL);
			ball.setGravity(ent.hasGravity());
			plugin.golfBalls.add(ball);

			// Update last player ball
			for (Entry<UUID, Snowball> entry : plugin.lastPlayerBall.entrySet())
			{
				// If same ball
				if (entry.getValue().equals(ent))
				{
					// Update to new ball
					entry.setValue(ball);
					break;
				}
			}

			// Par
			int par = c.get(plugin.parKey, PersistentDataType.INTEGER);
			PersistentDataContainer b = ball.getPersistentDataContainer();
			b.set(plugin.parKey, PersistentDataType.INTEGER, par);
			ball.setCustomName("Par " + par);
			ball.setCustomNameVisible(true);

			// Last pos
			double x = c.get(plugin.xKey, PersistentDataType.DOUBLE);
			double y = c.get(plugin.yKey, PersistentDataType.DOUBLE);
			double z = c.get(plugin.zKey, PersistentDataType.DOUBLE);
			b.set(plugin.xKey, PersistentDataType.DOUBLE, x);
			b.set(plugin.yKey, PersistentDataType.DOUBLE, y);
			b.set(plugin.zKey, PersistentDataType.DOUBLE, z);

			// Golf ball hit entity
			if (event.getHitBlockFace() == null)
			{
				// Move ball to last location
				ball.setVelocity(new Vector(0, 0, 0));
				ball.teleport(new Location(world, x, y, z));
				ball.setGravity(false);
				return;
			}

			// Bounce off surfaces
			Material mat = event.getHitBlock().getType();

			switch (event.getHitBlockFace())
			{
			case NORTH:
			case SOUTH:
				if (mat == Material.HONEY_BLOCK)
				{
					vel.setZ(0);
					//loc.setZ(Math.round(loc.getZ()));
					//ball.teleport(loc);
				}
				else if (mat == Material.SLIME_BLOCK)
				{
					vel.setZ(Math.copySign(0.25, -vel.getZ()));
				}
				else
				{
					vel.setZ(-vel.getZ());
				}
				break;

			case EAST:
			case WEST:
				if (mat == Material.HONEY_BLOCK)
				{
					vel.setX(0);
					//loc.setX(Math.round(loc.getX()));
					//ball.teleport(loc);
				}
				else if (mat == Material.SLIME_BLOCK)
				{
					vel.setX(Math.copySign(0.25, -vel.getX()));
				}
				else
				{
					vel.setX(-vel.getX());
				}
				break;

			case UP:
			case DOWN:
				if (event.getHitBlock().getType() == Material.SOUL_SAND || loc.getBlock().getType() == Material.WATER)
				{
					// Move ball to last location
					ball.setVelocity(new Vector(0, 0, 0));
					ball.teleport(new Location(world, x, y, z));
					ball.setGravity(false);
					return;
				}

				vel.setY(-vel.getY());
				vel.multiply(0.7);

				if (vel.getY() < 0.1)
				{
					vel.setY(0);
					loc.setY(Math.floor(loc.getY() * 2) / 2 + plugin.floorOffset);
					ball.teleport(loc);
					ball.setGravity(false);
				}
				break;

			default:
				break;
			}

			// Friction
			ball.setVelocity(vel);
		}
	}
}