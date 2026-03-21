package com.example.cgpacalculator;

import java.util.ArrayList;
import java.util.List;

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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class HomeFragment extends Fragment {
        private List<Course> courseList = new ArrayList<Course>();
        private LinearLayout layoutCourseRows;
        private TextView tvCgpa, tvTotalCredits;
        private LinearProgressIndicator progressCgpa;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_home, container, false);
        }

        // Start manipulating data AFTER the view is created.
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            progressCgpa = view.findViewById(R.id.progressCgpa);
            tvCgpa = view.findViewById(R.id.tvCgpa);
            tvTotalCredits = view.findViewById(R.id.tvTotalCredits);
            layoutCourseRows = view.findViewById(R.id.layoutCourseRows);

            // set your real values here
            double cgpa = 8.74;
            int totalCredits = 48;

            tvCgpa.setText(String.valueOf(cgpa));
            tvTotalCredits.setText(totalCredits + " credits");
            progressCgpa.setProgress((int)((cgpa / 10.0) * 100));

            view.findViewById(R.id.btnAddCourse).setOnClickListener(v -> showAddCourse());
        }

        private void showAddCourse() {
            BottomSheetDialog sheet = new BottomSheetDialog(requireContext());

            View sheetView = LayoutInflater.from(requireContext()).
                            inflate(R.layout.dialog_add_course, null);
            sheet.setContentView(sheetView);

            TextInputEditText etName    = sheetView.findViewById(R.id.etCourseName);
            TextInputEditText etCredits = sheetView.findViewById(R.id.etCredits);
            AutoCompleteTextView actvGrade = sheetView.findViewById(R.id.actvGrade);

            // set up grade dropdown
            String[] grades = {"A", "A-", "B", "B-", "C", "C-", "D", "F"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    grades
            );
            actvGrade.setAdapter(adapter);
            actvGrade.setText(grades[0], false); // default to O

            // Cancel button
            sheetView.findViewById(R.id.btnCancel)
                    .setOnClickListener(v -> sheet.dismiss());

            // Save button
            sheetView.findViewById(R.id.btnSave).setOnClickListener(v -> {
                String name    = etName.getText().toString().trim();
                String credits = etCredits.getText().toString().trim();
                String grade   = actvGrade.getText().toString().trim();

                // validate — don't save empty fields
                if (name.isEmpty()) {
                    etName.setError("Enter a course name");
                    return;
                }
                if (credits.isEmpty()) {
                    etCredits.setError("Enter credits");
                    return;
                }

                // create the course and add to list
                Course course = new Course(
                        name,
                        Integer.parseInt(credits),
                        grade
                );
                courseList.add(course);

                sheet.dismiss();
                updateUI(); // recalculate and refresh
            });

            sheet.show();
        }

    // recalculates CGPA and updates all views
    private void updateUI() {
            refreshCourseRows();
        double totalWeighted = 0;
        int    totalCredits   = 0;

        for (Course c : courseList) {
            totalWeighted += c.getGradePoints() * c.getCredits();
            totalCredits  += c.getCredits();
        }

        if (totalCredits == 0) {
            tvCgpa.setText("–");
            progressCgpa.setProgress(0);
            tvTotalCredits.setText("");
            return;
        }

        double cgpa = totalWeighted / totalCredits;
        tvCgpa.setText(String.format("%.2f", cgpa));
        progressCgpa.setProgress((int)((cgpa / 10.0) * 100));
        setTotalCredits(totalCredits);
    }

    private void refreshCourseRows() {
            layoutCourseRows.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(requireContext());

            for (int i = 0; i < courseList.size(); i++) {
                Course course = courseList.get(i);
                View row = inflater.inflate(
                        R.layout.item_course_row,
                        layoutCourseRows,
                        false
                );

                TextInputEditText etName = row.findViewById(R.id.etCourseName);
                TextInputEditText etCredits = row.findViewById(R.id.etCredits);

                etName.setText(course.getName());
                etCredits.setText(String.valueOf(course.getCredits()));
                etName.setEnabled(false);
                etCredits.setEnabled(true);

                final int index = i;
                row.findViewById(R.id.btnRemoveCourse)
                        .setOnClickListener(v -> {
                            courseList.remove(index);
                            updateUI();
                        });

                layoutCourseRows.addView(row);
            }
    }

    private void setTotalCredits(int totalCredits) {
        tvTotalCredits.setText(totalCredits + " credits");
    }
}

