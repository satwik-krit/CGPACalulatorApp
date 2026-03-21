package com.example.cgpacalculator;

public class Course {
    private String name;
    private int credits;
    private String grade;

    public Course(String name, int credits, String grade) {
        this.name = name;
        this.credits = credits;
        this.grade = grade;
    }

    public String getName() { return name; }
    public int getCredits() { return credits; }
    public String getGrade() { return grade; }

    public void setName(String name) { this.name  = name; }
    public void settCredits(int credits) { this.credits = credits; }
    public void setGrade(String grade) { this.grade = grade; }

    public int getGradePoints() {
        // TODO: Support for multiple formats
        switch(grade) {
            case "A": return 10;
            case "A-": return 9;
            case "B": return 8;
            case "B-": return 7;
            case "C": return 6;
            case "C-": return 5;
            case "D": return 4;
            default : return 0; // F
        }
    }
}
