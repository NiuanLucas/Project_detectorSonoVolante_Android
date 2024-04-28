package com.example.deteccaosonolencia;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {
    private Paint paint;
    private List<PointColor> eyeLandmarks;
    private boolean isUsingFrontCamera = false;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(10f);
        eyeLandmarks = new ArrayList<>();
    }
    public void setIsUsingFrontCamera(boolean isUsingFrontCamera) {
        this.isUsingFrontCamera = isUsingFrontCamera;
    }

    public void setImageDimensions(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public void setEyeLandmarks(List<PointColor> eyeLandmarks) {
        this.eyeLandmarks = eyeLandmarks;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float widthScaleFactor = (float) getWidth() / (float) imageWidth;
        float heightScaleFactor = (float) getHeight() / (float) imageHeight;

        for (PointColor pointColor : eyeLandmarks) {
            float offsetX = 240f;
            float x = (isUsingFrontCamera ? getWidth() - pointColor.point.x * widthScaleFactor : pointColor.point.x * widthScaleFactor) + offsetX;
            float y = pointColor.point.y * heightScaleFactor;
            paint.setColor(pointColor.color);
            canvas.drawCircle(x, y, 20, paint);
        }
    }

    public static class PointColor {
        Point point;
        int color;

        public PointColor(Point point, int color) {
            this.point = point;
            this.color = color;
        }
    }
}
