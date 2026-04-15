package com.example.cgpacalculator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.List;

public class SemesterPagerAdapter extends RecyclerView.Adapter<SemesterPagerAdapter.PageViewHolder> {
    public interface OnCourseRemovedListener {
        void onCourseRemoved(int semesterIndex, int courseIndex);
    }

    public interface OnCourseEditListener {
        void onCourseEdit(int semesterIndex, int courseIndex);
    }

    private final Context context;
    private final List<Semester> semesterList;
    private final OnCourseRemovedListener listener;

    private final OnCourseEditListener editListener;

    public SemesterPagerAdapter(Context context,
                                List<Semester> semesterList,
                                OnCourseRemovedListener removeListener,
                                OnCourseEditListener editListener) {
        this.context        = context;
        this.semesterList   = semesterList;
        this.listener       = removeListener;
        this.editListener   = editListener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.page_semester, parent, false);
        return new PageViewHolder(view);
    }

    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(semesterList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return semesterList.size(); // how many pages are there?
    }

    class PageViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvSgpa, tvEmpty;
        LinearLayout layoutRows;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvPageSemName);
            tvSgpa = itemView.findViewById(R.id.tvPageSemgpa);
            tvEmpty = itemView.findViewById(R.id.tvPageEmpty);
            layoutRows = itemView.findViewById(R.id.layoutPageCourseRows);
        }

        void bind(Semester sem, int semIndex) {
            tvIndex.setText(String.valueOf(sem.getIndex()));
            tvSgpa.setText(
                    sem.getTotalCredits() > 0
                            ? String.format("SPI: %.2f", sem.getSGPA())
                            : "No courses");
            List<Course> courses = sem.getCourses();

            if (courses.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                layoutRows.removeAllViews();
                return;
            }
            tvEmpty.setVisibility(View.GONE);
            layoutRows.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(context);
            for (int i = 0; i < courses.size(); i++) {
                Course course = courses.get(i);
                View row = inflater.inflate(
                        R.layout.item_course_row, layoutRows, false);

                ((TextView) row.findViewById(R.id.tvCourseName))
                        .setText(course.getName());
                ((TextView) row.findViewById(R.id.tvCourseCredits))
                        .setText(course.getCredits() + " cr");
                ((Chip) row.findViewById(R.id.chipCourseGrade))
                        .setText(course.getGrade());

                final int courseIndex = i;
                row.findViewById(R.id.btnRemoveCourse)
                        .setOnClickListener(v ->
                                listener.onCourseRemoved(semIndex, courseIndex));

                row.findViewById(R.id.btnEditCourse)
                        .setOnClickListener(v -> editListener.onCourseEdit(semIndex, courseIndex));
                layoutRows.addView(row);
            }
        }
    }
}
