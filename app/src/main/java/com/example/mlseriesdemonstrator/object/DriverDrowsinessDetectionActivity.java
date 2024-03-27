// Declaração do pacote.
package com.example.mlseriesdemonstrator.object;

// Importações necessárias para a classe.
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

// Definição da classe, que herda de MLVideoHelperActivity.
public class DriverDrowsinessDetectionActivity extends MLVideoHelperActivity {
    // Declaração de TextViews para exibir informações na tela.
    private TextView perclosTextView, leftEyeTextView, rightEyeTextView, drowsyTextView;
    
    // Intervalo de atualização das informações na tela.
    private static final long UPDATE_INTERVAL_MS = 500;
    
    // Processador para detecção de sonolência facial.
    private FaceDrowsinessDetectorProcessor processor;
    
    // Handler para executar tarefas em um intervalo definido.
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Instância da classe FaceDrowsiness para acessar dados de sonolência.
    private FaceDrowsiness faceDrowsiness;

    // Método onCreate chamado quando a atividade é criada.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Define o layout da atividade.
        // setContentView(R.layout.activity_video_helper_new);

        // Inicializa os TextViews.
        leftEyeTextView = findViewById(R.id.leftEyeTextView);
        rightEyeTextView = findViewById(R.id.rightEyeTextView);
        perclosTextView = findViewById(R.id.perclosTextView);
        drowsyTextView = findViewById(R.id.drowsyTextView);

        // Inicializa o processador de detecção de sonolência.
        processor = new FaceDrowsinessDetectorProcessor(graphicOverlay);
        
        // Obtém a instância única de FaceDrowsiness e inicializa-a.
        faceDrowsiness = FaceDrowsiness.getInstance();
        faceDrowsiness.initialize(getApplicationContext());

        // Define uma tarefa para ser executada repetidamente com o intervalo definido.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Atualiza o TextView com a probabilidade dos olhos estarem abertos.
                float leftEyeProb = faceDrowsiness.getLeftEyeOpenProb();
                float rightEyeProb = faceDrowsiness.getRightEyeOpenProb();
                leftEyeTextView.setText(String.format(Locale.US, "Left Eye: %.2f, Right Eye: %.2f", leftEyeProb, rightEyeProb));

                // Atualiza o TextView com o PERCLOS médio e o FPS.
                double averagePerclos = faceDrowsiness.getAveragePerclos();
                double fps = faceDrowsiness.getFps();
                rightEyeTextView.setText(String.format(Locale.US, "Avg Perclos: %.2f%%, FPS: %.2f", averagePerclos, fps));

                // Atualiza o TextView com o total de medições e as medições com olhos fechados.
                int totalMeasurements = faceDrowsiness.getTotalMeasurements();
                int closedEyeMeasurements = faceDrowsiness.getClosedEyeMeasurements();
                perclosTextView.setText(String.format(Locale.US, "Total: %d, Closed: %d", totalMeasurements, closedEyeMeasurements));

                // Atualiza o TextView com o estado de sonolência e a contagem de frames com olhos fechados.
                int closedEyeFrameCount = faceDrowsiness.getClosedEyeFrameCount();
                boolean drowsy = faceDrowsiness.getDrowsy();
                String drowsyStatus = drowsy ? "Detected" : "Not Detected";
                drowsyTextView.setText(String.format(Locale.US, "Drowsiness: %s, Closed Frames: %d", drowsyStatus, closedEyeFrameCount));
                if (drowsy) {
                    drowsyTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                } else {
                    drowsyTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.green));
                }

                // Reagenda a execução da tarefa após o intervalo definido.
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        }, UPDATE_INTERVAL_MS);
    }

    // Método chamado quando a atividade está sendo destruída.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove todas as callbacks e mensagens do handler para evitar vazamentos de memória.
        handler.removeCallbacksAndMessages(null);
        // Libera recursos associados ao mediaPlayer dentro do faceDrowsiness, se necessário.
        faceDrowsiness.releaseMediaPlayer();
    }

    // Método abstrato herdado que deve ser implementado para definir o processador de visão computacional utilizado.
    @Override
    protected VisionBaseProcessor setProcessor() {
        // Retorna uma nova instância do processador de detecção de sonolência facial.
        return new FaceDrowsinessDetectorProcessor(graphicOverlay);
    }
}

