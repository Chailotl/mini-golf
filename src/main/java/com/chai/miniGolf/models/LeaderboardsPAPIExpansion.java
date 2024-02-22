package com.chai.miniGolf.models;

import com.chai.miniGolf.Main;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LeaderboardsPAPIExpansion extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "yardminigolf";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TheYard";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("get")) {
            List<String> paramsList = List.of(params.split("_"));
            if (paramsList.size() < 3) {
                return null;
            }
            String courseName = paramsList.get(1).replace("-", " ");
            int placement;
            try {
                placement = Integer.parseInt(paramsList.get(2));
            } catch (NumberFormatException ex) {
                return null;
            }
            Optional<Course> maybeCourse = Main.getPlugin().config().courses().stream()
                .filter(c -> c.getName().equals(courseName))
                .findFirst();
            if (maybeCourse.isEmpty()) {
                return null;
            }
            Comparator<Map.Entry<String, Score>> scoreComparator = Comparator.comparingInt(e -> e.getValue().getScore());
            Comparator<Map.Entry<String, Score>> thenComparator = Comparator.comparingLong(e -> e.getValue().getTimestamp());
            List<Map.Entry<String, Score>> scores = maybeCourse.get().getLeaderboards().entrySet().stream()
                .sorted(scoreComparator.thenComparing(thenComparator))
                .toList();
            if (scores.size() >= placement && placement >= 1) {
                return String.format("%s - %s", scores.get(placement-1).getKey(), scores.get(placement-1).getValue().getScore());
            } else {
                return "No score";
            }
        }
        return null;
    }
}
