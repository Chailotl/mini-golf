package com.chai.miniGolf.configs;

import com.chai.miniGolf.models.Course;
import com.chai.miniGolf.models.Hole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.chai.miniGolf.Main.getPlugin;
import static com.chai.miniGolf.Main.logger;
import static com.chai.miniGolf.utils.fileutils.FileUtils.loadOriginalConfig;

public class MiniGolfConfig {
    private static final String courseDirectory = "courses";
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final YamlConfiguration originalConfig;
    private List<Course> courses;
    @Getter private Map<String, ClubPower> clubPowerMap;
    @Getter private Double friction;
    @Getter private Double sandFriction;
    private String scoreMsg;
    private String courseCompletedMsg;

    public MiniGolfConfig(YamlConfiguration config) {
        originalConfig = loadOriginalConfig("config.yml");
        updateOutOfDateConfig(config);
        loadConfig(config);
    }

    private void loadConfig(YamlConfiguration config) {
        scoreMsg = config.getString("scoreMsg", originalConfig.getString("scoreMsg"));
        courseCompletedMsg = config.getString("courseCompletedMsg", originalConfig.getString("courseCompletedMsg"));
        clubPowerMap = new HashMap<>();
        clubPowerMap.put(getPlugin().putterKey.getKey(), loadClubPower(config, getPlugin().putterKey.getKey()));
        clubPowerMap.put(getPlugin().wedgeKey.getKey(), loadClubPower(config, getPlugin().wedgeKey.getKey()));
        clubPowerMap.put(getPlugin().ironKey.getKey(), loadClubPower(config, getPlugin().ironKey.getKey()));
        friction = config.getDouble("friction", originalConfig.getDouble("friction"));
        sandFriction = config.getDouble("sand_friction", originalConfig.getDouble("sand_friction"));
        courses = loadCourses();
    }

    private ClubPower loadClubPower(YamlConfiguration config, String club) {
        String clubPowerPrefix = "club_power." + club + ".";
        return ClubPower.builder()
            .minPowerSneaking(config.getDouble(clubPowerPrefix + "min_power_sneaking", originalConfig.getDouble(clubPowerPrefix + "min_power_sneaking")))
            .minYPowerSneaking(config.getDouble(clubPowerPrefix + "min_y_power_sneaking", originalConfig.getDouble(clubPowerPrefix + "min_y_power_sneaking")))
            .maxPowerSneaking(config.getDouble(clubPowerPrefix + "max_power_sneaking", originalConfig.getDouble(clubPowerPrefix + "max_power_sneaking")))
            .maxYPowerSneaking(config.getDouble(clubPowerPrefix + "max_y_power_sneaking", originalConfig.getDouble(clubPowerPrefix + "max_y_power_sneaking")))
            .minPowerStanding(config.getDouble(clubPowerPrefix + "min_power_standing", originalConfig.getDouble(clubPowerPrefix + "min_power_standing")))
            .minYPowerStanding(config.getDouble(clubPowerPrefix + "min_y_power_standing", originalConfig.getDouble(clubPowerPrefix + "min_y_power_standing")))
            .maxPowerStanding(config.getDouble(clubPowerPrefix + "max_power_standing", originalConfig.getDouble(clubPowerPrefix + "max_power_standing")))
            .maxYPowerStanding(config.getDouble(clubPowerPrefix + "max_y_power_standing", originalConfig.getDouble(clubPowerPrefix + "max_y_power_standing")))
            .minPowerCrit(config.getDouble(clubPowerPrefix + "min_power_crit", originalConfig.getDouble(clubPowerPrefix + "min_power_crit")))
            .minYPowerCrit(config.getDouble(clubPowerPrefix + "min_y_power_crit", originalConfig.getDouble(clubPowerPrefix + "min_y_power_crit")))
            .maxPowerCrit(config.getDouble(clubPowerPrefix + "max_power_crit", originalConfig.getDouble(clubPowerPrefix + "max_power_crit")))
            .maxYPowerCrit(config.getDouble(clubPowerPrefix + "max_y_power_crit", originalConfig.getDouble(clubPowerPrefix + "max_y_power_crit")))
            .build();
    }

