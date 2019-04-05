package org.josefadventures.townyfly;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class TownyFly extends JavaPlugin implements Listener, CommandExecutor {

	private final String TOGGLE_SELF = "townyfly.toggle.self";
	private final String TOGGLE_OTHERS = "townyfly.toggle.others";
	private final String FLY_OTHERTOWN = "townyfly.othertown";

	private Logger logger;
	private FileConfiguration config;

	private ArrayList<UUID> playersWithTFly;

	private String disableMessage = ChatColor.GOLD + "Disabled townyfly for player " + ChatColor.GREEN + "{player}" + ChatColor.GOLD + ".";
	private String enableMessage = ChatColor.GOLD + "Enabled townyfly for player " + ChatColor.GREEN + "{player}" + ChatColor.GOLD + ".";
	
	@Override
	public void onEnable() {
		this.logger = this.getLogger();
		
		Towny towny = (Towny) this.getServer().getPluginManager().getPlugin("Towny");
		
		if (towny == null) {
			this.getPluginLoader().disablePlugin(this);
			this.logger.warning("TownyFly needs Towny to work.");
			return;
		} else {
			this.getServer().getPluginManager().registerEvents(this, this);
			this.logger.info("TownyFly running as should.");
		}

		this.playersWithTFly = new ArrayList<>();

		this.config = this.getConfig();
		this.config.addDefault("disableMessage", this.disableMessage);
		this.config.addDefault("enableMessage", this.enableMessage);
		this.config.options().copyDefaults(true);
		this.saveConfig();

		this.disableMessage = this.config.getString("disableMessage");
		this.enableMessage = this.config.getString("enableMessage");

		this.getCommand("townyfly").setExecutor(this);
	}
	
	@Override
	public void onDisable() {
		Player player;
		for (UUID uuid : playersWithTFly) {
			player = this.getServer().getPlayer(uuid);
			if (player != null) {
				tpToGround(player);
				player.setAllowFlight(false);
			}
		}
	}

	@EventHandler
	public void onLogout(PlayerQuitEvent e) {
		Player player = e.getPlayer();

		if (this.remove(player)) {
			this.tpToGround(player);
		}
	}


	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		Player player = e.getPlayer();

		this.remove(player);
	}


	@EventHandler
	public void onChangePlot(PlayerChangePlotEvent e) {
		Player player = e.getPlayer();

		Resident resident = null;
		Town town = null;

		if (this.has(player)) {
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				town = e.getTo().getTownBlock().getTown();

				if (resident.getTown() == town
						|| player.hasPermission(FLY_OTHERTOWN)) {
					player.setFallDistance(0f);
					player.setAllowFlight(true);
				} else {
					player.setFallDistance(0f);
					player.setAllowFlight(false);
					player.setFlying(false);
				}
			} catch (NotRegisteredException e1) {
				player.setFallDistance(0f);

				if (player.isFlying())
					this.tpToGround(player);

				player.setAllowFlight(false);
				player.setFlying(false);
			}
		}
	}

	private boolean add(Player player) {
		if (this.has(player)) {
			return false;
		}

		this.playersWithTFly.add(player.getUniqueId());
		return true;
	}


	private boolean remove(Player player) {
		if (!this.playersWithTFly.contains(player.getUniqueId())) {
			return false;
		}

		this.playersWithTFly.remove(player.getUniqueId());
		return true;
	}


	private boolean has(Player player) {
		return this.playersWithTFly.contains(player.getUniqueId());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length > 0) {
			if (sender.hasPermission(TOGGLE_OTHERS)) {
				Player target = Bukkit.getPlayer(args[0]);
				if (target != null) {
					toggleTFlyFor(target);
				}
				return true;
			}
		} else {
			if (sender.hasPermission(TOGGLE_SELF)) {
				if (sender instanceof Player) {
					toggleTFlyFor((Player) sender);
					return true;
				} else {
					sender.sendMessage("Console can only execute /tfly <player>.");
				}
			}
		}

		sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command.");
		return true;
	}

	private void toggleTFlyFor(Player player) {
		if (this.has(player)) {
			disableTFlyFor(player, false);
		} else {
			enableTFlyFor(player, false);
		}
	}

	private void disableTFlyFor(Player player, boolean silent) {
		this.remove(player);

		if (!silent)
			player.sendMessage(this.disableMessage.replace("{player}", player.getName()));

		player.setFallDistance(0f);
		player.setAllowFlight(false);
		player.setFlying(false);
	}

	private void enableTFlyFor(Player player, boolean silent) {
		this.add(player);

		if (!silent)
			player.sendMessage(this.enableMessage.replace("{player}", player.getName()));

		Resident resident = null;
		TownBlock townBlock = null;

		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			townBlock = TownyUniverse.getTownBlock(player.getLocation());

			if (townBlock.getTown() == resident.getTown()) {
				player.setFallDistance(0f);
				player.setAllowFlight(true);
			}
		} catch (NotRegisteredException|NullPointerException e) { }
	}

	private void tpToGround(Player player) {
		Location loc = player.getLocation();
		for (int y = loc.getBlockY(); 1 < y; y--) {
			loc.setY(y);
			if (loc.getBlock().getType() != Material.AIR) {
				player.teleport(loc.add(0, 2, 0));
				return;
			}
		}
		player.teleport(loc.add(0, 2, 0));
	}

}
