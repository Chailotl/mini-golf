package com.chai.miniGolf.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static com.chai.miniGolf.Main.getPlugin;

public class InfoCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Send message
        sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.GRAY + " Version " + getPlugin().getDescription().getVersion());
        return true;
    }
}