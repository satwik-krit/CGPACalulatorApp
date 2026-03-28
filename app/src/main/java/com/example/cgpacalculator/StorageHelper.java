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
    private static final String PREFS_NAME = "cgpa_prefs";
    private static final String KEY_SEMESTERS = "semesters";
    // Legacy key kept for migration
    private static final String KEY_COURSES = "courses";
    private static final String TAG = "StorageHelper";

    private final SharedPreferences prefs;
    private final Gson gson;

    public StorageHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveSemesters(List<Semester> semesters) {
        prefs.edit().putString(KEY_SEMESTERS, gson.toJson(semesters)).apply();
        Log.d(TAG, "Saved " + semesters.size() + " semesters.");
    }

    public List<Semester> loadSemesters() {
        String json = prefs.getString(KEY_SEMESTERS, null);
        if (json != null) {
            Type type = new TypeToken<List<Semester>>(){}.getType();
            List<Semester> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        }

        // Migrate old flat course list into "Semester 1"
        String oldJson = prefs.getString(KEY_COURSES, null);
        if (oldJson != null) {
            Type type = new TypeToken<List<Course>>(){}.getType();
            List<Course> oldCourses = gson.fromJson(oldJson, type);
            if (oldCourses != null && !oldCourses.isEmpty()) {
                Semester s = new Semester("Semester 1");
                s.setCourses(oldCourses);
                List<Semester> migrated = new ArrayList<>();
                migrated.add(s);
                saveSemesters(migrated);
                prefs.edit().remove(KEY_COURSES).apply();
                return migrated;
            }
        }
        return new ArrayList<>();
    }

    // Legacy helpers kept for backwards compat
    public void saveCourses(List<Course> courses) {
        prefs.edit().putString(KEY_COURSES, gson.toJson(courses)).apply();
    }

    public List<Course> loadCourses() {
        String json = prefs.getString(KEY_COURSES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Course>>(){}.getType();
        List<Course> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }
}
