package com.chai.miniGolf.commands;

import com.chai.miniGolf.models.Course;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.chai.miniGolf.Main.getPlugin;

public class DeleteCourseCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("mgop"))
        {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You do not have permission to run this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide a course name. Like: \"/mgdeletecourse Rolling Greens\".");
            return true;
        }
        Optional<Course> maybeCourse = getPlugin().config().courses().stream()
            .filter(c -> c.getName().equals(String.join(" ", args)))
            .findFirst();
        if (maybeCourse.isPresent()) {
            if (getPlugin().config().deleteCourse(maybeCourse.get())) {
                sender.sendMessage("Successfully deleted " + maybeCourse.get().getName());
            } else {
                sender.sendMessage("Error deleting " + maybeCourse.get().getName() + " check server logs for more details.");
            }
        } else {
            sender.sendMessage("No course named \"" + String.join(" ", args) + "\" exists.");
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return getPlugin().config().courses().stream().map(Course::getName)
            .filter(courseName -> courseName.toLowerCase().startsWith(String.join(" ", args).toLowerCase()))
            .map(courseName -> courseName.split(" "))
            .filter(courseNameArray -> courseNameArray.length >= args.length)
            .map(courseNameArray -> Arrays.copyOfRange(courseNameArray, args.length-1, courseNameArray.length))
            .map(courseNameArray -> String.join(" ", courseNameArray))
            .toList();
    }
}