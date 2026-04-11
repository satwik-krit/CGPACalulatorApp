package com.example.cgpacalculator.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.cgpacalculator.R;

/**
 * Custom donut/ring view showing current CGPA vs target CGPA on a 10-point scale.
 * - Grey arc = full scale (0–10)
 * - Primary color arc = current CGPA progress
 * - Secondary color arc = gap to target (dashed feel via alpha)
 */
public class CgpaRingView extends View {

    private Paint trackPaint;
    private Paint currentPaint;
    private Paint targetPaint;
    private Paint textPaint;
    private Paint labelPaint;

    private RectF oval = new RectF();

    private double currentCGPA = 0.0;
    private double targetCGPA = 9.0;

    private static final float STROKE_WIDTH_DP = 14f;
    private static final float START_ANGLE = 135f;
    private static final float SWEEP_TOTAL = 270f;

    public CgpaRingView(Context context) {
        super(context);
        init(context);
    }

    public CgpaRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CgpaRingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        float strokePx = STROKE_WIDTH_DP * density;

        // Track (background ring)
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokePx);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(0x1A6750A4); // primary at ~10% alpha

        // Current CGPA arc
        currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeWidth(strokePx);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        // Will be set to colorPrimary at draw time

        // Target gap arc
        targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        targetPaint.setStyle(Paint.Style.STROKE);
        targetPaint.setStrokeWidth(strokePx * 0.5f);
        targetPaint.setStrokeCap(Paint.Cap.BUTT);
        targetPaint.setAlpha(90);

        // Main CGPA number
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(density * 32f);
        textPaint.setFakeBoldText(true);

        // "current CGPA" sub-label
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(density * 12f);
    }

    public void setCGPAValues(double current, double target) {
        this.currentCGPA = Math.min(current, 10.0);
        this.targetCGPA = Math.min(target, 10.0);
        invalidate();
    }

    public void setCurrentCGPA(double current) {
        this.currentCGPA = Math.min(current, 10.0);
        invalidate();
    }

    public void setTargetCGPA(double target) {
        this.targetCGPA = Math.min(target, 10.0);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float strokePx = STROKE_WIDTH_DP * getResources().getDisplayMetrics().density;
        float padding = strokePx + 4;

        oval.set(padding, padding, w - padding, h - padding);

        // Resolve theme colors at draw time (handles dark mode)
        int colorPrimary;
        int colorOnSurface;
        int colorOnSurfaceVariant;
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
            colorPrimary = tv.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
            colorOnSurface = tv.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, tv, true);
            colorOnSurfaceVariant = tv.data;
        } catch (Exception e) {
            colorPrimary = 0xFF6750A4;
            colorOnSurface = 0xFF1C1B1F;
            colorOnSurfaceVariant = 0xFF49454F;
        }

        trackPaint.setColor(colorPrimary);
        trackPaint.setAlpha(30);
        currentPaint.setColor(colorPrimary);
        targetPaint.setColor(colorPrimary);
        targetPaint.setAlpha(80);
        textPaint.setColor(colorOnSurface);
        labelPaint.setColor(colorOnSurfaceVariant);

        // Draw background track
        canvas.drawArc(oval, START_ANGLE, SWEEP_TOTAL, false, trackPaint);

        // Draw current CGPA arc
        if (currentCGPA > 0) {
            float currentSweep = (float) (currentCGPA / 10.0 * SWEEP_TOTAL);
            canvas.drawArc(oval, START_ANGLE, currentSweep, false, currentPaint);
        }

        // Draw target marker (thin arc from current to target)
        if (targetCGPA > currentCGPA) {
            float currentSweep = (float) (currentCGPA / 10.0 * SWEEP_TOTAL);
            float targetSweep  = (float) (targetCGPA  / 10.0 * SWEEP_TOTAL);
            canvas.drawArc(oval, START_ANGLE + currentSweep, targetSweep - currentSweep, false, targetPaint);
        }

        // Center text: CGPA number
        float cx = w / 2f;
        float cy = h / 2f;
        String cgpaText = currentCGPA == 0 ? "--" : String.format("%.2f", currentCGPA);
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(cgpaText, cx, textY - labelPaint.getTextSize(), textPaint);

        // Sub-label
        canvas.drawText("current CGPA", cx, textY + 8, labelPaint);
    }
}
