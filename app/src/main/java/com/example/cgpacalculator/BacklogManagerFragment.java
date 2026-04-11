package com.example.cgpacalculator;

import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class BacklogManagerFragment extends Fragment {

    // Filter constants
    private static final int FILTER_ALL     = 0;
    private static final int FILTER_FAILED  = 1;
    private static final int FILTER_DROPPED = 2;
    private static final int FILTER_CLEARED = 3;

    private TextView tvFailedCount;
    private TextView tvDroppedCount;
    private TextView tvClearedCount;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvBacklogCourses;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fabAddBacklog;

    private List<BacklogCourse> allCourses = new ArrayList<>();
    private BacklogAdapter adapter;
    private int currentFilter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backlog_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupChipFilter();
        fabAddBacklog.setOnClickListener(v -> showAddBacklogSheet());
        loadData();
        refresh();
    }

    private void bindViews(View view) {
        tvFailedCount   = view.findViewById(R.id.tvFailedCount);
        tvDroppedCount  = view.findViewById(R.id.tvDroppedCount);
        tvClearedCount  = view.findViewById(R.id.tvClearedCount);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        rvBacklogCourses = view.findViewById(R.id.rvBacklogCourses);
        layoutEmpty     = view.findViewById(R.id.layoutEmpty);
        fabAddBacklog   = view.findViewById(R.id.fabAddBacklog);
    }

    private void setupRecyclerView() {
        adapter = new BacklogAdapter(new ArrayList<>(), new BacklogAdapter.Listener() {
            @Override
            public void onMarkCleared(BacklogCourse course, int position) {
                showMarkClearedSheet(course);
            }

            @Override
            public void onEdit(BacklogCourse course, int position) {
                showEditSheet(course);
            }
        });
        rvBacklogCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBacklogCourses.setAdapter(adapter);
    }

    private void setupChipFilter() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll)     currentFilter = FILTER_ALL;
            else if (id == R.id.chipFailed)  currentFilter = FILTER_FAILED;
            else if (id == R.id.chipDropped) currentFilter = FILTER_DROPPED;
            else if (id == R.id.chipCleared) currentFilter = FILTER_CLEARED;
            applyFilter();
        });
    }

    private void loadData() {
        // Load from StorageHelper (extend StorageHelper to persist backlog courses)
        allCourses = StorageHelper.getInstance(requireContext()).loadBacklogCourses();
    }

    private void saveData() {
        StorageHelper.getInstance(requireContext()).saveBacklogCourses(allCourses);
    }

    private void refresh() {
        updateSummaryCards();
        applyFilter();
    }

    private void updateSummaryCards() {
        int failed = 0, dropped = 0, cleared = 0;
        for (BacklogCourse c : allCourses) {
            switch (c.getStatus()) {
                case FAILED:  failed++;  break;
                case DROPPED: dropped++; break;
                case CLEARED: cleared++; break;
            }
        }
        tvFailedCount.setText(String.valueOf(failed));
        tvDroppedCount.setText(String.valueOf(dropped));
        tvClearedCount.setText(String.valueOf(cleared));
    }

    private void applyFilter() {
        List<BacklogCourse> filtered = new ArrayList<>();
        for (BacklogCourse c : allCourses) {
            boolean include = false;
            switch (currentFilter) {
                case FILTER_ALL:     include = true; break;
                case FILTER_FAILED:  include = c.getStatus() == BacklogCourse.Status.FAILED; break;
                case FILTER_DROPPED: include = c.getStatus() == BacklogCourse.Status.DROPPED; break;
                case FILTER_CLEARED: include = c.getStatus() == BacklogCourse.Status.CLEARED; break;
            }
            if (include) filtered.add(c);
        }
        adapter.updateData(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvBacklogCourses.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------
    // Bottom sheet: Add backlog course
    // ------------------------------------------------------------------
    private void showAddBacklogSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_add_backlog, null);
        sheet.setContentView(sheetView);

        TextInputEditText etCourseName = sheetView.findViewById(R.id.etBacklogCourseName);
        TextInputEditText etSemester   = sheetView.findViewById(R.id.etBacklogSemester);
        TextInputEditText etCredits    = sheetView.findViewById(R.id.etBacklogCredits);
        TextInputEditText etGrade      = sheetView.findViewById(R.id.etBacklogGrade);
        AutoCompleteTextView dropStatus = sheetView.findViewById(R.id.dropBacklogStatus);
        MaterialButton btnAdd          = sheetView.findViewById(R.id.btnAddBacklogConfirm);

        String[] statuses = {"Failed", "Dropped"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, statuses);
        dropStatus.setAdapter(statusAdapter);
        dropStatus.setText("Failed", false);

        btnAdd.setOnClickListener(v -> {
            String name = etCourseName.getText() != null ? etCourseName.getText().toString().trim() : "";
            String semStr = etSemester.getText() != null ? etSemester.getText().toString().trim() : "";
            String crStr  = etCredits.getText()  != null ? etCredits.getText().toString().trim()  : "";
            String grStr  = etGrade.getText()    != null ? etGrade.getText().toString().trim()    : "";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(semStr) || TextUtils.isEmpty(crStr)) {
                return; // basic validation
            }

            int sem = Integer.parseInt(semStr);
            int cr  = Integer.parseInt(crStr);
            double grade = TextUtils.isEmpty(grStr) ? 0.0 : Double.parseDouble(grStr);
            String statusStr = dropStatus.getText().toString();
            BacklogCourse.Status status = statusStr.equals("Dropped")
                    ? BacklogCourse.Status.DROPPED
                    : BacklogCourse.Status.FAILED;

            BacklogCourse course = new BacklogCourse(name, sem, cr, grade, status);
            allCourses.add(course);
            saveData();
            refresh();
            sheet.dismiss();
        });

        sheet.show();
    }

    // ------------------------------------------------------------------
    // Bottom sheet: Mark cleared
    // ------------------------------------------------------------------
    private void showMarkClearedSheet(BacklogCourse course) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_mark_cleared, null);
        sheet.setContentView(sheetView);

        TextInputEditText etNewGrade = sheetView.findViewById(R.id.etNewGrade);
        MaterialButton btnConfirm    = sheetView.findViewById(R.id.btnMarkClearedConfirm);

        btnConfirm.setOnClickListener(v -> {
            String grStr = etNewGrade.getText() != null ? etNewGrade.getText().toString().trim() : "";
            double newGrade = TextUtils.isEmpty(grStr) ? 0.0 : Double.parseDouble(grStr);
            course.markCleared(newGrade);
            saveData();
            refresh();
            sheet.dismiss();
        });

        sheet.show();
    }

    // ------------------------------------------------------------------
    // Bottom sheet: Edit
    // ------------------------------------------------------------------
    private void showEditSheet(BacklogCourse course) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_add_backlog, null);
        sheet.setContentView(sheetView);

        TextInputEditText etCourseName  = sheetView.findViewById(R.id.etBacklogCourseName);
        TextInputEditText etSemester    = sheetView.findViewById(R.id.etBacklogSemester);
        TextInputEditText etCredits     = sheetView.findViewById(R.id.etBacklogCredits);
        TextInputEditText etGrade       = sheetView.findViewById(R.id.etBacklogGrade);
        AutoCompleteTextView dropStatus  = sheetView.findViewById(R.id.dropBacklogStatus);
        MaterialButton btnAdd           = sheetView.findViewById(R.id.btnAddBacklogConfirm);

        // Pre-fill
        etCourseName.setText(course.getCourseName());
        etSemester.setText(String.valueOf(course.getSemesterNumber()));
        etCredits.setText(String.valueOf(course.getCredits()));
        if (course.getGradeObtained() > 0) {
            etGrade.setText(String.format("%.1f", course.getGradeObtained()));
        }
        String[] statuses = {"Failed", "Dropped", "Cleared"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, statuses);
        dropStatus.setAdapter(statusAdapter);
        dropStatus.setText(capitalize(course.getStatus().name()), false);

        btnAdd.setText("Save changes");
        btnAdd.setOnClickListener(v -> {
            String name = etCourseName.getText() != null ? etCourseName.getText().toString().trim() : "";
            String semStr = etSemester.getText() != null ? etSemester.getText().toString().trim() : "";
            String crStr  = etCredits.getText()  != null ? etCredits.getText().toString().trim()  : "";
            String grStr  = etGrade.getText()    != null ? etGrade.getText().toString().trim()    : "";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(semStr) || TextUtils.isEmpty(crStr)) return;

            course.setCourseName(name);
            course.setSemesterNumber(Integer.parseInt(semStr));
            course.setCredits(Integer.parseInt(crStr));
            course.setGradeObtained(TextUtils.isEmpty(grStr) ? 0 : Double.parseDouble(grStr));
            String st = dropStatus.getText().toString().toUpperCase();
            course.setStatus(BacklogCourse.Status.valueOf(st));

            saveData();
            refresh();
            sheet.dismiss();
        });

        sheet.show();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    // ------------------------------------------------------------------
    // RecyclerView Adapter (inner class)
    // ------------------------------------------------------------------
    static class BacklogAdapter extends RecyclerView.Adapter<BacklogAdapter.VH> {

        interface Listener {
            void onMarkCleared(BacklogCourse course, int position);
            void onEdit(BacklogCourse course, int position);
        }

        private List<BacklogCourse> data;
        private final Listener listener;

        BacklogAdapter(List<BacklogCourse> data, Listener listener) {
            this.data = data;
            this.listener = listener;
        }

        void updateData(List<BacklogCourse> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_backlog_course, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            BacklogCourse c = data.get(position);
            h.tvCourseName.setText(c.getCourseName());
            h.tvCourseMeta.setText(c.getMetaString());
            h.tvGradeLabel.setText(c.getGradeLabel());
            h.tvAttemptCount.setText("Attempt " + c.getAttemptCount());

            // Chip style
            switch (c.getStatus()) {
                case FAILED:
                    h.tvStatusChip.setText("Failed");
                    h.tvStatusChip.setBackgroundResource(R.drawable.bg_chip_red);
                    h.tvStatusChip.setTextColor(h.itemView.getContext().getColor(R.color.backlog_red_text));
                    break;
                case DROPPED:
                    h.tvStatusChip.setText("Dropped");
                    h.tvStatusChip.setBackgroundResource(R.drawable.bg_chip_amber);
                    h.tvStatusChip.setTextColor(h.itemView.getContext().getColor(R.color.backlog_amber_text));
                    break;
                case CLEARED:
                    h.tvStatusChip.setText("Cleared");
                    h.tvStatusChip.setBackgroundResource(R.drawable.bg_chip_green);
                    h.tvStatusChip.setTextColor(h.itemView.getContext().getColor(R.color.backlog_green_text));
                    break;
            }

            // Hide "Mark cleared" if already cleared
            h.btnMarkCleared.setVisibility(
                    c.getStatus() == BacklogCourse.Status.CLEARED ? View.GONE : View.VISIBLE);

            h.btnMarkCleared.setOnClickListener(v -> listener.onMarkCleared(c, h.getAdapterPosition()));
            h.btnEditBacklog.setOnClickListener(v -> listener.onEdit(c, h.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCourseName, tvCourseMeta, tvStatusChip, tvGradeLabel, tvAttemptCount;
            MaterialButton btnMarkCleared, btnEditBacklog;

            VH(View v) {
                super(v);
                tvCourseName   = v.findViewById(R.id.tvCourseName);
                tvCourseMeta   = v.findViewById(R.id.tvCourseMeta);
                tvStatusChip   = v.findViewById(R.id.tvStatusChip);
                tvGradeLabel   = v.findViewById(R.id.tvGradeLabel);
                tvAttemptCount = v.findViewById(R.id.tvAttemptCount);
                btnMarkCleared = v.findViewById(R.id.btnMarkCleared);
                btnEditBacklog = v.findViewById(R.id.btnEditBacklog);
            }
        }
    }
}
