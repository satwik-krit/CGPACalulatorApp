package com.example.cgpacalculator;

import java.util.ArrayList;
import java.util.List;

public class Semester {
    private String name;
    private List<Course> courses;

    public Semester(String name) {
        this.name = name;
        this.courses = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }

    public void addCourse(Course c) { courses.add(c); }
    public void removeCourse(int index) { courses.remove(index); }

    /** SGPA = weighted grade points / total credits for this semester */
    public double getSgpa() {
        double weighted = 0;
        int credits = 0;
        for (Course c : courses) {
            weighted += (double) c.getGradePoints() * c.getCredits();
            credits += c.getCredits();
        }
        return credits == 0 ? 0 : weighted / credits;
    }

    public int getTotalCredits() {
        int total = 0;
        for (Course c : courses) total += c.getCredits();
        return total;
    }
}
