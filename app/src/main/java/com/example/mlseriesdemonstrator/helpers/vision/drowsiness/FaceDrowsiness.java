// Declaração do pacote.
package com.example.mlseriesdemonstrator.helpers.vision.drowsiness;

// Importações necessárias.
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
    // Singleton instance da classe.
    private static FaceDrowsiness instance;

    // MediaPlayer para tocar um som de alerta.
    private MediaPlayer mediaPlayer;

    // Contexto da aplicação para criar o MediaPlayer.
    private Context context;

    // Constante para o máximo de valores PERCLOS a serem armazenados.
    private static final int MAX_PERCLOS_VALUES = 60;

    // Limiar para determinar a sonolência.
    private static final double DROWSINESS_THRESHOLD = 30.0;

    // Fila para armazenar os valores de PERCLOS.
    private Queue<Double> perclosValues = new LinkedList<>();

    // Média de PERCLOS calculada.
    private double averagePerclos = 0.0;

    // Contadores para medições e medições com olhos fechados.
    private int totalMeasurements = 0;
    private int closedEyeMeasurements = 0;

    // Contadores para frames processados e frames com olhos fechados.
    private int frameCount = 0;
    private int closedEyeFrameCount = 0;

    // Tempo de início para calcular o FPS.
    private long startTime = System.currentTimeMillis();

    // FPS (Frames Por Segundo) da câmera.
    private double fps = 0.0;

    // Probabilidades de abertura dos olhos esquerdo e direito.
    private float leftEyeOpenProbability = -1, rightEyeOpenProbability = -1;

    // Construtor privado para o padrão Singleton.
    private FaceDrowsiness() { }

    // Método estático público para obter a instância única da classe.
    public static synchronized FaceDrowsiness getInstance() {
        if (instance == null) {
            instance = new FaceDrowsiness();
        }
        return instance;
    }

    // Método para inicializar o contexto e o MediaPlayer.
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
        // Obtém as probabilidades de abertura dos olhos esquerdo e direito.
        double leftEyeOpenProb = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.0;
        double rightEyeOpenProb = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.0;

        // Atualiza as probabilidades na classe.
        leftEyeOpenProbability = (float) leftEyeOpenProb;
        rightEyeOpenProbability = (float) rightEyeOpenProb;

        // Determina se os olhos estão fechados.
        boolean isEyeClosed = (leftEyeOpenProb < 0.5) && (rightEyeOpenProb < 0.5);
        double currentPerclos = isEyeClosed ? 1.0 : 0.0;

        // Atualiza contadores e fila de PERCLOS.
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

        // Calcula o FPS e atualiza a média de PERCLOS.
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        if (elapsedTime >= 1000) {
            fps = (double) frameCount / (elapsedTime / 1000.0);
            frameCount = 0;
            startTime = currentTime;

            if (totalMeasurements > 0) {
                           // Calcula a média de PERCLOS.
            averagePerclos = ((double) closedEyeMeasurements / totalMeasurements) * 100;
        }

        // Reinicia a contagem se o número de medições atingir o máximo definido.
        if (totalMeasurements >= MAX_PERCLOS_VALUES) {
            totalMeasurements = 0;
            closedEyeMeasurements = 0;
            perclosValues.clear();
        }
    }

    // Se a média de PERCLOS for maior que o limiar de sonolência, toca o MediaPlayer e retorna verdadeiro.
    if (averagePerclos > DROWSINESS_THRESHOLD) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        return true;
    }
    return false;
}

// Método para liberar o MediaPlayer, chamado quando o recurso não é mais necessário.
public void releaseMediaPlayer() {
    if (mediaPlayer != null) {
        mediaPlayer.release();
        mediaPlayer = null;
    }
}

// Métodos getters para acessar os atributos privados da classe.

// Retorna o estado de sonolência baseado na média de PERCLOS.
public boolean getDrowsy() { return (averagePerclos > DROWSINESS_THRESHOLD); }

// Retorna o valor atual de PERCLOS.
public double getPerclos() {
    return averagePerclos;
}

// Retorna a probabilidade do olho esquerdo estar aberto.
public float getLeftEyeOpenProb() { return leftEyeOpenProbability; }

// Retorna a probabilidade do olho direito estar aberto.
public float getRightEyeOpenProb() { return rightEyeOpenProbability; }

// Retorna a média de PERCLOS calculada.
public double getAveragePerclos() {
    return averagePerclos;
}

// Retorna o FPS (Frames Por Segundo) calculado.
public double getFps() {
    return fps;
}

// Retorna o número total de medições realizadas.
public int getTotalMeasurements() {
    return totalMeasurements;
}

// Retorna o número de medições em que os olhos foram detectados como fechados.
public int getClosedEyeMeasurements() {
    return closedEyeMeasurements;
}

// Retorna o número total de frames processados.
public int getFrameCount() {
    return frameCount;
}

// Retorna o número de frames em que os olhos foram detectados como fechados.
public int getClosedEyeFrameCount() {
    return closedEyeFrameCount;
}
}

