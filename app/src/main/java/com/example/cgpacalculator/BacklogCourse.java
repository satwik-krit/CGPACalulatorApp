package com.example.cgpacalculator;

public class BacklogCourse {

    public enum Status { FAILED, DROPPED, CLEARED }

    private String courseName;
    private int semesterNumber;
    private int credits;
    private double gradeObtained;   // 0 if dropped/not attempted
    private Status status;
    private int attemptCount;

    public BacklogCourse(String courseName, int semesterNumber, int credits,
                         double gradeObtained, Status status) {
        this.courseName = courseName;
        this.semesterNumber = semesterNumber;
        this.credits = credits;
        this.gradeObtained = gradeObtained;
        this.status = status;
        this.attemptCount = 1;
    }

    // --- Getters ---
    public String getCourseName()    { return courseName; }
    public int getSemesterNumber()   { return semesterNumber; }
    public int getCredits()          { return credits; }
    public double getGradeObtained() { return gradeObtained; }
    public Status getStatus()        { return status; }
    public int getAttemptCount()     { return attemptCount; }

    // --- Setters ---
    public void setCourseName(String courseName)       { this.courseName = courseName; }
    public void setSemesterNumber(int semesterNumber)  { this.semesterNumber = semesterNumber; }
    public void setCredits(int credits)                { this.credits = credits; }
    public void setGradeObtained(double grade)         { this.gradeObtained = grade; }
    public void setStatus(Status status)               { this.status = status; }
    public void setAttemptCount(int count)             { this.attemptCount = count; }

    public void markCleared(double newGrade) {
        this.status = Status.CLEARED;
        this.gradeObtained = newGrade;
        this.attemptCount++;
    }

    public String getMetaString() {
        return "Sem " + semesterNumber + " · " + credits + " credits";
    }

    public String getGradeLabel() {
        if (status == Status.DROPPED) return "Not attempted";
        return "Grade obtained: " + String.format("%.1f", gradeObtained);
    }
}
