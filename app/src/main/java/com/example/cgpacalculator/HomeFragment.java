package com.example.cgpacalculator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private StorageHelper storage;
    private List<Semester> semesterList = new ArrayList<>();
    private int activeSemIndex = 0;

    private LinearLayout layoutCourseRows;
    private TextView tvCgpa, tvTotalCredits, tvSpi, tvSemCredits, tvSemCount, tvSemesterTitle;
    private LinearProgressIndicator progressCgpa;
    private ChipGroup chipGroupSemesters;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        storage = new StorageHelper(requireContext());
        semesterList = storage.loadSemesters();
        if (semesterList.isEmpty()) {
            semesterList.add(new Semester("Semester 1"));
            storage.saveSemesters(semesterList);
        }

        progressCgpa     = view.findViewById(R.id.progressCgpa);
        tvCgpa           = view.findViewById(R.id.tvCgpa);
        tvTotalCredits   = view.findViewById(R.id.tvTotalCredits);
        layoutCourseRows = view.findViewById(R.id.layoutCourseRows);
        tvSpi            = view.findViewById(R.id.tvSpi);
        tvSemCredits     = view.findViewById(R.id.tvSemCredits);
        tvSemCount       = view.findViewById(R.id.tvSemCount);
        tvSemesterTitle  = view.findViewById(R.id.tvSemesterTitle);
        chipGroupSemesters = view.findViewById(R.id.chipGroupSemesters);

        view.findViewById(R.id.btnAddCourse).setOnClickListener(v -> showAddCourseDialog());

        // "Edit" button renames active semester
        view.findViewById(R.id.btnEditSemester).setOnClickListener(v -> showRenameSemesterDialog());

        // "+ Add" chip adds a new semester
        view.findViewById(R.id.chipAddSemester).setOnClickListener(v -> addNewSemester());

        rebuildSemesterChips();
        updateUI();
    }

    // ── Semester chips ──────────────────────────────────────────────

    private void rebuildSemesterChips() {
        // Remove all except the last chip (the "+ Add" chip)
        int addChipId = R.id.chipAddSemester;
        View addChip = chipGroupSemesters.findViewById(addChipId);
        chipGroupSemesters.removeAllViews();

        for (int i = 0; i < semesterList.size(); i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(semesterList.get(i).getName());
            chip.setCheckable(true);
            chip.setChecked(i == activeSemIndex);
            final int idx = i;
            chip.setOnClickListener(v -> {
                activeSemIndex = idx;
                updateUI();
            });
            chipGroupSemesters.addView(chip);
        }

        // Re-add the "+ Add" chip
        if (addChip == null) {
            addChip = LayoutInflater.from(requireContext())
                    .inflate(R.layout.chip_add_semester, chipGroupSemesters, false);
            addChip.setId(addChipId);
            addChip.setOnClickListener(v -> addNewSemester());
        }
        chipGroupSemesters.addView(addChip);
    }

    private void addNewSemester() {
        String name = "Semester " + (semesterList.size() + 1);
        semesterList.add(new Semester(name));
        activeSemIndex = semesterList.size() - 1;
        storage.saveSemesters(semesterList);
        rebuildSemesterChips();
        updateUI();
    }

    private void showRenameSemesterDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sv = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_rename_semester, null);
        sheet.setContentView(sv);

        TextInputEditText etName = sv.findViewById(R.id.etSemesterName);
        etName.setText(semesterList.get(activeSemIndex).getName());

        sv.findViewById(R.id.btnCancelRename).setOnClickListener(v -> sheet.dismiss());
        sv.findViewById(R.id.btnSaveRename).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Enter a name"); return; }
            semesterList.get(activeSemIndex).setName(name);
            storage.saveSemesters(semesterList);
            sheet.dismiss();
            rebuildSemesterChips();
            updateUI();
        });

        sheet.show();
    }

    // ── Add Course dialog ────────────────────────────────────────────

    private void showAddCourseDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sv = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_course, null);
        sheet.setContentView(sv);

        TextInputEditText etName    = sv.findViewById(R.id.etCourseName);
        TextInputEditText etCredits = sv.findViewById(R.id.etCredits);
        AutoCompleteTextView actvGrade = sv.findViewById(R.id.actvGrade);

        String[] grades = {"A", "A-", "B", "B-", "C", "C-", "D", "F"};
        actvGrade.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, grades));
        actvGrade.setText(grades[0], false);

        sv.findViewById(R.id.btnCancel).setOnClickListener(v -> sheet.dismiss());
        sv.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name    = etName.getText().toString().trim();
            String credits = etCredits.getText().toString().trim();
            String grade   = actvGrade.getText().toString().trim();

            if (name.isEmpty())    { etName.setError("Enter a course name"); return; }
            if (credits.isEmpty()) { etCredits.setError("Enter credits");    return; }

            semesterList.get(activeSemIndex).addCourse(
                    new Course(name, Integer.parseInt(credits), grade));
            sheet.dismiss();
            storage.saveSemesters(semesterList);
            updateUI();
        });

        sheet.show();
    }

    // ── UI refresh ───────────────────────────────────────────────────

    private void updateUI() {
        Semester active = semesterList.get(activeSemIndex);
        tvSemesterTitle.setText(active.getName());

        refreshCourseRows(active);

        // CGPA across ALL semesters
        double totalWeighted = 0;
        int totalCredits = 0;
        for (Semester s : semesterList) {
            for (Course c : s.getCourses()) {
                totalWeighted += (double) c.getGradePoints() * c.getCredits();
                totalCredits  += c.getCredits();
            }
        }

        if (totalCredits == 0) {
            tvCgpa.setText("–");
            progressCgpa.setProgress(0);
            tvTotalCredits.setText("No courses yet");
        } else {
            double cgpa = totalWeighted / totalCredits;
            tvCgpa.setText(String.format("%.2f", cgpa));
            progressCgpa.setProgress((int)((cgpa / 10.0) * 100));
            tvTotalCredits.setText(totalCredits + " credits");
        }

        // Metrics: SGPA of active sem, its credits, total semesters
        double sgpa = active.getSgpa();
        if (tvSpi != null) tvSpi.setText(active.getCourses().isEmpty() ? "–" : String.format("%.2f", sgpa));
        if (tvSemCredits != null) tvSemCredits.setText(String.valueOf(active.getTotalCredits()));
        if (tvSemCount != null)   tvSemCount.setText(String.valueOf(semesterList.size()));
    }

    private void refreshCourseRows(Semester semester) {
        layoutCourseRows.removeAllViews();
        List<Course> courses = semester.getCourses();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            View row = inflater.inflate(R.layout.item_course_row, layoutCourseRows, false);

            TextInputEditText etName    = row.findViewById(R.id.etCourseName);
            TextInputEditText etCredits = row.findViewById(R.id.etCredits);
            AutoCompleteTextView actvGrade = row.findViewById(R.id.actvGrade);

            etName.setText(course.getName());
            etCredits.setText(String.valueOf(course.getCredits()));
            etName.setEnabled(false);
            etCredits.setEnabled(false);

            String[] grades = {"A", "A-", "B", "B-", "C", "C-", "D", "F"};
            actvGrade.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, grades));
            actvGrade.setText(course.getGrade(), false);
            actvGrade.setEnabled(false);

            final int idx = i;
            row.findViewById(R.id.btnRemoveCourse).setOnClickListener(v -> {
                semester.removeCourse(idx);
                storage.saveSemesters(semesterList);
                updateUI();
            });

            layoutCourseRows.addView(row);
        }
    }
}
