package com.example.deteccaosonolencia;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Size;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DrowsinessDetectorActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private PreviewView cameraPreviewView;
    private TextView probabilitiesTextView, blinkCountTextView, eyesClosedTimeTextView;
    private boolean usingFrontCamera = true;
    private long blinkCount = 0;
    private long lastBlinkTimestamp = 0;
    private long eyesClosedTime = 0;
    private long startTime = 0;
    private long elapsed = 0;
    private static final Float EYE_CLOSED_THRESHOLD = 0.5F;
    private static final int MEASURED_TIME_INTERVAL_MS = 1000, BLINK_THRESHOLD_MS  = 250, RESET_TIME_INTERVAL_MS = 30000;
    private DrawingView drawingView;
    private boolean eyesAreClosed;
    private long eyesClosedStartTime;
    private long lastEyesClosedUpdateTime;
    private MediaPlayer mediaPlayer;
    private final double DROWSINESS_THRESHOLD = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drowsiness_detector);

        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        drawingView = findViewById(R.id.drawingView);
        probabilitiesTextView = findViewById(R.id.probabilitiesTextView);
        mediaPlayer = MediaPlayer.create(this, R.raw.alarme);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            // Resetar os contadores e atualizar a UI conforme necessário
            resetTimers();
            timerHandler.postDelayed(this, RESET_TIME_INTERVAL_MS); // Reagendar após 60 segundos
        }
    };




    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = usingFrontCamera ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImage);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                startTime = SystemClock.elapsedRealtime();  // Iniciar a contagem de tempo
                timerHandler.postDelayed(timerRunnable, RESET_TIME_INTERVAL_MS); // Iniciar o timer para reset a cada 60 segundos
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private long lastFrameTime = 0;
    private void processImage(@NonNull ImageProxy image) {
        long currentTime = SystemClock.elapsedRealtime();
        if (lastFrameTime == 0) {
            lastFrameTime = currentTime; // Inicialize lastFrameTime se for a primeira frame
        }

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        drawingView.setImageDimensions(imageWidth, imageHeight);
        drawingView.setIsUsingFrontCamera(usingFrontCamera);

        @NonNull Task<List<Face>> result =
                FaceDetection.getClient(new FaceDetectorOptions.Builder()
                                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                .build())
                        .process(InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()))
                        .addOnSuccessListener(faces -> {
                            List<DrawingView.PointColor> eyeLandmarks = new ArrayList<>();
                            boolean eyesClosedInThisFrame = false;

                            Float leftEyeOpenProb = null;
                            Float rightEyeOpenProb = null;
                            for (Face face : faces) {
                                leftEyeOpenProb = face.getLeftEyeOpenProbability();
                                rightEyeOpenProb = face.getRightEyeOpenProbability();

                                if (leftEyeOpenProb != null && rightEyeOpenProb != null) {
                                    if (leftEyeOpenProb < EYE_CLOSED_THRESHOLD && rightEyeOpenProb < EYE_CLOSED_THRESHOLD) {
                                        if (!eyesAreClosed) {
                                            eyesAreClosed = true;
                                            eyesClosedStartTime = currentTime;
                                        } else if (currentTime - eyesClosedStartTime >= 1000) {
                                            eyesClosedTime += currentTime - lastEyesClosedUpdateTime;
                                        }
                                        eyesClosedInThisFrame = true;
                                        lastEyesClosedUpdateTime = currentTime;
                                    } else {
                                        eyesAreClosed = false;
                                    }

                                    // Lógica para contagem de piscadas
                                    if (eyesAreClosed && currentTime - lastBlinkTimestamp > BLINK_THRESHOLD_MS) {
                                        blinkCount++;
                                        lastBlinkTimestamp = currentTime;
                                    }

                                    // Definindo a cor com base na probabilidade dos olhos estarem abertos
                                    int leftEyeColor = getColorForEye(leftEyeOpenProb);
                                    int rightEyeColor = getColorForEye(rightEyeOpenProb);

                                    // Adicionando os pontos dos olhos à lista com as cores correspondentes
                                    for (FaceLandmark landmark : face.getAllLandmarks()) {
                                        if (landmark.getLandmarkType() == FaceLandmark.LEFT_EYE || landmark.getLandmarkType() == FaceLandmark.RIGHT_EYE) {
                                            int color = landmark.getLandmarkType() == FaceLandmark.LEFT_EYE ? leftEyeColor : rightEyeColor;
                                            eyeLandmarks.add(new DrawingView.PointColor(new Point((int) landmark.getPosition().x, (int) landmark.getPosition().y), color));
                                        }
                                    }
                                }
                            }

                            if (!eyesClosedInThisFrame && eyesAreClosed) {
                                eyesAreClosed = false;
                            }

                            drawingView.setEyeLandmarks(eyeLandmarks);
                            updateProbabilitiesTextView(leftEyeOpenProb, rightEyeOpenProb);  // Atualiza a UI
                            image.close();
                        })
                        .addOnFailureListener(e -> image.close());
        lastFrameTime = currentTime;
    }


    private int getColorForEye(Float eyeOpenProb) {
        if (eyeOpenProb == null) return Color.GRAY; // Cor padrão se a probabilidade não estiver disponível
        return eyeOpenProb > EYE_CLOSED_THRESHOLD ? Color.rgb(95, 220, 0) : Color.RED;
    }

    private void resetTimers() {

        // Resetar os contadores
        blinkCount = 0;
        eyesClosedTime = 0;
        startTime = SystemClock.elapsedRealtime();
    }


    private void updateProbabilitiesTextView(Float leftEyeOpenProb, Float rightEyeOpenProb) {
        runOnUiThread(() -> {
            long currentTime = SystemClock.elapsedRealtime();
            elapsed = currentTime - startTime;

            long closedTimeInSeconds = eyesClosedTime / 1000;
            long totalTimeInSeconds = elapsed / 1000;
            double closedEyePercentage = totalTimeInSeconds > 0 ? (double) closedTimeInSeconds / totalTimeInSeconds * 100 : 0;
            boolean drowsiness = closedEyePercentage > DROWSINESS_THRESHOLD;

            String text = String.format(Locale.US,
                    "Prob. Olho Esquerdo Aberto: %s\n" +
                            "Prob. Olho Direito Aberto: %s\n" +
                            "Piscadas: %d\n" +
                            "Tempo com olhos fechados: %d segundos\n" +
                            "Tempo total: %d segundos\n" +
                            "Porcentagem com olhos fechados: %.2f%%\n" +
                            "Status de sonolencia: %b",
                    leftEyeOpenProb != null ? String.format(Locale.US, "%.2f", leftEyeOpenProb) : "Indisponível",
                    rightEyeOpenProb != null ? String.format(Locale.US, "%.2f", rightEyeOpenProb) : "Indisponível",
                    blinkCount,
                    closedTimeInSeconds,
                    totalTimeInSeconds,
                    closedEyePercentage,
                    drowsiness);
            probabilitiesTextView.setText(text);

            if (drowsiness) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            }

        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable); // Limpar o handler
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}