    private List<Course> loadCourses() {
        File courseDir = new File(getPlugin().getDataFolder().getAbsolutePath() + File.separatorChar + courseDirectory);
        courseDir.mkdir();
        return Arrays.stream(courseDir.listFiles())
            .filter(f -> f.getName().endsWith(".yml"))
            .map(f -> {
                try {
                    return mapper.readValue(f, Course.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
    }

    public Optional<Course> getCourse(String courseName) {
        return courses.stream()
            .filter(c -> courseName.equals(c.getName()))
            .findFirst();
    }

    public List<Course> courses() {
        return List.copyOf(courses); // Immutable
    }

    public void newCourseCreated(Course course) {
        saveCourse(course);
    }

    public boolean deleteCourse(Course course) {
        File file = new File(getPlugin().getDataFolder().getAbsolutePath() + File.separatorChar + courseDirectory, course.getName() + ".yml");
        boolean wasSuccessful = file.delete();
        courses = loadCourses();
        return wasSuccessful;
    }

    public void newHoleCreated(Course course, int index, Hole hole) {
        course.addHole(hole, index);
        saveCourse(course);
    }

    public void removeHole(Course course, int holeToRemoveIndex) {
        course.removeHole(holeToRemoveIndex);
        saveCourse(course);
    }

    public void setParForHole(Course course, int holeIndex, int par) {
        course.getHoles().get(holeIndex).setPar(par);
        saveCourse(course);
    }

    private void updateOutOfDateConfig(YamlConfiguration config) {
        boolean madeAChange = false;
        for (String key : originalConfig.getKeys(true)) {
            if (!config.isString(key) && !config.isConfigurationSection(key) && !config.isBoolean(key) && !config.isDouble(key) && !config.isInt(key) && !config.isList(key)) {
                logger().info("The " + key + " is missing from config.yml, adding it now.");
                config.set(key, originalConfig.get(key));
                madeAChange = true;
            }
        }

        if (madeAChange) {
            try {
                config.save(getPlugin().getDataFolder() + "" + File.separatorChar + "config.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String scoreMsg(String v1, String v2, String v3) {
        return ChatColor.translateAlternateColorCodes('&',
            scoreMsg
                .replaceAll("&v1", v1)
                .replaceAll("&v2", v2)
                .replaceAll("&v3", v3));
    }

    public String courseCompletedMsg(String v1, String v2, String v3) {
        return ChatColor.translateAlternateColorCodes('&',
            courseCompletedMsg
                .replaceAll("&v1", v1)
                .replaceAll("&v2", v2)
                .replaceAll("&v3", v3));
    }

    private void saveCourse(Course course) {
        File file = new File(getPlugin().getDataFolder().getAbsolutePath() + File.separatorChar + courseDirectory, course.getName() + ".yml");
        try {
            file.getParentFile().mkdir();
            file.createNewFile();
            mapper.writeValue(file, course);
            courses = loadCourses();
        } catch (IOException e) {
            logger().severe("Unable to save the course: " + course.getName());
            logger().severe("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void setStartingLocation(Course course, int holeIndex, Location startingLoc) {
        course.getHoles().get(holeIndex).setStartingLocX(startingLoc.getX());
        course.getHoles().get(holeIndex).setStartingLocY(startingLoc.getY());
        course.getHoles().get(holeIndex).setStartingLocZ(startingLoc.getZ());
        course.getHoles().get(holeIndex).setStartingLocPitch(startingLoc.getPitch());
        course.getHoles().get(holeIndex).setStartingLocYaw(startingLoc.getYaw());
        saveCourse(course);
    }

    public void setBallStartingLocation(Course course, int holeIndex, Location startingBallLoc) {
        course.getHoles().get(holeIndex).setBallStartingLocX(startingBallLoc.getX());
        course.getHoles().get(holeIndex).setBallStartingLocY(startingBallLoc.getY());
        course.getHoles().get(holeIndex).setBallStartingLocZ(startingBallLoc.getZ());
        saveCourse(course);
    }

    public void setHoleLocation(Course course, int holeIndex, Location holeLoc) {
        course.getHoles().get(holeIndex).setHoleLocX(holeLoc.getX());
        course.getHoles().get(holeIndex).setHoleLocY(holeLoc.getY());
        course.getHoles().get(holeIndex).setHoleLocZ(holeLoc.getZ());
        saveCourse(course);
    }

    public void setCourseCompletionLocation(Course course, Location completionLoc) {
        course.setEndingLocX(completionLoc.getX());
        course.setEndingLocY(completionLoc.getY());
        course.setEndingLocZ(completionLoc.getZ());
        course.setEndingLocYaw(completionLoc.getYaw());
        course.setEndingLocPitch(completionLoc.getPitch());
        saveCourse(course);
    }

    public void newCourseScoreRecorded(Course course, Player golfer, int score) {
        if (course.playerGotNewPb(golfer, score)) {
            saveCourse(course);
        }
    }
}
