package com.example.mlseriesdemonstrator.object;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.mlseriesdemonstrator.R;
import com.example.mlseriesdemonstrator.helpers.MLVideoHelperActivity;
import com.example.mlseriesdemonstrator.helpers.vision.drowsiness.FaceDrowsiness;
import com.example.mlseriesdemonstrator.helpers.vision.drowsiness.FaceDrowsinessDetectorProcessor;
import com.example.mlseriesdemonstrator.helpers.vision.VisionBaseProcessor;

import java.util.Locale;

public class DriverDrowsinessDetectionActivity extends MLVideoHelperActivity {
    private TextView perclosTextView, leftEyeTextView, rightEyeTextView, drowsyTextView;
    private static final long UPDATE_INTERVAL_MS = 500; // 1000ms = 1 segund
    private FaceDrowsinessDetectorProcessor processor;
    private Handler handler = new Handler(Looper.getMainLooper());
    private FaceDrowsiness faceDrowsiness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_video_helper_new); // Certifique-se de que este é o layout correto.

        leftEyeTextView = findViewById(R.id.leftEyeTextView);
        rightEyeTextView = findViewById(R.id.rightEyeTextView);
        perclosTextView = findViewById(R.id.perclosTextView);
        drowsyTextView = findViewById(R.id.drowsyTextView);

        processor = new FaceDrowsinessDetectorProcessor(graphicOverlay);
        faceDrowsiness = FaceDrowsiness.getInstance();
        faceDrowsiness.initialize(getApplicationContext());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // TextView 1 - Probabilidades de Abertura dos Olhos
                float leftEyeProb = faceDrowsiness.getLeftEyeOpenProb();
                float rightEyeProb = faceDrowsiness.getRightEyeOpenProb();
                leftEyeTextView.setText(String.format(Locale.US, "Left Eye: %.2f, Right Eye: %.2f", leftEyeProb, rightEyeProb));

                // TextView 2 - Média de PERCLOS e FPS
                double averagePerclos = faceDrowsiness.getAveragePerclos();
                double fps = faceDrowsiness.getFps();
                rightEyeTextView.setText(String.format(Locale.US, "Avg Perclos: %.2f%%, FPS: %.2f", averagePerclos, fps));

                // TextView 3 - Contagem Total de Medições e Medições com Olhos Fechados
                int totalMeasurements = faceDrowsiness.getTotalMeasurements();
                int closedEyeMeasurements = faceDrowsiness.getClosedEyeMeasurements();
                perclosTextView.setText(String.format(Locale.US, "Total: %d, Closed: %d", totalMeasurements, closedEyeMeasurements));

                // TextView 4 - Estado de Sonolência e Contagem de Frames com Olhos Fechados
                int closedEyeFrameCount = faceDrowsiness.getClosedEyeFrameCount();
                boolean drowsy = faceDrowsiness.getDrowsy();
                String drowsyStatus = drowsy ? "Detected" : "Not Detected";
                drowsyTextView.setText(String.format(Locale.US, "Drowsiness: %s, Closed Frames: %d", drowsyStatus, closedEyeFrameCount));
                if (drowsy) {
                    drowsyTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else {
                    drowsyTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                }


                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        }, UPDATE_INTERVAL_MS);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Limpe o handler ao destruir a activity
        faceDrowsiness.releaseMediaPlayer();
    }

    @Override
    protected VisionBaseProcessor setProcessor() {
        return new FaceDrowsinessDetectorProcessor(graphicOverlay);
    }
}