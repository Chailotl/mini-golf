package com.chai.miniGolf.commands;

import com.chai.miniGolf.models.Course;
import com.chai.miniGolf.models.Hole;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static com.chai.miniGolf.Main.getPlugin;
import static org.bukkit.Material.CAULDRON;

public class EditCourseCommand implements CommandExecutor, TabCompleter {
    private static final Map<UUID, Course> playersEditingCourses = new HashMap<>();
    private static final Map<String, BiFunction<String[], Player, Boolean>> editActions = Map.of(
        "addhole", EditCourseCommand::addHole,
        "setpar", EditCourseCommand::setPar,
        "setstartinglocation", EditCourseCommand::setStartingLocation,
        "setholelocation", EditCourseCommand::setHoleLocation,
        "doneediting", EditCourseCommand::doneEditing
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mgop")) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You do not have permission to run this command.");
            return true;
        }
        if (!playersEditingCourses.containsKey(((Player)sender).getUniqueId())) {
            return handleStartingToEdit((Player) sender, command, label, args);
        } else {
            return handleAlreadyEditing((Player) sender, command, label, args);
        }
    }

    private boolean handleAlreadyEditing(Player sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide an action. Like: \"/mgedit Rolling Greens\".");
            return true;
        } else {
            BiFunction<String[], Player, Boolean> action = editActions.get(args[0]);
            if (action == null) {
                sender.sendMessage(String.format("%s[MiniGolf]%s %s is not a valid action. Valid actions are: %s[%s]%s", ChatColor.WHITE, ChatColor.RED, args[0], ChatColor.WHITE, String.join(", "), ChatColor.RESET));
                return true;
            }
            return action.apply(Arrays.copyOfRange(args, 1, args.length), sender);
        }
    }

    private boolean handleStartingToEdit(Player sender, Command command, String label, String[] args) {
        if (args.length < 1 && !playersEditingCourses.containsKey(sender.getUniqueId())) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide a course name. Like: \"/mgedit Rolling Greens\".");
            return true;
        }
        String courseName = String.join(" ", args);
        Optional<Course> maybeCourse = getPlugin().config().getCourse(courseName);
        if (maybeCourse.isEmpty()) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + courseName + " is not a course that exists.");
            return true;
        }
        playersEditingCourses.put(sender.getUniqueId(), maybeCourse.get());
        sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" +  " Now editing: " + maybeCourse.get().getName());
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length > 0 && !playersEditingCourses.containsKey(((Player)commandSender).getUniqueId())) {
            return getPlugin().config().courses().stream().map(Course::getName)
                .filter(courseName -> courseName.toLowerCase().startsWith(String.join(" ", args).toLowerCase()))
                .map(courseName -> courseName.split(" "))
                .filter(courseNameArray -> courseNameArray.length >= args.length)
                .map(courseNameArray -> Arrays.copyOfRange(courseNameArray, args.length-1, courseNameArray.length))
                .map(courseNameArray -> String.join(" ", courseNameArray))
                .toList();
        } else if (args.length > 0) {
            Course course = playersEditingCourses.get(((Player)commandSender).getUniqueId());
            if (args.length == 1) {
                return editActions.keySet().stream().filter(a -> a.startsWith(args[0].toLowerCase())).toList();
            } else if (args.length == 2 && "addhole".equals(args[0])) {
                return IntStream.range(0, course.getHoles().size()+1).boxed().map(String::valueOf).toList();
            } else if (args.length == 2 && List.of("setpar", "setstartinglocation", "setholelocation").contains(args[0]) && !course.getHoles().isEmpty()) {
                return IntStream.range(0, course.getHoles().size()).boxed().map(String::valueOf).toList();
            }
        }
        return List.of();
    }

    private static Boolean addHole(String[] args, Player sender) {
        int newHoleIndex = playersEditingCourses.get(sender.getUniqueId()).getHoles().size();
        if (args.length > 0) {
            try {
                newHoleIndex = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + args[0] + " is not a valid Hole index.");
                return true;
            }
        }
        if (newHoleIndex > playersEditingCourses.get(sender.getUniqueId()).getHoles().size()) {
            sender.sendMessage(String.format("%s[MiniGolf]%s There are only %s holes, you cannot add a hole with index %s%s", ChatColor.WHITE, ChatColor.RED, playersEditingCourses.get(sender.getUniqueId()).getHoles().size(), newHoleIndex, ChatColor.RESET));
            return true;
        }
        sender.sendMessage(String.format("%s[MiniGolf] Adding hole at index %s, any holes behind will be pushed back an index%s", ChatColor.WHITE, newHoleIndex, ChatColor.RESET));
        Location startingLoc = sender.getLocation();
        getPlugin().config().newHoleCreated(playersEditingCourses.get(sender.getUniqueId()), newHoleIndex, Hole.newHole(1, startingLoc, startingLoc));
        return true;
    }

    private static Boolean setPar(String[] args, Player sender) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " must provide an index and a par value. Like this: \"/mgedit setpar 0 4\".");
            return true;
        }
        int holeIndex;
        int par;
        try {
            holeIndex = Integer.parseInt(args[0]);
            par = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(String.format("%s[MiniGolf]%s One of the following was not an integer: [%s, %s]%s", ChatColor.WHITE, ChatColor.RED, args[0], args[1], ChatColor.RESET));
            return true;
        }
        Course course = playersEditingCourses.get(sender.getUniqueId());
        if (holeIndex < 0 || holeIndex >= course.getHoles().size()) {
            sender.sendMessage(String.format("%s[MiniGolf]%s %s is an index out of bounds (remember indices start at 0). There are %s holes.%s", ChatColor.WHITE, ChatColor.RED, holeIndex, course.getHoles().size(), ChatColor.RESET));
            return true;
        }
        sender.sendMessage(String.format("%s[MiniGolf] Setting par for hole %s to %s%s", ChatColor.WHITE, holeIndex, par, ChatColor.RESET));
        getPlugin().config().setParForHole(course, holeIndex, par);
        return true;
    }

    private static Boolean setStartingLocation(String[] args, Player sender) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " Must provide a hole index to set the starting location for. Like this: \"/mgedit setstartinglocation 2\".");
            return true;
        }
        int holeIndex;
        try {
            holeIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + args[0] + " is not a valid Hole index.");
            return true;
        }
        Course course = playersEditingCourses.get(sender.getUniqueId());
        if (holeIndex < 0 || holeIndex >= course.getHoles().size()) {
            sender.sendMessage(String.format("%s[MiniGolf]%s %s is an index out of bounds (remember indices start at 0). There are %s holes.%s", ChatColor.WHITE, ChatColor.RED, holeIndex, course.getHoles().size(), ChatColor.RESET));
            return true;
        }
        Location startingLoc = sender.getLocation();
        sender.sendMessage(String.format("%s[MiniGolf] Setting Starting Location for hole %s to your current location%s", ChatColor.WHITE, holeIndex, ChatColor.RESET));
        getPlugin().config().setStartingLocation(course, holeIndex, startingLoc);
        return true;
    }

    private static Boolean setHoleLocation(String[] args, Player sender) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " Must provide a hole index to set the hole location for. Like this: \"/mgedit setholelocation 2\".");
            return true;
        }
        int holeIndex;
        try {
            holeIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + args[0] + " is not a valid Hole index.");
            return true;
        }
        Course course = playersEditingCourses.get(sender.getUniqueId());
        if (holeIndex < 0 || holeIndex >= course.getHoles().size()) {
            sender.sendMessage(String.format("%s[MiniGolf]%s %s is an index out of bounds (remember indices start at 0). There are %s holes.%s", ChatColor.WHITE, ChatColor.RED, holeIndex, course.getHoles().size(), ChatColor.RESET));
            return true;
        }
        Location holeLoc = sender.getLocation();
        if (!holeLoc.getBlock().getType().equals(CAULDRON)) {
            sender.sendMessage(String.format("%s[MiniGolf]%s The hole must be a cauldron.%s", ChatColor.WHITE, ChatColor.RED, ChatColor.RESET));
            return true;
        }
        sender.sendMessage(String.format("%s[MiniGolf] Setting Hole Location for hole %s to the cauldron beneath you%s", ChatColor.WHITE, holeIndex, ChatColor.RESET));
        getPlugin().config().setHoleLocation(course, holeIndex, holeLoc);
        return true;
    }

    private static Boolean doneEditing(String[] args, Player sender) {
        sender.sendMessage(String.format("%s[MiniGolf] Finished editing \"%s\"%s", ChatColor.WHITE, playersEditingCourses.get(sender.getUniqueId()).getName(), ChatColor.RESET));
        playersEditingCourses.remove(sender.getUniqueId());
        return true;
    }
}