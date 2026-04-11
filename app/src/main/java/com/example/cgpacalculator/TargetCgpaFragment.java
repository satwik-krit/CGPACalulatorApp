package com.example.cgpacalculator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private TextView tvNeededSub;
    private TextView tvSliderValue;
    private Slider sliderTargetCGPA;
    private LinearLayout llSemesterBreakdown;
    private MaterialCardView cardFeasibility;
    private TextView tvFeasibility;

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
        bindViews(view);
        loadData();
        setupSlider();
        updateUI();
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
        llSemesterBreakdown = view.findViewById(R.id.llSemesterBreakdown);
        cardFeasibility     = view.findViewById(R.id.cardFeasibility);
        tvFeasibility       = view.findViewById(R.id.tvFeasibility);
    }

    private void loadData() {
        semesterList = StorageHelper.getInstance(requireContext()).loadSemesters();
        currentCGPA = computeCGPA(semesterList);
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
            cardFeasibility.setVisibility(View.GONE);
            buildBreakdown(0, 0);
            return;
        }

        // Required SGPA per remaining semester
        // Formula: targetCGPA * TOTAL = currentCGPA * completed + neededSGPA * remaining
        double totalPointsNeeded = targetCGPA * TOTAL_SEMESTERS;
        double currentPoints     = currentCGPA * completedSemesters;
        double pointsRequired    = totalPointsNeeded - currentPoints;
        double neededSGPA        = pointsRequired / remaining;

        tvNeededSGPA.setText(String.format("%.2f", neededSGPA));
        tvNeededSub.setText("next " + remaining + " semester" + (remaining > 1 ? "s" : ""));

        // Feasibility warning
        if (neededSGPA > 10.0) {
            cardFeasibility.setVisibility(View.VISIBLE);
            tvFeasibility.setText("Target is not achievable — you would need an average SGPA of "
                    + String.format("%.2f", neededSGPA) + ", which exceeds 10.0. Lower your target.");
        } else {
            cardFeasibility.setVisibility(View.GONE);
        }

        buildBreakdown(neededSGPA, remaining);
    }

    private void buildBreakdown(double neededSGPA, int remaining) {
        llSemesterBreakdown.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 1; i <= remaining; i++) {
            int semNumber = completedSemesters + i;
            View row = inflater.inflate(R.layout.item_semester_needed, llSemesterBreakdown, false);

            TextView tvSemLabel  = row.findViewById(R.id.tvSemLabel);
            TextView tvNeededVal = row.findViewById(R.id.tvNeededVal);
            TextView tvChip      = row.findViewById(R.id.tvStatusChip);
            ProgressBar progress = row.findViewById(R.id.progressSem);

            tvSemLabel.setText("Semester " + semNumber);
            tvNeededVal.setText("needs " + String.format("%.2f", neededSGPA));

            int progressVal = (int) Math.min(100, neededSGPA / 10.0 * 100);
            progress.setProgress(progressVal);

            // Chip label and color
            if (neededSGPA > 10.0) {
                tvChip.setText("impossible");
                tvChip.setBackgroundResource(R.drawable.bg_chip_red);
                tvChip.setTextColor(requireContext().getColor(R.color.backlog_red_text));
            } else if (neededSGPA >= 9.0) {
                tvChip.setText("hard");
                tvChip.setBackgroundResource(R.drawable.bg_chip_amber);
                tvChip.setTextColor(requireContext().getColor(R.color.backlog_amber_text));
            } else if (neededSGPA >= 7.5) {
                tvChip.setText("stretch");
                tvChip.setBackgroundResource(R.drawable.bg_chip_green);
                tvChip.setTextColor(requireContext().getColor(R.color.backlog_green_text));
            } else {
                tvChip.setText("achievable");
                tvChip.setBackgroundResource(R.drawable.bg_chip_green);
                tvChip.setTextColor(requireContext().getColor(R.color.backlog_green_text));
            }

            llSemesterBreakdown.addView(row);
        }
    }

    // -----------------------------------------------------------------------
    // CGPA computation (credit-weighted across all semesters)
    // -----------------------------------------------------------------------
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
}
