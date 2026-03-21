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
    private static final String KEY_COURSES = "courses";

    private static final String LOGCATTAG = "StorageHelper";

    private SharedPreferences prefs;
    private Gson gson;

    public StorageHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveCourses(List<Course> courseList) {
        String json = gson.toJson(courseList);
        prefs.edit().putString(KEY_COURSES, json).apply();
        Log.d(LOGCATTAG, "Saved courses.");
    }

    public List<Course> loadCourses() {
        String json = prefs.getString(KEY_COURSES, null);
        if (json == null) return new ArrayList<>();
        Log.d(LOGCATTAG, json);

        Type type = new TypeToken<List<Course>>(){}.getType();
        return gson.fromJson(json, type);
    }
}
