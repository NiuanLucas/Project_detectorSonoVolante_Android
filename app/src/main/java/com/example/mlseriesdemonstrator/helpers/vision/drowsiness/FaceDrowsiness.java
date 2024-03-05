package com.example.mlseriesdemonstrator.helpers.vision.drowsiness;

import com.example.mlseriesdemonstrator.R;
import com.google.mlkit.vision.face.Face;
import android.content.Context;
import android.media.MediaPlayer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Classe que utiliza a detecção de faces do ML Kit para calcular o PERCLOS
 * e determinar a sonolência com base na probabilidade de abertura dos olhos.
 */
public class FaceDrowsiness {
    private static FaceDrowsiness instance;
    private MediaPlayer mediaPlayer;
    private Context context;  // Contexto para criar o MediaPlayer
    private static final int MAX_PERCLOS_VALUES = 60;
    private static final double DROWSINESS_THRESHOLD = 30.0;
    private Queue<Double> perclosValues = new LinkedList<>();
    private double averagePerclos = 0.0;
    private int totalMeasurements = 0;
    private int closedEyeMeasurements = 0;
    private int frameCount = 0; // Contador de frames processados
    private int closedEyeFrameCount = 0; // Contador de frames com olhos fechados
    private long startTime = System.currentTimeMillis(); // Tempo de início para calcular o FPS
    private double fps = 0.0; // FPS da câmera
    private float leftEyeOpenProbability = -1, rightEyeOpenProbability = -1;


    // Método estático público para obter a instância
    // Construtor privado para o padrão Singleton
    public FaceDrowsiness() { }

    // Método estático público para obter a instância
    public static synchronized FaceDrowsiness getInstance() {
        if (instance == null) {
            instance = new FaceDrowsiness();
        }
        return instance;
    }

    // Método para inicializar o contexto e o MediaPlayer
    public void initialize(Context context) {
        this.context = context;
        this.mediaPlayer = MediaPlayer.create(context, R.raw.alarme);
    }

    /**
     * Avalia se a face detectada está sonolenta com base na probabilidade de abertura dos olhos.
     *
     * @param face A face detectada.
     * @return true se considerado sonolento, false caso contrário.
     */
    public boolean isDrowsy(Face face) {
        double leftEyeOpenProb = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.0;
        double rightEyeOpenProb = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.0;

        leftEyeOpenProbability = (float) leftEyeOpenProb;
        rightEyeOpenProbability = (float) rightEyeOpenProb;

        boolean isEyeClosed = (leftEyeOpenProb < 0.5) && (rightEyeOpenProb < 0.5);
        double currentPerclos = isEyeClosed ? 1.0 : 0.0;

        if (isEyeClosed) {
            closedEyeMeasurements++;
            closedEyeFrameCount++;
        }

        perclosValues.add(currentPerclos);
        if (perclosValues.size() > MAX_PERCLOS_VALUES) {
            if (perclosValues.poll() == 1.0) {
                closedEyeMeasurements--;
            }
        }

        totalMeasurements++;
        frameCount++;

        // Cálculo do FPS e atualização da lógica de PERCLOS baseada em tempo
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        if (elapsedTime >= 1000) { // Verifica se passou 1 segundo
            fps = (double) frameCount / (elapsedTime / 1000.0);
            frameCount = 0;
            startTime = currentTime;

            if (totalMeasurements > 0) {
                averagePerclos = ((double) closedEyeMeasurements / totalMeasurements) * 100;
            }

            if (totalMeasurements >= MAX_PERCLOS_VALUES) {
                totalMeasurements = 0;
                closedEyeMeasurements = 0;
                perclosValues.clear();
            }
        }

        if (averagePerclos > DROWSINESS_THRESHOLD) {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            return true;
        }
        return false;
    }

    // Método para liberar o MediaPlayer
    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    // Métodos para manipular e acessar perclos

    public boolean getDrowsy() { return (averagePerclos > DROWSINESS_THRESHOLD); }
    public double getPerclos() {
        return averagePerclos;
    }
    public float getLeftEyeOpenProb() {return leftEyeOpenProbability;}
    public float getRightEyeOpenProb() {return rightEyeOpenProbability;}
    // Método getter para a média de PERCLOS
    public double getAveragePerclos() {
        return averagePerclos;
    }

    // Método getter para o FPS (Frames Por Segundo)
    public double getFps() {
        return fps;
    }

    // Método getter para o total de medições realizadas
    public int getTotalMeasurements() {
        return totalMeasurements;
    }

    // Método getter para o total de medições com olhos fechados
    public int getClosedEyeMeasurements() {
        return closedEyeMeasurements;
    }

    // Método getter para o contador de frames processados
    public int getFrameCount() {
        return frameCount;
    }

    // Método getter para o contador de frames com olhos fechados
    public int getClosedEyeFrameCount() {
        return closedEyeFrameCount;
    }
}
