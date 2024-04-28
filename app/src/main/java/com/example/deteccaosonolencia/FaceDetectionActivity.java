package com.example.deteccaosonolencia;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class FaceDetectionActivity extends AppCompatActivity {

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;
    private boolean usingFrontCamera = true;
    private static final Float EYE_CLOSED_THRESHOLD = 0.18F;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);
        previewView = findViewById(R.id.facePreviewView);
        graphicOverlay = findViewById(R.id.overlay);
        Button switchCameraButton = findViewById(R.id.btn_switch_camera);

        switchCameraButton.setOnClickListener(v -> {
            usingFrontCamera = !usingFrontCamera;
            startCamera();
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 101);
        }
    }

    // # METODO DEPRECIADO - NÃO É UMA BOA PRATICA CALCULAR EAR EM ML KIT
//    private double calculateEAR(Face face, int eyeType) {
//        FaceLandmark leftCorner = eyeType == FaceLandmark.LEFT_EYE ?
//                face.getLandmark(FaceLandmark.LEFT_EYE_LEFT_CORNER) :
//                face.getLandmark(FaceLandmark.RIGHT_EYE_LEFT_CORNER);
//        FaceLandmark rightCorner = eyeType == FaceLandmark.LEFT_EYE ?
//                face.getLandmark(FaceLandmark.LEFT_EYE_RIGHT_CORNER) :
//                face.getLandmark(FaceLandmark.RIGHT_EYE_RIGHT_CORNER);
//
//        if (leftCorner == null || rightCorner == null) return 0.0;
//
//        double horizontalDistance = distance(leftCorner, rightCorner);
//        double verticalDistance = horizontalDistance / 4.0; // Estimativa
//
//        return verticalDistance / horizontalDistance;
//    }


    private double distance(FaceLandmark point1, FaceLandmark point2) {
        return Math.sqrt(Math.pow(point1.getPosition().x - point2.getPosition().x, 2) + Math.pow(point1.getPosition().y - point2.getPosition().y, 2));
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                CameraSelector cameraSelector = usingFrontCamera ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Definindo as opções do detector de faces
                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();


                FaceDetector detector = FaceDetection.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        detector.process(image)
                                .addOnSuccessListener(faces -> {
                                    List<Rect> rects = new ArrayList<>();
                                    for (Face face : faces) {
                                        Float leftEyeOpenProb = face.getLeftEyeOpenProbability();
                                        Float rightEyeOpenProb = face.getRightEyeOpenProbability();

                                        // Preparando strings para exibição das probabilidades e outros atributos
                                        String leftEyeProbText = leftEyeOpenProb != null ? leftEyeOpenProb.toString() : "Indisponível";
                                        String rightEyeProbText = rightEyeOpenProb != null ? rightEyeOpenProb.toString() : "Indisponível";
                                        String smileProbText = face.getSmilingProbability() != null ? face.getSmilingProbability().toString() : "Indisponível";

                                        rects.add(face.getBoundingBox());
                                        Rect bounds = face.getBoundingBox();
                                        String boundingBox = bounds.toString();

                                        float eulerY = face.getHeadEulerAngleY();  // Rotação ao redor do eixo Y (inclinado)
                                        float eulerZ = face.getHeadEulerAngleZ();  // Rotação ao redor do eixo Z (girado)

                                        // Verifique se as probabilidades são null antes de tentar usá-las
                                        boolean isLeftEyeOpen = leftEyeOpenProb != null && leftEyeOpenProb > EYE_CLOSED_THRESHOLD;
                                        boolean isRightEyeOpen = rightEyeOpenProb != null && rightEyeOpenProb > EYE_CLOSED_THRESHOLD;

                                        runOnUiThread(() -> {
                                            TextView textView = findViewById(R.id.tv_ear_perclos);

                                            String text = String.format(Locale.US,
                                                    "\n Prob. Olho Esquerdo Aberto: %s," +
                                                            "\n Prob. Olho Direito Aberto: %s," +
                                                            "\n Bool. Olho Esquerdo Aberto: %b," +
                                                            "\n Bool. Olho Direito Aberto: %b," +
                                                            "\n Probabilidade de Sorriso: %s," +
                                                            "\n Caixa Delimitadora: %s," +
                                                            "\n Rotação Y: %.2f," +
                                                            "\n Rotação Z: %.2f",
                                                    leftEyeProbText, rightEyeProbText, isLeftEyeOpen, isRightEyeOpen, smileProbText, boundingBox, eulerY, eulerZ);

                                            textView.setText(text);
                                        });
                                    }
                                    graphicOverlay.setRects(rects, imageProxy.getWidth(), imageProxy.getHeight());
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> {
                                    imageProxy.close();
                                    // Tratar erro
                                });

                    }
                });





                cameraProvider.bindToLifecycle((LifecycleOwner) FaceDetectionActivity.this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                // Tratar erro
            }
        }, ContextCompat.getMainExecutor(this));
    }



}
