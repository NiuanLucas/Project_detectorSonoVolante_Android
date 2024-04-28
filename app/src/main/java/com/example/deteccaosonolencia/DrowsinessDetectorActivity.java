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


// Esta é a classe principal que estende AppCompatActivity
public class DrowsinessDetectorActivity extends AppCompatActivity {

    // Definindo constantes e variáveis
    private static final int REQUEST_CAMERA_PERMISSION = 101; // Código de solicitação de permissão da câmera
    private PreviewView cameraPreviewView; // Visualização da câmera
    private TextView probabilitiesTextView, blinkCountTextView, eyesClosedTimeTextView; // TextViews para exibir informações
    private boolean usingFrontCamera = true; // Variável para verificar se a câmera frontal está sendo usada
    private long blinkCount = 0; // Contador de piscadas
    private long lastBlinkTimestamp = 0; // Timestamp da última piscada
    private long eyesClosedTime = 0; // Tempo total que os olhos estiveram fechados
    private long startTime = 0; // Hora de início para cálculos de tempo
    private long elapsed = 0; // Tempo decorrido
    private static final Float EYE_CLOSED_THRESHOLD = 0.5F; // Limiar para considerar os olhos como fechados
    private static final int MEASURED_TIME_INTERVAL_MS = 1000, BLINK_THRESHOLD_MS  = 250, RESET_TIME_INTERVAL_MS = 30000; // Constantes de tempo
    private DrawingView drawingView; // View para desenhar os pontos dos olhos
    private boolean eyesAreClosed; // Variável para verificar se os olhos estão fechados
    private long eyesClosedStartTime; // Hora de início do fechamento dos olhos
    private long lastEyesClosedUpdateTime; // Última atualização do tempo de fechamento dos olhos
    private MediaPlayer mediaPlayer; // Player de mídia para tocar o alarme
    private final double DROWSINESS_THRESHOLD = 20; // Limiar de sonolência

