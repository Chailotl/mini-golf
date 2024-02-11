package com.chai.miniGolf.configs;

import com.chai.miniGolf.models.Course;
import com.chai.miniGolf.models.Hole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.chai.miniGolf.Main.getPlugin;
import static com.chai.miniGolf.Main.logger;
import static com.chai.miniGolf.utils.fileutils.FileUtils.loadOriginalConfig;

public class MiniGolfConfig {
    private static final String courseDirectory = "courses";
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final YamlConfiguration originalConfig;
    private List<Course> courses;
    private String scoreMsg;

    public MiniGolfConfig(YamlConfiguration config) {
        originalConfig = loadOriginalConfig("config.yml");
        updateOutOfDateConfig(config);
        loadConfig(config);
    }

    private void loadConfig(YamlConfiguration config) {
        scoreMsg = config.getString("scoreMsg", originalConfig.getString("scoreMsg"));
        courses = loadCourses();
    }

    private List<Course> loadCourses() {
        return Arrays.stream(new File(getPlugin().getDataFolder().getAbsolutePath() + File.separatorChar + courseDirectory).listFiles())
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

    public void newHoleCreated(Course course, int index, Hole hole) {
        course.addHole(hole, index);
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

    public String scoreMsg() {
        return scoreMsg;
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

    public void setHoleLocation(Course course, int holeIndex, Location holeLoc) {
        course.getHoles().get(holeIndex).setHoleLocX(holeLoc.getX());
        course.getHoles().get(holeIndex).setHoleLocY(holeLoc.getY());
        course.getHoles().get(holeIndex).setHoleLocZ(holeLoc.getZ());
        saveCourse(course);
    }
}
