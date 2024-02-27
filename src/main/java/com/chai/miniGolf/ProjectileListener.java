package com.chai.miniGolf;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import com.chai.miniGolf.managers.GolfingCourseManager.GolfingInfo;
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

import static com.chai.miniGolf.Main.getPlugin;

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
			Optional<GolfingInfo> golfingInfo = getPlugin().golfingCourseManager().getGolfingInfoFromGolfball((Snowball) ent);
			if (!c.has(plugin.strokesKey, PersistentDataType.INTEGER) || golfingInfo.isEmpty()) {
				return;
			}

			// Golf ball hit entity
			if (event.getHitBlockFace() == null)
			{
				event.setCancelled(true);
				return;
			}

			// Get info
			Location loc = ent.getLocation();
			Vector vel = ent.getVelocity();
			World world = ent.getWorld();

			// Spawn new golf ball
			Snowball ball = (Snowball) world.spawnEntity(loc, EntityType.SNOWBALL);
			ball.setGravity(ent.hasGravity());
			golfingInfo.get().setGolfball(ball);

			// Par
			int par = c.get(plugin.strokesKey, PersistentDataType.INTEGER);
			String owner = c.get(plugin.ownerNameKey, PersistentDataType.STRING);
			PersistentDataContainer b = ball.getPersistentDataContainer();
			b.set(plugin.strokesKey, PersistentDataType.INTEGER, par);
			b.set(plugin.ownerNameKey, PersistentDataType.STRING, owner);
			ball.setCustomName(owner + " - " + par);
			ball.setCustomNameVisible(true);

			// Last pos
			double x = c.get(plugin.xKey, PersistentDataType.DOUBLE);
			double y = c.get(plugin.yKey, PersistentDataType.DOUBLE);
			double z = c.get(plugin.zKey, PersistentDataType.DOUBLE);
			b.set(plugin.xKey, PersistentDataType.DOUBLE, x);
			b.set(plugin.yKey, PersistentDataType.DOUBLE, y);
			b.set(plugin.zKey, PersistentDataType.DOUBLE, z);

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