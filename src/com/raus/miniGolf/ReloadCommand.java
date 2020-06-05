package com.raus.miniGolf;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class ReloadCommand implements CommandExecutor
{
	private final Main plugin = JavaPlugin.getPlugin(Main.class);

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (args.length == 0)
		{
			return false;
		}
		else if (args[0].equals("info"))
		{
			// Send message
			sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.GRAY + " Version " + plugin.getDescription().getVersion());
			return true;
		}
		else if (args[0].equals("reload"))
		{
			if (!sender.hasPermission("minigolf.reload"))
			{
				// Send message
				sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You do not have permission to run this command.");
				return true;
			}

			// Reload config
			plugin.reload();

			// Send message
			sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.GRAY + " Config reloaded!");

			return true;
		}
		else
		{
			return false;
		}
	}
}