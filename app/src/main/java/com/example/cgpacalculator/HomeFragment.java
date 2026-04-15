package com.example.cgpacalculator;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.w3c.dom.Text;

import java.util.List;

public class HomeFragment extends Fragment {
    private List<Semester> semesterList;
    private List<BacklogCourse> backlogCourses;
    private List<Course> courseList;
    private int currentSemIndex = 0;

    private ViewPager2 viewPager;
    private SemesterPagerAdapter pagerAdapter;
    private LinearLayout layoutDots;
    StorageHelper storage;
    private LinearLayout layoutSemesterCards;
    private TextView tvCgpa, tvTotalCredits, tvFooterSgpa, tvFooterTotalCredits, tvSemCount;

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

        View scrollView = view.findViewById(R.id.main);

        View bottomNav = requireActivity().findViewById(R.id.bottomNavView);

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
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

        storage = new StorageHelper(requireContext());
        semesterList = storage.loadSemesters();
        backlogCourses = storage.loadBacklogCourses();
        if (semesterList.size() == 0)
            semesterList.add(new Semester(1));
        currentSemIndex = 0;
        courseList = semesterList.get(currentSemIndex).getCourses();

        progressCgpa = view.findViewById(R.id.progressCgpa);
        tvCgpa = view.findViewById(R.id.tvCgpa);
        tvTotalCredits = view.findViewById(R.id.tvTotalCredits);
        layoutSemesterCards = view.findViewById(R.id.layoutSemesterCards);
        tvFooterSgpa = view.findViewById(R.id.tvSpi);
        tvFooterTotalCredits = view.findViewById(R.id.tvSemCredits);
        tvSemCount = view.findViewById(R.id.tvSemCount);

        viewPager   = view.findViewById(R.id.viewPagerSemesters);
        layoutDots  = view.findViewById(R.id.layoutDots);

        pagerAdapter = new SemesterPagerAdapter(
                requireContext(),
                semesterList,
                (semIndex, courseIndex) -> {
                    // existing remove logic
                    Course removedCourse = semesterList.get(semIndex).getCourses().get(courseIndex);
                    backlogCourses.removeIf(c -> c.getCourseName().equals(removedCourse.getName()));
                    semesterList.get(semIndex).getCourses().remove(courseIndex);
                    storage.saveSemesters(semesterList);
                    storage.saveBacklogCourses(backlogCourses);
                    updateUI();
                },
                (semIndex, courseIndex) -> {
                    // edit listener — open the edit sheet
                    Course course = semesterList.get(semIndex).getCourses().get(courseIndex);
                    showEditCourse(course, semIndex, courseIndex);
                }
        );        viewPager.setAdapter(pagerAdapter);

