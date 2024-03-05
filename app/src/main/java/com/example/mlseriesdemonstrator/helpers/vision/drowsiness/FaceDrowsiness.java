package com.example.mlseriesdemonstrator.helpers.vision.drowsiness;

import com.google.mlkit.vision.face.Face;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Classe que utiliza a detecção de faces do ML Kit para calcular o PERCLOS
 * e determinar a sonolência com base na probabilidade de abertura dos olhos.
 */
public class FaceDrowsiness {
    private static FaceDrowsiness instance;
    private static final float DROWSINESS_THRESHOLD = 0.5f; // Limiar para considerar um olho como fechado
    private static final long MEASUREMENT_INTERVAL_MS = 500; // Intervalo entre medições em milissegundos
    private static final long DROWSINESS_PERIOD_MS = 60000; // Período para calcular o PERCLOS (1 minuto)
    private final Queue<Long> closedEyeTimestamps = new LinkedList<>();
    private long lastMeasurementTime = 0;
    private double perclos;
    private boolean isDrowsy;
    private float leftEyeOpenProbability = -1, rightEyeOpenProbability = -1;


    // Método estático público para obter a instância
    public static synchronized FaceDrowsiness getInstance() {
        if (instance == null) {
            instance = new FaceDrowsiness();
        }
        return instance;
    }

    /**
     * Avalia se a face detectada está sonolenta com base na probabilidade de abertura dos olhos.
     *
     * @param face A face detectada.
     * @return true se considerado sonolento, false caso contrário.
     */
    public boolean isDrowsy(Face face) {
        long currentTime = System.currentTimeMillis();

        // Verifica se é hora de uma nova medição com base no intervalo definido
        if (currentTime - lastMeasurementTime < MEASUREMENT_INTERVAL_MS) {
            // Ainda não é hora da próxima medição
            return false;
        }
        lastMeasurementTime = currentTime;

        if (face.getLeftEyeOpenProbability() != null) {
            leftEyeOpenProbability = face.getLeftEyeOpenProbability();
        }

        if (face.getRightEyeOpenProbability() != null) {
            rightEyeOpenProbability = face.getRightEyeOpenProbability();
        }

        // Considera os olhos como fechados se ambas as probabilidades estiverem abaixo do limiar
        if (leftEyeOpenProbability < DROWSINESS_THRESHOLD && rightEyeOpenProbability < DROWSINESS_THRESHOLD) {
            closedEyeTimestamps.add(currentTime);
        }

        // Remove timestamps antigos que estão fora do período de cálculo do PERCLOS
        while (!closedEyeTimestamps.isEmpty() &&
                currentTime - closedEyeTimestamps.peek() > DROWSINESS_PERIOD_MS) {
            closedEyeTimestamps.poll();
        }

        // Calcula o PERCLOS como a proporção do tempo com os olhos fechados
        long periodLength = currentTime - (closedEyeTimestamps.isEmpty() ? currentTime : closedEyeTimestamps.peek());
        perclos = (double) closedEyeTimestamps.size() * MEASUREMENT_INTERVAL_MS / periodLength;

        // Determina sonolência se o PERCLOS exceder um limiar específico (e.g., 30% do tempo com olhos fechados)
        isDrowsy = perclos > 0.3;
        return isDrowsy;
    }

    // Métodos para manipular e acessar perclos

    public boolean getDrowsy() { return isDrowsy; }
    public double getPerclos() {
        return perclos;
    }
    public float getLeftEyeOpenProb() {return leftEyeOpenProbability;}
    public float getRightEyeOpenProb() {return rightEyeOpenProbability;}
}
