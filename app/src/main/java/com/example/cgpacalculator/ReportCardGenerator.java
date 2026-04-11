package com.example.cgpacalculator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportCardGenerator {

    private final Context context;

    public ReportCardGenerator(Context context) {
        this.context = context;
    }

    public void generateAndShare(Semester semester) {
        // step 1: inflate the report card layout
        View reportView = LayoutInflater.from(context)
                .inflate(R.layout.layout_report_card, null);

        // step 2: fill in the data
        fillReportData(reportView, semester);

        // step 3: measure and layout the view
        // we use a fixed width of 1080px (like a phone screen at high res)
        int width = 1080;
        reportView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        reportView.layout(0, 0,
                reportView.getMeasuredWidth(),
                reportView.getMeasuredHeight());

        // step 4: draw the view onto a bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                reportView.getMeasuredWidth(),
                reportView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        reportView.draw(canvas);

        // step 5: save bitmap to file
        File file = saveBitmap(bitmap, Integer.toString(semester.getIndex()));
        if (file == null) return;

        // step 6: share via Android share sheet
        shareFile(file);
    }

    private void fillReportData(View reportView, Semester semester) {
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date());

        ((TextView) reportView.findViewById(R.id.tvReportSemName))
                .setText(Integer.toString(semester.getIndex()));
        ((TextView) reportView.findViewById(R.id.tvReportDate))
                .setText("Generated on " + date);
        ((TextView) reportView.findViewById(R.id.tvReportSpi))
                .setText(String.format("%.2f", semester.getSGPA()));
        ((TextView) reportView.findViewById(R.id.tvReportCredits))
                .setText(String.valueOf(semester.getTotalCredits()));

        // add one row per course
        LinearLayout layoutRows =
                reportView.findViewById(R.id.layoutReportRows);
        LayoutInflater inflater = LayoutInflater.from(context);

        List<Course> courses = semester.getCourses();
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            View row = inflater.inflate(
                    R.layout.item_report_row, layoutRows, false);

            // alternate row background for readability
            if (i % 2 == 0) {
                row.setBackgroundColor(0xFFF3EDF7);
            }

            ((TextView) row.findViewById(R.id.tvReportCourseName))
                    .setText(course.getName());
            ((TextView) row.findViewById(R.id.tvReportCourseCredits))
                    .setText(String.valueOf(course.getCredits()));
            ((TextView) row.findViewById(R.id.tvReportCourseGrade))
                    .setText(course.getGrade());
            ((TextView) row.findViewById(R.id.tvReportCoursePoints))
                    .setText(String.valueOf((int) course.getGradePoints()));

            layoutRows.addView(row);
        }
    }

    private File saveBitmap(Bitmap bitmap, String semesterName) {
        try {
            // save to app's cache directory — no storage permission needed
            File cacheDir = new File(context.getCacheDir(), "reports");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            String fileName = "report_"
                    + semesterName.replaceAll("\\s+", "_")
                    + ".png";
            File file = new File(cacheDir, fileName);

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void shareFile(File file) {
        // FileProvider converts a file path to a content URI
        // that other apps are allowed to read
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, "Share Report Card"));
    }
}
