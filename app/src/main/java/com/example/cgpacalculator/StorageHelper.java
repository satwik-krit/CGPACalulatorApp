package com.example.cgpacalculator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper {
    private static StorageHelper instance;

    private static final String PREFS_NAME = "cgpa_prefs";
    private static final String KEY_SEMESTERS = "semesters";
    private static final String KEY_BACKLOG = "backlog_courses";


    private static final String LOGCATTAG = "StorageHelper";

    private SharedPreferences prefs;
    private Gson gson;

    public StorageHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static double calculateCGPA(List<Semester> semesterList) {
        double cgpa = 0.0;
        int i = 0;
        for (; i < semesterList.size(); i++) {
            cgpa += semesterList.get(i).getSGPA();
        }
        cgpa /= i;
        return cgpa;
    }

    public static StorageHelper getInstance(Context context) {
        if (instance == null) {
            instance = new StorageHelper(context);
        }
        return instance;
    }

    public void saveSemesters(List<Semester> semesters) {
        String json = gson.toJson(semesters);
        prefs.edit().putString(KEY_SEMESTERS, json).apply();
        Log.d(LOGCATTAG, "Saved semesters..");
    }

    public List<Semester> loadSemesters() {
        String json = prefs.getString(KEY_SEMESTERS, null);
        if (json == null) return new ArrayList<>();
        Log.d(LOGCATTAG, json);

        Type type = new TypeToken<List<Semester>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public List<BacklogCourse> loadBacklogCourses() {
        String json = prefs.getString(KEY_BACKLOG, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<BacklogCourse>>() {}.getType();
        List<BacklogCourse> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void saveBacklogCourses(List<BacklogCourse> courses) {
        prefs.edit().putString(KEY_BACKLOG, gson.toJson(courses)).apply();
    }

}
