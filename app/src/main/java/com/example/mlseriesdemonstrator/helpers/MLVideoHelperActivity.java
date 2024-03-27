package com.example.mlseriesdemonstrator.helpers;

import static androidx.camera.view.PreviewView.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.example.mlseriesdemonstrator.R;
import com.example.mlseriesdemonstrator.helpers.vision.GraphicOverlay;
import com.example.mlseriesdemonstrator.helpers.vision.VisionBaseProcessor;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Classe abstrata que fornece a estrutura para atividades que utilizam processamento de vídeo e visão computacional.
public abstract class MLVideoHelperActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1001; // Código de solicitação para a permissão da câmera
    protected PreviewView previewView; // Visualização de pré-visualização da câmera
    protected GraphicOverlay graphicOverlay; // Overlay para desenho gráfico sobre a pré-visualização da câmera
    private TextView outputTextView; // TextView para exibir saídas, como resultados da detecção
    private ExtendedFloatingActionButton addFaceButton; // Botão para adicionar faces, funcionalidade específica não detalhada aqui
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture; // Futuro para obter o provedor de câmera
    private Executor executor = Executors.newSingleThreadExecutor(); // Executor para processamento em segundo plano
    private int lensFacing = CameraSelector.LENS_FACING_BACK; // Padrão para câmera traseira
    private VisionBaseProcessor processor; // Processador para análise de imagem
    private ImageAnalysis imageAnalysis; // Análise de imagem para processamento de frames

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_helper_new); // Define o layout da atividade

        // Configura o botão para alternar entre câmeras frontal e traseira
        FloatingActionButton switchCameraButton = findViewById(R.id.switchCameraButton);
        switchCameraButton.setOnClickListener(view -> switchCamera()); // Define o listener para o botão

        // Inicializa os componentes da UI
        previewView = findViewById(R.id.camera_source_preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        outputTextView = findViewById(R.id.output_text_view);
        addFaceButton = findViewById(R.id.button_add_face);

        // Obtém o futuro do provedor de câmera
        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());

        // Define o processador de visão, que será implementado nas subclasses
        processor = setProcessor();

        // Verifica a permissão da câmera e inicializa a fonte se concedida, ou solicita permissão
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            initSource(); // Inicializa a fonte da câmera
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpa o processador ao destruir a atividade para evitar vazamentos de memória
        if (processor != null) {
            processor.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Inicializa a fonte da câmera se a permissão for concedida
        if (requestCode == REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initSource();
        }
    }

    // Método para definir o texto de saída no TextView de output.
    protected void setOutputText(String text) {
        outputTextView.setText(text);
    }

    // Inicializa a fonte da câmera e vincula os casos de uso após a permissão ser concedida.
    private void initSource() {
        cameraProviderFuture.addListener(() -> {
            try {
                // Obtém o ProcessCameraProvider de forma assíncrona.
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Vincula a pré-visualização e a análise de imagem ao ciclo de vida da atividade.
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Trata exceções que podem ocorrer ao tentar obter o ProcessCameraProvider.
                Log.e("MLVideoHelperActivity", "Exception", e);
            }
        }, ContextCompat.getMainExecutor(getApplicationContext()));
    }

    // Vincula a pré-visualização da câmera e a análise de imagem ao ciclo de vida da atividade.
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Define a proporção da pré-visualização.
                .build();

        // Define o provedor de superfície para a pré-visualização.
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing) // Define a seleção da câmera (frontal ou traseira).
                .build();

        // Constrói e configura a análise de imagem.
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Define a proporção para análise de imagem.
                .build();

        // Configura o processador de imagem.
        setFaceDetector(lensFacing);

        // Vincula a pré-visualização e a análise de imagem ao ciclo de vida da atividade.
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    // Configura o detector de faces, ajustando as propriedades de acordo com a pré-visualização.
    private void setFaceDetector(int lensFacing) {
        previewView.getPreviewStreamState().observe(this, new Observer<StreamState>() {
            @Override
            public void onChanged(StreamState streamState) {
                if (streamState != StreamState.STREAMING) {
                    return;
                }

                View preview = previewView.getChildAt(0);
                float width = preview.getWidth() * preview.getScaleX();
                float height = preview.getHeight() * preview.getScaleY();

                // Ajusta as dimensões se a orientação estiver em paisagem.
                float rotation = preview.getDisplay().getRotation();
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                    float temp = width;
                    width = height;
                    height = temp;
                }

                // Define o analisador na análise de imagem com as dimensões e orientação da câmera.
                imageAnalysis.setAnalyzer(executor, createFaceDetector((int) width, (int) height, lensFacing));
                previewView.getPreviewStreamState().removeObserver(this);
            }
        });
    }

    // Cria um analisador de imagem para detectar faces, convertendo a imagem para Bitmap.
    @OptIn(markerClass = ExperimentalGetImage.class)
    private ImageAnalysis.Analyzer createFaceDetector(int width, int height, int lensFacing) {
        graphicOverlay.setPreviewProperties(width, height, lensFacing);
        return imageProxy -> {
            if (imageProxy.getImage() == null) {
                imageProxy.close();
                return;
            }
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            // Detecta faces na imagem, convertendo-a para Bitmap antes do processamento.
            processor.detectInImage(imageProxy, toBitmap(imageProxy.getImage()), rotationDegrees);
            imageProxy.close();
        };
    }

    // Converte a imagem do formato YUV para Bitmap.
    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    // Retorna a orientação atual da câmera (frontal ou traseira).
    protected int getLensFacing() {
        return lensFacing;
    }

    // Alterna entre a câmera frontal e traseira.
    public void switchCamera() {
        // Se a câmera atual for a frontal, muda para a traseira, e vice-versa.
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT) ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;

        // Desvincula e vincula novamente os casos de uso para aplicar a nova configuração da câmera.
        bindCameraUseCases();
    }

    // Método para vincular novamente os casos de uso da câmera após a mudança da câmera.
    private void bindCameraUseCases() {
        // Adiciona um listener para quando o ProcessCameraProvider estiver disponível.
        cameraProviderFuture.addListener(() -> {
            try {
                // Obtém o ProcessCameraProvider e desvincula todos os casos de uso atuais.
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // Vincula os casos de uso da câmera novamente com a nova configuração da câmera.
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Trata exceções potenciais ao obter o ProcessCameraProvider.
                Log.e("MLVideoHelperActivity", "Erro ao vincular casos de uso da câmera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Método abstrato que deve ser implementado nas subclasses para definir o processador de visão.
    protected abstract VisionBaseProcessor setProcessor();

    // Método para tornar visível o botão de adicionar face.
    public void makeAddFaceVisible() {
        addFaceButton.setVisibility(View.VISIBLE);
    }

    // Método placeholder para quando o botão de adicionar face é clicado.
    // Deve ser implementado nas subclasses se necessário.
    public void onAddFaceClicked(View view) {
        // Implementação específica nas subclasses.
    }
}