        // when user swipes, sync the semester cards at the top
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        currentSemIndex = position;
                        refreshSemesterCards();
                        scrollToActiveCard();
                        renderDots();
                        recalcFooter();
                    }
                }
        );

        // set your real values here
        double cgpa = 0.0;
        int totalCredits = 0;

        tvCgpa.setText(String.valueOf(cgpa));
        tvTotalCredits.setText(totalCredits + " credits");
        progressCgpa.setProgress((int) ((cgpa / 10.0) * 100));

        view.findViewById(R.id.btnAddCourse).setOnClickListener(v -> showAddCourse());
        view.findViewById(R.id.btnAddSemester).setOnClickListener(v -> {
            semesterList.add(new Semester(semesterList.size() + 1));
            currentSemIndex = semesterList.size() - 1;
            storage.saveSemesters(semesterList);
            updateUI();

            // move ViewPager FIRST
            viewPager.setCurrentItem(currentSemIndex, true);

            // then scroll cards AFTER layout settles
            layoutSemesterCards.post(() -> scrollToActiveCard());
        });

        updateUI();
    }

    private void recalcHeader() {
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

    private void recalcFooter() {
        int currentPage = viewPager.getCurrentItem();

        if (currentPage >= semesterList.size()) {
            currentPage = semesterList.size() - 1;
        }
        if (currentPage < 0) currentPage = 0;

        Semester currentSemester = semesterList.get(currentPage);
        double sgpa         = currentSemester.getSGPA();
        int    totalCredits = currentSemester.getTotalCredits();
        int    semCount     = 0;
        for (Semester s : semesterList) semCount++;

        tvSemCount.setText(Integer.toString(semCount));

        if (totalCredits == 0) {
            tvFooterSgpa.setText("–");
            tvFooterTotalCredits.setText("0");
        } else {
            tvFooterSgpa.setText(String.format("%.2f", sgpa));
            tvFooterTotalCredits.setText(Integer.toString(totalCredits));
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
        sheetView.findViewById(R.id.tilSemester).setVisibility(View.GONE);
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
                    grade,
                    currentSemIndex
            );
            semesterList.get(currentSemIndex).getCourses().add(course);
            currentSemIndex = semesterList.size() - 1;

            if (grade.equals("F")) {
                List<BacklogCourse> backlogCourses = storage.loadBacklogCourses();
                backlogCourses.add(new BacklogCourse(course));
                storage.saveBacklogCourses(backlogCourses);
            }

            sheet.dismiss();
            storage.saveSemesters(semesterList);
            updateUI(); // recalculate and refresh
            scrollToActiveCard();
        });

        sheet.show();
    }

    // recalculates CGPA and updates all views
    private void updateUI() {
        refreshSemesterCards();
        pagerAdapter.notifyDataSetChanged();
        recalcHeader();
        recalcFooter();
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

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) card.getLayoutParams();
            params.setMarginEnd(dpToPx(8));
            card.setLayoutParams(params);

            // fill in the card values
            ((TextView) card.findViewById(R.id.tvSemCardName))
                    .setText(String.valueOf(sem.getIndex()));
            ((TextView) card.findViewById(R.id.tvSemCardSpi))
                    .setText(sem.getTotalCredits() > 0
                            ? String.format("%.2f", sem.getSGPA())
                            : "–");
            ((TextView) card.findViewById(R.id.tvSemCardCredits))
                    .setText(String.valueOf(sem.getTotalCredits()));

            ReportCardGenerator generator = new ReportCardGenerator(requireContext());


            int finalI = i;
            card.setOnLongClickListener(v -> {
                String courseList = "";
                for (Course c : sem.getCourses()) courseList += c.getName() + '\n';
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Semester " + sem.getIndex())
                        .setMessage(courseList)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // don't allow deleting the last semester
                            if (semesterList.size() == 1) {
                                Toast.makeText(requireContext(),
                                        "Can't delete the only semester",
                                        Toast.LENGTH_SHORT).show();
                            }

                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Delete " + sem.getIndex() + "?")
                                    .setMessage("All courses in this semester will be deleted.")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Delete", (_dialog, _which) -> {
                                        for (Course course : semesterList.get(finalI).getCourses()) {
                                            backlogCourses.removeIf(c -> c.getCourseName().equals(course.getName()));
                                        }
                                        // Delete all courses from the list of that semester.
                                        semesterList.get(finalI).getCourses().clear();
                                        semesterList.remove(finalI);

                                        // Fix index AFTER removal
                                        if (currentSemIndex >= semesterList.size()) {
                                            currentSemIndex = semesterList.size() - 1;
                                        }

                                        if (currentSemIndex < 0) {
                                            currentSemIndex = 0;
                                        }

                                        // Whenever we delete a semester, recalculate the indices of all
                                        // semesters.
                                        for (int j = 0; j < semesterList.size(); j++)
                                            semesterList.get(j).setIndex(j+1);

                                        storage.saveSemesters(semesterList);
                                        storage.saveBacklogCourses(backlogCourses);
                                        updateUI();
                                    })
                                    .show();
                        })
                        .setNegativeButton("Share", (dialog, which) -> {
                            if (sem.getCourses().isEmpty()) {
                                Toast.makeText(requireContext(),
                                        "Add courses first", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            generator.generateAndShare(sem);
                        })
                        .show();
                return true; // true means the long press was consumed
            });

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
            card.setOnClickListener(v -> {
                currentSemIndex = finalI;

                currentSemIndex = finalI;
                viewPager.setCurrentItem(finalI, true);

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

   private void showEditCourse(Course course, int semIndex, int courseIndex) {
    BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
    View sheetView = LayoutInflater.from(requireContext())
        .inflate(R.layout.dialog_add_course, null);
    sheet.setContentView(sheetView);

    // change title to Edit
    TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
    if (tvTitle != null) tvTitle.setText("Edit Course");

    TextInputEditText    etName    = sheetView.findViewById(R.id.etCourseName);
    TextInputEditText    etCredits = sheetView.findViewById(R.id.etCredits);
    AutoCompleteTextView actvGrade = sheetView.findViewById(R.id.actvGrade);

    // semester dropdown — new addition
    AutoCompleteTextView actvSemester = sheetView.findViewById(R.id.actvSemester);

    // pre-fill existing values
    etName.setText(course.getName());
    etCredits.setText(String.valueOf(course.getCredits()));

    String[] grades = {"O", "A+", "A", "B+", "B", "C", "P", "F"};
    ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(
        requireContext(),
        android.R.layout.simple_dropdown_item_1line,
        grades
    );
    actvGrade.setAdapter(gradeAdapter);
    actvGrade.setText(course.getGrade(), false);

    // build semester options from current semester list
    String[] semesterNames = new String[semesterList.size()];
    for (int i = 0; i < semesterList.size(); i++) {
        semesterNames[i] = "Semester " + semesterList.get(i).getIndex();
    }
    ArrayAdapter<String> semAdapter = new ArrayAdapter<>(
        requireContext(),
        android.R.layout.simple_dropdown_item_1line,
        semesterNames
    );
    actvSemester.setAdapter(semAdapter);
    actvSemester.setText(semesterNames[semIndex], false);

    // change save button text
    MaterialButton btnSave = sheetView.findViewById(R.id.btnSave);
    btnSave.setText("Save Changes");

    sheetView.findViewById(R.id.btnCancel)
        .setOnClickListener(v -> sheet.dismiss());

    btnSave.setOnClickListener(v -> {
        String name    = etName.getText().toString().trim();
        String credits = etCredits.getText().toString().trim();
        String grade   = actvGrade.getText().toString().trim();
        String semName = actvSemester.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Enter a course name");
            return;
        }
        if (credits.isEmpty()) {
            etCredits.setError("Enter credits");
            return;
        }

        // figure out which semester was selected
        int newSemIndex = semIndex; // default to current
        for (int i = 0; i < semesterList.size(); i++) {
            if (("Semester " + semesterList.get(i).getIndex()).equals(semName)) {
                newSemIndex = i;
                break;
            }
        }

        // update course values
        course.setName(name);
        course.setCredits(Integer.parseInt(credits));
        course.setGrade(grade);

        // handle backlog changes if grade changed to/from F
        if (grade.equals("F")) {
            // add to backlog if not already there
            boolean alreadyInBacklog = false;
            for (BacklogCourse b : backlogCourses) {
                if (b.getCourseName().equals(course.getName())) {
                    alreadyInBacklog = true;
                    break;
                }
            }
            if (!alreadyInBacklog) {
                backlogCourses.add(new BacklogCourse(course));
            }
        } else {
            // remove from backlog if grade was fixed
            backlogCourses.removeIf(b -> b.getCourseName().equals(course.getName()));
        }
        storage.saveBacklogCourses(backlogCourses);

        // if semester changed, move the course
        if (newSemIndex != semIndex) {
            semesterList.get(semIndex).getCourses().remove(courseIndex);
            semesterList.get(newSemIndex).getCourses().add(course);
        }

        storage.saveSemesters(semesterList);
        sheet.dismiss();
        updateUI();
    });

    sheet.show();
   }

   private void deleteCourse(int semIndex, int courseIndex) {
//       semesterList.get(semIndex).getCourses().removeIf()
   }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}

