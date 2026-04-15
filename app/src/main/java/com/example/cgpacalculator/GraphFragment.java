package com.example.cgpacalculator;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraphFragment extends Fragment {

    private LineChart chartCgpa;
    private LineChart chartSgpa;
    private TextView tvCurrentCgpa;
    private TextView tvLatestSgpa;
    private TextView tvBestSgpa;
    private TextView tvWorstSgpa;
    private TextView tvTrend;
    private LinearLayout layoutEmptyState;
    private View cardCgpaChart;
    private View cardSgpaChart;
    private View cardStats;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
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
        chartCgpa       = view.findViewById(R.id.chartCgpa);
        chartSgpa       = view.findViewById(R.id.chartSgpa);
        tvCurrentCgpa   = view.findViewById(R.id.tvCurrentCgpa);
        tvLatestSgpa    = view.findViewById(R.id.tvLatestSgpa);
        tvBestSgpa      = view.findViewById(R.id.tvBestSgpa);
        tvWorstSgpa     = view.findViewById(R.id.tvWorstSgpa);
        tvTrend         = view.findViewById(R.id.tvTrend);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        cardCgpaChart   = view.findViewById(R.id.cardCgpaChart);
        cardSgpaChart   = view.findViewById(R.id.cardSgpaChart);
        cardStats       = view.findViewById(R.id.cardStats);

        loadAndDisplayData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh whenever the user comes back to this tab
        loadAndDisplayData();
    }

    private void loadAndDisplayData() {
        StorageHelper storage = StorageHelper.getInstance(requireContext());
        List<Semester> semesters = storage.loadSemesters();

        // Filter to only semesters that have at least one course
        List<Semester> activeSemesters = new ArrayList<>();
        for (Semester s : semesters) {
            if (s.getCourses() != null && !s.getCourses().isEmpty()) {
                activeSemesters.add(s);
            }
        }

        if (activeSemesters.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            cardCgpaChart.setVisibility(View.GONE);
            cardSgpaChart.setVisibility(View.GONE);
            cardStats.setVisibility(View.GONE);
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        cardCgpaChart.setVisibility(View.VISIBLE);
        cardSgpaChart.setVisibility(View.VISIBLE);
        cardStats.setVisibility(View.VISIBLE);

        // Build data arrays
        List<Entry> cgpaEntries = new ArrayList<>();
        List<Entry> sgpaEntries = new ArrayList<>();
        String[] semLabels = new String[activeSemesters.size()];

        double runningCgpa = 0;
        double bestSgpa = Double.MIN_VALUE;
        double worstSgpa = Double.MAX_VALUE;

        for (int i = 0; i < activeSemesters.size(); i++) {
            Semester sem = activeSemesters.get(i);
            double sgpa = sem.getSGPA();
            runningCgpa += sgpa;
            double cgpa = runningCgpa / (i + 1);

            sgpaEntries.add(new Entry(i, (float) sgpa));
            cgpaEntries.add(new Entry(i, (float) cgpa));
            semLabels[i] = "Sem " + (sem.getIndex() + 1);

            if (sgpa > bestSgpa) bestSgpa = sgpa;
            if (sgpa < worstSgpa) worstSgpa = sgpa;
        }

        double finalCgpa = runningCgpa / activeSemesters.size();
        double latestSgpa = activeSemesters.get(activeSemesters.size() - 1).getSGPA();

        // Trend: compare last two SGPAs
        String trend = "—";
        if (activeSemesters.size() >= 2) {
            double prev = activeSemesters.get(activeSemesters.size() - 2).getSGPA();
            double curr = activeSemesters.get(activeSemesters.size() - 1).getSGPA();
            double diff = curr - prev;
            if (diff > 0.01)       trend = "▲ Up";
            else if (diff < -0.01) trend = "▼ Down";
            else                   trend = "→ Stable";
        }

        // Update summary TextViews
        tvCurrentCgpa.setText(String.format(Locale.US, "%.2f", finalCgpa));
        tvLatestSgpa.setText(String.format(Locale.US, "%.2f", latestSgpa));
        tvBestSgpa.setText(String.format(Locale.US, "%.2f", bestSgpa));
        tvWorstSgpa.setText(String.format(Locale.US, "%.2f", worstSgpa));
        tvTrend.setText(trend);

        // Resolve Material You colors from theme
        int colorSecondaryOnContainer = resolveAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer);
        int colorTertiaryOnContainer  = resolveAttrColor(com.google.android.material.R.attr.colorOnTertiaryContainer);
        int colorSecondary            = resolveAttrColor(com.google.android.material.R.attr.colorSecondary);
        int colorTertiary             = resolveAttrColor(com.google.android.material.R.attr.colorTertiary);

        setupLineChart(chartCgpa, cgpaEntries, semLabels, "CGPA",
                colorSecondary, colorSecondaryOnContainer);
        setupLineChart(chartSgpa, sgpaEntries, semLabels, "SGPA",
                colorTertiary, colorTertiaryOnContainer);
    }

    private void setupLineChart(LineChart chart,
                                List<Entry> entries,
                                String[] xLabels,
                                String label,
                                int lineColor,
                                int textColor) {

        // Dataset
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(lineColor);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleHoleColor(Color.TRANSPARENT);
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(40);
        dataSet.setFillColor(lineColor);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(textColor);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.2f", value);
            }
        });

        LineData lineData = new LineData(dataSet);

        // Chart global
        chart.setData(lineData);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setExtraBottomOffset(8f);
        chart.setExtraTopOffset(12f);
        chart.setExtraLeftOffset(4f);
        chart.setExtraRightOffset(12f);
        chart.getLegend().setEnabled(false);

        Description desc = new Description();
        desc.setText("");
        chart.setDescription(desc);

        // X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setTextColor(textColor);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(xLabels.length);

        // Left Y-axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(10.5f);
        leftAxis.setGranularity(1f);
        leftAxis.setGridColor(Color.argb(30, 128, 128, 128));
        leftAxis.setDrawAxisLine(false);
        leftAxis.setLabelCount(6, false);

        // Disable right axis
        chart.getAxisRight().setEnabled(false);

        chart.animateX(800);
        chart.invalidate();
    }

    /** Resolve a color from the current Material You theme attribute. */
    private int resolveAttrColor(int attrRes) {
        TypedArray ta = requireContext().obtainStyledAttributes(new int[]{ attrRes });
        int color = ta.getColor(0, Color.GRAY);
        ta.recycle();
        return color;
    }
}
