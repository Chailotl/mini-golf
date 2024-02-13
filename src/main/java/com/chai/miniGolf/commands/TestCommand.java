package com.chai.miniGolf.commands;

import com.chai.miniGolf.events.NextHoleRequestedEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.chai.miniGolf.Main.getPlugin;

public class TestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("tc")) {
            executeTestCommand(args, (Player) sender);
        }
        return true;
    }

    private void executeTestCommand(String[] args, Player sender) {
        switch(args[0]) {
            case "nexthole":
                nexthole(sender);
                break;
            default:
                System.out.println("Unknown test command: " + args[0]);
        }
    }

    private void nexthole(Player sender) {
        Bukkit.getPluginManager().callEvent(new NextHoleRequestedEvent(sender, getPlugin().golfingCourseManager().getGolfingInfo(sender).getCourse()));
    }
}
