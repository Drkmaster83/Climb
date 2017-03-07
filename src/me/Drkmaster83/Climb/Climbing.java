package me.Drkmaster83.Climb;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Climbing extends JavaPlugin implements Listener
{
	public ArrayList<Climber> climbing = new ArrayList<>();
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		for(Player p : getServer().getOnlinePlayers()) {
			climbing.add(new Climber(p));
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				for(Climber c : climbing) {
					if(!c.getPlayer().isFlying()) c.climb();
				}
			}
		}.runTaskTimer(this, 0L, 2L);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		climbing.add(new Climber(event.getPlayer()));
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		climbing.remove(getClimber(event.getPlayer()));
	}
	
	public Climber getClimber(Player p) {
		for(Climber c : climbing) {
			if(!c.getPlayer().getUniqueId().equals(p.getUniqueId())) continue;
			return c;
		}
		return null;
	}
	
	private class Climber
	{
		private Player player;
		private boolean climbing, mounting, canReclimb;
		private int prevMaxY, ticksMounting, maxMount, reclimbFallLimit;
		private double climbRate;
		
		public Climber(Player player) {
			this.player = player;
			this.climbing = true;
			this.canReclimb = true;
			this.mounting = false;
			this.ticksMounting = -1;
			this.maxMount = 3;
			this.reclimbFallLimit = 11;
			this.climbRate = .1;
		}
		
		public Player getPlayer() {
			return player;
		}
		
		public int getPrevY() {
			return prevMaxY;
		}
		
		public void climb() {
			Location l = player.getLocation();
			if(l.getBlockY() > prevMaxY) prevMaxY = l.getBlockY();
			if(!nearWall(.5)) {
				climbing = false;
				if(ticksMounting >= 0 && ticksMounting <= maxMount && canReclimb) { //TODO: Maybe put a check for l.getPitch() < -30 || l.getPitch() > 30, like climbing?
					mounting = true;                           //Only reason I haven't is so we can look horizontally to get on the block we're mounting.
					if(l.getBlockY() > prevMaxY) prevMaxY = l.getBlockY();
					ticksMounting++;
					player.setFallDistance(0.0f);
				}
				else { //Not climbing or mounting, likely just walking or flying (... or falling)
					if(prevMaxY - l.getBlockY() > reclimbFallLimit) canReclimb = false;
					mounting = false;
					ticksMounting = -1;
				}
			}
			else {
				mounting = false;
				if(!canReclimb) {
					if(player.isOnGround()) { //Not falling and near a wall, allow them to climb.
						canReclimb = true;
						prevMaxY = l.getBlockY();
					}
					else {
						canReclimb = false; //Still falling and trying to grab the wall
					}
				}
				if(canReclimb) { //Either restarting or just climbing for the first time
					//if(l.add(l.getDirection().normalize()).getBlock().getFace(l.getBlock()).) //attempt at rejection, see if we're looking at a block and then detect if we can scale that block
					climbing = true; //Removed check for  < -30 || > 30, else = false;
					ticksMounting = 0;
				}
			}
			
			if(climbing || mounting) {
				player.setVelocity(player.getVelocity().setY(climbing ? (l.getPitch() < -30 ? 1 : -1)*climbRate : climbRate));
				player.setGravity(false);
			}
			else player.setGravity(true);
		}
		
		public boolean isClimbing() {
			return climbing;
		}
		
		public boolean isMounting() {
			return mounting;
		}
		
		/** @Precondition: 0 <= dist <= 1 */
		public boolean nearWall(double dist) {
			Vector locale = player.getLocation().toVector();
			int y = locale.getBlockY() + 1;
			double x = locale.getX(), z = locale.getZ();
			World world = player.getWorld();
			Block b1 = world.getBlockAt(new Location(world, x + dist, y, z));
			Block b2 = world.getBlockAt(new Location(world, x - dist, y, z));
			Block b3 = world.getBlockAt(new Location(world, x, y, z + dist));
			Block b4 = world.getBlockAt(new Location(world, x, y, z - dist));
			if ((b1.getType() != Material.AIR) || (b2.getType() != Material.AIR) || (b3.getType() != Material.AIR) || (b4.getType() != Material.AIR))
				return true;
			return false;
		}
	}
}