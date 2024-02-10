package com.chai.miniGolf.commands;

import com.chai.miniGolf.models.Course;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static com.chai.miniGolf.Main.getPlugin;

public class CreateCourseCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("mgop"))
        {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You do not have permission to run this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide a course name. Like: \"/mgcreatecourse Rolling Greens\".");
            return true;
        }
        // TODO: Check if course name already exists and prevent overwriting
        String courseName = String.join(" ", args);
        Course course = Course.newCourse(courseName);
        getPlugin().config().newCourseCreated(course);
        sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" +  " New Course created: " + course.getName());
        return true;
    }
}