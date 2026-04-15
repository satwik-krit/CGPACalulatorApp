package com.example.cgpacalculator;

import android.animation.ArgbEvaluator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.example.cgpacalculator.views.CgpaRingView;

import java.util.List;

public class TargetCgpaFragment extends Fragment {

    private static final int TOTAL_SEMESTERS = 8; // adjust if needed

    private CgpaRingView cgpaRingView;
    private TextView tvRingSubLabel;
    private TextView tvSemestersDone;
    private TextView tvSemestersTotal;
    private TextView tvNeededSGPA;
    private MaterialCardView neededSGPACard;
    private TextView tvNeededSub;
    private TextView tvSliderValue;
    private Slider sliderTargetCGPA;

    private List<Semester> semesterList;
    private double currentCGPA;
    private int completedSemesters;
    private float targetCGPA = 9.0f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_target_cgpa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View bottomNav = requireActivity().findViewById(R.id.bottomNavView);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout()
            );

            int bottomNavHeight = bottomNav.getHeight();

            v.setPadding(
                    v.getPaddingLeft(),
                    bars.top, // FIX: respect status bar + cutout
                    v.getPaddingRight(),
                    bars.bottom + bottomNavHeight
            );

            return insets;
        });
        bindViews(view);
        loadData();
        setupSlider();
        updateUI();
    }


    @Override
    public void onResume() {
        super.onResume();
        // Re-load from storage and update the UI
        loadData();
    }

    private void bindViews(View view) {
        cgpaRingView        = view.findViewById(R.id.cgpaRingView);
        tvRingSubLabel      = view.findViewById(R.id.tvRingSubLabel);
        tvSemestersDone     = view.findViewById(R.id.tvSemestersDone);
        tvSemestersTotal    = view.findViewById(R.id.tvSemestersTotal);
        tvNeededSGPA        = view.findViewById(R.id.tvNeededSGPA);
        tvNeededSub         = view.findViewById(R.id.tvNeededSub);
        tvSliderValue       = view.findViewById(R.id.tvSliderValue);
        sliderTargetCGPA    = view.findViewById(R.id.sliderTargetCGPA);
        neededSGPACard      = view.findViewById(R.id.neededSGPACard);
    }

    private void loadData() {
        semesterList = StorageHelper.getInstance(requireContext()).loadSemesters();
        currentCGPA = StorageHelper.getInstance(requireContext()).calculateCGPA(semesterList);
        completedSemesters = semesterList.size();
    }

    private void setupSlider() {
        sliderTargetCGPA.setValue(targetCGPA);
        sliderTargetCGPA.addOnChangeListener((slider, value, fromUser) -> {
            targetCGPA = value;
            tvSliderValue.setText(String.format("%.2f", targetCGPA));
            updateUI();
        });
    }

    private void updateUI() {
        int remaining = TOTAL_SEMESTERS - completedSemesters;

        // Ring
        cgpaRingView.setCGPAValues(currentCGPA, targetCGPA);
        tvRingSubLabel.setText("Target: " + String.format("%.2f", targetCGPA));

        // Metric cards
        tvSemestersDone.setText(String.valueOf(completedSemesters));
        tvSemestersTotal.setText("of " + TOTAL_SEMESTERS + " total");

        if (remaining <= 0) {
            tvNeededSGPA.setText("--");
            tvNeededSub.setText("all semesters done");
            return;
        }

        // Required SGPA per remaining semester
        // Formula: targetCGPA * TOTAL = currentCGPA * completed + neededSGPA * remaining
        double totalPointsNeeded = targetCGPA * TOTAL_SEMESTERS;
        double currentPoints     = currentCGPA * completedSemesters;
        double pointsRequired    = totalPointsNeeded - currentPoints;
        double neededSGPA        = pointsRequired / remaining;

        tvNeededSGPA.setText(String.format("%.2f", neededSGPA));
        neededSGPACard.setCardBackgroundColor(getColorFromValue((float) neededSGPA, 10, 7));
        tvNeededSub.setText("next " + remaining + " semester" + (remaining > 1 ? "s" : ""));
    }

    private double computeCGPA(List<Semester> semesters) {
        double totalWeighted = 0;
        int totalCredits = 0;
        for (Semester sem : semesters) {
            for (Course course : sem.getCourses()) {
                totalWeighted += course.getGradePoints() * course.getCredits();
                totalCredits  += course.getCredits();
            }
        }
        return totalCredits == 0 ? 0.0 : totalWeighted / totalCredits;
    }


    private int getColorFromValue(float value, float min, float max) {
        float fraction = (value - min) / (max - min);

        // clamp between 0 and 1
        fraction = Math.max(0f, Math.min(1f, fraction));

        int startColor = 0xFFFF9E9E;
        int endColor   = 0xFFA5D6A7;

        ArgbEvaluator evaluator = new ArgbEvaluator();
        return (int) evaluator.evaluate(fraction, startColor, endColor);
    }
}
