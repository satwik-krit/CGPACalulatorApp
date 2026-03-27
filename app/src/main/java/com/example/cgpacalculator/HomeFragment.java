package com.example.cgpacalculator;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private List<Semester> semesterList;
    private List<Course> courseList;
    private int currentSemIndex = 0;

    private ViewPager2 viewPager;
    private SemesterPagerAdapter pagerAdapter;
    private LinearLayout layoutDots;
    StorageHelper storage;
    private LinearLayout layoutSemesterCards;
    private TextView tvCgpa, tvTotalCredits;
    private LinearProgressIndicator progressCgpa;

    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    // Start manipulating data AFTER the view is created.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        storage = new StorageHelper(requireContext());
        semesterList = storage.loadSemesters();
        if (semesterList.size() == 0)
            semesterList.add(new Semester(2));
        currentSemIndex = 0;
        courseList = semesterList.get(currentSemIndex).getCourses();

        progressCgpa = view.findViewById(R.id.progressCgpa);
        tvCgpa = view.findViewById(R.id.tvCgpa);
        tvTotalCredits = view.findViewById(R.id.tvTotalCredits);
        layoutSemesterCards = view.findViewById(R.id.layoutSemesterCards);

        viewPager   = view.findViewById(R.id.viewPagerSemesters);
        layoutDots  = view.findViewById(R.id.layoutDots);

        pagerAdapter = new SemesterPagerAdapter(
                requireContext(),
                semesterList,
                (semIndex, courseIndex) -> {
                    // remove the course, save, refresh
                    semesterList.get(semIndex).getCourses().remove(courseIndex);
                    storage.saveSemesters(semesterList);
                    updateUI();
                }
        );
        viewPager.setAdapter(pagerAdapter);

