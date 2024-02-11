package com.chai.miniGolf.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Jacksonized
@Builder
@Data
public class Course {
    private String name;
    private List<Hole> holes;

    private Course(String name, List<Hole> holes) {
        this.name = name;
        this.holes = holes;
    }

    public static Course newCourse(String name) {
        return new Course(name, new ArrayList<>());
    }

    public void addHole(Hole hole, Integer index) {
        this.holes.add(index, hole);
    }
}
