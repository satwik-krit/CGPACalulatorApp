package com.example.cgpacalculator;

import java.util.ArrayList;
import java.util.List;

public class Semester {
    private int index;
    private List<Course> courses;

    public Semester(int index) {
        this.index = index;
        courses = new ArrayList<>();
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public List<Course> getCourses() { return courses; }

    public double getSGPA() {
        double totalWeighted = 0;
        int    totalCredits  = 0;
        for (Course c : courses) {
            totalWeighted += c.getGradePoints() * c.getCredits();
            totalCredits  += c.getCredits();
        }
        return totalCredits > 0 ? totalWeighted / totalCredits : 0;
    }

    public int getTotalCredits() {
        int total = 0;
        for (Course c : courses) total += c.getCredits();
        return total;
    }
}