// when user swipes, sync the semester cards at the top
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        currentSemIndex = position;
                        refreshSemesterCards();
                        scrollToActiveCard();
                        recalcFooter();
                        renderDots();
                    }
                }
        );

        // set your real values here
        double cgpa = 8.74;
        int totalCredits = 48;

        tvCgpa.setText(String.valueOf(cgpa));
        tvTotalCredits.setText(totalCredits + " credits");
        progressCgpa.setProgress((int) ((cgpa / 10.0) * 100));

        view.findViewById(R.id.btnAddCourse).setOnClickListener(v -> showAddCourse());
        view.findViewById(R.id.btnAddSemester).setOnClickListener(v -> {
                    semesterList.add(new Semester(semesterList.size() + 1));
                    currentSemIndex = semesterList.size() - 1;
                    storage.saveSemesters(semesterList);
                    updateUI();
                    scrollToActiveCard();
        });
        updateUI();
    }

    private void recalcFooter() {
        double cgpa         = StorageHelper.calculateCGPA(semesterList);
        int    totalCredits = 0;
        for (Semester s : semesterList)
            totalCredits += s.getTotalCredits();

        if (totalCredits == 0) {
            tvCgpa.setText("–");
            progressCgpa.setProgress(0);
            tvTotalCredits.setText("");
        } else {
            tvCgpa.setText(String.format("%.2f", cgpa));
            progressCgpa.setProgress((int)((cgpa / 10.0) * 100));
            tvTotalCredits.setText(totalCredits + " credits");
        }
    }

    private void renderDots() {
        layoutDots.removeAllViews();
        for (int i = 0; i < semesterList.size(); i++) {
            View dot = new View(requireContext());
            // active dot = wide purple pill, inactive = small grey circle
            int width  = (i == currentSemIndex) ? dpToPx(18) : dpToPx(6);
            int height = dpToPx(6);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(width, height);
            params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundColor(i == currentSemIndex
                    ? getThemeColor(R.color.colorPrimary)
                    : getThemeColor(com.google.android.material.R.attr.colorOutlineVariant));
            int radius = dpToPx(3);
            dot.setBackground(makeRoundedDrawable(
                    dot.getBackground() != null ? 0 :
                            (i == currentSemIndex
                                    ? getThemeColor(R.color.colorPrimary)
                                    : getThemeColor(com.google.android.material.R.attr.colorOutlineVariant)),
                    radius));
            layoutDots.addView(dot);
        }
    }

    // Resolves a theme color attribute to an actual color integer
    // eg. colorPrimary → 0xFF6750A4
    private int getThemeColor(int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        getContext().getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    // converts dp to pixels — needed for programmatic layouts
    private int dpToPx(int dp) {
        return (int)(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // creates a simple rounded rectangle background for the dots
    private android.graphics.drawable.GradientDrawable makeRoundedDrawable(
            int color, int radius) {
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private void showAddCourse() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());

        View sheetView = LayoutInflater.from(requireContext()).
                inflate(R.layout.dialog_add_course, null);
        sheet.setContentView(sheetView);

        TextInputEditText etName = sheetView.findViewById(R.id.etCourseName);
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
            String name = etName.getText().toString().trim();
            String credits = etCredits.getText().toString().trim();
            String grade = actvGrade.getText().toString().trim();

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
            semesterList.get(currentSemIndex).getCourses().add(course);
            sheet.dismiss();
            storage.saveSemesters(semesterList);
            updateUI(); // recalculate and refresh
        });

        sheet.show();
    }

    // recalculates CGPA and updates all views
    private void updateUI() {
        refreshSemesterCards();
        pagerAdapter.notifyDataSetChanged();
        double totalWeighted = 0;
        int totalCredits = 0;

        for (Course c : semesterList.get(currentSemIndex).getCourses()) {
            totalWeighted += c.getGradePoints() * c.getCredits();
            totalCredits += c.getCredits();
        }

        if (totalCredits == 0) {
            tvCgpa.setText("–");
            progressCgpa.setProgress(0);
            tvTotalCredits.setText("");
            return;
        }

        double cgpa = totalWeighted / totalCredits;
        tvCgpa.setText(String.format("%.2f", cgpa));
        progressCgpa.setProgress((int) ((cgpa / 10.0) * 100));
        setTotalCredits(totalCredits);
    }

    private void refreshSemesterCards() {
        layoutSemesterCards.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < semesterList.size(); i++) {
            Semester sem = semesterList.get(i);

            View card = inflater.inflate(
                    R.layout.item_semester_card,
                    layoutSemesterCards,
                    false
            );

            // fill in the card values
            ((TextView) card.findViewById(R.id.tvSemCardName))
                    .setText(String.valueOf(sem.getIndex()));
            ((TextView) card.findViewById(R.id.tvSemCardSpi))
                    .setText(sem.getTotalCredits() > 0
                            ? String.format("%.2f", sem.getSGPA())
                            : "–");
            ((TextView) card.findViewById(R.id.tvSemCardCredits))
                    .setText(String.valueOf(sem.getTotalCredits()));

            // highlight the active card
            MaterialCardView cardView = card.findViewById(R.id.cardSemester);
            if (i == currentSemIndex) {
                cardView.setStrokeColor(
                        requireContext().getColor(R.color.md_theme_primary));
                cardView.setStrokeWidth(2);
                cardView.setCardBackgroundColor(
                        requireContext().getColor(R.color.md_theme_primaryContainer));
                ((TextView) card.findViewById(R.id.tvSemCardName))
                        .setTextColor(requireContext()
                                .getColor(R.color.md_theme_onPrimaryContainer));
                ((TextView) card.findViewById(R.id.tvSemCardSpi))
                        .setTextColor(requireContext()
                                .getColor(R.color.md_theme_onPrimaryContainer));
                ((TextView) card.findViewById(R.id.tvSemCardCredits))
                        .setTextColor(requireContext()
                                .getColor(R.color.md_theme_onPrimaryContainer));
            }

            // tap to switch semester
            final int index = i;
            card.setOnClickListener(v -> {
                currentSemIndex = index;
                updateUI();
                scrollToActiveCard();
            });

            layoutSemesterCards.addView(card);
        }
    }

    private void scrollToActiveCard() {
        HorizontalScrollView hsv =
                requireView().findViewById(R.id.hsvSemesters);

        // post so layout has finished before we scroll
        hsv.post(() -> {
            View activeCard = layoutSemesterCards.getChildAt(currentSemIndex);
            if (activeCard != null) {
                int cardCenter = activeCard.getLeft()
                        + activeCard.getWidth() / 2;
                int scrollTo   = cardCenter - hsv.getWidth() / 2;
                hsv.smoothScrollTo(scrollTo, 0);
            }
        });
    }

    private void setTotalCredits(int totalCredits) {
        tvTotalCredits.setText(totalCredits + " credits");
    }
}

