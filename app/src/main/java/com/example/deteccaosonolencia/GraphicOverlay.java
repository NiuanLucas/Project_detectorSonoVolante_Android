package com.example.deteccaosonolencia;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;
import com.google.mlkit.vision.face.Face;

public class GraphicOverlay extends View {
    private final Paint paint;
    private List<Rect> rects;
    private int imageWidth;
    private int imageHeight;
    private boolean isFrontFacing = false;

    public void setIsFrontFacing(boolean isFrontFacing) {
        this.isFrontFacing = isFrontFacing;
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
    }
    public void setRects(List<Rect> rects, int imageWidth, int imageHeight) {
        this.rects = rects;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        invalidate();  // Reinvoca o onDraw
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rects != null) {
            for (Rect rect : rects) {
                float scaleX = (float) getWidth() / imageWidth;
                float scaleY = (float) getHeight() / imageHeight;

                float left = rect.left * scaleX;
                float top = rect.top * scaleY;
                float right = rect.right * scaleX;
                float bottom = rect.bottom * scaleY;

                // Ajustar para a câmera frontal, se necessário
                if (isFrontFacing) {
                    left = getWidth() - left;
                    right = getWidth() - right;
                }

                canvas.drawRect(left, top, right, bottom, paint);
            }
        }
    }

}