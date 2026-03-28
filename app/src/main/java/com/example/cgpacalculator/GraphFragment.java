package com.example.cgpacalculator;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class GraphFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        StorageHelper storage = new StorageHelper(requireContext());
        List<Semester> semesters = storage.loadSemesters();

        // Summary stats
        double totalWeighted = 0;
        int totalCredits = 0;
        for (Semester s : semesters) {
            for (Course c : s.getCourses()) {
                totalWeighted += (double) c.getGradePoints() * c.getCredits();
                totalCredits  += c.getCredits();
            }
        }
        double cgpa = totalCredits > 0 ? totalWeighted / totalCredits : 0;

        ((TextView) view.findViewById(R.id.tvGraphCgpa)).setText(
                totalCredits > 0 ? String.format("%.2f", cgpa) : "–");
        ((TextView) view.findViewById(R.id.tvGraphCredits)).setText(String.valueOf(totalCredits));
        ((TextView) view.findViewById(R.id.tvGraphSems)).setText(String.valueOf(semesters.size()));

        setupCgpaChart(view, semesters);
        setupSgpaChart(view, semesters);
    }

    /** CGPA vs Semester — cumulative weighted average after each semester */
    private void setupCgpaChart(View view, List<Semester> semesters) {
        LineChart chart = view.findViewById(R.id.cgpaLineChart);

        if (semesters.isEmpty() || allEmpty(semesters)) {
            chart.setNoDataText("Add courses to see the CGPA trend");
            chart.setNoDataTextColor(Color.GRAY);
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        String[] labels = new String[semesters.size()];
        double cumWeighted = 0;
        int cumCredits = 0;

        for (int i = 0; i < semesters.size(); i++) {
            for (Course c : semesters.get(i).getCourses()) {
                cumWeighted += (double) c.getGradePoints() * c.getCredits();
                cumCredits  += c.getCredits();
            }
            double val = cumCredits > 0 ? cumWeighted / cumCredits : 0;
            entries.add(new Entry(i, (float) val));
            labels[i] = semesters.get(i).getName().replace("Semester ", "Sem ");
        }

        LineDataSet ds = buildLineDataSet(entries, "CGPA", "#6750A4", "#6750A4");
        applyChartStyle(chart, new LineData(ds), labels, "CGPA");
    }

    /** SGPA vs Semester — each semester's individual GPA */
    private void setupSgpaChart(View view, List<Semester> semesters) {
        LineChart chart = view.findViewById(R.id.sgpaLineChart);

        if (semesters.isEmpty() || allEmpty(semesters)) {
            chart.setNoDataText("Add courses to see SGPA per semester");
            chart.setNoDataTextColor(Color.GRAY);
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        String[] labels = new String[semesters.size()];

        for (int i = 0; i < semesters.size(); i++) {
            entries.add(new Entry(i, (float) semesters.get(i).getSgpa()));
            labels[i] = semesters.get(i).getName().replace("Semester ", "Sem ");
        }

        LineDataSet ds = buildLineDataSet(entries, "SGPA", "#E91E63", "#E91E63");
        applyChartStyle(chart, new LineData(ds), labels, "SGPA");
    }

    private LineDataSet buildLineDataSet(ArrayList<Entry> entries, String label,
                                         String lineColor, String fillColor) {
        LineDataSet ds = new LineDataSet(entries, label);
        ds.setColor(Color.parseColor(lineColor));
        ds.setCircleColor(Color.parseColor(lineColor));
        ds.setLineWidth(2.5f);
        ds.setCircleRadius(5f);
        ds.setDrawCircleHole(true);
        ds.setCircleHoleRadius(2.5f);
        ds.setCircleHoleColor(Color.WHITE);
        ds.setValueTextSize(10f);
        ds.setDrawFilled(true);
        ds.setFillColor(Color.parseColor(fillColor));
        ds.setFillAlpha(35);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return value == 0 ? "" : String.format("%.1f", value);
            }
        });
        return ds;
    }

    private void applyChartStyle(LineChart chart, LineData data, String[] labels, String yLabel) {
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.animateX(900);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setTextSize(11f);
        x.setLabelCount(labels.length);
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setLabelRotationAngle(labels.length > 4 ? -30f : 0f);

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(10.5f);
        left.setLabelCount(6, true);
        left.setDrawGridLines(true);
        left.setGridColor(Color.parseColor("#15000000"));
        left.setTextSize(10f);

        chart.getAxisRight().setEnabled(false);
        chart.invalidate();
    }

    private boolean allEmpty(List<Semester> semesters) {
        for (Semester s : semesters) if (!s.getCourses().isEmpty()) return false;
        return true;
    }
}
