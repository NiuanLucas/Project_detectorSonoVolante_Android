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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_video_helper_new); // Certifique-se de que este é o layout correto.

        leftEyeTextView = findViewById(R.id.leftEyeTextView);
        rightEyeTextView = findViewById(R.id.rightEyeTextView);
        perclosTextView = findViewById(R.id.perclosTextView);
        drowsyTextView = findViewById(R.id.drowsyTextView);

        processor = new FaceDrowsinessDetectorProcessor(graphicOverlay);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FaceDrowsiness faceDrowsiness = FaceDrowsiness.getInstance();

                float leftEyeProb = faceDrowsiness.getLeftEyeOpenProb();
                float rightEyeProb = faceDrowsiness.getRightEyeOpenProb();
                float perclos = (float) faceDrowsiness.getPerclos();

                Log.d("FaceDrowsiness", "Left eye open probability: " + leftEyeProb + "Right eye open probability: " + rightEyeProb);

                leftEyeTextView.setText(String.format(Locale.US, "Left Eye Open Probability: %.2f", leftEyeProb));
                rightEyeTextView.setText(String.format(Locale.US, "Right Eye Open Probability: %.2f", rightEyeProb));
                perclosTextView.setText(String.format(Locale.US, "Perclos Probability: %.2f", perclos));

                boolean drowsy = faceDrowsiness.getDrowsy();
                if (drowsy) {
                    // Sono detectado - Texto em vermelho
                    drowsyTextView.setText("Sono detectado");
                    drowsyTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else {
                    // Sono não detectado - Texto em verde
                    drowsyTextView.setText("Sono não detectado");
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
    }

    @Override
    protected VisionBaseProcessor setProcessor() {
        return new FaceDrowsinessDetectorProcessor(graphicOverlay);
    }
}