    // Método onCreate chamado quando a atividade é criada
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drowsiness_detector); // Definindo o layout da atividade

        // Inicializando as views
        cameraPreviewView = findViewById(R.id.cameraPreviewView);
        drawingView = findViewById(R.id.drawingView);
        probabilitiesTextView = findViewById(R.id.probabilitiesTextView);
        mediaPlayer = MediaPlayer.create(this, R.raw.alarme); // Inicializando o player de mídia com o arquivo de alarme

        // Verificando a permissão da câmera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(); // Iniciar a câmera se a permissão for concedida
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION); // Solicitar permissão se não for concedida
        }
    }

    // Handler e Runnable para o timer
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            // Resetar os contadores e atualizar a UI conforme necessário
            resetTimers();
            timerHandler.postDelayed(this, RESET_TIME_INTERVAL_MS); // Reagendar após 60 segundos
        }
    };

    // Método para iniciar a câmera
    private void startCamera() {
        // Obter uma instância do ProcessCameraProvider. Esta é uma classe segura para threads que pode ser usada para vincular o ciclo de vida de câmeras a um ciclo de vida do proprietário
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                // Obter uma instância do ProcessCameraProvider
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();

                // Construir um objeto Preview
                Preview preview = new Preview.Builder().build();

                // Selecionar a câmera frontal ou traseira
                CameraSelector cameraSelector = usingFrontCamera ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;

                // Vincular o SurfaceProvider do PreviewView ao preview
                preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

                // Construir um objeto ImageAnalysis e definir a resolução alvo e a estratégia de contrapressão
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Definir o analisador para o objeto ImageAnalysis
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImage);

                // Desvincular todas as câmeras do ciclo de vida
                cameraProvider.unbindAll();

                // Vincular o ciclo de vida ao uso de uma câmera com o CameraSelector e os casos de uso
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                // Obter o tempo atual para iniciar a contagem de tempo
                startTime = SystemClock.elapsedRealtime();

                // Iniciar o timer para resetar a cada 60 segundos
                timerHandler.postDelayed(timerRunnable, RESET_TIME_INTERVAL_MS);
            } catch (Exception e) {
                // Imprimir a pilha de rastreamento se houver uma exceção
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this)); // Fornecer um executor para quando a inicialização for concluída
    }


    // Variável para armazenar o tempo da última frame
    private long lastFrameTime = 0;

    // Método para processar a imagem
    private void processImage(@NonNull ImageProxy image) {
        // Obter o tempo atual
        long currentTime = SystemClock.elapsedRealtime();

        // Se for a primeira frame, inicialize lastFrameTime com o tempo atual
        if (lastFrameTime == 0) {
            lastFrameTime = currentTime;
        }

        // Obter a largura e a altura da imagem
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // Definir as dimensões da imagem e se a câmera frontal está sendo usada na DrawingView
        drawingView.setImageDimensions(imageWidth, imageHeight);
        drawingView.setIsUsingFrontCamera(usingFrontCamera);

        // Iniciar a detecção de rosto
        @NonNull Task<List<Face>> result =
                FaceDetection.getClient(new FaceDetectorOptions.Builder()
                                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // Definir o modo de marco para detectar todos os marcos faciais
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Definir o modo de classificação para classificar todos os aspectos faciais
                                .build())
                        .process(InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees())) // Processar a imagem
                        .addOnSuccessListener(faces -> { // Ouvinte para o sucesso da detecção de rosto
                            // Lista para armazenar os marcos dos olhos
                            List<DrawingView.PointColor> eyeLandmarks = new ArrayList<>();
                            // Variável para verificar se os olhos estão fechados nesta frame
                            boolean eyesClosedInThisFrame = false;

                            // Variáveis para armazenar a probabilidade dos olhos estarem abertos
                            Float leftEyeOpenProb = null;
                            Float rightEyeOpenProb = null;

                            // Loop através de todas as faces detectadas
                            for (Face face : faces) {
                                // Obter a probabilidade dos olhos estarem abertos
                                leftEyeOpenProb = face.getLeftEyeOpenProbability();
                                rightEyeOpenProb = face.getRightEyeOpenProbability();

                                // Se a probabilidade de ambos os olhos estarem abertos for menor que o limiar, considere os olhos como fechados
                                if (leftEyeOpenProb != null && rightEyeOpenProb != null) {
                                    if (leftEyeOpenProb < EYE_CLOSED_THRESHOLD && rightEyeOpenProb < EYE_CLOSED_THRESHOLD) {
                                        // Se os olhos estiverem fechados, atualize as variáveis de tempo
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

                            // Se os olhos não estiverem fechados nesta frame e estiverem fechados anteriormente, atualize a variável eyesAreClosed
                            if (!eyesClosedInThisFrame && eyesAreClosed) {
                                eyesAreClosed = false;
                            }

                            // Atualizar os marcos dos olhos na DrawingView
                            drawingView.setEyeLandmarks(eyeLandmarks);

                            // Atualizar o TextView com as probabilidades
                            updateProbabilitiesTextView(leftEyeOpenProb, rightEyeOpenProb);

                            // Fechar a imagem
                            image.close();
                        })
                        .addOnFailureListener(e -> image.close()); // Ouvinte para falha na detecção de rosto, fechar a imagem

        // Atualizar o tempo da última frame
        lastFrameTime = currentTime;
    }


    // Método para obter a cor com base na probabilidade dos olhos estarem abertos
    private int getColorForEye(Float eyeOpenProb) {
        if (eyeOpenProb == null) return Color.GRAY; // Cor padrão se a probabilidade não estiver disponível
        return eyeOpenProb > EYE_CLOSED_THRESHOLD ? Color.rgb(95, 220, 0) : Color.RED; // Retorna verde se os olhos estiverem abertos, caso contrário, retorna vermelho
    }

    // Método para resetar os timers
    private void resetTimers() {
        // Resetar os contadores
        blinkCount = 0;
        eyesClosedTime = 0;
        startTime = SystemClock.elapsedRealtime(); // Resetar o tempo de início
    }

    // Método para atualizar o TextView com as probabilidades
    private void updateProbabilitiesTextView(Float leftEyeOpenProb, Float rightEyeOpenProb) {
        runOnUiThread(() -> {
            long currentTime = SystemClock.elapsedRealtime();
            elapsed = currentTime - startTime; // Calcular o tempo decorrido

            long closedTimeInSeconds = eyesClosedTime / 1000; // Converter o tempo de fechamento dos olhos para segundos
            long totalTimeInSeconds = elapsed / 1000; // Converter o tempo total para segundos
            double closedEyePercentage = totalTimeInSeconds > 0 ? (double) closedTimeInSeconds / totalTimeInSeconds * 100 : 0; // Calcular a porcentagem de tempo com os olhos fechados
            boolean drowsiness = closedEyePercentage > DROWSINESS_THRESHOLD; // Verificar se a porcentagem excede o limiar de sonolência

            // Formatar o texto a ser exibido no TextView
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
            probabilitiesTextView.setText(text); // Atualizar o TextView

            // Tocar o alarme se a sonolência for detectada
            if (drowsiness) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            }
        });
    }

    // Método chamado quando a atividade é destruída
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable); // Limpar o handler
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Liberar o player de mídia
            mediaPlayer = null;
        }
    }

    // Método chamado quando as permissões são concedidas ou negadas
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera(); // Iniciar a câmera se a permissão for concedida
        }
    }
}
